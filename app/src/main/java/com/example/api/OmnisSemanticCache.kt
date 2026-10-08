package com.example.api

import android.util.Log
import com.example.telemetry.TelemetryEngine
import com.example.ui.UserExperienceMode

/**
 * High-performance middle-tier semantic cache for cognitive synthesis results.
 * Employs token-level Jaccard & lexical similarity matching to prevent redundant Gemini API calls,
 * protecting API rate limits and providing sub-millisecond offline response times.
 */
object OmnisSemanticCache {

    private const val TAG = "OmnisSemanticCache"
    private const val MAX_CAPACITY = 64
    private const val SIMILARITY_THRESHOLD = 0.88f // 88% token overlap for semantic match
    private const val TTL_MILLIS = 24 * 60 * 60 * 1000L // 24 hours validity

    data class CacheEntry(
        val originalQuery: String,
        val tokens: Set<String>,
        val domain: String,
        val userExperienceMode: UserExperienceMode,
        val timestamp: Long,
        val result: SynthesisResult
    )

    private val cache = object : LinkedHashMap<String, CacheEntry>(MAX_CAPACITY, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
            return size > MAX_CAPACITY
        }
    }

    private val lock = Any()

    /**
     * Attempts to find a matching synthesis result in the semantic cache.
     */
    fun findMatch(query: String, domain: String, mode: UserExperienceMode): SynthesisResult? {
        val clean = query.trim().lowercase()
        if (clean.length < 5) return null

        val queryTokens = tokenize(clean)
        if (queryTokens.isEmpty()) return null

        val now = System.currentTimeMillis()

        synchronized(lock) {
            // First check exact match
            val exactKey = makeKey(clean, domain, mode)
            val exactEntry = cache[exactKey]
            if (exactEntry != null && (now - exactEntry.timestamp) < TTL_MILLIS) {
                logCacheHit(query, exactEntry.originalQuery, 1.0f)
                return exactEntry.result.copy(
                    defenseNotes = appendCacheNote(exactEntry.result.defenseNotes, 1.0f)
                )
            }

            // Semantic token-level similarity search
            for (entry in cache.values) {
                if (entry.domain.equals(domain, ignoreCase = true) && entry.userExperienceMode == mode) {
                    if ((now - entry.timestamp) > TTL_MILLIS) continue

                    val similarity = calculateSimilarity(queryTokens, entry.tokens)
                    if (similarity >= SIMILARITY_THRESHOLD) {
                        logCacheHit(query, entry.originalQuery, similarity)
                        return entry.result.copy(
                            defenseNotes = appendCacheNote(entry.result.defenseNotes, similarity)
                        )
                    }
                }
            }
        }
        return null
    }

    /**
     * Stores a synthesis result in the semantic cache.
     */
    fun put(query: String, domain: String, mode: UserExperienceMode, result: SynthesisResult) {
        val clean = query.trim().lowercase()
        if (clean.length < 5 || result.composite < 0.30f) return

        val tokens = tokenize(clean)
        if (tokens.isEmpty()) return

        val key = makeKey(clean, domain, mode)
        val entry = CacheEntry(
            originalQuery = query,
            tokens = tokens,
            domain = domain,
            userExperienceMode = mode,
            timestamp = System.currentTimeMillis(),
            result = result
        )

        synchronized(lock) {
            cache[key] = entry
        }
        Log.d(TAG, "Cached synthesis result for query: '${query.take(40)}...' [Total entries: ${cache.size}]")
    }

    /**
     * Clears the entire semantic cache.
     */
    fun clear() {
        synchronized(lock) {
            cache.clear()
        }
        Log.i(TAG, "Semantic cache cleared.")
    }

    fun size(): Int = synchronized(lock) { cache.size }

    private fun makeKey(cleanQuery: String, domain: String, mode: UserExperienceMode): String {
        return "${domain.uppercase()}_${mode.name}_${cleanQuery.hashCode()}"
    }

    private fun tokenize(text: String): Set<String> {
        val stopWords = setOf("a", "i", "v", "s", "z", "o", "k", "na", "do", "pro", "je", "se", "to", "jak", "co", "kde", "kdy", "the", "in", "on", "at", "is", "it")
        return text
            .replace(Regex("[^\\p{L}\\p{Nd}\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopWords }
            .toSet()
    }

    private fun calculateSimilarity(setA: Set<String>, setB: Set<String>): Float {
        if (setA.isEmpty() || setB.isEmpty()) return 0f
        val intersection = setA.intersect(setB).size
        val union = setA.union(setB).size
        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }

    private fun logCacheHit(query: String, matchedQuery: String, score: Float) {
        Log.i(TAG, "⚡ SEMANTIC CACHE HIT (${(score * 100).toInt()}%): '$query' matched '$matchedQuery'")
        TelemetryEngine.log(
            type = "CACHE_HIT",
            component = "SemanticCache",
            message = "Kognitivní odpověď nalezena v sémantické cache (${(score * 100).toInt()}% shoda).",
            metadata = "{\"similarity\":$score, \"matchedQuery\":\"${matchedQuery.take(50)}\"}"
        )
    }

    private fun appendCacheNote(originalNotes: String, similarity: Float): String {
        val cacheTag = "[⚡ Sémantická mezipaměť: ${(similarity * 100).toInt()}% shoda]"
        return if (originalNotes.isBlank()) cacheTag else "$originalNotes $cacheTag"
    }
}
