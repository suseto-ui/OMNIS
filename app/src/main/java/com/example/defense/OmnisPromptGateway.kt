package com.example.defense

import android.util.Log
import java.util.regex.Pattern

/**
 * Prompt Gateway & Semantic Elevator:
 * 1. Evaluator (Quality Gate): Heuristická analýza kvality zadaného promptu (délka, akční slovesa, specifikace, nejednoznačnost).
 *    - Skóre >= 0.85: Zralý prompt (Bypass přímo na provedení).
 *    - Skóre < 0.85: Detekována sémantická nejednoznačnost -> aktivace Elevátoru.
 * 2. Elevator (Sémantický Povyšovač): Přepíše chaotický vstup do vysoce strukturovaného zadání
 *    s kontextem, parametry [DOPLŇTE ...] a jasným cílem bez odpovědi na samotný dotaz.
 * 3. Human-in-the-Loop Feedback: Vrácení zpět operátorovi k revizi, editaci a autorizovanému odeslání.
 */
data class PromptQualityEvaluation(
    val confidenceScore: Float, // 0.0 .. 1.0
    val isBypassApproved: Boolean,
    val needsElevation: Boolean,
    val identifiedGaps: List<String>,
    val actionVerbFound: Boolean
)

data class PromptGatewayResult(
    val status: String, // "approved_bypass" | "needs_review"
    val confidenceScore: Float,
    val originalPrompt: String,
    val suggestedPrompt: String,
    val evaluationMessage: String,
    val identifiedGaps: List<String>
)

object OmnisPromptGateway {

    private const val TAG = "OmnisPromptGateway"

    // Klíčová akční slovesa v češtině a angličtině indikující deterministický cíl
    private val ACTION_VERBS = listOf(
        "navrhni", "vytvoř", "implementuj", "analyzuj", "srovnej", "refaktoruj", 
        "optimalizuj", "zkontroluj", "napiš", "definuj", "integruj", "vyhodnoť",
        "design", "create", "implement", "analyze", "compare", "refactor", "optimize"
    )

    // Indikátory technických domén a parametrů
    private val DOMAIN_KEYWORDS = listOf(
        "databáz", "database", "sql", "api", "rest", "grpc", "model", "tenzor",
        "bezpečnost", "security", "architektur", "latenc", "paměť", "výkon",
        "frontend", "backend", "docker", "cloud", "ui", "compose", "kotlin"
    )

    /**
     * 1. FÁZE: Rychlý evaluátor kvality promptu (Quality Gate).
     */
    fun evaluatePromptQuality(rawText: String): PromptQualityEvaluation {
        val text = rawText.trim()
        if (text.isBlank()) {
            return PromptQualityEvaluation(
                confidenceScore = 0.0f,
                isBypassApproved = false,
                needsElevation = true,
                identifiedGaps = listOf("Prázdný vstup bez obsahu."),
                actionVerbFound = false
            )
        }

        val words = text.split("\\s+".toRegex())
        val wordCount = words.size
        val lower = text.lowercase()

        var score = 0.50f
        val gaps = mutableListOf<String>()

        // 1. Hodnocení délky a informační hustoty
        when {
            wordCount < 4 -> {
                score -= 0.30f
                gaps.add("Příliš stručný dotaz postrádající kontext.")
            }
            wordCount in 4..8 -> {
                score -= 0.10f
                gaps.add("Nízká specifikace okrajových podmínek.")
            }
            wordCount in 9..25 -> {
                score += 0.15f
            }
            else -> {
                score += 0.25f // Bohatý kontext
            }
        }

        // 2. Přítomnost jasného akčního slovesa
        val hasActionVerb = ACTION_VERBS.any { lower.contains(it) }
        if (hasActionVerb) {
            score += 0.15f
        } else {
            score -= 0.15f
            gaps.add("Chybí konkrétní akční sloveso nebo cíl operace.")
        }

        // 3. Přítomnost technického/doménového kontextu
        val hasDomainContext = DOMAIN_KEYWORDS.any { lower.contains(it) }
        if (hasDomainContext) {
            score += 0.10f
        } else {
            gaps.add("Neuvedena cílová technologie, modul ani architektura.")
        }

        // 4. Detekce nejednoznačných obecných výrazů (Garbage In indicators)
        val vagueTerms = listOf("něco", "jak to udělat", "pomoz", "funguje to", "nefunguje", "udělej", "proč")
        val containsVague = vagueTerms.any { lower == it || lower.startsWith("$it ") }
        if (containsVague && wordCount < 6) {
            score -= 0.25f
            gaps.add("Detekována vysoká sémantická neurčitost (GIGO riziko).")
        }

        val finalScore = score.coerceIn(0.10f, 1.0f)
        val isApproved = finalScore >= 0.85f

        return PromptQualityEvaluation(
            confidenceScore = finalScore,
            isBypassApproved = isApproved,
            needsElevation = !isApproved,
            identifiedGaps = gaps,
            actionVerbFound = hasActionVerb
        )
    }

