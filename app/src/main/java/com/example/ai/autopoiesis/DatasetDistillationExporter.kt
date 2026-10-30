package com.example.ai.autopoiesis

import android.util.Log
import com.example.data.OmnisDao
import com.example.data.OmnisRecord
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * FÁZE X (10.1): Dataset Distillation & Auto-Evolution Exporter.
 * Extrahuje vysoce kvalitní kognitivní řetězce (Chain-of-Thought) a 8D hodnocení
 * a konvertuje je do standardního formátu JSONL pro jemné doladění (Fine-Tuning)
 * menších lokálních modelů (Gemma-2, Llama-3, Phi-3).
 */
object DatasetDistillationExporter {

    private const val TAG = "DatasetDistillation"

    data class DistillationExportSummary(
        val totalExamples: Int,
        val approvedHighConfidenceCount: Int,
        val jsonlPayload: String,
        val averageConfidence: Float
    )

    /**
     * Vyexportuje schválené záznamy do JSONL formátu pro Fine-Tuning.
     */
    suspend fun generateFineTuningDataset(
        dao: OmnisDao,
        minConfidenceThreshold: Float = 0.85f
    ): DistillationExportSummary {
        val allRecords = dao.getAllRecords().first()
        val assistantRecords = allRecords.filter { it.role == "assistant" && it.compositeScore >= minConfidenceThreshold }

        val jsonlBuilder = StringBuilder()
        var totalScore = 0f

        for (record in assistantRecords) {
            totalScore += record.compositeScore

            val systemPrompt = "Jsi O.M.N.I.S. (Omni-Modal Network for Integrated Synthesis). Poskytuj transdisciplinární syntézu v pevném JSON formátu s 8D maticí."
            
            // Hledáme předchozí uživatelský dotaz ve stejném vlákně
            val userRecord = allRecords.find { 
                it.role == "user" && 
                it.threadId == record.threadId && 
                it.timestamp <= record.timestamp 
            }
            val userQuery = userRecord?.content ?: "Transdisciplinární analýza systémového stavu."

            val assistantOutputJson = JSONObject().apply {
                put("agent_name", "omnis-core-synthesizer")
                put("thought_process", record.cognitiveProcess)
                put("status", "SUCCESS")
                put("result_data", JSONObject().apply {
                    put("answer", record.content)
                    put("follow_up_questions", JSONArray(record.followUpQuestions.split("|").filter { it.isNotBlank() }))
                    put("val_sys", record.valSys)
                    put("val_econ", record.valEcon)
                    put("val_psych", record.valPsych)
                    put("val_eco", record.valEco)
                    put("val_law", record.valLaw)
                    put("val_sec", record.valSec)
                    put("val_phys", record.valPhys)
                    put("val_soc", record.valSoc)
                    put("composite_score", record.compositeScore)
                })
            }

            // OpenAI / Gemini standardní formát konverzačního trénovacího páru
            val trainingRow = JSONObject().apply {
                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userQuery)
                    })
                    put(JSONObject().apply {
                        put("role", "assistant")
                        put("content", assistantOutputJson.toString())
                    })
                }
                put("messages", messagesArray)
            }

            jsonlBuilder.append(trainingRow.toString()).append("\n")
        }

        val avgConfidence = if (assistantRecords.isNotEmpty()) totalScore / assistantRecords.size else 0.0f

        Log.i(TAG, "Dataset Distillation: Vygenerováno ${assistantRecords.size} příkladů s průměrnou důvěrou ${(avgConfidence * 100).toInt()}%.")

        return DistillationExportSummary(
            totalExamples = allRecords.size,
            approvedHighConfidenceCount = assistantRecords.size,
            jsonlPayload = jsonlBuilder.toString().trim(),
            averageConfidence = avgConfidence
        )
    }
}
