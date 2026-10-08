package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import com.example.api.OmnisGeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object PdfTextExtractor {
    private const val TAG = "PdfTextExtractor"
    private const val MAX_RENDER_PAGES = 5
    private const val MAX_BITMAP_DIMENSION = 1024f

    /**
     * Extrahuje text z PDF.
     * 1. Pokusí se o rychlý lokální parsing streamu (pro digitálně sázené PDF obsahující textové objekty BT...ET / Tj / TJ a FlateDecode dekompresi).
     * 2. Pokud lokální parsing nenajde dostatečný text (např. skenované PDF nebo vektorové křivky), vyrenderuje stránky přes standardní Android PdfRenderer
     *    na škálované bitmapy a provede optické rozpoznání (OCR) pomocí Gemini Vision API pro jednotlivé stránky.
     * 3. Pokud je soubor malý (<1.5MB), může využít i přímý Gemini Document Inline payload s automatickým fallbackem.
     */
    suspend fun extractText(
        context: Context,
        uri: Uri,
        fileName: String
    ): String = withContext(Dispatchers.IO) {
        // Krok 1: Zkusíme nejprve extrahovat čistý text přímo ze struktury PDF
        val localText = tryExtractLocalText(context, uri)
        if (!localText.isNullOrBlank() && localText.trim().length >= 40) {
            Log.d(TAG, "Local PDF stream extraction successful (${localText.length} chars).")
            return@withContext localText.trim()
        }

        // Krok 2: Pokud lokální stream text neobsahuje (nebo je skenovaný), zkusíme PdfRenderer + Page-by-page Vision OCR
        val renderedOcrText = tryRenderAndOcrPages(context, uri)
        if (!renderedOcrText.isNullOrBlank()) {
            Log.d(TAG, "PdfRenderer OCR extraction successful (${renderedOcrText.length} chars).")
            return@withContext renderedOcrText.trim()
        }

        // Krok 3: Fallback - pokud předchozí metody selhaly a soubor je menší než 2MB, zkusíme přímý Gemini Document API inlineData
        val directGeminiText = tryDirectGeminiExtraction(context, uri)
        if (!directGeminiText.isNullOrBlank()) {
            Log.d(TAG, "Direct Gemini document extraction successful.")
            return@withContext directGeminiText.trim()
        }

        // Pokud lokální stream obsahoval aspoň nějaký text (i kratší než 40 znaků), vrátíme ho
        if (!localText.isNullOrBlank()) {
            return@withContext localText.trim()
        }

        return@withContext ""
    }

    /**
     * Čte PDF stream a dekóduje textové sekvence ze standardních PDF textových bloků.
     * Podporuje jak nekomprimovaný text, tak dekompresi z FlateDecode streamů.
     */
    private fun tryExtractLocalText(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            if (bytes.size < 10) return null

            val sb = StringBuilder()

            // Pokus o extrakci z nekomprimovaných stringů v PDF syntaxi: (text) Tj / [(t)(e)(x)(t)] TJ
            val rawString = String(bytes, Charsets.ISO_8859_1)
            val extractedRaw = parsePdfTextBlocks(rawString)
            if (extractedRaw.isNotBlank() && extractedRaw.length > 50) {
                sb.append(extractedRaw)
            }

            // Hledání komprimovaných streamů (stream ... endstream s /FlateDecode)
            val streamRegex = Regex("""stream\r?\n([\s\S]*?)\r?\nendstream""", RegexOption.DOT_MATCHES_ALL)
            val matches = streamRegex.findAll(rawString)

            for (match in matches) {
                try {
                    val streamContent = match.groupValues[1]
                    val streamBytes = streamContent.toByteArray(Charsets.ISO_8859_1)
                    
                    // Zkusíme dekomprimovat zlib / FlateDecode
                    val inflater = java.util.zip.Inflater(false)
                    inflater.setInput(streamBytes)
                    val buffer = ByteArray(4096)
                    val outStream = ByteArrayOutputStream()
                    
                    try {
                        while (!inflater.finished() && !inflater.needsInput()) {
                            val count = inflater.inflate(buffer)
                            if (count == 0) break
                            outStream.write(buffer, 0, count)
                        }
                    } catch (e: Exception) {
                        // Zkusíme s nowrap=true pro raw deflate
                        inflater.reset()
                        val rawInflater = java.util.zip.Inflater(true)
                        rawInflater.setInput(streamBytes)
                        while (!rawInflater.finished() && !rawInflater.needsInput()) {
                            val count = rawInflater.inflate(buffer)
                            if (count == 0) break
                            outStream.write(buffer, 0, count)
                        }
                    }
                    
                    val decompressed = outStream.toByteArray()
                    if (decompressed.isNotEmpty()) {
                        val decompText = String(decompressed, Charsets.UTF_8)
                        val textFromStream = parsePdfTextBlocks(decompText)
                        if (textFromStream.isNotBlank()) {
                            if (sb.isNotEmpty()) sb.append("\n")
                            sb.append(textFromStream)
                        }
                    }
                } catch (ignored: Exception) {
                    // Ignorujeme vadné streamy (obrázky apod.)
                }
            }

            val result = sb.toString().trim()
            if (result.isNotEmpty()) result else null
        } catch (e: Exception) {
            Log.w(TAG, "Local PDF stream extraction error", e)
            null
        }
    }

    /**
     * Parsuje textové operátory z PDF textových bloků: (text) Tj a [(t)(e)(x)(t)] TJ
     */
    private fun parsePdfTextBlocks(pdfContent: String): String {
        val out = StringBuilder()
        
        // Regulární výraz pro bloky BT ... ET (Begin Text ... End Text)
        val btBlockRegex = Regex("""BT\s+([\s\S]*?)\s+ET""")
        val textBlocks = btBlockRegex.findAll(pdfContent)

        for (block in textBlocks) {
            val content = block.groupValues[1]
            
            // 1. Jednoduché stringy: (text) Tj nebo (text)' nebo (text)"
            val singleTjRegex = Regex("""\((.*?)\)\s*T[jJ'\"]""")
            for (m in singleTjRegex.findAll(content)) {
                val clean = cleanPdfEscapes(m.groupValues[1])
                if (clean.isNotBlank()) out.append(clean).append(" ")
            }

            // 2. Diskrétní array pole: [(t) -12 (e) 4 (x) (t)] TJ
            val tjArrayRegex = Regex("""\[(.*?)\]\s*TJ""")
            for (m in tjArrayRegex.findAll(content)) {
                val arrayContent = m.groupValues[1]
                val innerStrings = Regex("""\((.*?)\)""").findAll(arrayContent)
                val lineSb = StringBuilder()
                for (item in innerStrings) {
                    lineSb.append(cleanPdfEscapes(item.groupValues[1]))
                }
                if (lineSb.isNotBlank()) out.append(lineSb.toString()).append(" ")
            }
            out.append("\n")
        }

        // Čištění vícenásobných mezer a prázdných řádků
        return out.toString()
            .replace(Regex("""[ \t]+"""), " ")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private fun cleanPdfEscapes(str: String): String {
        return str
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "")
            .replace("\\t", "\t")
    }

    /**
     * Využije systémový Android PdfRenderer k renderování stránek PDF a provede OCR přes Gemini Vision API.
     */
    private suspend fun tryRenderAndOcrPages(context: Context, uri: Uri): String? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var tempFile: File? = null

        return try {
            // PdfRenderer vyžaduje seekable file descriptor, proto zkopírujeme do dočasného souboru
            val contentResolver = context.contentResolver
            tempFile = File(context.cacheDir, "temp_pdf_render_${System.currentTimeMillis()}.pdf")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            if (pageCount == 0) return null

            val pagesToProcess = min(pageCount, MAX_RENDER_PAGES)
            val sb = StringBuilder()

            for (i in 0 until pagesToProcess) {
                val page = renderer.openPage(i)
                try {
                    val width = page.width
                    val height = page.height
                    
                    // Přizpůsobení rozměrů pro zachování detailů při optimální paměti
                    val scale = min(MAX_BITMAP_DIMENSION / width, MAX_BITMAP_DIMENSION / height)
                    val renderWidth = (width * scale).toInt().coerceAtLeast(1)
                    val renderHeight = (height * scale).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                    // Bílý podklad pro transparentní PDF stránky
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val byteStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteStream)
                    val base64Page = Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP)
                    bitmap.recycle()

                    // OCR jedné stránky přes Gemini Vision
                    val pageText = OmnisGeminiClient.extractTextFromImage(base64Page)
                    if (!pageText.isNullOrBlank()) {
                        if (pagesToProcess > 1) {
                            sb.append("[Strana ${i + 1}/$pageCount]:\n")
                        }
                        sb.append(pageText.trim()).append("\n\n")
                    }
                } finally {
                    page.close()
                }
            }

            val finalOutput = sb.toString().trim()
            if (finalOutput.isNotEmpty()) finalOutput else null
        } catch (e: Exception) {
            Log.e(TAG, "PdfRenderer OCR processing failed", e)
            null
        } finally {
            try { renderer?.close() } catch (ignored: Exception) {}
            try { pfd?.close() } catch (ignored: Exception) {}
            tempFile?.delete()
        }
    }

    /**
     * Přímé odeslání PDF souboru do Gemini Document API (vhodné pro menší PDF do 1.5MB)
     */
    private suspend fun tryDirectGeminiExtraction(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            
            // Limit velikosti pro přímý inline Base64 payload, aby nedošlo k resource_exhausted
            if (bytes.size > 2 * 1024 * 1024) {
                Log.w(TAG, "PDF is too large for inline Base64 payload (${bytes.size} bytes), skipping direct Gemini call.")
                return null
            }

            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
            OmnisGeminiClient.extractTextFromDocument(base64Data, "application/pdf")
        } catch (e: Exception) {
            Log.e(TAG, "Direct Gemini extraction failed", e)
            null
        }
    }
}
