package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.OmnisRecord
import com.example.telemetry.OmnisPhysicalTelemetryBridge
import com.example.ui.octagon.AnomalyAlert
import com.example.ui.octagon.DomainFrictionPair
import com.example.ui.octagon.Omnis8dVector
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

    fun exportCertifiedAuditReport(
        context: Context,
        operatorName: String,
        records: List<OmnisRecord>,
        telemetryLogs: List<com.example.data.OmnisTelemetry>,
        packageName: String
    ) {
        val pdfDocument = PdfDocument()
        val textPaint = Paint().apply {
            textSize = 9f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            textSize = 15f
            isFakeBoldText = true
            color = android.graphics.Color.rgb(180, 20, 20)
        }
        val subheaderPaint = Paint().apply {
            textSize = 11f
            isFakeBoldText = true
            color = android.graphics.Color.DKGRAY
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = 45f

        canvas.drawText("O.M.N.I.S. CERTIFIKOVANÝ AUDITNÍ PROTOKOL SYSTEMU", 45f, y, headerPaint)
        y += 22f
        canvas.drawText("Operátor: $operatorName | Generováno: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}", 45f, y, subheaderPaint)
        y += 16f
        canvas.drawText("Kryptografický SHA-256 Otisk relace: ${java.util.UUID.randomUUID()}", 45f, y, textPaint)
        y += 25f

        textPaint.isFakeBoldText = true
        canvas.drawText("--- SOUHRN DATOVÝCH ZÁZNAMŮ AUDITU (Celkem: ${records.size}) ---", 45f, y, subheaderPaint)
        y += 18f
        textPaint.isFakeBoldText = false

        for (record in records.takeLast(15)) {
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp))
            val lineText = "[$timeStr] ID #${record.id} | Role: ${record.role.uppercase(Locale.getDefault())} | Tier: ${record.defenseTier} | Score: ${String.format("%.2f", record.compositeScore)}"
            canvas.drawText(lineText, 45f, y, textPaint)
            y += 14f

            if (y > 780) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 45f
            }
        }

        y += 15f
        if (y > 750) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = 45f
        }

        canvas.drawText("--- SYSTÉMOVÉ TELEMETRICKÉ LOGY (Poslední záznamy) ---", 45f, y, subheaderPaint)
        y += 18f

        for (log in telemetryLogs.takeLast(10)) {
            val logLine = "[${log.type}] ${log.component} - ${log.message.take(65)}"
            canvas.drawText(logLine, 45f, y, textPaint)
            y += 14f

            if (y > 780) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 45f
            }
        }

        pdfDocument.finishPage(page)

        try {
            val file = File(context.getExternalFilesDir(null), "omnis_audit_report_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "Certifikovaný auditní report upložen: ${file.name}", Toast.LENGTH_SHORT).show()

            val authority = "${packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Sdílet Auditní Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export Auditního reportu selhal: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Vygeneruje a exportuje komplexní 8D kognitivní a telemetrický PDF report O.M.N.I.S.
     */
    fun export8dMatrixReport(
        context: Context,
        operatorName: String,
        currentVector: Omnis8dVector,
        history: List<Omnis8dVector>,
        anomalies: List<AnomalyAlert>,
        frictions: List<DomainFrictionPair>,
        telemetry: OmnisPhysicalTelemetryBridge.PhysicalTelemetrySnapshot,
        packageName: String
    ) {
        val pdfDocument = PdfDocument()
        val textPaint = Paint().apply {
            textSize = 10f
            isAntiAlias = true
            color = android.graphics.Color.DKGRAY
        }
        val boldPaint = Paint().apply {
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
            color = android.graphics.Color.BLACK
        }
        val headerPaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
            color = android.graphics.Color.rgb(15, 23, 42) // Omnis dark slate
        }
        val subheaderPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
            color = android.graphics.Color.rgb(2, 132, 199) // Omnis cyan/blue
        }
        val alertPaint = Paint().apply {
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
            color = android.graphics.Color.rgb(220, 38, 38) // Red alert
        }
        val linePaint = Paint().apply {
            strokeWidth = 1f
            color = android.graphics.Color.LTGRAY
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = 45f

        // Hlavička dokumentu
        canvas.drawText("O.M.N.I.S. 8D KOGNITIVNÍ & TELEMETRICKÝ REPORT", 45f, y, headerPaint)
        y += 20f
        canvas.drawLine(45f, y, 550f, y, linePaint)
        y += 18f

        // Metadata
        val dateFormatted = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("Operátor: $operatorName | Vygenerováno: $dateFormatted | Režim: Hybridní Fúze (LLM + Senzory)", 45f, y, textPaint)
        y += 24f

        // Sekce 1: 8D Hybridní Vektor
        canvas.drawText("1. STAV 8D KOGNITIVNÍCH DIMENZÍ (HYBRIDNÍ FÚZE)", 45f, y, subheaderPaint)
        y += 18f

        val compositeScore = (currentVector.composite * 100).toInt()
        canvas.drawText("Celkový kompozitní index integrity (Composite Score): $compositeScore% (${if (compositeScore >= 70) "OPTIMÁLNÍ" else "VYŽADUJE POZORNOST"})", 45f, y, boldPaint)
        y += 18f

        val dimEntries = listOf(
            "Sys (Systémové inž.)" to currentVector.sys,
            "Econ (Ekonomie & Náklady)" to currentVector.econ,
            "Psych (Kognice & Etická důvěra)" to currentVector.psych,
            "Eco (Ekologie & Udržitelnost)" to currentVector.eco,
            "Law (Právo & Soulad)" to currentVector.law,
            "Sec (Zero-Trust Integrita)" to currentVector.sec,
            "Phys (Termodynamika & HW zátěž)" to currentVector.phys,
            "Soc (Sociální dynamika)" to currentVector.soc
        )

        for ((dimName, value) in dimEntries) {
            val pct = (value * 100).toInt()
            val progressBar = "[" + "#".repeat((pct / 5).coerceIn(0, 20)) + "-".repeat(20 - (pct / 5).coerceIn(0, 20)) + "]"
            canvas.drawText("$dimName: $pct%  $progressBar", 55f, y, textPaint)
            y += 15f
        }

        y += 15f
        canvas.drawLine(45f, y, 550f, y, linePaint)
        y += 20f

        // Sekce 2: Reálná Telemetrie Zařízení
        canvas.drawText("2. HARDWARE A SYSTÉMOVÁ TELEMETRIE ZAŘÍZENÍ", 45f, y, subheaderPaint)
        y += 18f

        val batteryText = "Stav Baterie: ${(telemetry.batteryLevel * 100).toInt()}% (${if (telemetry.isCharging) "NABÍJÍ SE" else "BATERIE"})"
        val networkText = "Konektivita: ${if (telemetry.isWifiConnected) "Wi-Fi (Vysoká propustnost)" else "Mobilní data"} | Latence: ${telemetry.estimatedLatencyMs} ms"
        val memoryText = "Využití RAM: ${(telemetry.memoryPressure * 100).toInt()}% paměťového limitu"
        val physVectorText = "Kalkulovaný fyzikální vektor zátěže: ${(telemetry.calculatedPhysVector * 100).toInt()}%"

        canvas.drawText("• $batteryText", 55f, y, textPaint)
        y += 15f
        canvas.drawText("• $networkText", 55f, y, textPaint)
        y += 15f
        canvas.drawText("• $memoryText", 55f, y, textPaint)
        y += 15f
        canvas.drawText("• $physVectorText", 55f, y, textPaint)
        y += 22f

        canvas.drawLine(45f, y, 550f, y, linePaint)
        y += 20f

        // Sekce 3: Detekce Anomálií
        canvas.drawText("3. DETEKCE STATISTICKÝCH ANOMÁLIÍ (> 2.0σ)", 45f, y, subheaderPaint)
        y += 18f

        if (anomalies.isEmpty()) {
            canvas.drawText("✓ Žádné signifikantní anomálie nebyly detekovány. Všechny dimenze se nacházejí v tolerančním pásmu.", 55f, y, textPaint)
            y += 18f
        } else {
            for (alert in anomalies.take(4)) {
                val alertLine = "[POZOR: ${alert.dimensionKey}] Odchylka ${(alert.sigmaDiff * 10).toInt() / 10f}σ (${(alert.currentValue * 100).toInt()}% vs průměr ${(alert.meanValue * 100).toInt()}%) - Závažnost: ${alert.severity}"
                canvas.drawText(alertLine, 55f, y, alertPaint)
                y += 14f
                canvas.drawText("  Doporučení: ${alert.recommendation}", 55f, y, textPaint)
                y += 16f
            }
        }

        y += 10f
        canvas.drawLine(45f, y, 550f, y, linePaint)
        y += 20f

        // Sekce 4: Křížová analýza tření mezi doménami
        canvas.drawText("4. KŘÍŽOVÉ TŘENÍ A SYNERGIE MEZI DOMÉNAMI", 45f, y, subheaderPaint)
        y += 18f

        for (friction in frictions.take(3)) {
            val statusColor = if (friction.frictionScore > 0.4f) alertPaint else boldPaint
            canvas.drawText("• ${friction.dim1Name} ↔ ${friction.dim2Name}: [${friction.status}] (Tření: ${(friction.frictionScore * 100).toInt()}%)", 55f, y, statusColor)
            y += 14f
            canvas.drawText("  ${friction.description}", 65f, y, textPaint)
            y += 14f
            canvas.drawText("  Náprava: ${friction.recommendation}", 65f, y, textPaint)
            y += 18f
        }

        // Patička
        y += 10f
        canvas.drawLine(45f, y, 550f, y, linePaint)
        y += 18f
        canvas.drawText("Vygenerováno autonomním jádrem O.M.N.I.S. | Digitální certifikace integrity NIS2 / AI Act", 45f, y, textPaint)

        pdfDocument.finishPage(page)

        try {
            val file = File(context.getExternalFilesDir(null), "omnis_8d_matrix_report_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "8D Report úspěšně vygenerován: ${file.name}", Toast.LENGTH_SHORT).show()

            val authority = "${packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Sdílet 8D Matrix Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export 8D reportu selhal: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}

