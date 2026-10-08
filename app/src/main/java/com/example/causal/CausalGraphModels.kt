package com.example.causal

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

/**
 * Typy uzlů pro kauzální CSR graf
 */
enum class CsrNodeType { CONCEPT, EVENT, THREAT, BYPASS }

/**
 * Typy hran pro kauzální CSR graf
 */
enum class CsrEdgeType { CAUSAL, INFLUENCE, MITIGATION }

/**
 * Reprezentace uzlu v kauzálním grafu (CSR - Causal Structural Representation)
 */
data class CsrNode(
    val id: String,
    val name: String,
    val type: CsrNodeType,
    val description: String,
    val vectorEmbedding: FloatArray? = null // Pro HNSW indexaci
)

/**
 * Kauzální orientovaná hrana v CSR grafu
 */
data class CsrEdge(
    val sourceId: String,
    val targetId: String,
    val type: CsrEdgeType,
    val weight: Float,
    val mechanismDescription: String
)

/**
 * Reprezentace uzlu v kauzálním grafu (SCM - Structural Causal Model)
 */
data class CausalNode(
    val id: String,
    val name: String,
    val description: String,
    val color: Color,
    val defaultNormalizedX: Float, // Relativní pozice pro 2D DAG Canvas (0..1)
    val defaultNormalizedY: Float
)

/**
 * Kauzální orientovaná hrana: source -> target s kauzální vahou
 */
data class CausalEdge(
    val sourceId: String,
    val targetId: String,
    val weight: Float, // Síla a polarita kauzálního vlivu (-1.0f .. +1.0f)
    val mechanismDescription: String
)

/**
 * Výsledek intervence pro jeden uzel
 */
data class NodeInterventionResult(
    val nodeId: String,
    val nodeName: String,
    val color: Color,
    val observedValue: Float,       // P(Y | X) - observational baseline
    val interventionalValue: Float,   // P(Y | do(X = v)) - post-intervention outcome
    val delta: Float,                // interventionalValue - observedValue
    val isDirectlyIntervened: Boolean, // Byl tento uzel cílem operátoru do()
    val impactClassification: String
)

/**
 * Celkový stav kauzální simulace
 */
data class CausalSimulationState(
    val activeInterventions: Map<String, Float> = emptyMap(), // nodeId -> clamped value
    val nodeResults: List<NodeInterventionResult> = emptyList(),
    val severedEdges: Set<Pair<String, String>> = emptySet(), // Hrany odříznuté Do-operátorem
    val systemicResilienceScore: Float = 0.5f,
    val cascadeRiskIndex: Float = 0.2f,
    val activePresetName: String? = null
)

/**
 * Knihovna předdefinovaných scénářů pro okamžité testování
 */
data class CausalPreset(
    val id: String,
    val title: String,
    val description: String,
    val interventions: Map<String, Float>
)
