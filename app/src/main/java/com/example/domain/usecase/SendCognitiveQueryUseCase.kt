package com.example.domain.usecase

import com.example.api.OmnisSemanticCache
import com.example.api.SynthesisResult
import com.example.data.repository.OmnisRecordRepository
import com.example.defense.DefenseTier
import com.example.defense.OmnisConfidenceGate
import com.example.defense.OmnisPromptSanitizer
import com.example.ui.UserExperienceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clean Architecture Use Case pro odeslání a zpracování kognitivního dotazu.
 * Zajišťuje bezpečnostní filtraci (Prompt Defense Sanitizer), sémantickou mezipaměť (Cache)
 * a přípravu kognitivního dotazu pro O.M.N.I.S.
 */
class SendCognitiveQueryUseCase(
    private val repository: OmnisRecordRepository? = null
) {
    suspend operator fun invoke(
        query: String,
        activeDomain: String = "SYSTEMS_INTELLIGENCE",
        userExperienceMode: UserExperienceMode = UserExperienceMode.STANDARD,
        threadId: String = "thread_main",
        threadTitle: String = "Hlavní vlákno",
        userName: String = "operator"
    ): CognitiveQueryResult = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return@withContext CognitiveQueryResult.Empty
        }

        // 1. Bezpečnostní filtrace dotazu (Prompt Sanitizer & Injection Scanner)
        val sanitization = OmnisPromptSanitizer.sanitize(trimmedQuery)
        if (sanitization.injectionDetected) {
            val eval = OmnisConfidenceGate.evaluate(
                generatorScore = 0.30f,
                opponentRiskScore = 0.85f,
                injectionDetected = true,
                hasMissingFields = false,
                isCircuitBreakerTripped = false
            )
            return@withContext CognitiveQueryResult.Blocked(
                reason = "Detekován pokus o prompt injection nebo nepovolené instrukce: ${sanitization.detectedVectors.joinToString()}",
                defenseNotes = eval.defenseNotes
            )
        }

        // 1b. Kontrola stavu jističe: Automatický průchod je zakázán
        val circuitBreaker = com.example.api.OmnisGeminiClient.circuitBreaker
        if (circuitBreaker.getState() == com.example.defense.OmnisCircuitBreaker.State.OPEN) {
            val acceptance = circuitBreaker.evaluateAndAcceptNewInput(trimmedQuery, activeDomain)
            if (acceptance is com.example.defense.OmnisCircuitBreaker.InputAcceptanceResult.Blocked) {
                return@withContext CognitiveQueryResult.Blocked(
                    reason = "Jistič OPEN: Automatický průchod zakázán (${acceptance.reason}). Čekám na nový akceptovaný vstup.",
                    defenseNotes = "Jistič systému je otevřen (OPEN). Vstup zablokován do zadání akceptovaného dotazu."
                )
            }
        }

        // 2. Kontrola sémantické mezipaměti (Semantic Cache)
        val cachedMatch = OmnisSemanticCache.findMatch(trimmedQuery, activeDomain, userExperienceMode)
        if (cachedMatch != null) {
            return@withContext CognitiveQueryResult.Cached(
                synthesisResult = cachedMatch
            )
        }

        CognitiveQueryResult.ProceedToInference(
            query = sanitization.cleanText,
            activeDomain = activeDomain,
            threadId = threadId,
            threadTitle = threadTitle,
            userName = userName
        )
    }
}

sealed class CognitiveQueryResult {
    object Empty : CognitiveQueryResult()
    data class Blocked(val reason: String, val defenseNotes: String) : CognitiveQueryResult()
    data class Cached(val synthesisResult: SynthesisResult) : CognitiveQueryResult()
    data class ProceedToInference(
        val query: String,
        val activeDomain: String,
        val threadId: String,
        val threadTitle: String,
        val userName: String
    ) : CognitiveQueryResult()
}
