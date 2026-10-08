package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Diagnostický stav a výsledky ověření GEMINI_API_KEY
 */
enum class GeminiKeyStatus {
    VALID_ACTIVE,           // HTTP 200 OK, kvóta a klíč aktivní
    CREDITS_DEPLETED_402,   // HTTP 402 Payment Required - vyčerpán kredit
    RATE_LIMITED_429,       // HTTP 429 Too Many Requests - překročen limit
    INVALID_KEY_400_403,    // HTTP 400/403 - neplatný klíč nebo nepovolené API
    SERVER_ERROR_5XX,       // HTTP 500+ - výpadek serverů Google
    NETWORK_ERROR,          // IOException / timeout / bez připojení k síti
    NOT_CONFIGURED          // Klíč je prázdný nebo placeholder
}

data class KeyFormatValidationResult(
    val slot: Int = 1,
    val slotName: String = "Slot 1 (GEMINI_API_KEY)",
    val isPresent: Boolean,
    val isPlaceholder: Boolean,
    val hasValidPrefix: Boolean,
    val hasStandardLength: Boolean,
    val length: Int,
    val maskedKey: String,
    val isValidFormat: Boolean,
    val warningLevel: WarningLevel,
    val warningTitle: String,
    val warningMessage: String,
    val checklistItems: List<KeyChecklistItem>
)

enum class WarningLevel {
    OK,         // Klíč přítomen a formát odpovídá standardnímu formátu AIzaSy... (39 znaků)
    WARNING,    // Klíč přítomen, ale odchylka v délce nebo prefixu
    CRITICAL    // Klíč chybí nebo je výchozí placeholder z BuildConfig
}

data class KeyChecklistItem(
    val title: String,
    val isPassed: Boolean,
    val detail: String
)

data class GeminiPingLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
    val targetEndpoint: String,
    val httpCode: Int?,
    val latencyMs: Long,
    val status: GeminiKeyStatus,
    val statusBadge: String,
    val details: String,
    val payloadSnippet: String? = null
)

data class GeminiDiagnosticReport(
    val isConfigured: Boolean,
    val isPlaceholder: Boolean,
    val maskedKey: String,
    val keyLength: Int,
    val status: GeminiKeyStatus,
    val httpCode: Int?,
    val latencyMs: Long,
    val statusTitle: String,
    val statusDescription: String,
    val quotaState: String,
    val discoveredModels: List<String>,
    val recommendation: String,
    val rawResponseSnippet: String? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val keySlot: Int = 1,
    val totalActiveKeysInPool: Int = 1
)

object GeminiDiagnosticService {
    private const val TAG = "GeminiDiagnosticService"
    private const val PROBE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val _pingLogs = MutableStateFlow<List<GeminiPingLogEntry>>(emptyList())
    val pingLogs: StateFlow<List<GeminiPingLogEntry>> = _pingLogs.asStateFlow()

    private val _latestReport = MutableStateFlow<GeminiDiagnosticReport?>(null)
    val latestReport: StateFlow<GeminiDiagnosticReport?> = _latestReport.asStateFlow()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun clearLogs() {
        _pingLogs.value = emptyList()
    }

