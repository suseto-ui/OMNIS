package com.example.data

import android.content.Context
import android.util.Log
import com.example.api.OmnisGeminiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Centrální synchronizační manažer pro obousměrnou synchronizaci konverzací (Room DB <-> Backend API).
 * Zajišťuje plnou podporu offline režimu a automatické sloučení stavů při obnovení připojení.
 */
object OmnisSyncManager {
    private const val TAG = "OmnisSyncManager"
    private val syncScope = CoroutineScope(Dispatchers.IO)
    private var isSyncing = false

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    /**
     * Spustí asynchronní obousměrnou synchronizaci s backendem.
     */
    fun triggerSync(context: Context, repository: OmnisRepository, onComplete: ((Boolean) -> Unit)? = null) {
        if (isSyncing) {
            onComplete?.invoke(false)
            return
        }
        isSyncing = true
        syncScope.launch {
            var success = false
            try {
                Log.i(TAG, "Zahajování obousměrné synchronizace konverzací...")

                // 1. Synchronizace lokálních vláken na backend (/api/memory/sync)
                val localThreads = repository.allThreads.first()
                if (localThreads.isNotEmpty()) {
                    val syncUrl = OmnisGeminiClient.baseUrl + "api/memory/sync"
                    val threadsArray = JSONArray()
                    for (t in localThreads) {
                        val threadObj = JSONObject().apply {
                            put("id", t.threadId)
                            put("title", t.threadTitle)
                            put("ontologyDomain", "SYSTEMS_INTELLIGENCE")
                        }
                        threadsArray.put(threadObj)
                    }
                    val payload = JSONObject().apply {
                        put("threads", threadsArray)
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val requestBody = payload.toString().toRequestBody(mediaType)
                    val postRequest = Request.Builder()
                        .url(syncUrl)
                        .post(requestBody)
                        .build()

                    val syncResponse = httpClient.newCall(postRequest).execute()
                    val syncContentType = syncResponse.header("Content-Type") ?: ""
                    val isJson = syncContentType.contains("application/json", ignoreCase = true)
                    if (syncResponse.isSuccessful && isJson) {
                        Log.i(TAG, "Lokální konverzační vlákna úspěšně nahrána na backend.")
                        repository.omnisDao.markAllAsSynced()
                    } else {
                        Log.w(TAG, "Nahrávání vláken selhalo nebo backend vrátil ne-JSON odpověď (možná přesměrování/HTML). HTTP: ${syncResponse.code}, Content-Type: $syncContentType")
                    }
                    syncResponse.close()
                }

                // 2. Stažení vláken a zpráv z backendu (/api/threads) a jejich uložení do Room DB
                val getUrl = OmnisGeminiClient.baseUrl + "api/threads"
                val getRequest = Request.Builder()
                    .url(getUrl)
                    .get()
                    .build()

                val getResponse = httpClient.newCall(getRequest).execute()
                val contentType = getResponse.header("Content-Type") ?: ""
                val isJson = contentType.contains("application/json", ignoreCase = true)
                if (getResponse.isSuccessful) {
                    val bodyString = getResponse.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val trimmed = bodyString.trim()
                        if (isJson && trimmed.startsWith("[")) {
                            try {
                                val jsonArray = JSONArray(trimmed)
                                Log.i(TAG, "Načteno ${jsonArray.length()} vláken z backendu k integraci.")
                                
                                val incomingRecords = mutableListOf<OmnisRecord>()
                                for (i in 0 until jsonArray.length()) {
                                    val threadObj = jsonArray.getJSONObject(i)
                                    val threadId = threadObj.getString("id")
                                    val threadTitle = threadObj.optString("title", "Synchronizované Vlákno")
                                    val domain = threadObj.optString("ontology_domain", "SYSTEMS_INTELLIGENCE")
                                    val messagesArray = threadObj.optJSONArray("messages") ?: JSONArray()

                                    for (j in 0 until messagesArray.length()) {
                                        val msgObj = messagesArray.getJSONObject(j)
                                        val msgId = msgObj.optString("id")
                                        val role = msgObj.getString("role")
                                        val content = msgObj.getString("content")
                                        val thoughts = msgObj.optString("cognitive_thoughts", "")
                                        
                                        val followUpsArray = msgObj.optJSONArray("follow_up_questions")
                                        val followUpsStr = if (followUpsArray != null) {
                                            val list = mutableListOf<String>()
                                            for (k in 0 until followUpsArray.length()) {
                                                list.add(followUpsArray.getString(k))
                                            }
                                            list.joinToString("|")
                                        } else ""

                                        // Bezpečný převod ID zprávy (UUID -> Long) pro Room DB primární klíč
                                        val parsedId = try {
                                            UUID.fromString(msgId).mostSignificantBits and Long.MAX_VALUE
                                        } catch (e: Exception) {
                                            (msgId.hashCode().toLong() and 0xFFFFFFFFL) + (j * 1000000)
                                        }

                                        val localRecord = OmnisRecord(
                                            id = parsedId,
                                            role = role,
                                            content = content,
                                            cognitiveProcess = thoughts,
                                            followUpQuestions = followUpsStr,
                                            threadId = threadId,
                                            threadTitle = threadTitle,
                                            domain = domain,
                                            isSyncedToPostgres = true
                                        )
                                        incomingRecords.add(localRecord)
                                    }
                                }
                                if (incomingRecords.isNotEmpty()) {
                                    repository.insertRecordsBatched(incomingRecords)
                                }
                                val prunedCount = repository.pruneOldSyncedRecords(daysOld = 14)
                                Log.i(TAG, "Obousměrná integrace dat dokončena (${incomingRecords.size} zpráv uloženo, $prunedCount starých záznamů pročištěno).")
                                success = true
                            } catch (e: org.json.JSONException) {
                                Log.e(TAG, "Chyba při parsování JSON odpovědi z backendu: ${e.message}")
                            }
                        } else {
                            Log.w(TAG, "Ignorována odpověď z backendu, protože to není platný JSONArray. Možná proxy přesměrování na HTML přihlašovací stránku. Začátek odpovědi: ${trimmed.take(100)}")
                        }
                    }
                } else {
                    Log.w(TAG, "Načtení vláken z backendu selhalo s HTTP: ${getResponse.code}, Content-Type: $contentType")
                }
                getResponse.close()
            } catch (e: Exception) {
                Log.e(TAG, "Chyba při obousměrné synchronizaci konverzací: ${e.message}", e)
            } finally {
                isSyncing = false
                onComplete?.invoke(success)
            }
        }
    }
}
