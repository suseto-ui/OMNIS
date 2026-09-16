package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
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
            Toast.makeText(context, "PDF uloženo: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            
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
}