    /**
     * 2. FÁZE: Sémantický Povyšovač (Elevator).
     * Převede surový, vágní vstup na vysoce strukturované zadání s parametry a zástupnými symboly.
     */
    fun elevatePromptSemantics(rawText: String, domain: String = "SYSTEMS_INTELLIGENCE"): String {
        val trimmed = rawText.trim()
        val lower = trimmed.lowercase()

        val domainContextLabel = when (domain) {
            "SYSTEMS_INTELLIGENCE" -> "Systémová architektura & integrace"
            "COGNITIVE_REASONING" -> "Kognitivní modely & logika"
            "SECURITY_AUDIT" -> "Zero-Trust bezpečnostní perimetr"
            "DATA_ENGINEERING" -> "Datové toky & persistetní schémata"
            else -> "O.M.N.I.S. Core Synthesis"
        }

        // Deterministické šablonování vylepšeného promptu
        return buildString {
            appendLine("### [SÉMANTICKÉ ZADÁNÍ PRO O.M.N.I.S. CORE]")
            appendLine("**Cílová doména:** $domainContextLabel")
            appendLine("**Výchozí záměr operátora:** \"$trimmed\"")
            appendLine()
            appendLine("#### 🎯 Strukturovaný cíl:")
            if (trimmed.length < 20) {
                appendLine("- Proveď detailní návrh a realizaci pro: **$trimmed**.")
            } else {
                appendLine("- $trimmed")
            }
            appendLine()
            appendLine("#### ⚙️ Požadované technické parametry:")
            appendLine("- **Cílové prostředí / platforma:** [DOPLŇTE NAPŘ. ANDROID KOTLIN / FASTAPI PYTHON]")
            appendLine("- **Datová vrstva & model:** [DOPLŇTE DATABÁZI ČI FORMÁT ENTIT]")
            appendLine("- **Nefunkční požadavky:** Zero-Fluff, deterministické typování, ošetření hraničních stavů.")
            appendLine()
            appendLine("#### 📋 Očekávaný výstupní formát:")
            appendLine("1. Technický rozbor a architektonické rozhodnutí.")
            appendLine("2. Produkční kód bez TODO komentářů.")
            appendLine("3. Validace integrity a bezpečnostní audit.")
        }
    }

    /**
     * Zpracování přes Prompt Gateway – jednotný vstupní bod.
     */
    fun processPromptGateway(rawText: String, domain: String = "SYSTEMS_INTELLIGENCE"): PromptGatewayResult {
        val evaluation = evaluatePromptQuality(rawText)
        return if (evaluation.isBypassApproved) {
            PromptGatewayResult(
                status = "approved_bypass",
                confidenceScore = evaluation.confidenceScore,
                originalPrompt = rawText,
                suggestedPrompt = rawText,
                evaluationMessage = "Prompt vykazuje vysokou kvalitu a specificitu (${(evaluation.confidenceScore * 100).toInt()}%). Odesláno přímo k exekuci.",
                identifiedGaps = emptyList()
            )
        } else {
            val elevated = elevatePromptSemantics(rawText, domain)
            PromptGatewayResult(
                status = "needs_review",
                confidenceScore = evaluation.confidenceScore,
                originalPrompt = rawText,
                suggestedPrompt = elevated,
                evaluationMessage = "Detekována sémantická nejednoznačnost (${(evaluation.confidenceScore * 100).toInt()}%). Vstup byl sémanticky optimalizován pro prevenci GIGO. Zkontrolujte parametry a potvrďte.",
                identifiedGaps = evaluation.identifiedGaps
            )
        }
    }
}
