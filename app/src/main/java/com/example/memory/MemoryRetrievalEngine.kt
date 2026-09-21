package com.example.memory

import android.util.Log
import com.example.data.MemoryFragment
import com.example.data.OmnisDao
import kotlinx.coroutines.flow.first
import kotlin.math.exp
import kotlin.math.ln

/**
 * Pokročilý hybridní engine pro prediktivní vyhledávání a retrieval paměťových fragmentů.
 * Využívá:
 * 1. BM25-inspirované TF-IDF skórování lexikální shody v názvu, shrnutí a tazích.
 * 2. Multiplikátory vah: Titul (2.0x), Tagy (2.5x), Tělo (1.0x).
 * 3. Doménovou afinitu (1.5x bonus při shodě s aktivní doménou dotazu).
 * 4. Časový rozpad (Recency Decay) s poločasem 14 dní: e^(-λ * dny).
 * 5. Adaptivní práh relevance pro eliminaci irelevantních halucinací.
 */
object MemoryRetrievalEngine {
    private const val TAG = "MemoryRetrievalEngine"

    private val STOP_WORDS = setOf(
        // České stop-slova
        "a", "i", "k", "o", "s", "u", "v", "z", "je", "se", "na", "to", "pro", "jak",
        "že", "co", "do", "od", "po", "ze", "za", "ale", "byl", "byla", "bylo", "byli",
        "tento", "tato", "toto", "tyto", "jsou", "bude", "budou", "může", "tak", "jen",
        // Anglické stop-slova
        "the", "and", "is", "in", "to", "of", "for", "with", "on", "at", "by", "from",
        "about", "into", "through", "after", "over", "between", "out", "against", "during"
    )

    data class ScoredFragment(
        val fragment: MemoryFragment,
        val score: Float,
        val lexicalScore: Float,
        val recencyMultiplier: Float,
        val domainBonus: Float,
        val matchedTerms: List<String>
    )

    /**
     * Vyhledá relevantní fragmenty na základě dotazu s hybridním skórováním
     */
    suspend fun findRelevantFragments(
        query: String, 
        dao: OmnisDao,
        domain: String = "SYSTEMS_INTELLIGENCE",
        topK: Int = 4,
        minScoreThreshold: Float = 0.25f
    ): List<MemoryFragment> {
        val allFragments = dao.getAllFragments().first()
        if (allFragments.isEmpty()) return emptyList()

        val queryTerms = tokenize(query)
        if (queryTerms.isEmpty()) return emptyList()

        val currentTime = System.currentTimeMillis()
        val scoredList = mutableListOf<ScoredFragment>()

        for (fragment in allFragments) {
            val titleTerms = tokenize(fragment.title)
            val summaryTerms = tokenize(fragment.summary)
            val tagTerms = fragment.tags.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }

            val matchedTerms = mutableListOf<String>()
            var lexicalScore = 0.0f

            for (term in queryTerms) {
                var termMatched = false

                // 1. Shoda v tagu (Váha 2.5)
                val inTag = tagTerms.any { it.contains(term) || term.contains(it) }
                if (inTag) {
                    lexicalScore += 2.5f
                    termMatched = true
                }

                // 2. Shoda v názvu (Váha 2.0)
                val inTitleCount = titleTerms.count { it == term || it.startsWith(term) || term.startsWith(it) }
                if (inTitleCount > 0) {
                    lexicalScore += 2.0f * (1.0f + ln(inTitleCount.toDouble())).toFloat()
                    termMatched = true
                }

                // 3. Shoda v souhrnu (Váha 1.0)
                val inSummaryCount = summaryTerms.count { it == term || it.startsWith(term) || term.startsWith(it) }
                if (inSummaryCount > 0) {
                    lexicalScore += 1.0f * (1.0f + ln(inSummaryCount.toDouble())).toFloat()
                    termMatched = true
                }

                if (termMatched) {
                    matchedTerms.add(term)
                }
            }

            if (lexicalScore <= 0.0f) continue

            // 4. Normalizace lexikálního skóre podle délky dotazu
            val normalizedLexical = (lexicalScore / (queryTerms.size * 1.5f)).coerceIn(0.1f, 3.0f)

            // 5. Časový rozpad (Recency decay) – poločas cca 14 dní (lambda = 0.05 / den)
            val ageInMillis = (currentTime - fragment.timestamp).coerceAtLeast(0L)
            val ageInDays = ageInMillis / (1000.0 * 60.0 * 60.0 * 24.0)
            val recencyMultiplier = exp(-0.04 * ageInDays).toFloat().coerceIn(0.2f, 1.0f)

            // 6. Doménová afinita (Bonus 1.5x)
            val domainNormalized = domain.lowercase().replace("_", " ")
            val domainTokens = domainNormalized.split(" ")
            val matchesDomain = tagTerms.any { tag -> domainTokens.any { tag.contains(it) } } ||
                                fragment.summary.lowercase().contains(domainNormalized)
            val domainBonus = if (matchesDomain) 1.5f else 1.0f

            // Výsledné hybridní skóre
            val totalScore = normalizedLexical * recencyMultiplier * domainBonus

            if (totalScore >= minScoreThreshold) {
                scoredList.add(
                    ScoredFragment(
                        fragment = fragment,
                        score = totalScore,
                        lexicalScore = normalizedLexical,
                        recencyMultiplier = recencyMultiplier,
                        domainBonus = domainBonus,
                        matchedTerms = matchedTerms
                    )
                )
            }
        }

