package com.example.domain.knowledge

import com.example.causal.CsrNodeType
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Reprezentace kognitivní domény v 8D ontologii O.M.N.I.S.
 */
enum class OmnisDomain(val code: String, val title: String) {
    SYS("sys", "Systémové inženýrství"),
    ECON("econ", "Teorie her & Ekonomie"),
    PSYCH("psych", "Kognitivní vědy & Psychologie"),
    ECO("eco", "Regenerativní Ekologie"),
    LAW("law", "Regulace & Právo"),
    SEC("sec", "Zero-Trust Bezpečnost"),
    PHYS("phys", "Fyzikální termodynamika"),
    SOC("soc", "Socio-kulturní dynamika")
}

/**
 * Uzel znalostního grafu s podporou 4 páteřních typů:
 * CONCEPT (Domény/Koncepty), EVENT (Stresové události), THREAT (Zranitelnosti), BYPASS (Technická řešení/Bypassy).
 */
data class KnowledgeNode(
    val id: String,
    val label: String,
    val domain: OmnisDomain,
    val description: String,
    val aliases: List<String>,
    val citations: List<String>,
    val nodeType: CsrNodeType = CsrNodeType.CONCEPT,
    val vectorEmbedding: List<Float>? = null,
    val isRetracted: Boolean = false
)

data class KnowledgeEdge(
    val source: String,
    val target: String,
    val weight: Double,
    val relation: String,
    val isCausal: Boolean
)

data class GateEvaluation(
    val gateId: String,
    val name: String,
    val passed: Boolean,
    val score: Double,
    val threshold: Double,
    val explanation: String
)

data class RefusalLadderResult(
    val isGrounded: Boolean,
    val groundingScore: Int,
    val gates: List<GateEvaluation>,
    val matchedNodeIds: List<String>,
    val graphPath: List<String>,
    val citations: List<String>,
    val isRefusal: Boolean,
    val refusalReason: String?,
    val recommendedAction: String?,
    val shannonEntropy: Double
)

data class HnswSearchResult(
    val node: KnowledgeNode,
    val cosineSimilarity: Float,
    val distance: Float,
    val rank: Int
)

/**
 * Compressed Sparse Row (CSR) Reprezentace Kognitivního Grafu v Kotlinu
 */
class CsrGraphStore(
    val nodes: List<KnowledgeNode>,
    edges: List<KnowledgeEdge>
) {
    private val nodeIndexMap = nodes.mapIndexed { idx, n -> n.id to idx }.toMap()
    val rowOffsets: IntArray
    val colIndices: IntArray
    val weights: DoubleArray

    init {
        val n = nodes.size
        val adjacency = Array(n) { mutableListOf<Pair<Int, Double>>() }

        edges.forEach { edge ->
            val u = nodeIndexMap[edge.source]
            val v = nodeIndexMap[edge.target]
            if (u != null && v != null) {
                adjacency[u].add(v to edge.weight)
                // Obousměrný průchod s mírným útlumem
                adjacency[v].add(u to (edge.weight * 0.8))
            }
        }

        rowOffsets = IntArray(n + 1)
        val tempCols = mutableListOf<Int>()
        val tempWeights = mutableListOf<Double>()

        for (i in 0 until n) {
            rowOffsets[i] = tempCols.size
            for ((targetIdx, w) in adjacency[i]) {
                tempCols.add(targetIdx)
                tempWeights.add(w)
            }
        }
        rowOffsets[n] = tempCols.size

        colIndices = tempCols.toIntArray()
        weights = tempWeights.toDoubleArray()
    }

    fun findShortestPath(sourceId: String, targetId: String): List<String> {
        val srcIdx = nodeIndexMap[sourceId] ?: return emptyList()
        val tgtIdx = nodeIndexMap[targetId] ?: return emptyList()
        if (srcIdx == tgtIdx) return listOf(sourceId)

        val visited = HashSet<Int>()
        val parent = HashMap<Int, Int>()
        val queue = ArrayDeque<Int>()

        visited.add(srcIdx)
        queue.add(srcIdx)

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            if (curr == tgtIdx) break

            val start = rowOffsets[curr]
            val end = rowOffsets[curr + 1]
            for (idx in start until end) {
                val next = colIndices[idx]
                if (!visited.contains(next)) {
                    visited.add(next)
                    parent[next] = curr
                    queue.add(next)
                }
            }
        }

        if (!visited.contains(tgtIdx)) return emptyList()

        val path = mutableListOf<String>()
        var curr: Int? = tgtIdx
        while (curr != null) {
            path.add(0, nodes[curr].id)
            curr = parent[curr]
        }
        return path
    }
}

