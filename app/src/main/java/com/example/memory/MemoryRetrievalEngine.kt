package com.example.memory

import android.util.Log
import com.example.data.MemoryFragment
import com.example.data.OmnisDao
import kotlinx.coroutines.flow.first

/**
 * Engine pro prediktivní vyhledávání a retrieval paměťových fragmentů
 */
object MemoryRetrievalEngine {
    private const val TAG = "MemoryRetrievalEngine"

    /**
     * Vyhledá relevantní fragmenty na základě dotazu
     */
    suspend fun findRelevantFragments(query: String, dao: OmnisDao): List<MemoryFragment> {
        val allFragments = dao.getAllFragments().first()
        if (allFragments.isEmpty()) return emptyList()

        // V reálném systému by zde probíhalo vektorové vyhledávání (Embeddings).
        // V této implementaci používáme sémantické klíčové slovo a shodu tagů.
        val queryLower = query.lowercase()
        val keywords = queryLower.split(" ", ",", ".").filter { it.length > 3 }

        return allFragments.filter { fragment ->
            val titleMatch = fragment.title.lowercase().contains(queryLower)
            val summaryMatch = fragment.summary.lowercase().contains(queryLower)
            val tagMatch = fragment.tags.split(",").any { tag -> 
                keywords.any { keyword -> tag.lowercase().contains(keyword) || keyword.contains(tag.lowercase()) }
            }
            
            titleMatch || summaryMatch || tagMatch
        }.sortedByDescending { it.timestamp }.take(3) // Bereme max 3 nejrelevantnější
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
                "FRAGMENT [${f.title}]:\n${f.summary}" 
            }}
            --- KONEC PAMĚŤOVÉ INJEKCE ---
        """.trimIndent()
    }
}
