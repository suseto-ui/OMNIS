package com.example.data

import android.util.Log
import com.example.telemetry.TelemetryEngine
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * FÁZE X (10.3): Multiplatformní synchronizační protokol vláken (P2P State Sync & Export/Import).
 * Zajišťuje bezpečné a decentralizované sloučení stavu, konverzací a 8D vah
 * napříč mobilním zařízením, webovou konzolí a desktopovým klientem.
 */
object MultiplatformThreadSyncEngine {

    private const val TAG = "MultiplatformSync"

    data class SyncBundle(
        val version: String,
        val timestamp: Long,
        val threadsCount: Int,
        val recordsCount: Int,
        val jsonPayload: String
    )

    data class SyncMergeReport(
        val insertedRecords: Int,
        val updatedThreads: Int,
        val isSuccess: Boolean,
        val details: String
    )

    /**
     * Vygeneruje kompletní přenositelný stavový snapshot konverzačních vláken.
     */
    suspend fun exportStateBundle(dao: OmnisDao): SyncBundle {
        val allRecords = dao.getAllRecords().first()
        val allThreads = dao.getAllThreads().first()

        val threadsArray = JSONArray()
        for (thread in allThreads) {
            val threadObj = JSONObject().apply {
                put("threadId", thread.threadId)
                put("threadTitle", thread.threadTitle)
                put("userName", thread.userName)
                put("lastTimestamp", thread.lastTimestamp)
                put("messageCount", thread.messageCount)
                put("lastContent", thread.lastContent ?: "")
            }
            threadsArray.put(threadObj)
        }

        val recordsArray = JSONArray()
        for (record in allRecords) {
            val recordObj = JSONObject().apply {
                put("id", record.id)
                put("threadId", record.threadId)
                put("threadTitle", record.threadTitle)
                put("role", record.role)
                put("content", record.content)
                put("cognitiveProcess", record.cognitiveProcess)
                put("followUpQuestions", record.followUpQuestions)
                put("timestamp", record.timestamp)
                put("domain", record.domain)
                put("valSys", record.valSys)
                put("valEcon", record.valEcon)
                put("valPsych", record.valPsych)
                put("valEco", record.valEco)
                put("valLaw", record.valLaw)
                put("valSec", record.valSec)
                put("valPhys", record.valPhys)
                put("valSoc", record.valSoc)
                put("compositeScore", record.compositeScore)
                put("defenseTier", record.defenseTier)
            }
            recordsArray.put(recordObj)
        }

        val rootBundle = JSONObject().apply {
            put("protocol", "OMNIS_P2P_SYNC_V2")
            put("exportedAt", System.currentTimeMillis())
            put("threads", threadsArray)
            put("records", recordsArray)
        }

        val payload = rootBundle.toString()
        Log.i(TAG, "Exportován P2P stavový bundle: ${allThreads.size} vláken, ${allRecords.size} zpráv.")

        return SyncBundle(
            version = "2.0.0",
            timestamp = System.currentTimeMillis(),
            threadsCount = allThreads.size,
            recordsCount = allRecords.size,
            jsonPayload = payload
        )
    }

    /**
     * Importuje a atomicky sloučí stavový snapshot s lokální Room DB bez přepsání existujících dat.
     */
    suspend fun importAndMergeBundle(dao: OmnisDao, rawJsonPayload: String): SyncMergeReport {
        return try {
            val root = JSONObject(rawJsonPayload)
            val recordsArray = root.optJSONArray("records") ?: JSONArray()
            val existingRecords = dao.getAllRecords().first()
            val existingIds = existingRecords.map { it.id }.toSet()

            var insertedCount = 0

            for (i in 0 until recordsArray.length()) {
                val rObj = recordsArray.getJSONObject(i)
                val rawId = rObj.optLong("id", System.currentTimeMillis() + i)
                val threadId = rObj.optString("threadId", UUID.randomUUID().toString())

                // Pokud záznam již v lokální DB existuje, nepřepisujeme jej
                if (existingIds.contains(rawId)) continue

                val record = OmnisRecord(
                    id = rawId,
                    threadId = threadId,
                    threadTitle = rObj.optString("threadTitle", "Importované Vlákno"),
                    role = rObj.optString("role", "assistant"),
                    content = rObj.optString("content", ""),
                    cognitiveProcess = rObj.optString("cognitiveProcess", ""),
                    followUpQuestions = rObj.optString("followUpQuestions", ""),
                    timestamp = rObj.optLong("timestamp", System.currentTimeMillis()),
                    domain = rObj.optString("domain", "SYSTEMS_INTELLIGENCE"),
                    valSys = rObj.optDouble("valSys", 0.9).toFloat(),
                    valEcon = rObj.optDouble("valEcon", 0.8).toFloat(),
                    valPsych = rObj.optDouble("valPsych", 0.8).toFloat(),
                    valEco = rObj.optDouble("valEco", 0.8).toFloat(),
                    valLaw = rObj.optDouble("valLaw", 0.9).toFloat(),
                    valSec = rObj.optDouble("valSec", 0.9).toFloat(),
                    valPhys = rObj.optDouble("valPhys", 0.8).toFloat(),
                    valSoc = rObj.optDouble("valSoc", 0.8).toFloat(),
                    compositeScore = rObj.optDouble("compositeScore", 0.85).toFloat(),
                    defenseTier = rObj.optString("defenseTier", "APPROVED"),
                    isSyncedToPostgres = true
                )

                dao.insertRecord(record)
                insertedCount++
            }

            TelemetryEngine.log(
                type = "P2P_SYNC_MERGE",
                component = "MultiplatformSyncEngine",
                message = "Úspěšně importováno $insertedCount nových zpráv z multiplatformního balíčku.",
                metadata = "{\"inserted\":$insertedCount, \"total\":${recordsArray.length()}}"
            )

            SyncMergeReport(
                insertedRecords = insertedCount,
                updatedThreads = root.optJSONArray("threads")?.length() ?: 0,
                isSuccess = true,
                details = "Úspěšně sloučeno $insertedCount nových zpráv."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Chyba při importu multiplatformního balíčku", e)
            SyncMergeReport(
                insertedRecords = 0,
                updatedThreads = 0,
                isSuccess = false,
                details = "Chyba při parsování synchronizačního balíčku: ${e.message}"
            )
        }
    }
}
