package com.example.domain.usecase

import com.example.data.MemoryFragment
import com.example.data.OmnisRecord
import com.example.domain.vector.LocalVectorEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reprezentace nalezeného výsledku ve Znalostním Nexusu.
 */
data class NexusSearchResult(
    val id: String,
    val title: String,
    val content: String,
    val domain: String,
    val similarityScore: Float,
    val isVectorMatch: Boolean,
    val timestamp: Long
)

/**
 * Use Case pro lokální vektorové sémantické vyhledávání ve Znalostním Nexusu.
 * Umožňuje plnohodnotné offline RAG vyhledávání v paměťových fragmentech a záznamech.
 */
class SearchLocalKnowledgeNexusUseCase {

    suspend fun execute(
        query: String,
        memoryFragments: List<MemoryFragment>,
        records: List<OmnisRecord>,
        isVectorMode: Boolean = true,
        minScoreThreshold: Float = 0.15f
    ): List<NexusSearchResult> = withContext(Dispatchers.Default) {
        if (query.isBlank()) return@withContext emptyList()

        val queryVector = if (isVectorMode) LocalVectorEngine.createEmbedding(query) else null
        val queryLower = query.lowercase().trim()
        val queryKeywords = queryLower.split(Regex("\\s+")).filter { it.length > 2 }

        val results = mutableListOf<NexusSearchResult>()

        // 1. Prohledání paměťových fragmentů Nexusu
        for (fragment in memoryFragments) {
            val content = "${fragment.title} ${fragment.summary} ${fragment.tags}"
            val score = if (isVectorMode && queryVector != null) {
                val fragmentVector = LocalVectorEngine.createEmbedding(content)
                LocalVectorEngine.cosineSimilarity(queryVector, fragmentVector)
            } else {
                var matchCount = 0
                for (kw in queryKeywords) {
                    if (content.lowercase().contains(kw)) matchCount++
                }
                if (queryKeywords.isNotEmpty()) matchCount.toFloat() / queryKeywords.size else 0f
            }

            if (score >= minScoreThreshold || (!isVectorMode && score > 0f)) {
                results.add(
                    NexusSearchResult(
                        id = fragment.id.toString(),
                        title = fragment.title.ifBlank { "Fragment Nexusu" },
                        content = fragment.summary,
                        domain = fragment.tags.ifBlank { "NEXUS" },
                        similarityScore = score,
                        isVectorMatch = isVectorMode,
                        timestamp = fragment.timestamp
                    )
                )
            }
        }

        // 2. Prohledání záznamů historie
        for (record in records) {
            val content = "${record.domain} ${record.cognitiveProcess} ${record.content}"
            val score = if (isVectorMode && queryVector != null) {
                val recordVector = LocalVectorEngine.createEmbedding(content)
                LocalVectorEngine.cosineSimilarity(queryVector, recordVector)
            } else {
                var matchCount = 0
                for (kw in queryKeywords) {
                    if (content.lowercase().contains(kw)) matchCount++
                }
                if (queryKeywords.isNotEmpty()) matchCount.toFloat() / queryKeywords.size else 0f
            }

            if (score >= minScoreThreshold || (!isVectorMode && score > 0f)) {
                results.add(
                    NexusSearchResult(
                        id = record.id.toString(),
                        title = "${record.domain} [${record.cognitiveProcess}]",
                        content = record.content,
                        domain = record.domain,
                        similarityScore = score,
                        isVectorMatch = isVectorMode,
                        timestamp = record.timestamp
                    )
                )
            }
        }

        // Seřazení podle relevance
        results.sortedByDescending { it.similarityScore }
    }
}
