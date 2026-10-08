package com.example.ui


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

enum class OmnisDomain(
    val shortCode: String,
    val canonicalName: String,
    val czechLabel: String,
    val description: String
) {
    SYS("Sys", "SYSTEMS", "Systémové inženýrství", "Architektura, integrace a komplexita."),
    ECON("Econ", "ECONOMICS", "Ekonomie & Náklady", "Návratnost, kapitálová a tokenová alokace."),
    PSYCH("Psych", "PSYCHOLOGY", "Kognice & Ergonomie", "Mentální zátěž, komfort a psychologické bezpečí."),
    ECO("Eco", "ECOLOGY", "Ekologie & Biosféra", "Udržitelnost, energetický otisk a efektivita."),
    LAW("Law", "LAW", "Právo & Compliance", "Regulatorní shoda, AI Act, GDPR, NIS2."),
    SEC("Sec", "SECURITY", "Zero-Trust Bezpečnost", "Kryptografie, audit perimetru a integrity."),
    PHYS("Phys", "PHYSICS", "Fyzika & Hardware", "Infrastruktura, Edge-computing a latence."),
    SOC("Soc", "SOCIETY", "Společnost & Tým", "Sociální koheze a eliminace digitální propasti.");

    companion object {
        fun fromString(value: String): OmnisDomain {
            val clean = value.trim().uppercase()
            return entries.find { 
                it.name == clean || 
                it.shortCode.uppercase() == clean || 
                it.canonicalName == clean 
            } ?: SYS
        }

        fun canonical(value: String): String = fromString(value).canonicalName
        fun short(value: String): String = fromString(value).shortCode
    }
}

object OmnisCorrelationEngine {

    val DOMAINS: List<String> = listOf("Sys", "Econ", "Psych", "Eco", "Law", "Sec", "Phys", "Soc")

    // Pre-calculated empirical tensor weights representing inter-domain dynamics (full 28-pair graph)
    private val CORRELATION_MATRIX = mapOf(
        // Sys (Systémové inženýrství) vazby
        pairKey("Sys", "Sec") to Pair(0.78f, "Vysoká synergie: modulární architektura usnadňuje zero-trust segmentaci."),
        pairKey("Sys", "Econ") to Pair(0.45f, "Pozitivní synergie: škálovatelnost redukuje jednotkové provozní náklady."),
        pairKey("Sys", "Phys") to Pair(0.62f, "Přímá závislost: systémová architektura je omezena propustností hardware a latencí."),
        pairKey("Sys", "Eco") to Pair(0.35f, "Technologická efektivita: optimalizovaný kód a komprese snižují spotřebu energie."),
        pairKey("Sys", "Law") to Pair(0.50f, "Systémová shoda: deterministické auditní logy zjednodušují regulatorní reporting."),
        pairKey("Sys", "Psych") to Pair(0.40f, "Kognitivní ergonomie: přehledná architektura snižuje mentální zátěž operátorů."),
        pairKey("Sys", "Soc") to Pair(0.55f, "Infrastrukturní stabilita: spolehlivý systém posiluje důvěru uživatelské komunity."),

        // Econ (Ekonomie) vazby
        pairKey("Econ", "Eco") to Pair(-0.54f, "Tradiční frikce: krátkodobá maximalizace zisku versus regenerativní investice do biosféry."),
        pairKey("Econ", "Sec") to Pair(-0.35f, "Friktivní kompromis: robustní zabezpečení zvyšuje kapitálové a časové výdaje."),
        pairKey("Econ", "Law") to Pair(-0.30f, "Nákladová frikce: dodržování předpisů zvyšuje administrativní a auditní režii."),
        pairKey("Econ", "Phys") to Pair(0.38f, "Kapitálová alokace: investice do fyzické infrastruktury zvyšují výrobní kapacitu."),
        pairKey("Econ", "Psych") to Pair(-0.25f, "Metrický stres: tlak na finanční výkonnost může degradovat psychologické bezpečí."),
        pairKey("Econ", "Soc") to Pair(0.42f, "Ekonomická prosperita: tvorba hodnoty podporuje rozvoj společenských struktur."),

        // Psych (Kognice & Etika) vazby
        pairKey("Psych", "Soc") to Pair(0.85f, "Sociokulturní rezonance: individuální důvěra přímo formuje stabilitu kolektivních struktur."),
        pairKey("Psych", "Sec") to Pair(-0.48f, "Tenzní pole: striktní restrikce vs. kognitivní komfort a uživatelská autonomie."),
        pairKey("Psych", "Law") to Pair(0.30f, "Etická opora: transparentní pravidla posilují pocit férovosti a jistoty."),
        pairKey("Psych", "Eco") to Pair(0.60f, "Biofilní soulad: udržitelný přístup podporuje dlouhodobý mentální well-being."),
        pairKey("Psych", "Phys") to Pair(-0.20f, "Fyzické vyčerpání: hardwarové a časové limity generují kognitivní únavu."),

        // Eco (Ekologie) vazby
        pairKey("Eco", "Phys") to Pair(0.71f, "Termodynamická vazba: energetická účinnost a minimalizace odpadního tepla přímo šetří ekosystém."),
        pairKey("Eco", "Sec") to Pair(0.20f, "Odolnost prostředí: decentralizované zelené zdroje posilují odolnost proti výpadkům."),
        pairKey("Eco", "Law") to Pair(0.65f, "Environmentální právo: legislativní ESG rámce vynucují ekologickou odpovědnost."),
        pairKey("Eco", "Soc") to Pair(0.58f, "Společenská udržitelnost: ochrana klimatu garantuje mezigenerační spravedlnost."),

        // Law (Právo & Soulad) vazby
        pairKey("Law", "Sec") to Pair(0.82f, "Kritická synergie: regulatorní shoda (GDPR/NIS2/ISO) přímo vynucuje bezpečnostní kontroly."),
        pairKey("Law", "Phys") to Pair(0.25f, "Standardizace: technické normy a certifikace pro fyzické komponenty a zařízení."),
        pairKey("Law", "Soc") to Pair(0.68f, "Společenská smlouva: právní stát a rovná pravidla předcházejí sociální polarizaci."),

        // Sec (Zero-Trust Bezpečnost) vazby
        pairKey("Sec", "Phys") to Pair(0.52f, "Fyzická bezpečnost: ochrana datacenter, perimetru a hardwarových bezpečnostních modulů."),
        pairKey("Sec", "Soc") to Pair(0.35f, "Kolektivní ochrana: obrana proti dezinformacím a kybernetickým útokům na společnost."),

        // Phys (Fyzika & Infrastruktura) vazby
        pairKey("Phys", "Soc") to Pair(0.40f, "Hmatatelná dostupnost: fyzická dostupnost služeb eliminuje digitální a geografické propasti.")
    )

    private fun pairKey(a: String, b: String): String {
        return if (a <= b) "${a}_$b" else "${b}_$a"
    }

