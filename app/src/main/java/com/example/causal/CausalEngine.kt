package com.example.causal

import androidx.compose.ui.graphics.Color
import com.example.data.OmnisRecord
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Kauzální kalkulátor pro Do(X) intervence dle Judea Pearl's Do-Calculus
 * Provádí grafovou chirurgii (severing parents) a propagaci kauzálních toků po DAGu.
 */
object CausalEngine {

    val nodes = listOf(
        CausalNode("SYS", "Systém", "Architektura jádra a výpočetní orchestrace", OmnisCyan, 0.22f, 0.20f),
        CausalNode("ECON", "Ekonomika", "Nákladová efektivita a finanční výkon", OmnisAmber, 0.50f, 0.15f),
        CausalNode("PSYCH", "Psychologie", "Uživatelská důvěra a kognitivní zátěž", OmnisViolet, 0.82f, 0.25f),
        CausalNode("ECO", "Ekologie", "Udržitelnost a environmentální stopa", OmnisEmerald, 0.80f, 0.75f),
        CausalNode("LAW", "Právo & AI Act", "Regulatorní soulad, etika a AI Governance", Color(0xFF60A5FA), 0.48f, 0.85f),
        CausalNode("SEC", "Bezpečnost", "Kryptografická a kybernetická integrita", Color(0xFFEF4444), 0.20f, 0.55f),
        CausalNode("PHYS", "Fyzická vrstva", "Hardware, telemetrie a edge senzory", Color(0xFFFB923C), 0.15f, 0.85f),
        CausalNode("SOC", "Společnost", "Společenská akceptace a adopce", Color(0xFFEC4899), 0.50f, 0.50f)
    )

    val edges = listOf(
        CausalEdge("PHYS", "SYS", 0.40f, "Dostupnost hardwaru přímo určuje kapacitu systému"),
        CausalEdge("SYS", "ECON", 0.45f, "Automatizace a systémová efektivita snižuje náklady"),
        CausalEdge("SYS", "SEC", 0.35f, "Robustní kód a jádro posilují bezpečnost"),
        CausalEdge("SEC", "LAW", 0.40f, "Technická bezpečnost je nutným předpokladem NIS2/AI Act"),
        CausalEdge("SEC", "PSYCH", 0.30f, "Pocit kybernetického bezpečí snižuje stres"),
        CausalEdge("ECON", "ECO", -0.25f, "Nekontrolovaný růst zvyšuje spotřebu zdrojů"),
        CausalEdge("ECON", "SOC", 0.30f, "Ekonomická stabilita zvyšuje spokojenost společnosti"),
        CausalEdge("LAW", "SOC", 0.35f, "Férová pravidla a AI etika budují důvěru veřejnosti"),
        CausalEdge("ECO", "PSYCH", 0.25f, "Udržitelná řešení posilují etickou integritu"),
        CausalEdge("SOC", "ECON", 0.20f, "Důvěra trhu podporuje investice a růst"),
        CausalEdge("LAW", "SYS", -0.15f, "Striktní regulatorní audity přinášejí režijní náklady")
    )

    val presets = listOf(
        CausalPreset(
            id = "ai_act_strict",
            title = "Striktní AI Act & NIS2",
            description = "Intervence do(LAW = 0.98) a do(SEC = 0.92) pro dosažení maximálního souladu s regulací.",
            interventions = mapOf("LAW" to 0.98f, "SEC" to 0.92f)
        ),
        CausalPreset(
            id = "cyber_zero_day",
            title = "Zero-Day Kybernetický Šok",
            description = "Katastrofické narušení bezpečnosti do(SEC = 0.15) — modelování kaskádového selhání.",
            interventions = mapOf("SEC" to 0.15f)
        ),
        CausalPreset(
            id = "quantum_tech_leap",
            title = "Kvantový Technologický Skok",
            description = "Masivní intervence do výpočetní kapacity a fyzické vrstvy do(SYS = 0.95), do(PHYS = 0.90).",
            interventions = mapOf("SYS" to 0.95f, "PHYS" to 0.90f)
        ),
        CausalPreset(
            id = "green_esg_shift",
            title = "Zelená ESG Transformace",
            description = "Přímý důraz na nulovou uhlíkovou stopu do(ECO = 0.95) a analýza dopadů na ekonomiku.",
            interventions = mapOf("ECO" to 0.95f)
        )
    )

