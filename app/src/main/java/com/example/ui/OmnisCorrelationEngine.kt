package com.example.ui

import androidx.compose.ui.graphics.Color

/**
 * Deterministic domain correlation weights and systemic relationship engine
 * for O.M.N.I.S. multi-domain synthesis and cross-impact analysis.
 */
data class DomainCorrelation(
    val domainA: String,
    val domainB: String,
    val correlation: Float, // -1.0f to +1.0f (negative = friction/trade-off, positive = synergy)
    val impactDescription: String
)

object OmnisCorrelationEngine {

    // Pre-calculated empirical tensor weights representing inter-domain dynamics
    private val CORRELATION_MATRIX = mapOf(
        pairKey("Sys", "Sec") to Pair(0.78f, "Vysoká synergie: modulární architektura usnadňuje zero-trust segmentaci."),
        pairKey("Sys", "Econ") to Pair(0.45f, "Pozitivní synergie: škálovatelnost redukuje jednotkové provozní náklady."),
        pairKey("Sys", "Phys") to Pair(0.62f, "Přímá závislost: systémová architektura je omezena propustností hardware a latencí."),
        pairKey("Econ", "Eco") to Pair(-0.54f, "Tradiční frikce: krátkodobá maximalizace zisku versus regenerativní investice do biosféry."),
        pairKey("Econ", "Sec") to Pair(-0.35f, "Friktivní kompromis: robustní zabezpečení zvyšuje kapitálové a časové výdaje."),
        pairKey("Sec", "Psych") to Pair(-0.48f, "Tenzní pole: striktní restrikce vs. kognitivní komfort a uživatelská autonomie."),
        pairKey("Law", "Sec") to Pair(0.82f, "Kritická synergie: regulatorní shoda (GDPR/NIS2/ISO) přímo vynucuje bezpečnostní kontroly."),
        pairKey("Eco", "Phys") to Pair(0.71f, "Termodynamická vazba: energetická účinnost a minimalizace odpadního tepla přímo šetří ekosystém."),
        pairKey("Psych", "Soc") to Pair(0.85f, "Sociokulturní rezonance: individuální důvěra přímo formuje stabilitu kolektivních struktur."),
        pairKey("Law", "Econ") to Pair(-0.30f, "Nákladová frikce: dodržování předpisů zvyšuje administrativní a auditní režii.")
    )

    private fun pairKey(a: String, b: String): String {
        return if (a <= b) "${a}_$b" else "${b}_$a"
    }

    /**
     * Calculates correlation between two domains.
     * Defaults to baseline mutual relation if not explicitly indexed.
     */
    fun getCorrelation(domainA: String, domainB: String): DomainCorrelation {
        if (domainA == domainB) {
            return DomainCorrelation(domainA, domainB, 1.0f, "Identická doména (100% soulad).")
        }
        val key = pairKey(domainA, domainB)
        val entry = CORRELATION_MATRIX[key]
        return if (entry != null) {
            DomainCorrelation(domainA, domainB, entry.first, entry.second)
        } else {
            DomainCorrelation(domainA, domainB, 0.15f, "Neutrální křížová vazba bez přímého systémového tření.")
        }
    }

    /**
     * Evaluates multiple domains and computes overall tension score & primary friction/synergy.
     */
    fun evaluateCluster(domains: Set<String>): ClusterAnalysis {
        if (domains.size < 2) {
            return ClusterAnalysis(
                averageSynergy = 1.0f,
                hasFriction = false,
                summary = "Vyberte alespoň 2 domény pro křížovou analýzu interferencí."
            )
        }

        val domainList = domains.toList()
        val pairs = mutableListOf<DomainCorrelation>()

        for (i in 0 until domainList.size) {
            for (j in i + 1 until domainList.size) {
                pairs.add(getCorrelation(domainList[i], domainList[j]))
            }
        }

        val avg = pairs.map { it.correlation }.average().toFloat()
        val lowest = pairs.minByOrNull { it.correlation }
        val highest = pairs.maxByOrNull { it.correlation }

        val hasFriction = pairs.any { it.correlation < 0f }
        val summary = when {
            lowest != null && lowest.correlation < -0.3f ->
                "Detekována interference: ${lowest.domainA} vs ${lowest.domainB} (${(lowest.correlation * 100).toInt()}%) - ${lowest.impactDescription}"
            highest != null && highest.correlation > 0.6f ->
                "Silná synergie: ${highest.domainA} + ${highest.domainB} (+${(highest.correlation * 100).toInt()}%) - ${highest.impactDescription}"
            else ->
                "Stabilní rovnováha domén: průměrný index synergie je ${(avg * 100).toInt()}%."
        }

        return ClusterAnalysis(
            averageSynergy = avg,
            hasFriction = hasFriction,
            summary = summary,
            primaryPair = lowest ?: highest
        )
    }

    data class ClusterAnalysis(
        val averageSynergy: Float,
        val hasFriction: Boolean,
        val summary: String,
        val primaryPair: DomainCorrelation? = null
    )
}
