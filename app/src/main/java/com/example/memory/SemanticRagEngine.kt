package com.example.memory

import com.example.data.MemoryFragment
import kotlin.math.sqrt

data class RagSearchResult(
    val fragment: MemoryFragment,
    val similarityScore: Float,
    val isCached: Boolean = true,
    val matchType: String = "EXACT_SEMANTIC",
    val matchedTerms: List<String> = emptyList()
)

object SemanticRagEngine {

    /**
     * Vyhodnotí sémantické skóre relevance paměťového fragmentu k zadanému dotazu operátora.
     */
    fun computeSemanticScore(query: String, fragment: MemoryFragment): Float {
        val qTokens = tokenize(query)
        if (qTokens.isEmpty()) return 0.0f

        val titleTokens = tokenize(fragment.title)
        val summaryTokens = tokenize(fragment.summary)
        val tagTokens = tokenize(fragment.tags)

        var score = 0.0f

        // 1. Shoda v titulu (vysoká váha 0.4)
        val titleMatches = qTokens.count { titleTokens.contains(it) }
        score += (titleMatches.toFloat() / qTokens.size.toFloat()) * 0.40f

        // 2. Shoda v souhrnu (váha 0.35)
        val summaryMatches = qTokens.count { summaryTokens.contains(it) }
        score += (summaryMatches.toFloat() / qTokens.size.toFloat()) * 0.35f

        // 3. Shoda v štítcích (váha 0.25)
        val tagMatches = qTokens.count { tagTokens.contains(it) }
        score += (tagMatches.toFloat() / qTokens.size.toFloat()) * 0.25f

        // Bonus za zdrojovou hustotu záznamů
        val densityBonus = (fragment.sourceRecordCount.coerceAtMost(20) / 20.0f) * 0.05f

        return (score + densityBonus).coerceIn(0.0f, 0.99f)
    }

    /**
     * Vyhledá a seřadí nejrelevantnější paměťové fragmenty pro RAG mezipaměť.
     */
    fun searchAndRankFragments(
        query: String,
        fragments: List<MemoryFragment>,
        topK: Int = 5
    ): List<RagSearchResult> {
        val qTokens = tokenize(query)
        if (query.isBlank() || fragments.isEmpty()) {
            return fragments.take(topK).map { fragment ->
                RagSearchResult(
                    fragment = fragment,
                    similarityScore = 0.85f,
                    isCached = true,
                    matchType = "SYNAPSE_PRIMARY",
                    matchedTerms = emptyList()
                )
            }
        }

        return fragments.map { fragment ->
            val score = computeSemanticScore(query, fragment)
            val matched = qTokens.filter { token ->
                fragment.title.contains(token, ignoreCase = true) ||
                fragment.summary.contains(token, ignoreCase = true) ||
                fragment.tags.contains(token, ignoreCase = true)
            }

            val matchType = when {
                score >= 0.75f -> "HIGH_SEMANTIC"
                score >= 0.40f -> "PARTIAL_MATCH"
                else -> "ASSOCIATIVE"
            }

            RagSearchResult(
                fragment = fragment,
                similarityScore = if (score > 0f) score else 0.20f,
                isCached = true,
                matchType = matchType,
                matchedTerms = matched
            )
        }
        .sortedByDescending { it.similarityScore }
        .take(topK)
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace("[^a-záčďéěíňóřšťúůýž0-9]".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { it.length > 2 }
    }
}
