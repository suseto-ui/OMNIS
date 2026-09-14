package com.example.api

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.squareup.moshi.Json

data class SynthesisResult(
    val answer: String,
    val cognitiveProcess: String,
    val followUpQuestions: List<String>,
    val valSys: Float,
    val valEcon: Float,
    val valPsych: Float,
    val valEco: Float,
    val valLaw: Float,
    val valSec: Float,
    val valPhys: Float,
    val valSoc: Float,
    val composite: Float
)

data class ExecutionPlanStep(
    val step_number: Int,
    val action_description: String,
    val tool_call: Map<String, Any>?
)

data class HybridOmnisResponse(
    val intent: String,
    val confidence_score: Float,
    val execution_plan: List<ExecutionPlanStep>,
    val immediate_response: String?,
    val required_output_format: String
)

interface OmnisApiService {
    @POST("api/v1/omnis/process")
    suspend fun processHybridIntent(@Query("user_input") userInput: String): HybridOmnisResponse
}

object OmnisGeminiClient {

    private val BASE_URL = "https://your-omnis-backend-api.com/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .client(OkHttpClient.Builder().apply {
            connectTimeout(15, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
            addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        }.build())
        .build()

    private val apiService = retrofit.create(OmnisApiService::class.java)

    suspend fun synthesize(query: String, domain: String): SynthesisResult = withContext(Dispatchers.IO) {
        try {
            val response = apiService.processHybridIntent(query)
            
            // Transformace hybridní odpovědi na SynthesisResult pro zachování kompatibility UI
            SynthesisResult(
                answer = response.immediate_response ?: "Plán exekuce vytvořen: ${response.execution_plan.size} kroků.",
                cognitiveProcess = "Intent: ${response.intent} (Confidence: ${response.confidence_score}). Plán obsahuje ${response.execution_plan.size} deterministických kroků.",
                followUpQuestions = listOf("Spustit tento exekuční plán?", "Upravit parametry nástrojů?", "Zobrazit detailní kroky?"),
                valSys = 0.9f, valEcon = 0.8f, valPsych = 0.8f, valEco = 0.9f,
                valLaw = 1.0f, valSec = 0.95f, valPhys = 0.85f, valSoc = 0.85f,
                composite = response.confidence_score
            )
        } catch (e: Exception) {
            SynthesisResult(
                answer = "Kritické selhání při komunikaci s O.M.N.I.S. Gateway: ${e.localizedMessage}",
                cognitiveProcess = "Network Failure / Schema Mismatch",
                followUpQuestions = listOf("Restartovat gateway?", "Zkontrolovat logy backendu?"),
                valSys = 0.0f, valEcon = 0.0f, valPsych = 0.0f, valEco = 0.0f,
                valLaw = 0.0f, valSec = 0.0f, valPhys = 0.0f, valSoc = 0.0f,
                composite = 0.0f
            )
        }
    }

}