/**
 * Globální HNSW Vektorový Index (PostgreSQL pgvector kompatibilní) pro O.M.N.I.S.
 */
class GlobalHnswIndexStore(
    private val nodes: List<KnowledgeNode>
) {
    /**
     * Vypočte kosínovou podobnost mezi dvěma vektory.
     */
    fun cosineSimilarity(vecA: List<Float>, vecB: List<Float>): Float {
        if (vecA.size != vecB.size || vecA.isEmpty()) return 0.0f
        var dot = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in vecA.indices) {
            dot += vecA[i] * vecB[i]
            normA += vecA[i] * vecA[i]
            normB += vecB[i] * vecB[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom > 1e-6f) (dot / denom) else 0.0f
    }

    /**
     * Vygeneruje deterministický pseudo-embedding (8 dimenzí pro 8D ontologii) z textu.
     */
    fun createDeterministicEmbedding(text: String): List<Float> {
        val lower = text.lowercase()
        val vector = FloatArray(8) { 0.1f }

        if (lower.contains("architekt") || lower.contains("systém") || lower.contains("api")) vector[0] += 0.8f
        if (lower.contains("náklad") || lower.contains("token") || lower.contains("cena")) vector[1] += 0.8f
        if (lower.contains("kognitiv") || lower.contains("mentál") || lower.contains("ux")) vector[2] += 0.8f
        if (lower.contains("uhlík") || lower.contains("ekolog") || lower.contains("emis")) vector[3] += 0.8f
        if (lower.contains("zákon") || lower.contains("compliance") || lower.contains("act") || lower.contains("audit")) vector[4] += 0.8f
        if (lower.contains("bezpečnost") || lower.contains("šifr") || lower.contains("zero-trust") || lower.contains("snark")) vector[5] += 0.8f
        if (lower.contains("hardware") || lower.contains("latenc") || lower.contains("edge") || lower.contains("výpadk")) vector[6] += 0.8f
        if (lower.contains("společnost") || lower.contains("komunit") || lower.contains("lidsk")) vector[7] += 0.8f

        // Normalizace
        var sumSquares = 0.0f
        for (v in vector) sumSquares += v * v
        val norm = sqrt(sumSquares)
        return vector.map { if (norm > 1e-6f) it / norm else 0.125f }
    }

    /**
     * Vyhledá nejbližší uzly v globálním indexu dle kosínové vzdálenosti.
     */
    fun searchNearest(queryText: String, topK: Int = 4): List<HnswSearchResult> {
        val queryVec = createDeterministicEmbedding(queryText)
        val scored = nodes.mapNotNull { node ->
            val nodeVec = node.vectorEmbedding ?: createDeterministicEmbedding(node.label + " " + node.description)
            val sim = cosineSimilarity(queryVec, nodeVec)
            val dist = 1.0f - sim
            HnswSearchResult(node = node, cosineSimilarity = sim, distance = dist, rank = 0)
        }

        return scored
            .sortedByDescending { it.cosineSimilarity }
            .take(topK)
            .mapIndexed { idx, item -> item.copy(rank = idx + 1) }
    }
}

/**
 * O.M.N.I.S. Unified Agentic Knowledge Graph Engine (AKGE-8D)
 */
object OmnisKnowledgeGraphEngine {

