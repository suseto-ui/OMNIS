package com.example.domain.usecase

import com.example.data.MemoryFragment
import com.example.data.OmnisRecord
import com.example.data.repository.OmnisRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clean Architecture Use Case pro konsolidaci sémantické paměti a extrakci klíčových fragmentů
 * z proběhlých konverzací a zpráv.
 */
class MemoryConsolidationUseCase {
    suspend operator fun invoke(
        records: List<OmnisRecord>,
        repository: OmnisRecordRepository? = null
    ): ConsolidationResult = withContext(Dispatchers.IO) {
        if (records.isEmpty()) {
            return@withContext ConsolidationResult(emptyList(), "Žádné záznamy k analýze")
        }

        val fragments = mutableListOf<MemoryFragment>()
        val groupedByDomain = records.groupBy { it.domain }

        groupedByDomain.forEach { (domain, domainRecords) ->
            if (domainRecords.size >= 2) {
                val combinedText = domainRecords.takeLast(5).joinToString(" \n") { it.content }
                val summary = if (combinedText.length > 180) combinedText.take(177) + "..." else combinedText

                val fragment = MemoryFragment(
                    id = System.currentTimeMillis() + fragments.size,
                    title = "Kognitivní syntéza: $domain",
                    summary = summary,
                    timestamp = System.currentTimeMillis(),
                    sourceRecordCount = domainRecords.size,
                    tags = "$domain,CONSOLIDATED_MEMORY,AUTONOMOUS"
                )

                fragments.add(fragment)
                repository?.insertFragment(fragment)
            }
        }

        ConsolidationResult(
            consolidatedFragments = fragments,
            message = "Konsolidováno ${fragments.size} paměťových fragmentů z ${records.size} zpráv."
        )
    }
}

data class ConsolidationResult(
    val consolidatedFragments: List<MemoryFragment>,
    val message: String
)
