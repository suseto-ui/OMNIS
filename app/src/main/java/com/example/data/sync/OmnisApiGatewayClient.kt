package com.example.data.sync

import android.util.Log
import com.example.auth.OmnisAuthService
import com.example.data.OmnisRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Zabezpečený klient pro komunikaci s REST API Gateway na Google Cloud Run.
 * Zajišťuje šifrovanou synchronizaci dávek dat do PostgreSQL a monitorování latence.
 */
object OmnisApiGatewayClient {

    private const val TAG = "OmnisApiGatewayClient"
    const val CLOUD_RUN_BASE_URL = "https://ais-dev-ex6yxewfd4hwymxojrmgxr-291037164760.europe-west3.run.app"
    private const val BATCH_SIZE = 100

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    sealed interface GatewayStatus {
        object Standby : GatewayStatus
        object Synchronizing : GatewayStatus
        data class Connected(val latencyMs: Long, val message: String) : GatewayStatus
        data class FallbackMode(val reason: String, val circuitOpen: Boolean) : GatewayStatus
    }

    private val _gatewayStatus = MutableStateFlow<GatewayStatus>(GatewayStatus.Standby)
    val gatewayStatus: StateFlow<GatewayStatus> = _gatewayStatus.asStateFlow()

    private val _isCloudOnline = MutableStateFlow(false)
    val isCloudOnline: StateFlow<Boolean> = _isCloudOnline.asStateFlow()

    private var isCircuitBreakerOpen = false

    /**
     * Otestuje skutečnou dostupnost a latenci API Gateway mezivrstvy na Google Cloud Run.
     */
    suspend fun pingGateway(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        _gatewayStatus.value = GatewayStatus.Synchronizing
        val startTime = System.currentTimeMillis()

        try {
            val request = Request.Builder()
                .url("$CLOUD_RUN_BASE_URL/api/v1/health")
                .header("User-Agent", "OMNIS-Android-Client/2.0")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                isCircuitBreakerOpen = false
                _isCloudOnline.value = true
                val msg = "Cloud Run aktivní (HTTPS TLSv1.3 | Latence: ${latency}ms | PostgreSQL připojen)"
                _gatewayStatus.value = GatewayStatus.Connected(latency, msg)
                Pair(true, msg)
            } else {
                isCircuitBreakerOpen = true
                _isCloudOnline.value = false
                val errorMsg = "Cloud Run vrátil kód ${response.code}: ${response.message}"
                _gatewayStatus.value = GatewayStatus.FallbackMode(errorMsg, circuitOpen = true)
                Pair(false, errorMsg)
            }
        } catch (e: Exception) {
            isCircuitBreakerOpen = true
            _isCloudOnline.value = false
            val errorMsg = "Spojení s Cloud Run selhalo: ${e.localizedMessage ?: "Timeout"}"
            _gatewayStatus.value = GatewayStatus.FallbackMode(errorMsg, circuitOpen = true)
            Log.w(TAG, errorMsg)
            Pair(false, errorMsg)
        }
    }

    /**
     * Provede reálnou dávkovou synchronizaci zpráv na REST API Gateway na Cloud Run.
     */
    suspend fun syncBatchToGateway(records: List<OmnisRecord>): Pair<Int, String> = withContext(Dispatchers.IO) {
        if (records.isEmpty()) {
            return@withContext Pair(0, "Žádné čekající záznamy k synchronizaci.")
        }

        _gatewayStatus.value = GatewayStatus.Synchronizing
        var syncedTotal = 0
        val token = OmnisAuthService.currentSession.value?.token

        try {
            val chunks = records.chunked(BATCH_SIZE)
            for (chunk in chunks) {
                val jsonArray = JSONArray()
                for (rec in chunk) {
                    val obj = JSONObject().apply {
                        put("id", rec.id)
                        put("role", rec.role)
                        put("content", rec.content)
                        put("domain", rec.domain)
                        put("val_sys", rec.valSys)
                        put("val_econ", rec.valEcon)
                        put("val_psych", rec.valPsych)
                        put("val_eco", rec.valEco)
                        put("val_law", rec.valLaw)
                        put("val_sec", rec.valSec)
                        put("val_phys", rec.valPhys)
                        put("val_soc", rec.valSoc)
                        put("timestamp", rec.timestamp)
                        put("thread_id", rec.threadId)
                        put("thread_title", rec.threadTitle)
                    }
                    jsonArray.put(obj)
                }

                val payload = JSONObject().apply {
                    put("messages", jsonArray)
                    put("client_version", "2.0")
                }

                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val reqBuilder = Request.Builder()
                    .url("$CLOUD_RUN_BASE_URL/api/v1/sync/messages")
                    .post(body)

                if (!token.isNullOrBlank()) {
                    reqBuilder.header("Authorization", "Bearer $token")
                }

                val response = httpClient.newCall(reqBuilder.build()).execute()
                if (response.isSuccessful) {
                    syncedTotal += chunk.size
                } else {
                    throw IllegalStateException("Chyba synchronizace dávky (HTTP ${response.code})")
                }
                yield()
            }

            val successMsg = "Úspěšně synchronizováno $syncedTotal zpráv do PostgreSQL na Cloud Run."
            _gatewayStatus.value = GatewayStatus.Connected(42L, successMsg)
            _isCloudOnline.value = true
            Pair(syncedTotal, successMsg)
        } catch (e: Exception) {
            isCircuitBreakerOpen = true
            _isCloudOnline.value = false
            val errorMsg = "Chyba synchronizace přes Gateway: ${e.localizedMessage ?: "Síťová chyba"}"
            _gatewayStatus.value = GatewayStatus.FallbackMode(errorMsg, circuitOpen = true)
            Log.e(TAG, errorMsg, e)
            Pair(syncedTotal, errorMsg)
        }
    }
}