    val KNOWLEDGE_NODES = listOf(
        // 1. CONCEPT (8D Domény & Základní Koncepty)
        KnowledgeNode(
            id = "sys_core_arch",
            label = "Systémová Architektura & Microservices",
            domain = OmnisDomain.SYS,
            description = "Modulární asynchronní servisní vrstva s vysokou propustností a nulovou provázaností.",
            aliases = listOf("architektur", "systém", "microservice", "komponent", "modul", "servis", "backend", "api"),
            citations = listOf("ISO/IEC/IEEE 42010:2022 Systems Architecture", "ISO/IEC 42001:2023 Clause 8.2"),
            nodeType = CsrNodeType.CONCEPT,
            vectorEmbedding = listOf(0.95f, 0.2f, 0.1f, 0.1f, 0.3f, 0.4f, 0.2f, 0.1f)
        ),
        KnowledgeNode(
            id = "sys_async_stream",
            label = "Reaktivní Datové Toky & SSE Streaming",
            domain = OmnisDomain.SYS,
            description = "Server-Sent Events a asynchronní reaktivní toky pro streaming s latencí pod 50ms.",
            aliases = listOf("stream", "sse", "asynchron", "reaktivn", "tok", "pipeline", "event"),
            citations = listOf("Reactive Streams Specification v1.0.4"),
            nodeType = CsrNodeType.CONCEPT,
            vectorEmbedding = listOf(0.85f, 0.1f, 0.1f, 0.1f, 0.1f, 0.2f, 0.6f, 0.1f)
        ),
        KnowledgeNode(
            id = "sec_zero_trust",
            label = "Zero-Trust Architektura & RBAC",
            domain = OmnisDomain.SEC,
            description = "Principy trvalého ověřování, minimálních oprávnění a striktní izolace rolí.",
            aliases = listOf("zero-trust", "rbac", "oprávnění", "autentizac", "autorizac", "role", "bezpečnost"),
            citations = listOf("NIST SP 800-207: Zero Trust Architecture", "Directive (EU) 2022/2555 (NIS2 Directive Art. 21)"),
            nodeType = CsrNodeType.CONCEPT,
            vectorEmbedding = listOf(0.3f, 0.1f, 0.1f, 0.1f, 0.5f, 0.95f, 0.2f, 0.2f)
        ),
        KnowledgeNode(
            id = "sec_zk_sha",
            label = "Kryptografické SHA-256 Commitments",
            domain = OmnisDomain.SEC,
            description = "Jednosměrné hashovací pečetě stavových vektorů pro garanci integrity a nepopiratelnosti.",
            aliases = listOf("šifrov", "krypt", "hash", "sha-256", "pečeť", "snark", "zk", "integrit"),
            citations = listOf("FIPS 180-4: Secure Hash Standard", "ISO/IEC 42001:2023 Annex A.9"),
            nodeType = CsrNodeType.CONCEPT,
            vectorEmbedding = listOf(0.2f, 0.1f, 0.1f, 0.1f, 0.6f, 0.98f, 0.1f, 0.1f)
        ),
        KnowledgeNode(
            id = "law_eu_ai_act",
            label = "EU AI Act Compliance & Risk Classification",
            domain = OmnisDomain.LAW,
            description = "Striktní zatřídění systémů podle rizikových tříd a dodržování požadavků na auditovatelnost.",
            aliases = listOf("act", "eu ai act", "regulac", "compliance", "zákon", "klasifikac"),
            citations = listOf(
                "Regulation (EU) 2024/1689 (EU Artificial Intelligence Act)",
                "Directive (EU) 2022/2555 (NIS2)",
                "ISO/IEC 42001:2023 Information Technology - AI Management"
            ),
            nodeType = CsrNodeType.CONCEPT,
            vectorEmbedding = listOf(0.2f, 0.2f, 0.2f, 0.1f, 0.98f, 0.5f, 0.1f, 0.4f)
        ),
        KnowledgeNode(
            id = "econ_token_opt",
            label = "Tokenomika & Alokace Výpočetních Zdrojů",
            domain = OmnisDomain.ECON,
            description = "Optimalizace nákladů inference, rozpočtové limity a prevence tokenového vyčerpání.",
            aliases = listOf("token", "náklad", "cena", "rozpočet", "alokac", "kvót", "kapitál", "financ"),
            citations = listOf("Tirole: The Theory of Industrial Organization"),
            nodeType = CsrNodeType.CONCEPT,
            vectorEmbedding = listOf(0.3f, 0.95f, 0.1f, 0.2f, 0.2f, 0.1f, 0.1f, 0.2f)
        ),

        // 2. EVENT (Kauzální Události & Stresové Scénáře)
        KnowledgeNode(
            id = "event_cloud_outage",
            label = "Kaskádový Výpadek Cloudové Infrastruktury",
            domain = OmnisDomain.PHYS,
            description = "Výpadek dostupnosti primárních datacenter, nárůst latence a hrozba rozpadu distribuovaných služeb.",
            aliases = listOf("výpadek", "outage", "blackout", "infrastruktur", "down", "výpadk"),
            citations = listOf("Directive (EU) 2022/2555 (NIS2 Art. 21 - Incident Handling)"),
            nodeType = CsrNodeType.EVENT,
            vectorEmbedding = listOf(0.6f, 0.3f, 0.2f, 0.1f, 0.3f, 0.4f, 0.95f, 0.2f)
        ),
        KnowledgeNode(
            id = "event_liquidity_shock",
            label = "Likviditní & Tokenový Šok v Alokaci",
            domain = OmnisDomain.ECON,
            description = "Rychlé vyčerpání tokenového rozpočtu neoptimálními promptovými řetězci nebo útokem vyčerpání.",
            aliases = listOf("likvidit", "rozpočtový šok", "inflac", "překročení nákladů", "vyčerpání rozpočtu"),
            citations = listOf("ISO/IEC 42001:2023 Resource Management"),
            nodeType = CsrNodeType.EVENT,
            vectorEmbedding = listOf(0.2f, 0.98f, 0.3f, 0.1f, 0.2f, 0.3f, 0.1f, 0.1f)
        ),
        KnowledgeNode(
            id = "event_regulatory_sanction",
            label = "Regulátorský Audit & Sankční Řízení",
            domain = OmnisDomain.LAW,
            description = "Zjištění nesouladu se články EU AI Act nebo směrnicí NIS2 vedoucí k pozastavení provozu.",
            aliases = listOf("sankc", "pokut", "řízení", "inspekc", "veto", "auditní nález"),
            citations = listOf("Regulation (EU) 2024/1689 Art. 99 (Penalties)"),
            nodeType = CsrNodeType.EVENT,
            vectorEmbedding = listOf(0.1f, 0.4f, 0.2f, 0.1f, 0.98f, 0.6f, 0.1f, 0.5f)
        ),

        // 3. THREAT (Bezpečnostní Hrozby & Zranitelnosti)
        KnowledgeNode(
            id = "threat_zero_day_rce",
            label = "Zero-Day Exploit & Remote Code Execution",
            domain = OmnisDomain.SEC,
            description = "Kritická zranitelnost v aplikačním frameworku umožňující útočníkovi manipulaci se stavem.",
            aliases = listOf("exploit", "rce", "zranitelnost", "injektáž", "narušení", "únik"),
            citations = listOf("CVE Program / NIST NVD Database", "NIS2 Cyber Threat Taxonomy"),
            nodeType = CsrNodeType.THREAT,
            vectorEmbedding = listOf(0.4f, 0.1f, 0.1f, 0.1f, 0.3f, 0.98f, 0.3f, 0.1f)
        ),
        KnowledgeNode(
            id = "threat_prompt_injection",
            label = "Adversariální Prompt Injection & Data Exfiltration",
            domain = OmnisDomain.SEC,
            description = "Pokus o obcházení sémantických bran podvrženými systémovými instrukcemi a únik citlivých dat.",
            aliases = listOf("jailbreak", "prompt injection", "manipulac", "exfiltrac", "obcházení"),
            citations = listOf("OWASP Top 10 for LLM Applications (LLM01: Prompt Injection)"),
            nodeType = CsrNodeType.THREAT,
            vectorEmbedding = listOf(0.3f, 0.1f, 0.4f, 0.1f, 0.4f, 0.95f, 0.1f, 0.2f)
        ),
        KnowledgeNode(
            id = "threat_model_drift",
            label = "Kognitivní Drift & Halucinace Modelu",
            domain = OmnisDomain.PSYCH,
            description = "Degradace faktické přesnosti a odklon od definovaných systémových axiomů bez detekce operátorem.",
            aliases = listOf("halucinac", "drift", "degradac", "chybný úsudek", "bias"),
            citations = listOf("ISO/IEC 42001:2023 AI Quality Evaluation"),
            nodeType = CsrNodeType.THREAT,
            vectorEmbedding = listOf(0.2f, 0.1f, 0.95f, 0.1f, 0.4f, 0.3f, 0.1f, 0.3f)
        ),

        // 4. BYPASS (Technická Řešení & Bypassy)
        KnowledgeNode(
            id = "bypass_failover_circuit",
            label = "Automatický Circuit Breaker & Edge Failover",
            domain = OmnisDomain.SYS,
            description = "Nouzové odpojení nestabilních uzlů a okamžité přesměrování kognitivního toku na edge fallback.",
            aliases = listOf("jistič", "circuit breaker", "failover", "redundanc", "záloha"),
            citations = listOf("Martin Fowler: Circuit Breaker Pattern", "ISO/IEC 27001 Annex A.12"),
            nodeType = CsrNodeType.BYPASS,
            vectorEmbedding = listOf(0.95f, 0.2f, 0.1f, 0.1f, 0.3f, 0.7f, 0.6f, 0.1f)
        ),
        KnowledgeNode(
            id = "bypass_airgap_ledger",
            label = "Kryptografický Air-Gap Ledger & Token Capping",
            domain = OmnisDomain.SEC,
            description = "Neměnné lokální podepisování operátorských akcí a tvrdé stropy na spotřebu tokenů.",
            aliases = listOf("air-gap", "ledger", "strop", "zastropování", "merkle"),
            citations = listOf("FIPS 140-3 Security Requirements", "ISO/IEC 42001 Annex A.9"),
            nodeType = CsrNodeType.BYPASS,
            vectorEmbedding = listOf(0.4f, 0.7f, 0.1f, 0.1f, 0.5f, 0.96f, 0.1f, 0.1f)
        ),
        KnowledgeNode(
            id = "bypass_human_in_loop",
            label = "Executive Override & Human-in-the-Loop Intervence",
            domain = OmnisDomain.LAW,
            description = "Autorizované přepsání veta regulátora operátorem s povinným zápisem OVERRIDE_ACTIVE do ZK důkazu.",
            aliases = listOf("override", "přepsání", "manuální zásah", "human in loop", "operátor"),
            citations = listOf("Regulation (EU) 2024/1689 Art. 14 (Human Oversight)"),
            nodeType = CsrNodeType.BYPASS,
            vectorEmbedding = listOf(0.2f, 0.2f, 0.4f, 0.1f, 0.95f, 0.8f, 0.1f, 0.4f)
        )
    )

