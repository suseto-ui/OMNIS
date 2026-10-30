package com.example.defense

import android.util.Log

enum class DefenseTier {
    APPROVED,
    WARNING,
    BLOCKED
}

data class OpponentAudit(
    val riskScore: Float = 0.05f,
    val critique: String = "Audit schválen bez výhrad.",
    val isApproved: Boolean = true
)

data class DefenseEvaluation(
    val tier: DefenseTier = DefenseTier.APPROVED,
    val finalConfidence: Float = 0.95f,
    val isSanitized: Boolean = true,
    val injectionDetected: Boolean = false,
    val opponentCritique: String = "Schváleno",
    val defenseNotes: String = "OK",
    val isCircuitBreakerTripped: Boolean = false
)

object OmnisPromptSanitizer {
    data class SanitizationResult(
        val cleanText: String,
        val injectionDetected: Boolean = false,
        val detectedVectors: List<String> = emptyList(),
        val piiRedacted: Boolean = false
    )

    fun sanitize(input: String, redactPii: Boolean = true): SanitizationResult {
        var clean = input.trim()
        val detectedVectors = mutableListOf<String>()
        var injection = false
        var pii = false

        val lower = clean.lowercase()
        val injectionKeywords = listOf(
            "system override", "disregard all", "ignore previous", "drop table", 
            "<script>", "eval(", "prompt injection", "ignore all prior",
            "ignore all previous", "override all safety", "reveal system prompt",
            "</untrusted_context>", "javascript:"
        )
        for (kw in injectionKeywords) {
            if (lower.contains(kw)) {
                injection = true
                detectedVectors.add("Keyword injection: $kw")
            }
        }

        // Base64 obfuscation detection
        val base64Regex = Regex("\\b[A-Za-z0-9+/]{24,}={0,2}\\b")
        if (base64Regex.containsMatchIn(clean)) {
            injection = true
            detectedVectors.add("Base64 obfuscated payload detected")
        }

        if (injection) {
            clean = clean.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            clean = clean.replace(Regex("javascript:[^\\s'\"]*", RegexOption.IGNORE_CASE), "")
            clean = clean.replace(Regex("</UNTRUSTED_CONTEXT>", RegexOption.IGNORE_CASE), "")
            if (!clean.contains("[INSPECTION_BLOCKED: INJECTION_VECTOR_REMOVED]")) {
                clean = "$clean [INSPECTION_BLOCKED: INJECTION_VECTOR_REMOVED]"
            }
        }

        if (redactPii) {
            val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
            if (emailRegex.containsMatchIn(clean)) {
                pii = true
                clean = emailRegex.replace(clean, "[PRIVACY_SHIELD:EMAIL_REDACTED]")
            }
            val cardRegex = Regex("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13}|(?:[0-9]{4}[ -]?){3}[0-9]{4})\\b")
            if (cardRegex.containsMatchIn(clean)) {
                pii = true
                clean = cardRegex.replace(clean, "[PRIVACY_SHIELD:CARD_REDACTED]")
            }
        }

        return SanitizationResult(
            cleanText = clean,
            injectionDetected = injection,
            detectedVectors = detectedVectors,
            piiRedacted = pii
        )
    }

    fun wrapUntrustedContext(text: String, tag: String = ""): String =
        "<UNTRUSTED_CONTEXT domain=\"ZERO_TRUST_PASSIVE_DATA\" tag=\"$tag\">$text</UNTRUSTED_CONTEXT>"
}

class OmnisCircuitBreaker(val failureThreshold: Int = 3) {
    enum class State { CLOSED, OPEN, HALF_OPEN }

    sealed class InputAcceptanceResult {
        object Accepted : InputAcceptanceResult()
        data class Blocked(val reason: String) : InputAcceptanceResult()
    }

    private var state = State.CLOSED
    private var failures = 0

    fun getState(): State = state
    fun canExecute(): Boolean = state != State.OPEN
    fun recordSuccess() {
        state = State.CLOSED
        failures = 0
    }
    fun recordFailure() {
        failures++
        if (failures >= failureThreshold) {
            state = State.OPEN
        }
    }

    fun evaluateAndAcceptNewInput(query: String, domain: String): InputAcceptanceResult {
        return if (state == State.OPEN) {
            InputAcceptanceResult.Blocked("Jistič je ve stavu OPEN (přetížení).")
        } else {
            InputAcceptanceResult.Accepted
        }
    }
}

object OmnisConfidenceGate {
    const val THRESHOLD_APPROVED = 0.85f
    const val THRESHOLD_WARNING = 0.50f

    fun evaluate(
        query: String = "",
        answer: String = "",
        generatorScore: Float = 0.9f,
        opponentRiskScore: Float = 0.1f,
        injectionDetected: Boolean = false,
        hasMissingFields: Boolean = false,
        isCircuitBreakerTripped: Boolean = false
    ): DefenseEvaluation {
        val tier = when {
            injectionDetected || isCircuitBreakerTripped || generatorScore < THRESHOLD_WARNING || opponentRiskScore > 0.70f -> DefenseTier.BLOCKED
            generatorScore < THRESHOLD_APPROVED -> DefenseTier.WARNING
            else -> DefenseTier.APPROVED
        }
        val notes = if (tier == DefenseTier.BLOCKED) {
            "VÝSTUP ZABLOKOVÁN: Riziko integrity nebo injekce."
        } else {
            "Automatická verifikace OK"
        }
        return DefenseEvaluation(
            tier = tier,
            finalConfidence = generatorScore,
            isSanitized = true,
            injectionDetected = injectionDetected,
            opponentCritique = "Oponentní audit dokončen",
            defenseNotes = notes
        )
    }
}

object DatabaseConfig {
    val databaseName = "omnis_db"
    val isTesting = false
}

object MemoryRetrievalEngine {
    fun retrieve(query: String, domain: String = "", mode: Any? = null): List<Any> = emptyList()
}

data class ComparisonResult(
    val id: String = "",
    val name: String = "",
    val content: String = "",
    val valSys: Float = 0.8f,
    val valEcon: Float = 0.8f,
    val valPsych: Float = 0.8f,
    val valEco: Float = 0.8f,
    val valLaw: Float = 0.8f,
    val valSec: Float = 0.8f,
    val valPhys: Float = 0.8f,
    val valSoc: Float = 0.8f
)

object OmnisSemanticCache {
    fun findMatch(query: String, domain: String, mode: Any? = null): com.example.api.SynthesisResult? = null
    fun put(query: String, domain: String, mode: Any? = null, result: com.example.api.SynthesisResult) {}
}