    private fun addLog(entry: GeminiPingLogEntry) {
        val current = _pingLogs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 50) {
            _pingLogs.value = current.take(50)
        } else {
            _pingLogs.value = current
        }
    }

    /**
     * Spustí diagnostický test runtime injekce GEMINI_API_KEY a provede online ping test
     * napříč všemi klíči ve fondu.
     */
    suspend fun diagnoseApiKey(customKey: String? = null): GeminiDiagnosticReport = withContext(Dispatchers.IO) {
        val allEntries = OmnisGeminiClient.KeyPool.getAllEntries()
        val apiKey = (customKey ?: allEntries.firstOrNull()?.key ?: BuildConfig.GEMINI_API_KEY).trim()
        val isPlaceholder = apiKey == "MY_GEMINI_API_KEY" || apiKey.equals("YOUR_API_KEY_HERE", ignoreCase = true) || apiKey == "YOUR_GEMINI_API_KEY"
        val isConfigured = apiKey.isNotBlank() && !isPlaceholder

        val masked = maskApiKey(apiKey)

        if (!isConfigured) {
            val report = GeminiDiagnosticReport(
                isConfigured = false,
                isPlaceholder = isPlaceholder,
                maskedKey = masked,
                keyLength = apiKey.length,
                status = GeminiKeyStatus.NOT_CONFIGURED,
                httpCode = null,
                latencyMs = 0L,
                statusTitle = if (isPlaceholder) "PLACEHOLDER DETEKOVÁN" else "KLÍČ NENÍ INJEKTOVÁN",
                statusDescription = if (isPlaceholder)
                    "BuildConfig obsahuje výchozí zástupný text 'YOUR_GEMINI_API_KEY'."
                else
                    "V proměnné BuildConfig.GEMINI_API_KEY je prázdná hodnota.",
                quotaState = "Není k dispozici (Offline fallback)",
                discoveredModels = emptyList(),
                recommendation = "Přejděte do AI Studio -> Secrets -> vložte GEMINI_API_KEY (Slot 1), GEMINI_API_KEY_2 (Slot 2) a GEMINI_API_KEY_3 (Slot 3).",
                rawResponseSnippet = null,
                keySlot = 1,
                totalActiveKeysInPool = allEntries.size
            )
            _latestReport.value = report
            addLog(
                GeminiPingLogEntry(
                    targetEndpoint = "BuildConfig.GEMINI_API_KEY",
                    httpCode = null,
                    latencyMs = 0L,
                    status = GeminiKeyStatus.NOT_CONFIGURED,
                    statusBadge = "NENÍ NASTAVEN",
                    details = report.statusDescription,
                    payloadSnippet = null
                )
            )
            return@withContext report
        }

        val startTime = System.currentTimeMillis()
        try {
            val url = "$PROBE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val code = response.code
                val bodyStr = response.body?.string().orEmpty()

                val report = parseProbeResponse(code, bodyStr, masked, apiKey.length, latency).copy(
                    totalActiveKeysInPool = allEntries.size
                )
                _latestReport.value = report

                addLog(
                    GeminiPingLogEntry(
                        targetEndpoint = "GET /v1beta/models",
                        httpCode = code,
                        latencyMs = latency,
                        status = report.status,
                        statusBadge = if (code == 200) "200 OK" else "HTTP $code",
                        details = "${report.statusTitle} - ${report.statusDescription} [Aktivních klíčů: ${allEntries.size}]",
                        payloadSnippet = report.rawResponseSnippet
                    )
                )

                report
            }
        } catch (e: IOException) {
            val latency = System.currentTimeMillis() - startTime
            Log.e(TAG, "Diagnostic network failure", e)
            val report = GeminiDiagnosticReport(
                isConfigured = true,
                isPlaceholder = false,
                maskedKey = masked,
                keyLength = apiKey.length,
                status = GeminiKeyStatus.NETWORK_ERROR,
                httpCode = null,
                latencyMs = latency,
                statusTitle = "CHYBA PŘIPOJENÍ K SÍTI",
                statusDescription = "Mobilní zařízení se nemohlo spojit se servery Google (${e.message ?: "Timeout"}).",
                quotaState = "Neznámý (Zařízení je offline)",
                discoveredModels = emptyList(),
                recommendation = "Zkontrolujte Wi-Fi nebo mobilní data na zařízení. Aplikace bezpečně pokračuje v lokálním 8D režimu.",
                rawResponseSnippet = e.localizedMessage,
                totalActiveKeysInPool = allEntries.size
            )
            _latestReport.value = report

            addLog(
                GeminiPingLogEntry(
                    targetEndpoint = "GET /v1beta/models",
                    httpCode = null,
                    latencyMs = latency,
                    status = GeminiKeyStatus.NETWORK_ERROR,
                    statusBadge = "OFFLINE",
                    details = "Chyba sítě: ${e.message ?: "Timeout"}",
                    payloadSnippet = e.localizedMessage
                )
            )

            report
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            Log.e(TAG, "Unexpected diagnostic error", e)
            val report = GeminiDiagnosticReport(
                isConfigured = true,
                isPlaceholder = false,
                maskedKey = masked,
                keyLength = apiKey.length,
                status = GeminiKeyStatus.NETWORK_ERROR,
                httpCode = null,
                latencyMs = latency,
                statusTitle = "NEOČEKÁVANÁ CHYBA DIAGNOSTIKY",
                statusDescription = e.message ?: "Neznámá výjimka",
                quotaState = "Chyba",
                discoveredModels = emptyList(),
                recommendation = "Restartujte diagnostický modul nebo zkontrolujte systémové logy.",
                rawResponseSnippet = e.toString(),
                totalActiveKeysInPool = allEntries.size
            )
            _latestReport.value = report

            addLog(
                GeminiPingLogEntry(
                    targetEndpoint = "GET /v1beta/models",
                    httpCode = null,
                    latencyMs = latency,
                    status = GeminiKeyStatus.NETWORK_ERROR,
                    statusBadge = "ERROR",
                    details = e.message ?: "Neznámá chyba",
                    payloadSnippet = e.toString()
                )
            )

            report
        }
    }

    /**
     * Provede cílený ping na konkrétní model Gemini (např. gemini-3.5-flash) přes aktivní klíč z fondu.
     */
    suspend fun pingPrimaryModel(model: String = "gemini-3.5-flash"): GeminiPingLogEntry = withContext(Dispatchers.IO) {
        val allEntries = OmnisGeminiClient.KeyPool.getAllEntries()
        val apiKey = allEntries.firstOrNull()?.key ?: BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "YOUR_GEMINI_API_KEY") {
            val entry = GeminiPingLogEntry(
                targetEndpoint = "POST /v1beta/models/$model:generateContent",
                httpCode = null,
                latencyMs = 0L,
                status = GeminiKeyStatus.NOT_CONFIGURED,
                statusBadge = "CHYBÍ KLÍČ",
                details = "Test přeskočen - GEMINI_API_KEY není injektován v BuildConfig ani ve fondu.",
                payloadSnippet = null
            )
            addLog(entry)
            return@withContext entry
        }

        val startTime = System.currentTimeMillis()
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "ping") })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", 5)
                    put("temperature", 0.1)
                })
            }

            val body = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val code = response.code
                val bodyStr = response.body?.string().orEmpty()

                val status = when (code) {
                    200 -> GeminiKeyStatus.VALID_ACTIVE
                    402 -> GeminiKeyStatus.CREDITS_DEPLETED_402
                    429 -> GeminiKeyStatus.RATE_LIMITED_429
                    400, 403 -> GeminiKeyStatus.INVALID_KEY_400_403
                    else -> if (code >= 500) GeminiKeyStatus.SERVER_ERROR_5XX else GeminiKeyStatus.INVALID_KEY_400_403
                }

                val snippet = extractErrorMessage(bodyStr)
                val entry = GeminiPingLogEntry(
                    targetEndpoint = "POST /v1beta/models/$model:generateContent",
                    httpCode = code,
                    latencyMs = latency,
                    status = status,
                    statusBadge = if (code == 200) "200 OK (${latency}ms)" else "HTTP $code (${latency}ms)",
                    details = when (code) {
                        200 -> "Úspěšný ping na $model. Klíčový fond aktivní (${allEntries.size} klíčů)."
                        402 -> "HTTP 402 - Vyčerpán předplacený kredit (Prepayment credits depleted)."
                        429 -> "HTTP 429 - Překročen limit požadavků (Rate Limit)."
                        else -> "Server vrátil HTTP $code: $snippet"
                    },
                    payloadSnippet = snippet
                )
                addLog(entry)
                entry
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            val entry = GeminiPingLogEntry(
                targetEndpoint = "POST /v1beta/models/$model:generateContent",
                httpCode = null,
                latencyMs = latency,
                status = GeminiKeyStatus.NETWORK_ERROR,
                statusBadge = "TIMEOUT",
                details = "Chyba spojení s modelem $model: ${e.message}",
                payloadSnippet = e.localizedMessage
            )
            addLog(entry)
            entry
        }
    }

    fun parseProbeResponse(
        code: Int,
        bodyStr: String,
        maskedKey: String,
        keyLength: Int,
        latencyMs: Long
    ): GeminiDiagnosticReport {
        return when (code) {
            200 -> {
                val modelsList = mutableListOf<String>()
                try {
                    val root = JSONObject(bodyStr)
                    val modelsArr = root.optJSONArray("models")
                    if (modelsArr != null) {
                        for (i in 0 until modelsArr.length()) {
                            val modelObj = modelsArr.optJSONObject(i)
                            val name = modelObj?.optString("name")?.removePrefix("models/")
                            if (!name.isNullOrBlank()) {
                                modelsList.add(name)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed parsing models JSON", e)
                }

                GeminiDiagnosticReport(
                    isConfigured = true,
                    isPlaceholder = false,
                    maskedKey = maskedKey,
                    keyLength = keyLength,
                    status = GeminiKeyStatus.VALID_ACTIVE,
                    httpCode = 200,
                    latencyMs = latencyMs,
                    statusTitle = "API KLÍČ JE PLATNÝ A AKTIVNÍ (200 OK)",
                    statusDescription = "Klíč byl úspěšně ověřen vůči Google Generative Language API. K dispozici je ${modelsList.size} modelů.",
                    quotaState = "Aktivní (Standardní / Pay-as-you-go kvóta)",
                    discoveredModels = modelsList.take(6),
                    recommendation = "Systém je plně připraven pro online inferenci. Není vyžadována žádná akce.",
                    rawResponseSnippet = if (modelsList.isNotEmpty()) "Nalezené modely: ${modelsList.take(4).joinToString(", ")}..." else "OK"
                )
            }
            402 -> {
                GeminiDiagnosticReport(
                    isConfigured = true,
                    isPlaceholder = false,
                    maskedKey = maskedKey,
                    keyLength = keyLength,
                    status = GeminiKeyStatus.CREDITS_DEPLETED_402,
                    httpCode = 402,
                    latencyMs = latencyMs,
                    statusTitle = "VYČERPÁN PŘEDPLACENÝ KREDIT (HTTP 402)",
                    statusDescription = "Klíč je syntakticky platný a injektován, ale účet v Google AI Studio vyčerpal předplacené kredity.",
                    quotaState = "0 kreditů (Vyčerpáno / Pozastaveno)",
                    discoveredModels = emptyList(),
                    recommendation = "Přejděte na https://aistudio.google.com/ -> doplňte kredit nebo přidejte náhradní klíč (Slot 2/3).",
                    rawResponseSnippet = extractErrorMessage(bodyStr)
                )
            }
            429 -> {
                GeminiDiagnosticReport(
                    isConfigured = true,
                    isPlaceholder = false,
                    maskedKey = maskedKey,
                    keyLength = keyLength,
                    status = GeminiKeyStatus.RATE_LIMITED_429,
                    httpCode = 429,
                    latencyMs = latencyMs,
                    statusTitle = "PŘEKROČEN RATE LIMIT (HTTP 429)",
                    statusDescription = "Byl dočasně vyčerpán limit požadavků za minutu/den (RPM/RPD kvóta).",
                    quotaState = "Dočasně pozastaveno (Rate Limit)",
                    discoveredModels = emptyList(),
                    recommendation = "Systém automaticky rotuje na další klíč ze slotu 2 nebo 3.",
                    rawResponseSnippet = extractErrorMessage(bodyStr)
                )
            }
            400, 403 -> {
                GeminiDiagnosticReport(
                    isConfigured = true,
                    isPlaceholder = false,
                    maskedKey = maskedKey,
                    keyLength = keyLength,
                    status = GeminiKeyStatus.INVALID_KEY_400_403,
                    httpCode = code,
                    latencyMs = latencyMs,
                    statusTitle = "NEPLATNÝ KLÍČ NEBO CHYBĚJÍCÍ OPRÁVNĚNÍ (HTTP $code)",
                    statusDescription = "Google odmítl klíč jako neplatný nebo pro něj není aktivováno Generative Language API.",
                    quotaState = "Neautorizováno",
                    discoveredModels = emptyList(),
                    recommendation = "Zkontrolujte vložení klíče v Secrets panelu AI Studia.",
                    rawResponseSnippet = extractErrorMessage(bodyStr)
                )
            }
            else -> {
                GeminiDiagnosticReport(
                    isConfigured = true,
                    isPlaceholder = false,
                    maskedKey = maskedKey,
                    keyLength = keyLength,
                    status = if (code >= 500) GeminiKeyStatus.SERVER_ERROR_5XX else GeminiKeyStatus.INVALID_KEY_400_403,
                    httpCode = code,
                    latencyMs = latencyMs,
                    statusTitle = "CHYBA SLUŽBY GOOGLE (HTTP $code)",
                    statusDescription = "Server vrátil neočekávaný kód: HTTP $code.",
                    quotaState = "Neznámý stav",
                    discoveredModels = emptyList(),
                    recommendation = "Zkontrolujte stav služeb Google Cloud Status.",
                    rawResponseSnippet = extractErrorMessage(bodyStr)
                )
            }
        }
    }

    fun maskApiKey(key: String): String {
        if (key.isBlank()) return "[NENÍ NASTAVEN]"
        if (key.length <= 8) return "${key.take(2)}****"
        return "${key.take(6)}...${key.takeLast(4)}"
    }

    fun validateSingleKeyFormat(slot: Int, slotName: String, rawKey: String?): KeyFormatValidationResult {
        val key = rawKey?.trim().orEmpty()
        val isPresent = key.isNotEmpty()
        val isPlaceholder = key == "MY_GEMINI_API_KEY" ||
                key.equals("YOUR_API_KEY_HERE", ignoreCase = true) ||
                key.equals("YOUR_GEMINI_API_KEY", ignoreCase = true) ||
                key.equals("YOUR_GEMINI_API_KEY_2", ignoreCase = true) ||
                key.equals("YOUR_GEMINI_API_KEY_3", ignoreCase = true) ||
                key.equals("YOUR_KEY", ignoreCase = true) ||
                key.equals("TODO", ignoreCase = true)
        val hasValidPrefix = key.startsWith("AIzaSy")
        val hasStandardLength = key.length == 39
        val hasWhitespace = rawKey?.let { it.startsWith(" ") || it.endsWith(" ") } ?: false

        val checklist = listOf(
            KeyChecklistItem(
                title = "Existence v konfiguraci",
                isPassed = isPresent && !isPlaceholder,
                detail = if (!isPresent) "Klíč nebyl nalezen (prázdná hodnota)."
                else if (isPlaceholder) "Obsahuje výchozí placeholder '$key'."
                else "Hodnota pro $slotName nalezena."
            ),
            KeyChecklistItem(
                title = "Google AI Studio Prefix ('AIzaSy')",
                isPassed = isPresent && !isPlaceholder && hasValidPrefix,
                detail = if (hasValidPrefix) "Klíč začíná standardním identifikátorem 'AIzaSy'."
                else "Klíč nemá standardní prefix 'AIzaSy'."
            ),
            KeyChecklistItem(
                title = "Délka klíče (39 znaků)",
                isPassed = isPresent && !isPlaceholder && hasStandardLength,
                detail = "Délka: ${key.length} znaků ${if (hasStandardLength) "(Standardní)" else "(Očekáváno 39 znaků)"}"
            ),
            KeyChecklistItem(
                title = "Sanitizace mezer",
                isPassed = !hasWhitespace,
                detail = if (hasWhitespace) "Varování: Obsahuje mezery na začátku nebo na konci." else "Bez okrajových mezer."
            )
        )

        val isValid = isPresent && !isPlaceholder && hasValidPrefix && hasStandardLength && !hasWhitespace
        val level = when {
            !isPresent || isPlaceholder -> WarningLevel.CRITICAL
            !hasValidPrefix || !hasStandardLength || hasWhitespace -> WarningLevel.WARNING
            else -> WarningLevel.OK
        }

        val (title, msg) = when (level) {
            WarningLevel.CRITICAL -> Pair(
                "⚠️ $slotName NENÍ INJEKTOVÁN",
                "V tomto slotu není platný klíč. Zadejte jej v Secrets panelu AI Studia nebo v nastavení níže."
            )
            WarningLevel.WARNING -> Pair(
                "⚠️ NESTANDARDNÍ FORMÁT PRO $slotName",
                "Klíč byl detekován, ale jeho formát se liší od standardu (39 znaků s prefixem 'AIzaSy')."
            )
            WarningLevel.OK -> Pair(
                "✅ $slotName ÚSPĚŠNĚ INJEKTOVÁN",
                "Klíč byl korektně vložen a splňuje standardy Google AI Studio."
            )
        }

        return KeyFormatValidationResult(
            slot = slot,
            slotName = slotName,
            isPresent = isPresent,
            isPlaceholder = isPlaceholder,
            hasValidPrefix = hasValidPrefix,
            hasStandardLength = hasStandardLength,
            length = key.length,
            maskedKey = maskApiKey(key),
            isValidFormat = isValid,
            warningLevel = level,
            warningTitle = title,
            warningMessage = msg,
            checklistItems = checklist
        )
    }

    fun validateAllKeysFormat(): List<KeyFormatValidationResult> {
        val entries = OmnisGeminiClient.KeyPool.getAllEntries()
        val key1 = entries.find { it.slotNumber == 1 }?.key ?: try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        val key2 = entries.find { it.slotNumber == 2 }?.key ?: try {
            val clazz = Class.forName("com.example.BuildConfig")
            clazz.getField("GEMINI_API_KEY_2").get(null) as? String ?: ""
        } catch (_: Exception) { "" }
        val key3 = entries.find { it.slotNumber == 3 }?.key ?: try {
            val clazz = Class.forName("com.example.BuildConfig")
            clazz.getField("GEMINI_API_KEY_3").get(null) as? String ?: ""
        } catch (_: Exception) { "" }

        return listOf(
            validateSingleKeyFormat(1, "Slot 1 (GEMINI_API_KEY)", key1),
            validateSingleKeyFormat(2, "Slot 2 (GEMINI_API_KEY_2)", key2),
            validateSingleKeyFormat(3, "Slot 3 (GEMINI_API_KEY_3)", key3)
        )
    }

    fun validateBuildConfigKeyFormat(rawKey: String = BuildConfig.GEMINI_API_KEY): KeyFormatValidationResult {
        return validateSingleKeyFormat(1, "GEMINI_API_KEY (Slot 1)", rawKey)
    }

    private fun extractErrorMessage(jsonBody: String): String {
        return try {
            val root = JSONObject(jsonBody)
            val errObj = root.optJSONObject("error")
            errObj?.optString("message") ?: jsonBody.take(200)
        } catch (e: Exception) {
            jsonBody.take(200)
        }
    }
}