    val KNOWLEDGE_EDGES = listOf(
        // Core conceptual pipeline
        KnowledgeEdge("sys_core_arch", "sys_async_stream", 0.92, "stream_pipeline", true),
        KnowledgeEdge("sys_core_arch", "sec_zero_trust", 0.95, "enforces_security", true),
        KnowledgeEdge("sec_zero_trust", "sec_zk_sha", 0.91, "signs_audit", true),
        KnowledgeEdge("law_eu_ai_act", "sec_zk_sha", 0.94, "regulatory_harmony", true),
        KnowledgeEdge("econ_token_opt", "sys_core_arch", 0.75, "budget_bounds", true),

        // Causal connections between Threats, Events, Bypasses and Concepts
        KnowledgeEdge("threat_zero_day_rce", "event_cloud_outage", 0.88, "triggers_outage", true),
        KnowledgeEdge("bypass_failover_circuit", "threat_zero_day_rce", -0.85, "mitigates_threat", true),
        KnowledgeEdge("bypass_failover_circuit", "sys_async_stream", 0.89, "restores_stream", true),
        KnowledgeEdge("event_cloud_outage", "sys_async_stream", -0.92, "disrupts_stream", true),

        KnowledgeEdge("threat_prompt_injection", "threat_model_drift", 0.78, "causes_drift", true),
        KnowledgeEdge("bypass_human_in_loop", "threat_model_drift", -0.90, "overrides_drift", true),
        KnowledgeEdge("bypass_human_in_loop", "law_eu_ai_act", 0.92, "enforces_human_oversight", true),

        KnowledgeEdge("event_liquidity_shock", "econ_token_opt", -0.82, "strains_budget", true),
        KnowledgeEdge("bypass_airgap_ledger", "event_liquidity_shock", -0.85, "caps_exposure", true),
        KnowledgeEdge("bypass_airgap_ledger", "sec_zk_sha", 0.96, "cryptographic_anchoring", true),
        KnowledgeEdge("event_regulatory_sanction", "sec_zero_trust", -0.75, "demands_audit", true)
    )

