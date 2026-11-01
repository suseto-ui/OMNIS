package com.example.api

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

/**
 * Production-ready Firebase AI Logic service delivering Gemini API inference
 * strictly on the zero-cost free tier (gemini-3.8-flash & gemini-3.5-flash-lite).
 */
object FirebaseAiService {
    private const val TAG = "FirebaseAiService"

    const val MODEL_FLASH = "gemini-3.8-flash"
    const val MODEL_FLASH_LITE = "gemini-3.5-flash-lite"

    private val flashModel by lazy {
        Firebase.ai.generativeModel(MODEL_FLASH)
    }

    private val flashLiteModel by lazy {
        Firebase.ai.generativeModel(MODEL_FLASH_LITE)
    }

    suspend fun generateContent(
        prompt: String,
        useLite: Boolean = false,
        timeoutMs: Long = 60_000L
    ): Result<String> {
        return try {
            val model = if (useLite) flashLiteModel else flashModel
            val response = withTimeout(timeoutMs) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Gemini API vrátilo prázdnou odpověď."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini inference timed out after ${timeoutMs}ms", e)
            Result.failure(IllegalStateException("Generování vypršelo (časový limit $timeoutMs ms). Zkuste to znovu."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic inference failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun generateStream(
        prompt: String,
        useLite: Boolean = false,
        timeoutMs: Long = 60_000L
    ): Flow<String> = flow {
        val model = if (useLite) flashLiteModel else flashModel
        try {
            withTimeout(timeoutMs) {
                model.generateContentStream(prompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        emit(chunkText)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Streaming timed out", e)
            throw IllegalStateException("Streamování odpovědi vypršelo.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Streaming failure", e)
            throw e
        }
    }
}
