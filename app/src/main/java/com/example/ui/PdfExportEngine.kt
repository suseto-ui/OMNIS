package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.auth.OmnisAuthService
import com.example.data.OmnisRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportEngine {
    fun exportToPdf(context: Context, record: OmnisRecord, packageName: String) {
        val pdfDocument = PdfDocument()
        val textPaint = Paint().apply {
            textSize = 12f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = android.graphics.Color.BLUE
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = 50f

        canvas.drawText("O.M.N.I.S. Kognitivní Report", 50f, y, headerPaint)
        y += 40f

        textPaint.isFakeBoldText = true
        canvas.drawText("Identifikátor: #${record.id}", 50f, y, textPaint)
        y += 20f
        canvas.drawText("Role: ${record.role.uppercase(Locale.getDefault())}", 50f, y, textPaint)
        y += 20f
        canvas.drawText("Čas: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(record.timestamp))}", 50f, y, textPaint)
        y += 40f

        textPaint.isFakeBoldText = false
        val contentLines = record.content.split("\n")
        val maxWidth = 500f

        for (line in contentLines) {
            val words = line.split(" ")
            var currentLine = java.lang.StringBuilder()
            
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val width = textPaint.measureText(testLine)
                
                if (width > maxWidth) {
                    canvas.drawText(currentLine.toString(), 50f, y, textPaint)
                    y += 20f
                    currentLine = java.lang.StringBuilder(word)
                    
                    if (y > 780) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        y = 50f
                    }
                } else {
                    currentLine.append(if (currentLine.isEmpty()) word else " $word")
                }
            }
            
            if (currentLine.isNotEmpty()) {
                canvas.drawText(currentLine.toString(), 50f, y, textPaint)
                y += 20f
            }

            if (y > 780) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }
        }

        if (record.cognitiveProcess.isNotBlank()) {
            y += 20f
            textPaint.isFakeBoldText = true
            canvas.drawText("Kognitivní Introspekce:", 50f, y, textPaint)
            y += 20f
            textPaint.isFakeBoldText = false
            
            val processLines = record.cognitiveProcess.split("\n")
            for (line in processLines) {
                canvas.drawText(line.take(80), 50f, y, textPaint)
                y += 18f
                if (y > 780) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }
            }
        }

        pdfDocument.finishPage(page)

        try {
            val file = File(context.getExternalFilesDir(null), "omnis_export_${record.id}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "PDF uloženo: ${file.name}", Toast.LENGTH_SHORT).show()
            
            val authority = "${packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Sdílet PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export selhal: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    fun exportThreadToPdf(context: Context, threadTitle: String, userName: String, records: List<OmnisRecord>, packageName: String) {
        if (records.isEmpty()) {
            Toast.makeText(context, "Vlákno je prázdné, nelze exportovat.", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val textPaint = Paint().apply {
            textSize = 10f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
            color = android.graphics.Color.BLUE
        }
        val subheaderPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            color = android.graphics.Color.DKGRAY
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = 50f

        canvas.drawText("O.M.N.I.S. EXPORT VLÁKNA", 50f, y, headerPaint)
        y += 24f
        canvas.drawText("Téma: $threadTitle | Uživatel: $userName", 50f, y, subheaderPaint)
        y += 18f
        canvas.drawText("Vygenerováno: ${SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())} | Počet zpráv: ${records.size}", 50f, y, textPaint)
        y += 30f

        val maxWidth = 495f

        for (record in records.sortedBy { it.timestamp }) {
            val isModel = record.role.lowercase(Locale.getDefault()) == "model" || record.role.lowercase(Locale.getDefault()) == "assistant"
            val roleLabel = if (isModel) "🤖 O.M.N.I.S. (Kernel)" else "👤 Uživatel ($userName)"
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp))

            if (y > 750) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }

            textPaint.isFakeBoldText = true
            textPaint.color = if (isModel) android.graphics.Color.rgb(0, 100, 150) else android.graphics.Color.rgb(50, 50, 50)
            canvas.drawText("[$timeStr] $roleLabel:", 50f, y, textPaint)
            y += 16f
            textPaint.isFakeBoldText = false
            textPaint.color = android.graphics.Color.BLACK

            val contentLines = record.content.split("\n")
            for (line in contentLines) {
                val words = line.split(" ")
                var currentLine = java.lang.StringBuilder()

                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    val width = textPaint.measureText(testLine)

                    if (width > maxWidth) {
                        canvas.drawText(currentLine.toString(), 50f, y, textPaint)
                        y += 14f
                        currentLine = java.lang.StringBuilder(word)

                        if (y > 790) {
                            pdfDocument.finishPage(page)
                            pageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            y = 50f
                        }
                    } else {
                        currentLine.append(if (currentLine.isEmpty()) word else " $word")
                    }
                }

                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine.toString(), 50f, y, textPaint)
                    y += 14f
                }

                if (y > 790) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }
            }
            y += 12f
        }

        pdfDocument.finishPage(page)

        try {
            val safeTitle = threadTitle.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)
            val file = File(context.getExternalFilesDir(null), "omnis_thread_${safeTitle}_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "PDF vlákna uloženo: ${file.name}", Toast.LENGTH_SHORT).show()

            val authority = "${packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Sdílet PDF vlákna"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export selhal: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    fun exportThreadToMarkdown(context: Context, threadTitle: String, userName: String, records: List<OmnisRecord>, packageName: String) {
        if (records.isEmpty()) {
            Toast.makeText(context, "Vlákno je prázdné, nelze exportovat.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val sb = java.lang.StringBuilder()
            sb.append("# O.M.N.I.S. Kognitivní Záznam Vlákna\n\n")
            sb.append("**Vlákno:** ").append(threadTitle).append("\n")
            sb.append("**Uživatel:** ").append(userName).append("\n")
            sb.append("**Datum:** ").append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())).append("\n\n")
            sb.append("---\n\n")

            for (record in records.sortedBy { it.timestamp }) {
                val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp))
                val isModel = record.role.lowercase(Locale.getDefault()) == "model" || record.role.lowercase(Locale.getDefault()) == "assistant"
                val sender = if (isModel) "🤖 O.M.N.I.S. Kernel" else "👤 $userName"

                sb.append("### ").append(sender).append(" `[").append(timeStr).append("]`\n\n")
                sb.append(record.content).append("\n\n")

                if (record.cognitiveProcess.isNotBlank()) {
                    sb.append("> **Introspekce:** ").append(record.cognitiveProcess).append("\n\n")
                }
                sb.append("---\n\n")
            }

            val safeTitle = threadTitle.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)
            val file = File(context.getExternalFilesDir(null), "omnis_thread_${safeTitle}_${System.currentTimeMillis()}.md")
            file.writeText(sb.toString())

            Toast.makeText(context, "Markdown uložen: ${file.name}", Toast.LENGTH_SHORT).show()

            val authority = "${packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/markdown"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, sb.toString())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Sdílet Markdown vlákna"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export Markdown selhal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