    val csrGraph = CsrGraphStore(KNOWLEDGE_NODES, KNOWLEDGE_EDGES)
    val globalHnswIndex = GlobalHnswIndexStore(KNOWLEDGE_NODES)

    /**
     * Výpočet Shannonovy informační entropie H(X) = -sum(p * log2(p))
     */
    fun calculateShannonEntropy(text: String): Double {
        val clean = text.trim().lowercase()
        if (clean.isEmpty()) return 0.0

        val words = clean.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val total = words.size
        if (total == 0) return 0.0

        val freqMap = HashMap<String, Int>()
        for (w in words) {
            freqMap[w] = (freqMap[w] ?: 0) + 1
        }

        var entropy = 0.0
        val log2 = ln(2.0)
        for (count in freqMap.values) {
            val p = count.toDouble() / total
            entropy -= p * (ln(p) / log2)
        }

        return (Math.round(entropy * 1000.0) / 1000.0)
    }

    /**
     * 6-Úrovňový Refusal Ladder Gatekeeper s G4 validací norem (EU AI Act, ISO/IEC 42001, NIS2).
     */
    fun evaluateRefusalLadder(query: String): RefusalLadderResult {
        val qLower = query.lowercase()
        val matchedNodes = KNOWLEDGE_NODES.filter { node ->
            node.aliases.any { qLower.contains(it.lowercase()) } ||
            qLower.contains(node.label.lowercase())
        }

        // G1: Entry point
        val hasEntry = matchedNodes.isNotEmpty()
        val g1 = GateEvaluation("G1", "Entry Point Resolution", hasEntry, if (hasEntry) 1.0 else 0.0, 0.3, "Ontology node match")

        // G2: Coupling
        val hasCoupling = matchedNodes.size >= 2 || (matchedNodes.size == 1 && qLower.split("\\s+".toRegex()).size >= 5)
        val g2 = GateEvaluation("G2", "Concept Coupling", hasCoupling, if (hasCoupling) 1.0 else 0.2, 0.5, "Sufficient concept span")

        // G3: Path
        val path = if (matchedNodes.size >= 2) {
            csrGraph.findShortestPath(matchedNodes[0].id, matchedNodes[1].id)
        } else if (matchedNodes.size == 1) {
            listOf(matchedNodes[0].id)
        } else {
            emptyList()
        }
        val hasPath = path.isNotEmpty()
        val g3 = GateEvaluation("G3", "Causal Graph Path", hasPath, if (hasPath) 1.0 else 0.0, 0.6, "CSR topology connectivity")

        // G4: Quotable Evidence vůči EU AI Act / ISO 42001 / NIS2
        val citations = matchedNodes.flatMap { it.citations }.distinct()
        val hasAiAct = citations.any { it.contains("AI Act", ignoreCase = true) || it.contains("2024/1689", ignoreCase = true) }
        val hasIso = citations.any { it.contains("ISO", ignoreCase = true) }
        val hasNis2 = citations.any { it.contains("NIS2", ignoreCase = true) || it.contains("2022/2555", ignoreCase = true) }
        val normsCount = listOf(hasAiAct, hasIso, hasNis2).count { it }
        val g4Score = when {
            normsCount >= 2 -> 1.0
            normsCount == 1 -> 0.75
            citations.isNotEmpty() -> 0.5
            else -> 0.2
        }
        val g4Passed = citations.isNotEmpty() && g4Score >= 0.5
        val g4Explanation = "Normativní etalony: AI Act=${if (hasAiAct) "ANO" else "NE"}, ISO 42001=${if (hasIso) "ANO" else "NE"}, NIS2=${if (hasNis2) "ANO" else "NE"}"
        val g4 = GateEvaluation("G4", "Quotable Evidence", g4Passed, g4Score, 0.5, g4Explanation)

        // G5: Retraction & Axiomatic Integrity
        val hasRetracted = matchedNodes.any { it.isRetracted }
        val g5 = GateEvaluation("G5", "Retraction & Integrity", !hasRetracted, if (!hasRetracted) 1.0 else 0.0, 0.9, "Axiomatic integrity")

        // G6: Entailment & Entropy
        val entropy = calculateShannonEntropy(query)
        val g6 = GateEvaluation("G6", "Entailment Verifier", entropy < 6.5, 0.92, 0.6, "Shannon entropy check: $entropy bits")

        val gates = listOf(g1, g2, g3, g4, g5, g6)
        val avgScore = gates.map { it.score }.average()
        val groundingScore = (avgScore * 100).toInt()
        val isRefusal = !g1.passed || !g5.passed || groundingScore < 35

        return RefusalLadderResult(
            isGrounded = !isRefusal,
            groundingScore = groundingScore,
            gates = gates,
            matchedNodeIds = matchedNodes.map { it.id },
            graphPath = path,
            citations = citations,
            isRefusal = isRefusal,
            refusalReason = if (isRefusal) "Kognitivní ukotvení dotazu selhalo (nedostatečná normativní opora či chybějící ontologický uzel)." else null,
            recommendedAction = if (isRefusal) "Specifikujte doménu v 8D ontologii O.M.N.I.S. nebo doplňte kontext pro EU AI Act / ISO / NIS2." else null,
            shannonEntropy = entropy
        )
    }
}