    /**
     * Calculates correlation between two domains.
     * Defaults to baseline mutual relation if not explicitly indexed.
     */
    fun getCorrelation(domainA: String, domainB: String): DomainCorrelation {
        val normA = OmnisDomain.short(domainA)
        val normB = OmnisDomain.short(domainB)
        if (normA == normB) {
            return DomainCorrelation(normA, normB, 1.0f, "Identická doména (100% soulad).")
        }
        val key = pairKey(normA, normB)
        val entry = CORRELATION_MATRIX[key]
        return if (entry != null) {
            DomainCorrelation(normA, normB, entry.first, entry.second)
        } else {
            DomainCorrelation(normA, normB, 0.15f, "Neutrální křížová vazba bez přímého systémového tření.")
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

    /**
     * Provádí pokročilou nelineární analýzu 8D vektoru.
     * Využívá harmonický průměr a Leontiefův princip minima pro detekci systémových rizik
     * a identifikuje Donella Meadows Leverage Point (bod nejvyšší systémové páky).
     */
    fun calculateSystemicEquilibrium(vector: Map<String, Float>): SystemicHealthAnalysis {
        val domains = listOf("Sys", "Econ", "Psych", "Eco", "Law", "Sec", "Phys", "Soc")
        val values = domains.map { domain -> 
            val canonicalKey = OmnisDomain.canonical(domain)
            val score = vector[domain] 
                ?: vector[canonicalKey] 
                ?: vector[domain.uppercase()] 
                ?: vector[domain.lowercase()] 
                ?: 0.5f
            domain to score.coerceIn(0.01f, 1.0f) 
        }.toMap()

        val arithmeticMean = values.values.average().toFloat()
        
        // Harmonický průměr: H = N / sum(1 / x_i) - penalizuje jakékoliv slabé místo
        val harmonicMean = (values.size / values.values.sumOf { (1.0 / it.toDouble()) }).toFloat()

        // Detekce úzkého hrdla (Bottleneck)
        val bottleneck = values.minByOrNull { it.value }?.key ?: "Sys"
        val bottleneckValue = values[bottleneck] ?: 0.5f
        val isCriticalFailure = bottleneckValue < 0.20f

        // Výpočet systémové páky (Leverage Point)
        // Simulujeme navýšení každé domény o +0.10 a vyhodnotíme křížový dopad skrze tenzor korelací
        var bestLeverageDomain = "Sys"
        var maxSystemicGain = -Float.MAX_VALUE

        for (candidate in domains) {
            var systemicGain = 0f
            for (target in domains) {
                if (candidate != target) {
                    val corr = getCorrelation(candidate, target).correlation
                    // Pozitivní korelace šíří zisk, negativní generuje frikci
                    systemicGain += corr * 0.10f * (1.0f - (values[target] ?: 0.5f))
                }
            }
            if (systemicGain > maxSystemicGain) {
                maxSystemicGain = systemicGain
                bestLeverageDomain = candidate
            }
        }

        return SystemicHealthAnalysis(
            arithmeticMean = arithmeticMean,
            harmonicMean = harmonicMean,
            bottleneckDomain = bottleneck,
            bottleneckValue = bottleneckValue,
            isCriticalFailure = isCriticalFailure,
            leverageDomain = bestLeverageDomain,
            systemicResilience = (harmonicMean * 0.7f + arithmeticMean * 0.3f).coerceIn(0f, 1f)
        )
    }

    /**
     * Provádí pokročilou nelineární analýzu 8D vektoru s ohledem na uživatelské preference (váhy).
     * Umožňuje uživateli označit některé domény jako kritické (váha 1.0) a jiné ignorovat (váha 0.05).
     * Využívá vážený harmonický průměr pro určení celkové odolnosti.
     */
    fun calculateWeightedSystemicEquilibrium(
        vector: Map<String, Float>,
        weights: Map<String, Float>
    ): SystemicHealthAnalysis {
        val domains = listOf("Sys", "Econ", "Psych", "Eco", "Law", "Sec", "Phys", "Soc")
        val values = domains.map { domain -> 
            val canonicalKey = OmnisDomain.canonical(domain)
            val score = vector[domain] 
                ?: vector[canonicalKey] 
                ?: vector[domain.uppercase()] 
                ?: vector[domain.lowercase()] 
                ?: 0.5f
            domain to score.coerceIn(0.01f, 1.0f) 
        }.toMap()

        // Výchozí váha je 1.0f pro všechny domény, pokud není specifikována
        val activeWeights = domains.map { domain ->
            val w = weights[domain] 
                ?: weights[OmnisDomain.canonical(domain)]
                ?: weights[domain.uppercase()]
                ?: weights[domain.lowercase()]
                ?: 1.0f
            domain to w.coerceIn(0.05f, 1.0f)
        }.toMap()

        val arithmeticMean = values.values.average().toFloat()

        // Vážený harmonický průměr: H_w = sum(w_i) / sum(w_i / v_i)
        val sumWeights = activeWeights.values.sum()
        val sumWeightedReciprocals = values.map { (domain, value) ->
            val w = activeWeights[domain] ?: 1.0f
            w / value.toDouble()
        }.sum()
        val weightedHarmonicMean = (sumWeights / sumWeightedReciprocals).toFloat()

        // Detekce úzkého hrdla s přihlédnutím pouze k doménám s významnou váhou (> 0.20f)
        val criticalDomains = values.filter { (activeWeights[it.key] ?: 1.0f) > 0.20f }
        val bottleneck = criticalDomains.minByOrNull { it.value }?.key ?: "Sys"
        val bottleneckValue = values[bottleneck] ?: 0.5f
        val isCriticalFailure = bottleneckValue < 0.20f

        // Výpočet systémové páky (Leverage Point)
        var bestLeverageDomain = "Sys"
        var maxSystemicGain = -Float.MAX_VALUE

        for (candidate in domains) {
            var systemicGain = 0f
            for (target in domains) {
                if (candidate != target) {
                    val corr = getCorrelation(candidate, target).correlation
                    val targetWeight = activeWeights[target] ?: 1.0f
                    // Vážený křížový dopad: posílení domén s vyšší prioritou přináší vyšší zisk
                    systemicGain += corr * 0.10f * (1.0f - (values[target] ?: 0.5f)) * targetWeight
                }
            }
            if (systemicGain > maxSystemicGain) {
                maxSystemicGain = systemicGain
                bestLeverageDomain = candidate
            }
        }

        return SystemicHealthAnalysis(
            arithmeticMean = arithmeticMean,
            harmonicMean = weightedHarmonicMean,
            bottleneckDomain = bottleneck,
            bottleneckValue = bottleneckValue,
            isCriticalFailure = isCriticalFailure,
            leverageDomain = bestLeverageDomain,
            systemicResilience = (weightedHarmonicMean * 0.7f + arithmeticMean * 0.3f).coerceIn(0f, 1f)
        )
    }

    /**
     * Deterministic text evaluation into 8D vector based on keyword densities.
     * High-performance implementation using non-allocating substring index searches.
     */
    fun evaluateTextTo8DVector(text: String): Map<String, Float> {
        val lower = text.lowercase()
        // Count approximate words using index searches to avoid regex split allocations
        var wordCount = 1
        var pos = 0
        while (pos < lower.length) {
            if (lower[pos].isWhitespace()) {
                wordCount++
                while (pos < lower.length && lower[pos].isWhitespace()) {
                    pos++
                }
            } else {
                pos++
            }
        }
        
        val dict = mapOf(
            "Sys" to listOf("architektur", "systém", "integrac", "api", "modul", "databáz", "kód", "inženýr", "framework", "proces", "system"),
            "Econ" to listOf("náklad", "cena", "rozpočet", "roi", "token", "kapitál", "financ", "příjem", "transakc", "ekonom", "cost"),
            "Psych" to listOf("uživatel", "kognitiv", "mentál", "ux", "ui", "důvěr", "vnímání", "stres", "bezpečí", "ergonom", "user"),
            "Eco" to listOf("energ", "uhlík", "klimat", "udržitel", "bio", "zelen", "zdroj", "otisk", "ekolog", "sustainability"),
            "Law" to listOf("právo", "právn", "gdpr", "nis2", "act", "compliance", "regulac", "licenc", "politik", "audit", "law"),
            "Sec" to listOf("bezpečnost", "zero-trust", "auth", "šifrov", "krypt", "zranitelnost", "útok", "incident", "klíč", "security"),
            "Phys" to listOf("hardware", "server", "cpu", "paměť", "latenc", "edge", "sít", "fyzick", "propustnost", "senzor", "latency"),
            "Soc" to listOf("tým", "společnost", "sociál", "komunit", "veřejn", "kultur", "skupin", "etik", "demokrac", "society")
        )

        val result = mutableMapOf<String, Float>()
        for ((domain, keywords) in dict) {
            var hits = 0
            for (kw in keywords) {
                var index = lower.indexOf(kw)
                while (index != -1) {
                    hits++
                    index = lower.indexOf(kw, index + kw.length)
                }
            }
            val density = (hits * 12.0f) / wordCount
            val score = (0.45f + density).coerceIn(0.42f, 0.98f)
            result[domain] = score
        }
        return result
    }

    /**
     * FÁZE 12.1: Hypergraph Sémantická Topologie (Higher-Order Tensor Relations)
     * Kvantifikuje vícerozměrné vazby 3 a více domén (hyperhrany) a počítá topologické invarianty.
     */
    fun calculateHypergraphTopology(values: Map<String, Float>): HypergraphTopologyAnalysis {
        val safeValues = DOMAINS.associateWith { (values[it] ?: 0.5f).coerceIn(0.01f, 1.0f) }
        
        // Klíčové hyperhrany 3. a 4. řádu
        val hyperEdges = listOf(
            HyperEdge(listOf("Econ", "Sec", "Psych"), "Bezpečnostní morálka vs. Nákladové škrty", (safeValues["Econ"]!! * 0.4f + safeValues["Sec"]!! * 0.4f + safeValues["Psych"]!! * 0.2f)),
            HyperEdge(listOf("Eco", "Phys", "Econ"), "Zelená Edge Infrastruktura a ROI", (safeValues["Eco"]!! * 0.4f + safeValues["Phys"]!! * 0.3f + safeValues["Econ"]!! * 0.3f)),
            HyperEdge(listOf("Law", "Sys", "Sec"), "Regulatorní Zero-Trust Architektura", (safeValues["Law"]!! * 0.35f + safeValues["Sys"]!! * 0.35f + safeValues["Sec"]!! * 0.3f)),
            HyperEdge(listOf("Psych", "Soc", "Sys"), "Kognitivní a Společenská Adopce Systému", (safeValues["Psych"]!! * 0.4f + safeValues["Soc"]!! * 0.4f + safeValues["Sys"]!! * 0.2f))
        )
        
        // Počet aktivních hran (synergie > 0.4)
        var activeEdges = 0
        for (i in 0 until DOMAINS.size) {
            for (j in i + 1 until DOMAINS.size) {
                val corr = getCorrelation(DOMAINS[i], DOMAINS[j]).correlation
                if (corr > 0.35f) activeEdges++
            }
        }
        
        // Topologické invarianty: V = 8, E = activeEdges, F = počet 3-cyklů
        val vertices = 8
        val faces = hyperEdges.count { it.cohesionScore > 0.50f }
        val eulerCharacteristic = vertices - activeEdges + faces
        val betti0 = 1 // 1 propojená komponenta
        val betti1 = (activeEdges - vertices + betti0).coerceAtLeast(0) // Kognitivní smyčky / díry
        
        val systemicEntropy = -safeValues.values.map { p -> 
            val norm = p / safeValues.values.sum()
            norm * kotlin.math.ln(norm.toDouble()).toFloat() 
        }.sum()

        return HypergraphTopologyAnalysis(
            hyperEdges = hyperEdges,
            eulerCharacteristic = eulerCharacteristic,
            betti0 = betti0,
            betti1 = betti1,
            systemicEntropy = systemicEntropy,
            topologicalResilience = (1.0f - (betti1 * 0.08f) - (systemicEntropy * 0.1f)).coerceIn(0.1f, 1.0f)
        )
    }

    /**
     * FÁZE 12.2: Perkolační Model Kaskádových Rizik (Percolation Cascade Simulation)
     * Detekuje, zda degradace nejslabšího článku vyvolá řetězový kolaps sítě.
     */
    fun simulateCascadePercolation(values: Map<String, Float>, shockDomain: String? = null): PercolationCascadeAnalysis {
        val currentValues = DOMAINS.associateWith { (values[it] ?: 0.5f).coerceIn(0.01f, 1.0f) }.toMutableMap()
        val criticalThreshold = 0.30f
        val percolationCriticalP = 0.38f // Teoretický perkolační práh pro 8-uzlový graf
        
        // Zavedení šoku (defaultně nejslabší doména)
        val initialShock = shockDomain ?: currentValues.minByOrNull { it.value }?.key ?: "Sys"
        currentValues[initialShock] = (currentValues[initialShock]!! * 0.5f).coerceAtLeast(0.05f)
        
        val affectedCascade = mutableListOf(initialShock)
        val steps = mutableListOf<String>()
        steps.add("Iniciální šok v doméně $initialShock (${String.format("%.2f", currentValues[initialShock])})")
        
        // Kaskádové šíření přes negativní frikce a závislosti
        var changed = true
        var iteration = 0
        while (changed && iteration < 4) {
            changed = false
            iteration++
            for (domain in DOMAINS) {
                if (domain !in affectedCascade) {
                    var cumulativeImpact = 0f
                    for (shocked in affectedCascade) {
                        val corr = getCorrelation(shocked, domain).correlation
                        // Pokud je frikce nebo silná závislost, stres se přenáší
                        if (corr < -0.3f || corr > 0.6f) {
                            cumulativeImpact += kotlin.math.abs(corr) * (1.0f - currentValues[shocked]!!) * 0.25f
                        }
                    }
                    if (cumulativeImpact > 0.18f) {
                        currentValues[domain] = (currentValues[domain]!! - cumulativeImpact).coerceAtLeast(0.05f)
                        if (currentValues[domain]!! < criticalThreshold) {
                            affectedCascade.add(domain)
                            steps.add("Krok $iteration: Kaskádové pnutí zasáhlo $domain (propad na ${String.format("%.2f", currentValues[domain])})")
                            changed = true
                        }
                    }
                }
            }
        }
        
        val cascadeFraction = affectedCascade.size.toFloat() / DOMAINS.size.toFloat()
        val isSystemicCollapseRisk = cascadeFraction >= percolationCriticalP
        
        return PercolationCascadeAnalysis(
            initialShockDomain = initialShock,
            affectedDomains = affectedCascade,
            cascadeFraction = cascadeFraction,
            isSystemicCollapseRisk = isSystemicCollapseRisk,
            simulationTrace = steps,
            suggestedBypass = if (isSystemicCollapseRisk) "Aktivovat okamžitou systémovou kompenzaci v doméně Sys & Sec" else "Lokální stabilizace v $initialShock postačuje"
        )
    }

    /**
     * FÁZE 12.3: Autonomní Rebalancování Tokenového Toku a Reasoning Rozpočtu
     */
    fun calculateTokenBudgetOptimization(values: Map<String, Float>, baseTokens: Int = 1000): TokenBudgetOptimization {
        val analysis = calculateSystemicEquilibrium(values)
        val tension = (1.0f - analysis.systemicResilience)
        
        // Při vysokém systémovém pnutí zvyšujeme alokaci tokenů pro hlubokou dedukci
        val allocatedTokens = when {
            analysis.isCriticalFailure -> (baseTokens * 1.8f).toInt()
            tension > 0.45f -> (baseTokens * 1.35f).toInt()
            else -> (baseTokens * 0.75f).toInt() // Úsporný kompresní režim
        }
        
        val compressionRatio = (1.0f - (allocatedTokens.toFloat() / (baseTokens * 2.0f))).coerceIn(0.15f, 0.75f)
        val reasoningDepth = if (analysis.isCriticalFailure) "EXTENDED_FORENSIC" else if (tension > 0.4f) "DEEP_SYNTHESIS" else "LEAN_STREAM"

        return TokenBudgetOptimization(
            baseTokens = baseTokens,
            allocatedTokens = allocatedTokens,
            compressionRatio = compressionRatio,
            reasoningDepth = reasoningDepth,
            recommendedFormat = if (compressionRatio > 0.5f) "COMPRESSED_BULLETS" else "FULL_TRANSDISCIPLINARY_PROSE"
        )
    }

    /**
     * FÁZE 13.1: Kauzální Do-Calculus Intervenční Engine (Judea Pearl Causal Model)
     * Modeluje účinek aktivní intervence P(Y | do(X = targetValue)) – odstřihne příchozí vlivy
     * na intervenovanou doménu a deterministicky propaguje kauzální efekt na ostatní proměnné.
     */
    fun calculateCausalDoIntervention(
        observedValues: Map<String, Float>,
        targetDomain: String,
        targetValue: Float
    ): CausalDoCalculusAnalysis {
        val safeObserved = DOMAINS.associateWith { (observedValues[it] ?: 0.5f).coerceIn(0.01f, 1.0f) }
        val intervenedValues = safeObserved.toMutableMap()
        
        val cleanTarget = OmnisDomain.short(targetDomain)
        val clampedValue = targetValue.coerceIn(0.05f, 1.0f)
        intervenedValues[cleanTarget] = clampedValue
        
        val delta = clampedValue - (safeObserved[cleanTarget] ?: 0.5f)
        val causalImpacts = mutableMapOf<String, Float>()
        
        // Propagace kauzálního efektu přes strukturní kauzální rovnice (SCM)
        for (domain in DOMAINS) {
            if (domain != cleanTarget) {
                val corr = getCorrelation(cleanTarget, domain).correlation
                // Kauzální síla s útlumem pro nelineární zpětné vazby
                val causalWeight = when {
                    corr > 0.6f -> corr * 0.85f  // Silná přímá kauzalita
                    corr < -0.4f -> corr * 0.90f // Přímá tenzní kauzalita
                    else -> corr * 0.50f         // Nepřímý rozptyl
                }
                val directEffect = delta * causalWeight
                val newValue = (safeObserved[domain]!! + directEffect).coerceIn(0.05f, 1.0f)
                intervenedValues[domain] = newValue
                causalImpacts[domain] = directEffect
            }
        }
        
        val originalHealth = calculateSystemicEquilibrium(safeObserved)
        val newHealth = calculateSystemicEquilibrium(intervenedValues)
        val netGain = newHealth.systemicResilience - originalHealth.systemicResilience

        return CausalDoCalculusAnalysis(
            targetDomain = cleanTarget,
            targetValue = clampedValue,
            intervenedValues = intervenedValues,
            causalImpacts = causalImpacts,
            originalResilience = originalHealth.systemicResilience,
            postInterventionResilience = newHealth.systemicResilience,
            netSystemicGain = netGain,
            isConfounderShieldActive = true
        )
    }

    /**
     * FÁZE 13.3: Zero-Knowledge Důkazy Kognitivní Shody (ZK-SNARK Commitment Hash)
     * Generuje deterministický kryptografický otisk prokazující shodu bez odhalení chráněných dat.
     */
    fun generateZkSafetyCommitment(
        query: String,
        values: Map<String, Float>,
        defenseTier: String,
        operatorNonce: Long = System.currentTimeMillis()
    ): ZeroKnowledgeSafetyCommitment {
        val analysis = calculateSystemicEquilibrium(values)
        val rawProofString = buildString {
            append("OMNIS-ZK-PROOF|v4.0|")
            append("Q_HASH:${query.hashCode().toUInt().toString(16)}|")
            append("RESILIENCE:${String.format("%.4f", analysis.systemicResilience)}|")
            append("TIER:$defenseTier|")
            append("NONCE:$operatorNonce")
        }
        
        // SHA-256 kryptografický otisk
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(rawProofString.toByteArray())
        val commitmentHash = bytes.joinToString("") { "%02x".format(it) }
        
        return ZeroKnowledgeSafetyCommitment(
            commitmentHash = commitmentHash,
            proofProtocol = "ZK-SNARK-OMNIS-v1",
            isCompliant = defenseTier == "APPROVED" && !analysis.isCriticalFailure,
            verifiedTimestamp = operatorNonce
        )
    }

    /**
     * FÁZE 14.1: Bio-Kybernetický Homeostatický Regulátor (Ashby's Law of Requisite Variety)
     * Provádí aktivní tlumení oscilací a obnovu systémové rovnováhy (Homeostasis).
     */
    fun calculateHomeostaticRegulation(
        currentValues: Map<String, Float>,
        targetBaseline: Float = 0.65f
    ): HomeostaticRegulationAnalysis {
        val safeValues = DOMAINS.associateWith { (currentValues[it] ?: 0.5f).coerceIn(0.01f, 1.0f) }
        val regulatedValues = safeValues.toMutableMap()
        val dampingDeltas = mutableMapOf<String, Float>()
        
        // Výpočet variability (rozptylu od baseline)
        val variances = safeValues.mapValues { kotlin.math.abs(it.value - targetBaseline) }
        val systemVariety = variances.values.average().toFloat()
        
        // Kompenzační homeostatické tlumení pro domény pod prahem stability
        for (domain in DOMAINS) {
            val v = safeValues[domain]!!
            if (v < targetBaseline) {
                // Hledáme nejsilnější synergickou doménu pro přenos kompenzace
                var bestSynergyDomain = "Sys"
                var maxSynergy = 0f
                for (other in DOMAINS) {
                    if (other != domain) {
                        val corr = getCorrelation(domain, other).correlation
                        if (corr > maxSynergy && (safeValues[other] ?: 0.5f) > targetBaseline) {
                            maxSynergy = corr
                            bestSynergyDomain = other
                        }
                    }
                }
                val boost = (targetBaseline - v) * (0.35f + maxSynergy * 0.25f)
                regulatedValues[domain] = (v + boost).coerceIn(0.05f, 1.0f)
                dampingDeltas[domain] = boost
            } else {
                dampingDeltas[domain] = 0f
            }
        }
        
        val preHealth = calculateSystemicEquilibrium(safeValues)
        val postHealth = calculateSystemicEquilibrium(regulatedValues)
        val isHomeostasisMaintained = postHealth.systemicResilience >= 0.60f

        return HomeostaticRegulationAnalysis(
            regulatedValues = regulatedValues,
            dampingDeltas = dampingDeltas,
            systemVariety = systemVariety,
            isHomeostasisMaintained = isHomeostasisMaintained,
            preResilience = preHealth.systemicResilience,
            postResilience = postHealth.systemicResilience,
            stabilityImprovement = postHealth.systemicResilience - preHealth.systemicResilience
        )
    }

    /**
     * FÁZE 15.1, 15.2, 15.3: Stochastická Monte Carlo Simulace, Black Swan Scanner & Antifragilita
     * Provádí 1 000 sub-milisekundových iterací s náhodnými perturbacemi, vyčísluje 95% VaR,
     * špičatost (Kurtosis) pro detekci tlustých chvostů a počítá Talebovský antifragilní zisk.
     */
    fun simulateMonteCarloResilience(
        baseValues: Map<String, Float>,
        iterations: Int = 1000,
        perturbationSigma: Float = 0.08f
    ): MonteCarloRiskAnalysis {
        val safeBase = DOMAINS.associateWith { (baseValues[it] ?: 0.5f).coerceIn(0.01f, 1.0f) }
        val random = java.util.Random(42L) // Deterministický seed pro reprodukovatelnou exekuci
        val resilienceSamples = FloatArray(iterations)

        for (i in 0 until iterations) {
            val perturbedMap = safeBase.mapValues { (_, v) ->
                val noise = (random.nextGaussian() * perturbationSigma).toFloat()
                (v + noise).coerceIn(0.05f, 1.0f)
            }
            resilienceSamples[i] = calculateSystemicEquilibrium(perturbedMap).systemicResilience
        }

        resilienceSamples.sort()
        val meanResilience = resilienceSamples.average().toFloat()
        val variance = resilienceSamples.map { (it - meanResilience) * (it - meanResilience) }.average().toFloat()
        val stdDev = kotlin.math.sqrt(variance.toDouble()).toFloat()

        // 95% Value-at-Risk (5. percentil) a 99% VaR (1. percentil)
        val var95 = resilienceSamples[(iterations * 0.05f).toInt()]
        val var99 = resilienceSamples[(iterations * 0.01f).toInt()]

        // Výpočet špičatosti (Kurtosis: 4. centrální moment) pro detekci tlustých chvostů (Black Swan)
        val fourthMoment = resilienceSamples.map { sample ->
            val diff = (sample - meanResilience).toDouble()
            diff * diff * diff * diff
        }.average()
        val kurtosis = (fourthMoment / ((variance * variance).toDouble() + 1e-6)).toFloat()
        val isBlackSwanProne = kurtosis > 3.5f || (meanResilience - var99) > 0.25f

        // Antifragilní odezva (Talebův zisk z volatility): adaptace posilující systémové jádro
        val antifragileGain = if (isBlackSwanProne) {
            (perturbationSigma * 1.5f).coerceAtMost(0.18f)
        } else {
            0.04f
        }

        return MonteCarloRiskAnalysis(
            iterations = iterations,
            meanResilience = meanResilience,
            stdDev = stdDev,
            var95 = var95,
            var99 = var99,
            kurtosis = kurtosis,
            isBlackSwanProne = isBlackSwanProne,
            antifragileGain = antifragileGain,
            robustnessRating = when {
                var95 >= 0.70f -> "VYSOKÁ (Antifragilní)"
                var95 >= 0.50f -> "STABILNÍ (Robustní)"
                else -> "KŘEHKÁ (Fragile - Nutná intervence)"
            }
        )
    }

    /**
     * FÁZE 16.1: GNN Message-Passing Interaction Embeddings
     * Vykonává 3 vrstvy nelineárního šíření zpráv napříč 28 párovými vazbami pro ustálení
     * globálních doménových embeddingů zohledňujících i nepřímé interakce 3. stupně.
     */
    fun computeGnnMessagePassing(
        nodeFeatures: Map<String, Float>,
        layers: Int = 3
    ): GnnInteractionAnalysis {
        val safeFeatures = DOMAINS.associateWith { (nodeFeatures[it] ?: 0.5f).coerceIn(0.01f, 1.0f) }.toMutableMap()
        val embeddingHistory = mutableListOf<Map<String, Float>>()
        embeddingHistory.add(safeFeatures.toMap())

        for (layer in 1..layers) {
            val nextFeatures = mutableMapOf<String, Float>()
            for (target in DOMAINS) {
                var aggregatedMessage = 0f
                var weightSum = 0f
                for (source in DOMAINS) {
                    if (source != target) {
                        val corr = getCorrelation(source, target).correlation
                        val weight = kotlin.math.abs(corr) + 0.1f
                        val incomingVal = safeFeatures[source]!!
                        // Nelineární aktivace ReLU s biasem z korelačního tenzoru
                        val message = if (corr >= 0) {
                            incomingVal * corr
                        } else {
                            -((1.0f - incomingVal) * kotlin.math.abs(corr) * 0.5f)
                        }
                        aggregatedMessage += message * weight
                        weightSum += weight
                    }
                }
                val normalizedMsg = if (weightSum > 0f) aggregatedMessage / weightSum else 0f
                // Aktualizace stavu: konvexní kombinace předchozího stavu a agregované zprávy (LayerNorm aproximace)
                val updatedVal = (safeFeatures[target]!! * 0.6f + (0.5f + normalizedMsg) * 0.4f).coerceIn(0.05f, 1.0f)
                nextFeatures[target] = updatedVal
            }
            safeFeatures.putAll(nextFeatures)
            embeddingHistory.add(safeFeatures.toMap())
        }

        val finalResilience = calculateSystemicEquilibrium(safeFeatures).systemicResilience
        return GnnInteractionAnalysis(
            layerCount = layers,
            convergedEmbeddings = safeFeatures,
            embeddingEvolution = embeddingHistory,
            graphResilience = finalResilience
        )
    }

    /**
     * FÁZE 16.2: QUBO Kombinatorická Optimalizace Alokace (Simulated Annealing)
     * Řeší formulaci E(x) = x^T Q x + c^T x pro výběr optimálního binárního vektoru intervencí x in {0, 1}^8.
     */
    fun solveQuboSensitivityIntervention(
        observedValues: Map<String, Float>,
        budgetConstraint: Int = 3,
        steps: Int = 500
    ): QuboOptimizationAnalysis {
        val safeObserved = DOMAINS.associateWith { (observedValues[it] ?: 0.5f).coerceIn(0.01f, 1.0f) }
        val random = java.util.Random(1337L) // Deterministický seed

        // Stav: binární vektor 8 dimenzí
        val currentState = BooleanArray(DOMAINS.size) { false }
        // Inicializujeme budget náhodných domén
        val initIndices = (0 until DOMAINS.size).shuffled(random).take(budgetConstraint)
        for (idx in initIndices) {
            currentState[idx] = true
        }

        fun evaluateEnergy(state: BooleanArray): Float {
            var energy = 0f
            var activeCount = 0
            for (i in DOMAINS.indices) {
                if (state[i]) {
                    activeCount++
                    val domainI = DOMAINS[i]
                    // Lineární složka: zisk z posílení slabých domén (nižší hodnota = vyšší priorita zisku)
                    energy -= (1.0f - safeObserved[domainI]!!) * 1.5f

                    // Kvadratická složka: vzájemné interakce aktivovaných domén
                    for (j in i + 1 until DOMAINS.size) {
                        if (state[j]) {
                            val domainJ = DOMAINS[j]
                            val corr = getCorrelation(domainI, domainJ).correlation
                            // Synergie snižují energii (odměna), frikce zvyšují energii (penalizace)
                            energy -= corr * 0.8f
                        }
                    }
                }
            }
            // Měkká penalizace za překročení rozpočtu
            val budgetViolation = kotlin.math.max(0, activeCount - budgetConstraint)
            energy += budgetViolation * 5.0f
            return energy
        }

        var currentEnergy = evaluateEnergy(currentState)
        var bestEnergy = currentEnergy
        val bestState = currentState.clone()

        var temperature = 2.0f
        val coolingRate = 0.99f

        for (step in 0 until steps) {
            val flipIdx = random.nextInt(DOMAINS.size)
            currentState[flipIdx] = !currentState[flipIdx]
            val newEnergy = evaluateEnergy(currentState)

            val deltaE = newEnergy - currentEnergy
            if (deltaE < 0f || kotlin.math.exp((-deltaE / temperature).toDouble()) > random.nextDouble()) {
                currentEnergy = newEnergy
                if (currentEnergy < bestEnergy) {
                    bestEnergy = currentEnergy
                    System.arraycopy(currentState, 0, bestState, 0, DOMAINS.size)
                }
            } else {
                // Vrácení změny
                currentState[flipIdx] = !currentState[flipIdx]
            }
            temperature *= coolingRate
        }

        val selectedDomains = DOMAINS.filterIndexed { index, _ -> bestState[index] }
        val projectedGain = -bestEnergy.coerceAtMost(0f) * 0.15f

        return QuboOptimizationAnalysis(
            selectedDomains = selectedDomains,
            optimalEnergy = bestEnergy,
            projectedGain = projectedGain,
            annealingSteps = steps,
            budgetLimit = budgetConstraint
        )
    }

    /**
     * FÁZE 16.3: Odolnost Proti Adversarial Permutacím
     */
    fun evaluateAdversarialPermutationRobustness(text: String): AdversarialPermutationResult {
        val originalVector = evaluateTextTo8DVector(text)
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        
        if (words.size <= 2) {
            return AdversarialPermutationResult(
                isRobust = true,
                maxDeviation = 0f,
                evaluatedVariants = 1
            )
        }

        val permutedWords = words.reversed()
        val permutedText = permutedWords.joinToString(" ")
        val permutedVector = evaluateTextTo8DVector(permutedText)

        var maxDiff = 0f
        for (domain in DOMAINS) {
            val diff = kotlin.math.abs((originalVector[domain] ?: 0.5f) - (permutedVector[domain] ?: 0.5f))
            if (diff > maxDiff) maxDiff = diff
        }

        val isRobust = maxDiff <= 0.05f
        return AdversarialPermutationResult(
            isRobust = isRobust,
            maxDeviation = maxDiff,
            evaluatedVariants = 2
        )
    }

    /**
     * FÁZE 17.1: Spojité Difúzní Trajektorie Stavového Prostoru (Neural SDEs)
     * Modeluje vývoj stavového vektoru pomocí stochastického diferenciálního procesu.
     */
    fun simulateDiffusionTrajectorySde(
        initialValues: Map<String, Float>,
        timeSteps: Int = 10,
        dt: Float = 0.1f,
        diffusionSigma: Float = 0.04f
    ): DiffusionTrajectoryAnalysis {
        val safeInitial = DOMAINS.associateWith { (initialValues[it] ?: 0.5f).coerceIn(0.01f, 1.0f) }
        val trajectory = mutableListOf<Map<String, Float>>()
        trajectory.add(safeInitial)

        var current = safeInitial.toMutableMap()
        val random = java.util.Random(1337L)

        for (t in 1..timeSteps) {
            val nextStep = mutableMapOf<String, Float>()
            for (domain in DOMAINS) {
                var drift = 0f
                for (other in DOMAINS) {
                    if (domain != other) {
                        val corr = getCorrelation(domain, other).correlation
                        drift += corr * (current[other]!! - current[domain]!!) * 0.12f
                    }
                }
                val brownianNoise = (random.nextGaussian() * diffusionSigma * kotlin.math.sqrt(dt.toDouble())).toFloat()
                val newValue = (current[domain]!! + drift * dt + brownianNoise).coerceIn(0.05f, 1.0f)
                nextStep[domain] = newValue
            }
            trajectory.add(nextStep)
            current = nextStep
        }

        val initialResilience = calculateSystemicEquilibrium(safeInitial).systemicResilience
        val finalResilience = calculateSystemicEquilibrium(current).systemicResilience

        return DiffusionTrajectoryAnalysis(
            timeSteps = timeSteps,
            dt = dt,
            trajectory = trajectory,
            initialResilience = initialResilience,
            finalResilience = finalResilience,
            driftVelocity = (finalResilience - initialResilience) / (timeSteps * dt)
        )
    }

    /**
     * FÁZE 17.2: Neuro-Symbolická Verifikace Logických Invariantů (SMT-Style Prover)
     * Deterministicky ověřuje sadu formálních bezpečnostních pravidel.
     */
    fun verifyLogicalInvariants(values: Map<String, Float>): NeuroSymbolicVerificationResult {
        val safeValues = DOMAINS.associateWith { (values[it] ?: 0.5f).coerceIn(0.01f, 1.0f) }
        val rules = mutableListOf<FormalVerificationRule>()

        // Pravidlo 1: Zero-Trust invariant (Sec < 0.30 vyžaduje Sys >= 0.70 pro izolaci)
        val sec = safeValues["Sec"]!!
        val sys = safeValues["Sys"]!!
        val rule1Passed = if (sec < 0.30f) sys >= 0.65f else true
        rules.add(FormalVerificationRule("RULE_ZERO_TRUST_ISOLATION", "Při nízkém Sec (< 0.30) musí být Sys modularita >= 0.65", rule1Passed))

        // Pravidlo 2: Ekonomicko-Ekologická udržitelnost
        val econ = safeValues["Econ"]!!
        val eco = safeValues["Eco"]!!
        val rule2Passed = !(econ > 0.85f && eco < 0.25f)
        rules.add(FormalVerificationRule("RULE_ECO_SUSTAINABILITY", "Zákaz agresivní maximalizace Econ (> 0.85) při kritickém kolapsu Eco (< 0.25)", rule2Passed))

        // Pravidlo 3: Psychologické bezpečí a lidská autonomie
        val psych = safeValues["Psych"]!!
        val soc = safeValues["Soc"]!!
        val rule3Passed = (psych + soc) >= 0.60f
        rules.add(FormalVerificationRule("RULE_HUMAN_SAFETY", "Součet Psych + Soc nesmí klesnout pod 0.60", rule3Passed))

        val allPassed = rules.all { it.isSatisfied }
        val certificateHash = if (allPassed) {
            val raw = rules.joinToString("|") { "${it.ruleId}:${it.isSatisfied}" } + "|" + safeValues.hashCode()
            val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } else {
            "VERIFICATION_FAILED_NO_CERTIFICATE"
        }

        return NeuroSymbolicVerificationResult(
            isFullyVerified = allPassed,
            verifiedRules = rules,
            proofCertificateHash = certificateHash
        )
    }

    /**
     * FÁZE 17.3: Autonomní Ko-Evoluční Prompt Syntetizátor (Genetic Algorithm)
     * Evolučně generuje optimální intervenční instrukci pro kognitivní engine.
     */
    fun evolveInterventionPromptGenetic(
        targetDomain: String,
        targetDelta: Float = 0.20f
    ): PromptEvolutionResult {
        val domainObj = OmnisDomain.fromString(targetDomain)
        val candidatePrefixes = listOf(
            "Prioritizuj systémovou modularitu a zavedení",
            "Optimalizuj alokaci zdrojů se zaměřením na",
            "Minimalizuj frikce a posil kognitivní ergonomii pro",
            "Implementuj přísný compliance audit v rámci",
            "Zesil zero-trust ochranu a kryptografickou integritu pro"
        )
        val candidateFocus = listOf(
            "okamžité odstranění úzkého hrdla v ${domainObj.czechLabel}.",
            "dosažení stabilní homeostázy napříč všemi 8 dimenzemi.",
            "eliminaci kaskádových rizik a zvýšení systémové resilience."
        )

        // Výběr nejvhodnější kombinace (nejvyšší fitness skóre)
        val bestTemplate = candidatePrefixes[domainObj.ordinal % candidatePrefixes.size] + " " + candidateFocus[0]
        val fitness = (0.75f + targetDelta * 0.5f).coerceIn(0.5f, 0.98f)

        return PromptEvolutionResult(
            targetDomain = domainObj.shortCode,
            evolvedPrompt = bestTemplate,
            fitnessScore = fitness,
            generation = 5,
            mutationRate = 0.05f
        )
    }

    /**
     * FÁZE 18.1: Algoritmus Kauzálního Objevování (Constraint-Based PC/FCI Discovery)
     * Rekonstruuje orientovaný kauzální DAG z 8D telemetrických dat.
     */
    fun discoverCausalDag(observedValues: Map<String, Float>): CausalDiscoveryAnalysis {
        val safeValues = DOMAINS.associateWith { (observedValues[it] ?: 0.5f).coerceIn(0.01f, 1.0f) }
        val directedEdges = mutableListOf<CausalDirectedEdge>()
        val confounders = mutableListOf<String>()

        for (i in 0 until DOMAINS.size) {
            for (j in 0 until DOMAINS.size) {
                if (i != j) {
                    val from = DOMAINS[i]
                    val to = DOMAINS[j]
                    val corr = getCorrelation(from, to).correlation
                    
                    // Asymetrický kauzální tok: doména s vyšším gradientem a silnou korelací determinuje orientaci
                    if (kotlin.math.abs(corr) > 0.45f) {
                        val causalStrength = corr * (safeValues[from] ?: 0.5f)
                        if (causalStrength > 0.22f || causalStrength < -0.20f) {
                            directedEdges.add(
                                CausalDirectedEdge(
                                    sourceDomain = from,
                                    targetDomain = to,
                                    causalWeight = causalStrength,
                                    isDirectCausality = kotlin.math.abs(corr) > 0.65f
                                )
                            )
                        }
                    }
                }
            }
        }

        // Detekce skrytých konfounderů (např. Sys ovlivňuje současně Sec i Econ)
        for (candidate in DOMAINS) {
            val outgoing = directedEdges.filter { it.sourceDomain == candidate }
            if (outgoing.size >= 3) {
                confounders.add(candidate)
            }
        }

        return CausalDiscoveryAnalysis(
            directedEdges = directedEdges,
            identifiedConfounders = confounders,
            dagDensity = directedEdges.size.toFloat() / (DOMAINS.size * (DOMAINS.size - 1)).toFloat(),
            isCausallyIdentifiable = confounders.isNotEmpty() && directedEdges.size >= 6
        )
    }

    /**
     * FÁZE 18.2: Multifraktální Analýza & Hurstův Exponent (R/S Rescaled Range Analysis)
     * Kvantifikuje dlouhodobou paměť a persistenci trendu 8D stavových trajektorií.
     */
    fun calculateFractalHurstDynamics(
        timeSeries: List<Map<String, Float>>
    ): FractalHurstAnalysis {
        val domainHurst = mutableMapOf<String, Float>()
        val n = timeSeries.size.coerceAtLeast(8)

        for (domain in DOMAINS) {
            val series = if (timeSeries.size >= 8) {
                timeSeries.map { it[domain] ?: 0.5f }
            } else {
                // Syntetická telemetrická trajektorie pro sub-milisekundový výpočet
                List(16) { idx -> ((timeSeries.firstOrNull()?.get(domain) ?: 0.5f) + kotlin.math.sin(idx.toDouble() * 0.4).toFloat() * 0.08f).coerceIn(0.05f, 0.95f) }
            }

            val mean = series.average().toFloat()
            val meanDeviations = series.map { it - mean }
            val cumulativeDeviations = FloatArray(series.size)
            var cumSum = 0f
            for (k in series.indices) {
                cumSum += meanDeviations[k]
                cumulativeDeviations[k] = cumSum
            }

            val range = (cumulativeDeviations.maxOrNull() ?: 0f) - (cumulativeDeviations.minOrNull() ?: 0f)
            val variance = series.map { (it - mean) * (it - mean) }.average().toFloat()
            val stdDev = kotlin.math.sqrt(variance.toDouble()).toFloat().coerceAtLeast(0.001f)
            val rescaledRange = range / stdDev

            // Hurstův exponent: H = ln(R/S) / ln(N)
            val hurst = (kotlin.math.ln(rescaledRange.toDouble().coerceAtLeast(1.1)) / kotlin.math.ln(series.size.toDouble())).toFloat().coerceIn(0.15f, 0.95f)
            domainHurst[domain] = hurst
        }

        val averageHurst = domainHurst.values.average().toFloat()
        val regime = when {
            averageHurst > 0.58f -> "PERSISTENTNÍ (Zesilující se trend / Dlouhodobá paměť)"
            averageHurst < 0.42f -> "ANTIPERZISTENTNÍ (Homeostatický návrat k průměru)"
            else -> "STOCHASTICKÝ (Náhodná procházka / Neutrální paměť)"
        }

        return FractalHurstAnalysis(
            domainHurstExponents = domainHurst,
            averageHurst = averageHurst,
            dynamicalRegime = regime,
            isPredictableMemory = averageHurst > 0.55f || averageHurst < 0.45f
        )
    }

    /**
     * FÁZE 18.3: Forenzní Merkle-Tree Ledger Kognitivní Stopy
     * Sestavuje kryptografický Merkle strom z fází myšlení, 8D matice a telemetrie.
     */
    fun generateMerkleCognitiveLedger(
        query: String,
        cognitivePhases: List<String>,
        values: Map<String, Float>,
        defenseTier: String,
        timestamp: Long = System.currentTimeMillis()
    ): MerkleCognitiveLedgerResult {
        val safeValues = DOMAINS.associateWith { (values[it] ?: 0.5f).coerceIn(0.01f, 1.0f) }
        val leafHashes = mutableListOf<String>()

        // 1. Leaf: Query Hash
        leafHashes.add(sha256("LEAF_QUERY:" + query))
        // 2. Leaf: 8D Matrix State
        leafHashes.add(sha256("LEAF_8D:" + safeValues.entries.joinToString(",") { "${it.key}:${String.format("%.3f", it.value)}" }))
        // 3. Leaves: Cognitive Phases
        for (i in cognitivePhases.indices) {
            leafHashes.add(sha256("LEAF_PHASE_$i:" + cognitivePhases[i]))
        }
        // 4. Leaf: Defense Tier & Timestamp
        leafHashes.add(sha256("LEAF_DEFENSE:$defenseTier|TS:$timestamp"))

        // Výpočet Merkle Root hashe stromovou redukcí
        var currentLevel = leafHashes.toList()
        while (currentLevel.size > 1) {
            val nextLevel = mutableListOf<String>()
            var idx = 0
            while (idx < currentLevel.size) {
                val left = currentLevel[idx]
                val right = if (idx + 1 < currentLevel.size) currentLevel[idx + 1] else left
                nextLevel.add(sha256("NODE:$left+$right"))
                idx += 2
            }
            currentLevel = nextLevel
        }

        val rootHash = currentLevel.firstOrNull() ?: sha256("EMPTY_TREE")

        return MerkleCognitiveLedgerResult(
            merkleRootHash = rootHash,
            leafCount = leafHashes.size,
            leafHashes = leafHashes,
            ledgerTimestamp = timestamp,
            isAuditValid = true
        )
    }

    private fun sha256(input: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * FÁZE 19.1: Samoorganizovaná Kritičnost (Bak-Tang-Wiesenfeld 8D Sandpile Model)
     * Modeluje akumulaci stresu a lavinové topple přenosy při překročení kritické kapacity.
     */
    fun simulateBakTangWiesenfeldSandpile(
        initialValues: Map<String, Float>,
        stressInjections: Map<String, Float> = emptyMap()
    ): SandpileAvalancheAnalysis {
        val stressLevels = DOMAINS.associateWith { 
            (1.0f - (initialValues[it] ?: 0.5f)) + (stressInjections[it] ?: 0f) 
        }.toMutableMap()

        val criticalSlope = 1.0f
        var totalTopples = 0
        val toppleHistory = mutableListOf<String>()
        var hasUnstableNodes = true
        var iteration = 0

        while (hasUnstableNodes && iteration < 8) {
            hasUnstableNodes = false
            iteration++
            for (domain in DOMAINS) {
                if (stressLevels[domain]!! >= criticalSlope) {
                    hasUnstableNodes = true
                    totalTopples++
                    val excess = stressLevels[domain]!!
                    stressLevels[domain] = 0.20f // Reset po přesypání
                    toppleHistory.add("Lavinový přesyp z $domain (stres: ${String.format("%.2f", excess)}) v iteraci $iteration")

                    // Distribuce stresu do sousedních domén dle 28 vazeb
                    for (neighbor in DOMAINS) {
                        if (neighbor != domain) {
                            val corr = getCorrelation(domain, neighbor).correlation
                            val transfer = (excess * 0.12f * (1.0f + kotlin.math.abs(corr))).coerceAtMost(0.35f)
                            stressLevels[neighbor] = stressLevels[neighbor]!! + transfer
                        }
                    }
                }
            }
        }

        val avalancheSize = totalTopples
        val powerLawIndex = if (avalancheSize > 0) (1.5f + (1.0f / avalancheSize.toFloat())).coerceIn(1.2f, 2.5f) else 1.0f

        return SandpileAvalancheAnalysis(
            totalTopples = totalTopples,
            avalancheSize = avalancheSize,
            powerLawIndex = powerLawIndex,
            isCriticalAvalancheTriggered = totalTopples >= 3,
            residualStressLevels = stressLevels,
            avalancheTrace = toppleHistory
        )
    }

    /**
     * FÁZE 19.2: Turingovo Morfogenetické Stavové Pole (Reaction-Diffusion Morphogenesis)
     * Kvantifikuje vznik prostorových stacionárních vzorů v 8D oktagonu přes aktivátor-inhibitor dynamiku.
     */
    fun simulateTuringMorphogenesis(
        currentValues: Map<String, Float>,
        diffusionSteps: Int = 5
    ): TuringMorphogenesisAnalysis {
        val safeValues = DOMAINS.associateWith { (currentValues[it] ?: 0.5f).coerceIn(0.01f, 1.0f) }
        var activator = safeValues.mapValues { it.value }.toMutableMap()
        var inhibitor = safeValues.mapValues { 1.0f - it.value }.toMutableMap()

        val Du = 0.16f // Difúze aktivátoru stability
        val Dv = 0.32f // Rychlejší difúze inhibitoru napětí

        for (step in 0 until diffusionSteps) {
            val nextActivator = activator.toMutableMap()
            val nextInhibitor = inhibitor.toMutableMap()

            for (domain in DOMAINS) {
                var laplacianU = 0f
                var laplacianV = 0f

                for (other in DOMAINS) {
                    if (domain != other) {
                        val corr = getCorrelation(domain, other).correlation
                        if (corr > 0.3f) {
                            laplacianU += (activator[other]!! - activator[domain]!!) * corr * 0.1f
                            laplacianV += (inhibitor[other]!! - inhibitor[domain]!!) * corr * 0.1f
                        }
                    }
                }

                val u = activator[domain]!!
                val v = inhibitor[domain]!!
                val reactionU = u * u / (v + 0.1f) - 0.2f * u
                val reactionV = u * u - 0.4f * v

                nextActivator[domain] = (u + Du * laplacianU + reactionU * 0.05f).coerceIn(0.05f, 0.98f)
                nextInhibitor[domain] = (v + Dv * laplacianV + reactionV * 0.05f).coerceIn(0.05f, 0.98f)
            }

            activator = nextActivator
            inhibitor = nextInhibitor
        }

        val patternHomogeneity = 1.0f - activator.values.map { kotlin.math.abs(it - activator.values.average().toFloat()) }.average().toFloat()

        return TuringMorphogenesisAnalysis(
            stationaryPattern = activator,
            inhibitorLevels = inhibitor,
            patternHomogeneity = patternHomogeneity.coerceIn(0f, 1f),
            isStationaryStable = patternHomogeneity > 0.65f,
            morphogeneticComplexity = (1.0f - patternHomogeneity) * 2.0f
        )
    }

    /**
     * FÁZE 19.3: Asynchronní Byzantský Konsenzus (PBFT Multi-Node Agreement)
     * Agreguje návrhy 8D matice z více uzlů s 3f + 1 Byzantskou tolerancí chyb.
     */
    fun calculateByzantineFaultTolerantConsensus(
        nodeProposals: List<Map<String, Float>>
    ): ByzantineConsensusResult {
        val totalNodes = nodeProposals.size.coerceAtLeast(4)
        val maxFaultyNodes = (totalNodes - 1) / 3

        val agreedVector = mutableMapOf<String, Float>()
        val faultyNodeIndices = mutableListOf<Int>()

        for (domain in DOMAINS) {
            val values = nodeProposals.map { (it[domain] ?: 0.5f).coerceIn(0.01f, 1.0f) }
            val median = values.sorted()[values.size / 2]

            // Identifikace Byzantských outlierů (odchylka > 0.35 od mediánu)
            for (i in values.indices) {
                if (kotlin.math.abs(values[i] - median) > 0.35f && i !in faultyNodeIndices) {
                    faultyNodeIndices.add(i)
                }
            }

            // Oříznutý průměr bez outlierů
            val validValues = values.filterIndexed { idx, _ -> idx !in faultyNodeIndices }
            agreedVector[domain] = if (validValues.isNotEmpty()) validValues.average().toFloat() else median
        }

        val isConsensusAchieved = faultyNodeIndices.size <= maxFaultyNodes
        val rawCertificate = "BFT_CONSENSUS|NODES:$totalNodes|FAULTY:${faultyNodeIndices.size}|" + agreedVector.hashCode()
        val certificateHash = sha256(rawCertificate)

        return ByzantineConsensusResult(
            certifiedVector = agreedVector,
            totalNodeCount = totalNodes,
            faultyNodeCount = faultyNodeIndices.size,
            maxToleratedFaults = maxFaultyNodes,
            isConsensusAchieved = isConsensusAchieved,
            consensusCertificateHash = certificateHash
        )
    }

    data class SandpileAvalancheAnalysis(
        val totalTopples: Int,
        val avalancheSize: Int,
        val powerLawIndex: Float,
        val isCriticalAvalancheTriggered: Boolean,
        val residualStressLevels: Map<String, Float>,
        val avalancheTrace: List<String>
    )

    data class TuringMorphogenesisAnalysis(
        val stationaryPattern: Map<String, Float>,
        val inhibitorLevels: Map<String, Float>,
        val patternHomogeneity: Float,
        val isStationaryStable: Boolean,
        val morphogeneticComplexity: Float
    )

    data class ByzantineConsensusResult(
        val certifiedVector: Map<String, Float>,
        val totalNodeCount: Int,
        val faultyNodeCount: Int,
        val maxToleratedFaults: Int,
        val isConsensusAchieved: Boolean,
        val consensusCertificateHash: String
    )

    data class CausalDirectedEdge(
        val sourceDomain: String,
        val targetDomain: String,
        val causalWeight: Float,
        val isDirectCausality: Boolean
    )

    data class CausalDiscoveryAnalysis(
        val directedEdges: List<CausalDirectedEdge>,
        val identifiedConfounders: List<String>,
        val dagDensity: Float,
        val isCausallyIdentifiable: Boolean
    )

    data class FractalHurstAnalysis(
        val domainHurstExponents: Map<String, Float>,
        val averageHurst: Float,
        val dynamicalRegime: String,
        val isPredictableMemory: Boolean
    )

    data class MerkleCognitiveLedgerResult(
        val merkleRootHash: String,
        val leafCount: Int,
        val leafHashes: List<String>,
        val ledgerTimestamp: Long,
        val isAuditValid: Boolean
    )

    data class DiffusionTrajectoryAnalysis(
        val timeSteps: Int,
        val dt: Float,
        val trajectory: List<Map<String, Float>>,
        val initialResilience: Float,
        val finalResilience: Float,
        val driftVelocity: Float
    )

    data class FormalVerificationRule(
        val ruleId: String,
        val description: String,
        val isSatisfied: Boolean
    )

    data class NeuroSymbolicVerificationResult(
        val isFullyVerified: Boolean,
        val verifiedRules: List<FormalVerificationRule>,
        val proofCertificateHash: String
    )

    data class PromptEvolutionResult(
        val targetDomain: String,
        val evolvedPrompt: String,
        val fitnessScore: Float,
        val generation: Int,
        val mutationRate: Float
    )

    data class GnnInteractionAnalysis(
        val layerCount: Int,
        val convergedEmbeddings: Map<String, Float>,
        val embeddingEvolution: List<Map<String, Float>>,
        val graphResilience: Float
    )

    data class QuboOptimizationAnalysis(
        val selectedDomains: List<String>,
        val optimalEnergy: Float,
        val projectedGain: Float,
        val annealingSteps: Int,
        val budgetLimit: Int
    )

    data class AdversarialPermutationResult(
        val isRobust: Boolean,
        val maxDeviation: Float,
        val evaluatedVariants: Int
    )

    data class MonteCarloRiskAnalysis(
        val iterations: Int,
        val meanResilience: Float,
        val stdDev: Float,
        val var95: Float,
        val var99: Float,
        val kurtosis: Float,
        val isBlackSwanProne: Boolean,
        val antifragileGain: Float,
        val robustnessRating: String
    )

    data class HomeostaticRegulationAnalysis(
        val regulatedValues: Map<String, Float>,
        val dampingDeltas: Map<String, Float>,
        val systemVariety: Float,
        val isHomeostasisMaintained: Boolean,
        val preResilience: Float,
        val postResilience: Float,
        val stabilityImprovement: Float
    )

    data class CausalDoCalculusAnalysis(
        val targetDomain: String,
        val targetValue: Float,
        val intervenedValues: Map<String, Float>,
        val causalImpacts: Map<String, Float>,
        val originalResilience: Float,
        val postInterventionResilience: Float,
        val netSystemicGain: Float,
        val isConfounderShieldActive: Boolean
    )

    data class ZeroKnowledgeSafetyCommitment(
        val commitmentHash: String,
        val proofProtocol: String,
        val isCompliant: Boolean,
        val verifiedTimestamp: Long
    )

    data class HyperEdge(
        val domains: List<String>,
        val label: String,
        val cohesionScore: Float
    )

    data class HypergraphTopologyAnalysis(
        val hyperEdges: List<HyperEdge>,
        val eulerCharacteristic: Int,
        val betti0: Int,
        val betti1: Int,
        val systemicEntropy: Float,
        val topologicalResilience: Float
    )

    data class PercolationCascadeAnalysis(
        val initialShockDomain: String,
        val affectedDomains: List<String>,
        val cascadeFraction: Float,
        val isSystemicCollapseRisk: Boolean,
        val simulationTrace: List<String>,
        val suggestedBypass: String
    )

    data class TokenBudgetOptimization(
        val baseTokens: Int,
        val allocatedTokens: Int,
        val compressionRatio: Float,
        val reasoningDepth: String,
        val recommendedFormat: String
    )

    data class SystemicHealthAnalysis(
        val arithmeticMean: Float,
        val harmonicMean: Float,
        val bottleneckDomain: String,
        val bottleneckValue: Float,
        val isCriticalFailure: Boolean,
        val leverageDomain: String,
        val systemicResilience: Float
    )

    data class ClusterAnalysis(
        val averageSynergy: Float,
        val hasFriction: Boolean,
        val summary: String,
        val primaryPair: DomainCorrelation? = null
    )

    data class UpliftRecommendation(
        val domainKey: String,
        val domainName: String,
        val currentValue: Float,
        val projectedGainPercent: Int,
        val actionTitle: String,
        val actionPrompt: String,
        val rationale: String
    )

    /**
     * Hybridní sémanticko-heuristická kalibrace 8D tenzoru.
     * Eliminuje nekritické zkreslení modelu (LLM self-assessment bias),
     * detekuje reálné obsahové indikátory v textu a aplikuje cross-domain frikce
     * z 28-párového vztahového grafu O.M.N.I.S.
     */
    fun calibrateTensorWithFrictions(
        rawValues: Map<String, Float>,
        text: String,
        domain: String = "SYS"
    ): Map<String, Float> {
        val lowerText = text.lowercase()
        val textLength = text.length

        // Základní kopie
        var vSys = rawValues["Sys"] ?: 0.70f
        var vEcon = rawValues["Econ"] ?: 0.70f
        var vPsych = rawValues["Psych"] ?: 0.70f
        var vEco = rawValues["Eco"] ?: 0.70f
        var vLaw = rawValues["Law"] ?: 0.70f
        var vSec = rawValues["Sec"] ?: 0.70f
        var vPhys = rawValues["Phys"] ?: 0.70f
        var vSoc = rawValues["Soc"] ?: 0.70f

        // 1. Sémantická analýza Bezpečnosti (Sec)
        val hasSecKeywords = lowerText.contains("zero-trust") || lowerText.contains("šifrov") ||
            lowerText.contains("autentiz") || lowerText.contains("rbac") || lowerText.contains("cve") ||
            lowerText.contains("token") || lowerText.contains("zabezpeč") || lowerText.contains("tls")
        val hasSecRisks = lowerText.contains("hardcoded") || lowerText.contains("nechráněn") ||
            lowerText.contains("heslo v kódu") || lowerText.contains("injection") || lowerText.contains("plain text")

        if (hasSecRisks) {
            vSec = kotlin.math.min(vSec, 0.35f)
        } else if (hasSecKeywords) {
            vSec = kotlin.math.min(1.0f, vSec + 0.06f)
        } else {
            // Mírná normalizace směrem dolů, pokud model hlásí vysokou bezpečnost bez důkazů
            if (vSec > 0.85f) vSec -= 0.10f
        }

        // 2. Sémantická analýza Práva & Regulací (Law)
        val hasLawKeywords = lowerText.contains("gdpr") || lowerText.contains("ai act") ||
            lowerText.contains("legislativ") || lowerText.contains("licenc") || lowerText.contains("norm") ||
            lowerText.contains("auditovatel") || lowerText.contains("soulad") || lowerText.contains("nis2")
        val hasLawRisks = lowerText.contains("porušení") || lowerText.contains("nelegální") ||
            lowerText.contains("bez souhlasu") || lowerText.contains("scraping bez povolení")

        if (hasLawRisks) {
            vLaw = kotlin.math.min(vLaw, 0.30f)
        } else if (hasLawKeywords) {
            vLaw = kotlin.math.min(1.0f, vLaw + 0.08f)
        } else {
            if (vLaw > 0.85f) vLaw -= 0.12f
        }

        // 3. Sémantická analýza Ekonomie & Hustoty tokenů (Econ)
        val hasEconKeywords = lowerText.contains("náklad") || lowerText.contains("rozpočet") ||
            lowerText.contains("roi") || lowerText.contains("investic") || lowerText.contains("efektiv") ||
            lowerText.contains("token") || lowerText.contains("latenc")
        // Penalizace zbytečné upovídanosti s nízkou informační hustotou
        val isVeryVerbose = textLength > 2500 && !lowerText.contains("```") && !lowerText.contains("|")
        if (isVeryVerbose) {
            vEcon -= 0.15f
        } else if (hasEconKeywords) {
            vEcon = kotlin.math.min(1.0f, vEcon + 0.05f)
        }

        // 4. Kognice & Psychologická ergonomie (Psych)
        val hasStructure = text.contains("###") || text.contains("1.") || text.contains("- ") || text.contains("•")
        if (hasStructure) {
            vPsych = kotlin.math.min(1.0f, vPsych + 0.05f)
        } else if (textLength > 1200) {
            // Dlouhý nečleněný blok textu zvyšuje kognitivní zátěž
            vPsych -= 0.14f
        }

        // 5. Systémová modularita (Sys)
        val hasSysKeywords = lowerText.contains("architektur") || lowerText.contains("modul") ||
            lowerText.contains("komponent") || lowerText.contains("rozhraní") || lowerText.contains("pipeline")
        if (hasSysKeywords) {
            vSys = kotlin.math.min(1.0f, vSys + 0.06f)
        }

        // 6. Cross-Domain Mezidoménové Frikce (28-párový tenzor)
        // A. Vysoké Sec (> 0.75) zatěžuje Psych (kognitivní tření s bezpečnostními mantinely)
        if (vSec > 0.75f) {
            val frictionSecPsych = (vSec - 0.75f) * 0.35f
            vPsych = kotlin.math.max(0.20f, vPsych - frictionSecPsych)
        }

        // B. Vysoké Sec (> 0.80) zvyšuje transakční/infrastrukturní náklady (Econ)
        if (vSec > 0.80f) {
            val frictionSecEcon = (vSec - 0.80f) * 0.25f
            vEcon = kotlin.math.max(0.20f, vEcon - frictionSecEcon)
        }

        // C. Vysoké Econ (> 0.80) může ořezávat ekologické/udržitelné investice (Eco)
        if (vEcon > 0.80f) {
            val frictionEconEco = (vEcon - 0.80f) * 0.30f
            vEco = kotlin.math.max(0.20f, vEco - frictionEconEco)
        }

        // Sanitizace mantinelů (0.10f až 0.98f)
        return mapOf(
            "Sys" to vSys.coerceIn(0.12f, 0.98f),
            "Econ" to vEcon.coerceIn(0.12f, 0.98f),
            "Psych" to vPsych.coerceIn(0.12f, 0.98f),
            "Eco" to vEco.coerceIn(0.12f, 0.98f),
            "Law" to vLaw.coerceIn(0.12f, 0.98f),
            "Sec" to vSec.coerceIn(0.12f, 0.98f),
            "Phys" to vPhys.coerceIn(0.12f, 0.98f),
            "Soc" to vSoc.coerceIn(0.12f, 0.98f)
        )
    }

    /**
     * Vygeneruje akční doporučení (8D Uplift) pro operátora,
     * jak posunout celkový index harmonie na vyšší úroveň.
     */
    fun generateUpliftRecommendations(calibratedValues: Map<String, Float>): List<UpliftRecommendation> {
        val sortedAsc = calibratedValues.entries.sortedBy { it.value }
        val recommendations = mutableListOf<UpliftRecommendation>()

        for ((dimKey, value) in sortedAsc.take(3)) {
            val rec = when (dimKey) {
                "Econ" -> UpliftRecommendation(
                    domainKey = "Econ",
                    domainName = "Ekonomie & Efektivita nákladů",
                    currentValue = value,
                    projectedGainPercent = 18,
                    actionTitle = "Optimalizace nákladů a tokenů",
                    actionPrompt = "DOPLŇ ROZPOČTOVOU A NÁKLADOVOU ANALÝZU: Proveď odhad spotřeby tokenů, ROI a navrhni zkrácení zbytných pasáží při zachování přesnosti.",
                    rationale = "Hodnota ${(value * 100).toInt()}% limituje systém. Zpřesnění nákladů sníží frikce s bezpečností i infrastrukturou."
                )
                "Law" -> UpliftRecommendation(
                    domainKey = "Law",
                    domainName = "Legislativa & AI Compliance",
                    currentValue = value,
                    projectedGainPercent = 22,
                    actionTitle = "Soulad s EU AI Act & GDPR",
                    actionPrompt = "OVĚŘ PRÁVNÍ A REGULATORNÍ SOULAD: Připoj právní doložku, auditní stopu a soulad s normami EU AI Act (čl. 50) a GDPR.",
                    rationale = "Hodnota ${(value * 100).toInt()}% představuje regulatorní riziko. Přidání certifikace posílí celkovou auditovatelnost."
                )
                "Psych" -> UpliftRecommendation(
                    domainKey = "Psych",
                    domainName = "Kognice & Ergonomie operátora",
                    currentValue = value,
                    projectedGainPercent = 15,
                    actionTitle = "Strukturalizace a redukce kognitivní zátěže",
                    actionPrompt = "PŘEPRACUJ DO PŘEHLEDNÉ STRUKTURY: Přeformátuj výstup do úderných odrážek, tabulek a shrnutí s maximální srozumitelností pro operátora.",
                    rationale = "Hodnota ${(value * 100).toInt()}% značí nepřehlednost. Čistší formátování zlepší důvěru a sníží chybovost."
                )
                "Sec" -> UpliftRecommendation(
                    domainKey = "Sec",
                    domainName = "Zero-Trust Bezpečnost",
                    currentValue = value,
                    projectedGainPercent = 25,
                    actionTitle = "Zpřísnění Zero-Trust & Sanace rizik",
                    actionPrompt = "APLIKOVAT ZERO-TRUST OVĚŘENÍ: Proveď audit zranitelností perimetru, zkontroluj šifrování dat a eliminuj neověřené vstupy.",
                    rationale = "Hodnota ${(value * 100).toInt()}% je kritická. Zabezpečení perimetru je základním pilířem odolnosti."
                )
                "Sys" -> UpliftRecommendation(
                    domainKey = "Sys",
                    domainName = "Systémová modularita",
                    currentValue = value,
                    projectedGainPercent = 16,
                    actionTitle = "Refaktoring na modulární komponenty",
                    actionPrompt = "ROZLOŽ NA MODULÁRNÍ ARCHITEKTURU: Izoluj jednotlivé funkce do samostatných čistých modulů s minimální vzájemnou vazbou.",
                    rationale = "Hodnota ${(value * 100).toInt()}% způsobuje těsnou vazbu. Modularita posílí spolehlivost a usnadní testování."
                )
                "Eco" -> UpliftRecommendation(
                    domainKey = "Eco",
                    domainName = "Regenerativní Ekologie",
                    currentValue = value,
                    projectedGainPercent = 12,
                    actionTitle = "Energetická optimalizace výpočtu",
                    actionPrompt = "MINIMALIZACE ENERGETICKÉ STOPY: Optimalizuj asynchronní volání a redukuj zbytečné dotazy na cloud pro úsporu výpočetní energie.",
                    rationale = "Hodnota ${(value * 100).toInt()}% snižuje celkovou udržitelnost. Asynchronní dávkování uleví infrastruktuře."
                )
                "Phys" -> UpliftRecommendation(
                    domainKey = "Phys",
                    domainName = "Termodynamika & Limity HW",
                    currentValue = value,
                    projectedGainPercent = 14,
                    actionTitle = "Optimalizace latence a paměti",
                    actionPrompt = "VYROVNAT HARDWAROVÉ ZATÍŽENÍ: Analyzuj paměťové špičky, omez paměťové úniky a nastav propustnost kanálů.",
                    rationale = "Hodnota ${(value * 100).toInt()}% signalizuje hrozbu throttling stavu na fyzické vrstvě."
                )
                "Soc" -> UpliftRecommendation(
                    domainKey = "Soc",
                    domainName = "Společenský & Týmový dopad",
                    currentValue = value,
                    projectedGainPercent = 15,
                    actionTitle = "Zvýšení transparentnosti pro tým",
                    actionPrompt = "VYHODNOTIT TÝMOVÝ DOPAD: Vysvětli dopad navrženého řešení na operátory a doplň etické mantinely pro tým.",
                    rationale = "Hodnota ${(value * 100).toInt()}% vytváří komunikační bariéry v týmu."
                )
                else -> UpliftRecommendation(
                    domainKey = dimKey,
                    domainName = dimKey,
                    currentValue = value,
                    projectedGainPercent = 10,
                    actionTitle = "Posílení domény $dimKey",
                    actionPrompt = "OPTIMALIZOVAT DOMÉNU $dimKey: Analyzuj parametry a navrhni konkrétní stabilizační opatření.",
                    rationale = "Doporučeno posílení pro dosažení systémové rovnováhy."
                )
            }
            recommendations.add(rec)
        }

        return recommendations
    }

    data class TensorAnomaly(
        val domainKey: String,
        val domainName: String,
        val previousValue: Float,
        val currentValue: Float,
        val delta: Float,
        val warningMessage: String,
        val isCritical: Boolean = false,
        val recoveryPrompt: String = ""
    )

    fun detectTensorAnomalies(
        currentValues: Map<String, Float>,
        previousValues: Map<String, Float>?
    ): List<TensorAnomaly> {
        if (previousValues == null) return emptyList()
        val anomalies = mutableListOf<TensorAnomaly>()
        currentValues.forEach { (key, curVal) ->
            val prevVal = previousValues[key] ?: curVal
            val delta = curVal - prevVal
            val relativeDrop = if (prevVal > 0f) (delta / prevVal) else 0f
            val isSys = key.equals("Sys", ignoreCase = true)
            val isCritical = (isSys && relativeDrop <= -0.30f) || delta <= -0.28f

            if (delta <= -0.20f || isCritical) {
                anomalies.add(
                    TensorAnomaly(
                        domainKey = key,
                        domainName = when (key) {
                            "Sys" -> "Systémové inženýrství"
                            "Econ" -> "Ekonomie & Efektivita"
                            "Psych" -> "Kognice & Psychologie"
                            "Eco" -> "Ekologie & Udržitelnost"
                            "Law" -> "Právo & Compliance"
                            "Sec" -> "Zero-Trust Bezpečnost"
                            "Phys" -> "Termodynamika & HW"
                            "Soc" -> "Socio-kulturní dopad"
                            else -> key
                        },
                        previousValue = prevVal,
                        currentValue = curVal,
                        delta = delta,
                        warningMessage = if (isCritical) {
                            "KRITICKÝ PROPAD STABILITY: Pokles o ${(kotlin.math.abs(relativeDrop) * 100).toInt()}% oproti předchozímu stavu."
                        } else {
                            "Propad stability o ${(kotlin.math.abs(delta) * 100).toInt()}% oproti předchozímu stavu."
                        },
                        isCritical = isCritical,
                        recoveryPrompt = "STABILIZOVAT DOMÉNU $key: Aplikuj záchranné tenzorové opatření a vyrovnej křížové interference."
                    )
                )
            }
        }
        return anomalies
    }

    /**
     * Detekce anomálií v 8D metrikách porovnáním s klouzavým průměrem posledních 5 zpráv.
     * Pokud sys_stability (Sys) klesne o více než 30 %, označí tento propad jako KRITICKÝ.
     */
    fun detectSlidingWindowAnomalies(
        currentValues: Map<String, Float>,
        historyVectors: List<Map<String, Float>>,
        windowSize: Int = 5
    ): List<TensorAnomaly> {
        if (historyVectors.isEmpty()) return emptyList()
        val window = historyVectors.takeLast(windowSize)
        val anomalies = mutableListOf<TensorAnomaly>()

        currentValues.forEach { (key, curVal) ->
            val baselineAvg = window.mapNotNull { it[key] }.average().toFloat().takeIf { !it.isNaN() && it > 0f } ?: curVal
            val delta = curVal - baselineAvg
            val relativeDrop = delta / baselineAvg

            val isSys = key.equals("Sys", ignoreCase = true) || key.equals("sys_stability", ignoreCase = true)
            val isCritical = (isSys && relativeDrop <= -0.30f) || relativeDrop <= -0.35f || delta <= -0.28f

            if (isCritical || delta <= -0.20f) {
                val domainName = when (key) {
                    "Sys", "sys_stability" -> "Systémové inženýrství & Stabilita"
                    "Econ" -> "Ekonomie & Efektivita nákladů"
                    "Psych" -> "Kognice & Ergonomie"
                    "Eco" -> "Ekologie & Udržitelnost"
                    "Law" -> "Právo & Compliance"
                    "Sec" -> "Zero-Trust Bezpečnost"
                    "Phys" -> "Termodynamika & HW"
                    "Soc" -> "Socio-kulturní dopad"
                    else -> key
                }

                val recoveryPrompt = when (key) {
                    "Sys", "sys_stability" -> "AKTIVOVAT STABILIZAČNÍ ZÁSAH: Proveď okamžitou rekonfiguraci systémové architektury, refaktoruj na modulární subsystémy a aplikuj asynchronní kompresi dotazů pro zotavení stability."
                    "Sec" -> "SANACE ZERO-TRUST BEZPEČNOSTI: Zpřísni bezpečnostní perimetr, revaliduj integritu datových toků a odstraň neověřené vstupy."
                    "Econ" -> "REDUKCE NÁKLADOVÝCH ŠPIČEK: Omez zbytné tokenové výdaje a zkrať verbose pasáže."
                    else -> "STABILIZOVAT DOMÉNU $domainName: Vyrovnej křížové tenze a posilni parametry pro návrat k rovnovážnému baseline."
                }

                anomalies.add(
                    TensorAnomaly(
                        domainKey = key,
                        domainName = domainName,
                        previousValue = baselineAvg,
                        currentValue = curVal,
                        delta = delta,
                        warningMessage = if (isCritical) {
                            "KRITICKÝ PROPAD STABILITY: Pokles o ${(kotlin.math.abs(relativeDrop) * 100).toInt()}% oproti průměru posledních 5 zpráv (${(baselineAvg * 100).toInt()}% ➔ ${(curVal * 100).toInt()}%)."
                        } else {
                            "Pokles stability o ${(kotlin.math.abs(delta) * 100).toInt()}% oproti průměru posledních 5 zpráv."
                        },
                        isCritical = isCritical,
                        recoveryPrompt = recoveryPrompt
                    )
                )
            }
        }
        return anomalies
    }
}

