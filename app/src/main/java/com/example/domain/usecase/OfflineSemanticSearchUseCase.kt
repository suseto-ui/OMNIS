package com.example.domain.usecase

import com.example.data.OmnisRecord
import kotlin.math.sqrt

/**
 * Výsledek offline sémantického vyhledávání.
 */
data class SemanticSearchResult(
    val record: OmnisRecord,
    val relevanceScore: Float, // 0.0 - 1.0
    val matchedKeywords: List<String>
)

/**
 * Use Case pro lokální offline sémantické vyhledávání a RAG filtrování
 * ve Znalostním Nexusu a historii zpráv bez nutnosti internetového připojení.
 */
class OfflineSemanticSearchUseCase {

    /**
     * Vyhledá v seznamu záznamů nejvíce sémanticky relevantní položky.
     */
    fun search(
        query: String,
        records: List<OmnisRecord>,
        targetDomain: String? = null,
        limit: Int = 10
    ): List<SemanticSearchResult> {
        if (query.isBlank()) {
            return records.take(limit).map {
                SemanticSearchResult(it, relevanceScore = 1.0f, matchedKeywords = emptyList())
            }
        }

        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return emptyList()

        val results = records.mapNotNull { record ->
            // Filtrování domény, pokud je zadána
            if (targetDomain != null && !record.domain.equals(targetDomain, ignoreCase = true) && targetDomain != "ALL") {
                return@mapNotNull null
            }

            val textToSearch = "${record.content} ${record.cognitiveProcess} ${record.domain}"
            val recordTokens = tokenize(textToSearch)

            val matchedTokens = queryTokens.filter { qToken ->
                recordTokens.any { rToken -> rToken.contains(qToken) || qToken.contains(rToken) }
            }

            if (matchedTokens.isEmpty()) {
                return@mapNotNull null
            }

            // Výpočet skóre relevance: podíl shodných tokenů + bonus za výskyt v doméně + kognitivní váha
            val tokenMatchRatio = matchedTokens.size.toFloat() / queryTokens.size.toFloat()
            val domainBonus = if (record.domain.contains(query, ignoreCase = true)) 0.2f else 0.0f
            val compositeBonus = record.compositeScore * 0.1f

            val finalScore = (tokenMatchRatio * 0.7f + domainBonus + compositeBonus).coerceIn(0.1f, 1.0f)

            SemanticSearchResult(
                record = record,
                relevanceScore = finalScore,
                matchedKeywords = matchedTokens
            )
        }

        return results.sortedByDescending { it.relevanceScore }.take(limit)
    }

    private fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .replace(Regex("[^a-záčďéěíňóřšťúůýž0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.length >= 3 }
            .toSet()
    }
}