        val topResults = scoredList.sortedByDescending { it.score }.take(topK)
        
        Log.i(TAG, "Hybrid Retrieval: Query='$query', nalezeno ${topResults.size} relevantních fragmentů z ${allFragments.size} celkem.")
        topResults.forEach { 
            Log.d(TAG, "-> Fragment [${it.fragment.title}] Skóre: ${String.format("%.2f", it.score)} (Lex: ${String.format("%.2f", it.lexicalScore)}, Recency: ${String.format("%.2f", it.recencyMultiplier)}, Dom: ${it.domainBonus}), Termíny: ${it.matchedTerms}")
        }

        return topResults.map { it.fragment }
    }

    /**
     * Tokenizuje text s odstraněním stop-slov a interpunkce
     */
    private fun tokenize(text: String): List<String> {
        val sanitized = text.lowercase().replace(Regex("[^a-záčďéěíňóřšťúůýž0-9\\s]"), " ")
        return sanitized.split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length >= 3 && !STOP_WORDS.contains(it) }
    }

    /**
     * Formátuje fragmenty pro injekci do System Promptu
     */
    fun formatForContext(fragments: List<MemoryFragment>): String {
        if (fragments.isEmpty()) return ""
        
        return """
            --- AKTIVOVANÁ DLOUHODOBÁ PAMĚŤ (Knowledge Fragments) ---
            Následující fragmenty byly vyhodnoceny jako vysoce relevantní pro aktuální kontext:
            
            ${fragments.joinToString("\n\n") { f -> 
                "FRAGMENT [${f.title}] (Záznamů: ${f.sourceRecordCount}, Tagy: #${f.tags.replace(",", " #")}):\n${f.summary}" 
            }}
            --- KONEC PAMĚŤOVÉ INJEKCE ---
        """.trimIndent()
    }

    /**
     * Exportuje paměťové fragmenty do formátu JSON-LD (Schema.org / O.M.N.I.S. Knowledge Graph)
     */
    fun exportToJsonLd(fragments: List<MemoryFragment>): String {
        val itemsJson = fragments.joinToString(",\n    ") { f ->
            val escapedTitle = f.title.replace("\"", "\\\"")
            val escapedSummary = f.summary.replace("\"", "\\\"").replace("\n", "\\n")
            val tagsArr = f.tags.split(",").map { "\"${it.trim()}\"" }.joinToString(", ")
            """{
      "@type": "KnowledgeArticle",
      "identifier": "omnis:fragment:${f.id}",
      "headline": "$escapedTitle",
      "articleBody": "$escapedSummary",
      "dateCreated": "${java.time.Instant.ofEpochMilli(f.timestamp)}",
      "keywords": [$tagsArr],
      "interactionStatistic": {
        "@type": "InteractionCounter",
        "userInteractionCount": ${f.sourceRecordCount}
      }
    }"""
        }

        return """{
  "@context": "https://schema.org",
  "@graph": [
    $itemsJson
  ]
}"""
    }
}