    /**
     * Spočte kauzální stav po aplikaci intervencí do(X = v)
     */
    fun simulate(
        baselineRecord: OmnisRecord?,
        interventions: Map<String, Float>,
        presetName: String? = null
    ): CausalSimulationState {
        val observedMap = mutableMapOf<String, Float>()
        if (baselineRecord != null) {
            observedMap["SYS"] = baselineRecord.valSys
            observedMap["ECON"] = baselineRecord.valEcon
            observedMap["PSYCH"] = baselineRecord.valPsych
            observedMap["ECO"] = baselineRecord.valEco
            observedMap["LAW"] = baselineRecord.valLaw
            observedMap["SEC"] = baselineRecord.valSec
            observedMap["PHYS"] = baselineRecord.valPhys
            observedMap["SOC"] = baselineRecord.valSoc
        } else {
            // Výchozí průměrné hodnoty
            nodes.forEach { observedMap[it.id] = 0.60f }
        }

        // Grafová chirurgie: identifikace odříznutých hran (všechny hrany směřující DO intervenovaného uzlu)
        val severedEdges = mutableSetOf<Pair<String, String>>()
        for (edge in edges) {
            if (interventions.containsKey(edge.targetId)) {
                severedEdges.add(edge.sourceId to edge.targetId)
            }
        }

        // Výpočet po intervenci:
        // Pro intervenované uzly je hodnota pevně fixována do(X = v)
        // Pro ostatní uzly se šíří kauzální efekt podél neodříznutých hran
        val interventionalMap = observedMap.toMutableMap()
        interventions.forEach { (nodeId, clampedVal) ->
            interventionalMap[nodeId] = clamp(clampedVal)
        }

        // Propagace vlivů (3 iterační kroky k dosažení ekvilibria v DAGu s tlumením)
        val damping = 0.65f
        for (pass in 0 until 4) {
            for (edge in edges) {
                // Pokud je hrana odříznuta, nepropouští vliv!
                if (severedEdges.contains(edge.sourceId to edge.targetId)) {
                    continue
                }
                // Pokud je cílový uzel přímo fixován intervencí, jeho hodnota se nemění!
                if (interventions.containsKey(edge.targetId)) {
                    continue
                }

                val sourceDelta = interventionalMap[edge.sourceId]!! - observedMap[edge.sourceId]!!
                if (abs(sourceDelta) > 0.001f) {
                    val currentVal = interventionalMap[edge.targetId]!!
                    val propagatedInfluence = sourceDelta * edge.weight * damping
                    interventionalMap[edge.targetId] = clamp(currentVal + propagatedInfluence)
                }
            }
        }

        // Konstrukce výsledků pro každý uzel
        val results = nodes.map { node ->
            val obs = observedMap[node.id] ?: 0.5f
            val post = interventionalMap[node.id] ?: obs
            val delta = post - obs
            val isIntervened = interventions.containsKey(node.id)

            val classification = when {
                isIntervened -> "Přímá intervence do(${node.id})"
                delta > 0.15f -> "Silný pozitivní spillover"
                delta > 0.03f -> "Mírné zlepšení"
                delta < -0.15f -> "Kritický negativní protitlak"
                delta < -0.03f -> "Mírné zhoršení"
                else -> "Stabilní bez odezvy"
            }

            NodeInterventionResult(
                nodeId = node.id,
                nodeName = node.name,
                color = node.color,
                observedValue = obs,
                interventionalValue = post,
                delta = delta,
                isDirectlyIntervened = isIntervened,
                impactClassification = classification
            )
        }

        // Skóre celkové systémové odolnosti (0..1)
        val avgScore = interventionalMap.values.average().toFloat()
        val secWeight = interventionalMap["SEC"] ?: 0.5f
        val sysWeight = interventionalMap["SYS"] ?: 0.5f
        val lawWeight = interventionalMap["LAW"] ?: 0.5f
        val systemicResilience = clamp((avgScore * 0.4f) + (secWeight * 0.25f) + (sysWeight * 0.20f) + (lawWeight * 0.15f))

        // Index kaskádového rizika (0..1)
        val negativeDeltas = results.filter { it.delta < 0 }.map { abs(it.delta) }
        val cascadeRisk = if (negativeDeltas.isNotEmpty()) clamp(negativeDeltas.sum() / 2.0f) else 0.05f

        return CausalSimulationState(
            activeInterventions = interventions,
            nodeResults = results,
            severedEdges = severedEdges,
            systemicResilienceScore = systemicResilience,
            cascadeRiskIndex = cascadeRisk,
            activePresetName = presetName
        )
    }

    private fun clamp(v: Float): Float = max(0.0f, min(1.0f, v))
}
