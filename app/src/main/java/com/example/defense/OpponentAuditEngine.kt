package com.example.defense

import android.util.Log

enum class ReliabilityStatus {
    HIGH,          // Confidence >= 0.82
    MEDIUM,        // 0.68 <= Confidence < 0.82
    LOW_WARNING    // Confidence < 0.68
}

data class AuditVerificationResult(
    val confidenceScore: Float,
    val status: ReliabilityStatus,
    val detectedDiscrepancies: List<String>,
    val suggestedClarifications: List<String>,
    val auditSummary: String
)

object OpponentAuditEngine {
    private const val TAG = "OpponentAuditEngine"

    fun verifyResponse(
        prompt: String,
        synthesizedAnswer: String,
        domainScores: Map<String, Float> = emptyMap(),
        isEcoMode: Boolean = false
    ): AuditVerificationResult {
        var baseScore = 0.92f
        val discrepancies = mutableListOf<String>()
        val clarifications = mutableListOf<String>()

        val answerLength = synthesizedAnswer.trim().length
        val lowerAnswer = synthesizedAnswer.lowercase()
        val lowerPrompt = prompt.lowercase()

        // 1. Délková a strukturální prověrka
        if (answerLength < 30) {
            baseScore -= 0.25f
            discrepancies.add("Odpověď je příliš stručná a schází jí požadovaná hloubka.")
            clarifications.add("Můžete uvést konkrétní podrobnosti nebo příklad?")
        }

        // 2. Detekce spekulativních výrazů / indikátorů nejistoty
        val uncertaintyTriggers = listOf(
            "nejsem si jist", "možná", "pravděpodobně nelze", "strojový odhad",
            "nedostatek dat", "chybí informace", "nelze jednoznačně potvrdit"
        )
        val uncertaintyCount = uncertaintyTriggers.count { lowerAnswer.contains(it) }
        if (uncertaintyCount > 0) {
            baseScore -= (0.10f * uncertaintyCount)
            discrepancies.add("Odpověď obsahuje spekulativní výrazy nebo přiznání nedostatečných dat ($uncertaintyCount×).")
        }

        // 3. Kontrola vyváženosti domén 8D AKGE vektoru
        if (domainScores.isNotEmpty()) {
            val minScore = domainScores.values.minOrNull() ?: 0.5f
            val maxScore = domainScores.values.maxOrNull() ?: 0.5f
            val spread = maxScore - minScore
            if (spread > 0.65f) {
                baseScore -= 0.12f
                discrepancies.add("Výrazný rozptyl hodnot mezi doménami 8D vektoru (rozptyl: ${"%.2f".format(spread)}).")
            }
            val lowDomains = domainScores.filter { it.value < 0.35f }.keys
            if (lowDomains.isNotEmpty()) {
                discrepancies.add("Nízké vyhodnocení v doménách: ${lowDomains.joinToString(", ")}.")
            }
        }

        // 4. Soulad s typem dotazu
        if (lowerPrompt.contains("cena") || lowerPrompt.contains("náklady") || lowerPrompt.contains("rozpočet")) {
            if (!lowerAnswer.contains("kč") && !lowerAnswer.contains("czk") && !lowerAnswer.contains("usd") && !lowerAnswer.contains("eur") && !lowerAnswer.contains("cena") && !lowerAnswer.contains("náklad")) {
                baseScore -= 0.18f
                discrepancies.add("Dotaz požadoval finanční/nákladové vyčíslení, ale v odpovědi chybí konkrétní měna nebo částky.")
                clarifications.add("Chcete přesný finanční odhad a specifikaci rozpočtu?")
            }
        }

        if (lowerPrompt.contains("csr") || lowerPrompt.contains("ekolog") || lowerPrompt.contains("esg")) {
            if (!lowerAnswer.contains("csr") && !lowerAnswer.contains("ekolog") && !lowerAnswer.contains("udržitel") && !lowerAnswer.contains("emise")) {
                baseScore -= 0.15f
                discrepancies.add("Dotaz se týkal CSR/ESG, avšak v odpovědi chybí klíčové indikátory udržitelnosti.")
                clarifications.add("Má odpověď podrobněji rozebrat dopad na CSR a životní prostředí?")
            }
        }

        // 5. Doplnění výchozích návrhů upřesnění
        if (clarifications.isEmpty()) {
            if (baseScore < 0.80f) {
                clarifications.add("Chcete doplňující analýzu a konkrétní příklady?")
                clarifications.add("Přejete si upřesnit konkrétní doménu či časový rámec?")
            } else {
                clarifications.add("Zobrazit podrobné rozpadové metriky 8D vektoru?")
                clarifications.add("Generovat navazující akční plán?")
            }
        }

        val finalScore = baseScore.coerceIn(0.20f, 0.99f)
        val status = when {
            finalScore >= 0.82f -> ReliabilityStatus.HIGH
            finalScore >= 0.68f -> ReliabilityStatus.MEDIUM
            else -> ReliabilityStatus.LOW_WARNING
        }

        val summary = if (discrepancies.isEmpty()) {
            "Oponentní audit potvrzuje vysokou věcnou konzistenci a shodu s 8D pravidly AKGE."
        } else {
            "Oponentní audit odhalil potenciální odchylky: ${discrepancies.joinToString("; ")}"
        }

        return AuditVerificationResult(
            confidenceScore = finalScore,
            status = status,
            detectedDiscrepancies = discrepancies,
            suggestedClarifications = clarifications,
            auditSummary = summary
        )
    }
}
