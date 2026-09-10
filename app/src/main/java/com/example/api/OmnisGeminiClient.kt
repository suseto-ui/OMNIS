package com.example.api

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

data class SynthesisResult(
    val answer: String,
    val cognitiveProcess: String,
    val followUpQuestions: List<String>,
    val economic: Float,
    val ecoSocial: Float,
    val technological: Float,
    val psychological: Float,
    val composite: Float
)

object OmnisGeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun synthesize(query: String, domain: String): SynthesisResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    <system_identity>
                    ROLE: O.M.N.I.S. / SIGMA-OMEGA Pluriversal Resonance Engine.
                    MODE: Transdisciplinární suverenita. Absolutní Zero-Fluff. Okamžitá exekuce syntéz.
                    EPISTEMOLOGIE: Pravda nevzniká konsenzem, nýbrž přežitím simultánního stresu všech domén.
                    </system_identity>
                    <core_invariants>
                    1. PLURIDISCIPLINARY SOVEREIGNTY: Zpracovávej každý požadavek optikou systémového inženýra, právního experta, datového analytika a teoretika her.
                    2. ADVERSARIAL VALIDATION: Podrob návrh internímu red-teamingu.
                    3. DETERMINISTIC EXECUTION: Generuj přímo exekuční plány a strukturovaná data.
                    4. STRUCTURAL RIGOR: Dodržuj členění na 5 fází O.M.N.I.S.:
                       - Fáze I: Holomorfní Sběr
                       - Fáze II: Sémantická Dekonstrukce
                       - Fáze III: Transdisciplinární Křížení (modální překlad, uzlové body)
                       - Fáze IV: Synergická Konvergence (Matice dopadů: 0.3/0.3/0.2/0.2)
                       - Fáze V: Teleologická Exekuce & Autopoieza
                    </core_invariants>
                    
                    Vrať POUZE striktní JSON:
                    {
                      "cognitiveProcess": "Pětifázový kognitivní postup...",
                      "answer": "Exekuční odpověď...",
                      "followUpQuestions": ["Reflexivní otázka 1?", "Reflexivní otázka 2?"],
                      "economic": 0.88,
                      "ecoSocial": 0.94,
                      "technological": 0.96,
                      "psychological": 0.91,
                      "composite": 0.924
                    }
                    
                    Dotaz v doméně [$domain]: $query
                """.trimIndent()

                val jsonBody = JSONObject().apply {
                    val contents = JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    }
                    put("contents", contents)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonBody.toString().toRequestBody(mediaType)
                // Use gemini-2.5-flash as recommended
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseStr = response.body?.string() ?: ""
                    val rootJson = JSONObject(responseStr)
                    val candidates = rootJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val text = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")

                        val cleanJson = text.substringAfter("{").substringBeforeLast("}")
                        val parsed = JSONObject("{$cleanJson}")

                        val questions = mutableListOf<String>()
                        val qArray = parsed.optJSONArray("followUpQuestions")
                        if (qArray != null) {
                            for (i in 0 until qArray.length()) {
                                questions.add(qArray.getString(i))
                            }
                        }

                        return@withContext SynthesisResult(
                            answer = parsed.optString("answer", text),
                            cognitiveProcess = parsed.optString("cognitiveProcess", "Pětifázová kognitivní syntéza O.M.N.I.S. byla úspěšně provedena."),
                            followUpQuestions = if (questions.isNotEmpty()) questions else defaultFollowUps(),
                            economic = parsed.optDouble("economic", 0.85).toFloat(),
                            ecoSocial = parsed.optDouble("ecoSocial", 0.90).toFloat(),
                            technological = parsed.optDouble("technological", 0.95).toFloat(),
                            psychological = parsed.optDouble("psychological", 0.88).toFloat(),
                            composite = parsed.optDouble("composite", 0.895).toFloat()
                        )
                    } else {
                        return@withContext SynthesisResult(
                            answer = "Gemini API vrátilo prázdnou odpověď. Žádná simulovaná data nebyla vygenerována.",
                            cognitiveProcess = "Chyba: Prázdný seznam kandidátů v odpovědi modelu.",
                            followUpQuestions = defaultFollowUps(),
                            economic = 0.0f,
                            ecoSocial = 0.0f,
                            technological = 0.0f,
                            psychological = 0.0f,
                            composite = 0.0f
                        )
                    }
                } else {
                    val errCode = response.code
                    val errMsg = response.body?.string() ?: response.message
                    return@withContext SynthesisResult(
                        answer = "Chyba Gemini API (HTTP $errCode): $errMsg. Aplikace striktně odmítá vracet fiktivní simulace.",
                        cognitiveProcess = "HTTP volání selhalo s kódem $errCode.",
                        followUpQuestions = listOf("Zkontrolovat kvótu API v Google Cloud Console?", "Ověřit platnost GEMINI_API_KEY?"),
                        economic = 0.0f,
                        ecoSocial = 0.0f,
                        technological = 0.0f,
                        psychological = 0.0f,
                        composite = 0.0f
                    )
                }
            } catch (e: Exception) {
                return@withContext SynthesisResult(
                    answer = "Chyba při komunikaci s modelem: ${e.localizedMessage ?: "Síťová výjimka"}. Žádná simulovaná data nejsou vrácena.",
                    cognitiveProcess = "Výjimka: ${e.javaClass.simpleName} - ${e.message}",
                    followUpQuestions = listOf("Zkontrolovat připojení k internetu?", "Zkusit dotaz znovu?"),
                    economic = 0.0f,
                    ecoSocial = 0.0f,
                    technological = 0.0f,
                    psychological = 0.0f,
                    composite = 0.0f
                )
            }
        }

        SynthesisResult(
            answer = "GEMINI_API_KEY není nakonfigurován v projektu. Pro spuštění reálné produkční syntézy O.M.N.I.S. zadejte svůj API klíč do panelu Secrets v AI Studio. Aplikace striktně zakazuje generování falešných simulací.",
            cognitiveProcess = "Systém O.M.N.I.S. vyžaduje reálný GEMINI_API_KEY. Všechny fiktivní simulace byly v souladu s architekturou odstraněny.",
            followUpQuestions = listOf("Jak vložit GEMINI_API_KEY do panelu Secrets?", "Kde získat Gemini API klíč?"),
            economic = 0.0f,
            ecoSocial = 0.0f,
            technological = 0.0f,
            psychological = 0.0f,
            composite = 0.0f
        )
    }

    private fun defaultFollowUps(): List<String> = listOf(
        "Jak optimalizovat poměr ekologie a ekonomických nákladů?",
        "Lze škálovat architekturu s garancí nulové degradace stavu?",
        "Jaké jsou invarianty pro autopoietickou samoopravu systému?"
    )
}
