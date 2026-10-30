package com.example.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import kotlin.math.abs

/**
 * CSR (Compressed Sparse Row) reprezentace řídké matice pro 8D kognitivní graf.
 */
data class AkgeCsrMatrix(
    val rowPointers: IntArray,
    val columnIndices: IntArray,
    val values: FloatArray,
    val dimension: Int
) {
    /**
     * Spočte násobení matice s vektorem: y = M * x
     */
    fun multiplyVector(vector: FloatArray): FloatArray {
        require(vector.size == dimension) { "Dimenze vektoru (${vector.size}) neodpovídá matici ($dimension)" }
        val result = FloatArray(dimension)
        for (i in 0 until dimension) {
            var sum = 0.0f
            val start = rowPointers[i]
            val end = rowPointers[i + 1]
            for (k in start until end) {
                val j = columnIndices[k]
                sum += values[k] * vector[j]
            }
            result[i] = sum
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AkgeCsrMatrix
        if (!rowPointers.contentEquals(other.rowPointers)) return false
        if (!columnIndices.contentEquals(other.columnIndices)) return false
        if (!values.contentEquals(other.values)) return false
        return dimension == other.dimension
    }

    override fun hashCode(): Int {
        var result = rowPointers.contentHashCode()
        result = 31 * result + columnIndices.contentHashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + dimension
        return result
    }
}

data class AkgeNode(
    val id: String,
    val domain: String,
    val weight: Float,
    val epistemicStatus: String = "VERIFIED"
)

data class AkgeEdge(
    val sourceDomain: String,
    val targetDomain: String,
    val couplingWeight: Float,
    val causalDirection: String = "BIDIRECTIONAL"
)

data class AkgeKernelSnapshot(
    val timestamp: Long,
    val domainWeights: Map<String, Float>,
    val csrMatrix: AkgeCsrMatrix,
    val topologicalMetrics: TopologicalMetrics,
    val percolationStatus: PercolationStatus,
    val systemStressSigma: Float,
    val stateCommitmentHash: String,
    val activeAnomaliesCount: Int
)

/**
 * AKGE-8D Tenzorový Grafový Kernel (Agentic Knowledge Graph Engine)
 *
 * Implementuje optimalizovaný kognitivní kernel pro O.M.N.I.S. v4.2 / AKGE-8.
 * Poskytuje řídkou CSR maticovou reprezentaci vazeb mezi 8 doménami (SYS, ECON, PSYCH, ECO, LAW, SEC, PHYS, SOC),
 * difúzi tenzorového toku a deterministické kryptografické otisky stavu.
 */
object AkgeTensorKernel {

    val DOMAIN_KEYS = listOf("SYS", "ECON", "PSYCH", "ECO", "LAW", "SEC", "PHYS", "SOC")

    private val _kernelState = MutableStateFlow(createDefaultSnapshot())
    val kernelState: StateFlow<AkgeKernelSnapshot> = _kernelState.asStateFlow()

    fun createDefaultSnapshot(): AkgeKernelSnapshot {
        val initialWeights = mapOf(
            "SYS" to 0.85f,
            "ECON" to 0.72f,
            "PSYCH" to 0.65f,
            "ECO" to 0.58f,
            "LAW" to 0.90f,
            "SEC" to 0.92f,
            "PHYS" to 0.80f,
            "SOC" to 0.70f
        )
        val defaultEdges = listOf(
            AkgeEdge("SYS", "SEC", 0.85f),
            AkgeEdge("SEC", "LAW", 0.75f),
            AkgeEdge("SYS", "PHYS", 0.80f),
            AkgeEdge("ECON", "SYS", 0.60f),
            AkgeEdge("ECO", "ECON", 0.65f),
            AkgeEdge("PSYCH", "SOC", 0.70f),
            AkgeEdge("SOC", "LAW", 0.65f),
            AkgeEdge("SEC", "PHYS", 0.50f)
        )
        val csr = buildCsrMatrix(defaultEdges, DOMAIN_KEYS)
        val domainStatuses = initialWeights.mapValues { if (it.value >= 0.5f) "STABLE" else "STRESSED" }

        val topoMetrics = TopologicalInferenceEngine.evaluateTopologicalMetrics(
            domainScores = initialWeights,
            customNodesCount = 8,
            customEdgesCount = defaultEdges.size,
            customFacesCount = 4
        )
        val percolation = TopologicalInferenceEngine.evalCascadeRisk(domainStatuses)
        val sigma = computeSystemStress(initialWeights)
        val hash = computeStateCommitment(initialWeights, sigma, System.currentTimeMillis())

        return AkgeKernelSnapshot(
            timestamp = System.currentTimeMillis(),
            domainWeights = initialWeights,
            csrMatrix = csr,
            topologicalMetrics = topoMetrics,
            percolationStatus = percolation,
            systemStressSigma = sigma,
            stateCommitmentHash = hash,
            activeAnomaliesCount = if (percolation.isCascadeRiskActive) 1 else 0
        )
    }

    /**
     * Vytvoří CSR matici z hran a seznamu domén.
     */
    fun buildCsrMatrix(edges: List<AkgeEdge>, domains: List<String>): AkgeCsrMatrix {
        val n = domains.size
        val domainIndexMap = domains.withIndex().associate { it.value to it.index }

        // Sestavení seznamu sousedů pro každý řádek (i -> list of (j, weight))
        val rowEntries = Array(n) { mutableListOf<Pair<Int, Float>>() }
        for (edge in edges) {
            val srcIdx = domainIndexMap[edge.sourceDomain] ?: continue
            val dstIdx = domainIndexMap[edge.targetDomain] ?: continue
            rowEntries[srcIdx].add(Pair(dstIdx, edge.couplingWeight))
            if (edge.causalDirection == "BIDIRECTIONAL") {
                rowEntries[dstIdx].add(Pair(srcIdx, edge.couplingWeight))
            }
        }

        val rowPointers = IntArray(n + 1)
        val colList = mutableListOf<Int>()
        val valList = mutableListOf<Float>()

        for (i in 0 until n) {
            rowPointers[i] = colList.size
            // Seřadit podle indexu sloupce
            val sorted = rowEntries[i].sortedBy { it.first }
            for (entry in sorted) {
                colList.add(entry.first)
                valList.add(entry.second)
            }
        }
        rowPointers[n] = colList.size

        return AkgeCsrMatrix(
            rowPointers = rowPointers,
            columnIndices = colList.toIntArray(),
            values = valList.toFloatArray(),
            dimension = n
        )
    }

    /**
     * Spočte difúzi tenzorového pole: T_new = α * T + (1-α) * (M_CSR * T_norm)
     */
    fun propagateTensorField(
        currentWeights: Map<String, Float>,
        csr: AkgeCsrMatrix,
        alpha: Float = 0.70f
    ): Map<String, Float> {
        val n = DOMAIN_KEYS.size
        val vector = FloatArray(n) { idx ->
            currentWeights[DOMAIN_KEYS[idx]] ?: 0.5f
        }

        // Násobení CSR maticí
        val diffused = csr.multiplyVector(vector)

        // Normalizace a lineární kombinace s předchozím stavem
        val updated = mutableMapOf<String, Float>()
        for (i in 0 until n) {
            val domain = DOMAIN_KEYS[i]
            val prevVal = vector[i]
            val start = csr.rowPointers[i]
            val end = csr.rowPointers[i + 1]
            var weightSum = 0.0f
            for (k in start until end) {
                weightSum += csr.values[k]
            }
            val normFactor = if (weightSum > 0.001f) weightSum else 1.0f
            val diffVal = (diffused[i] / normFactor).coerceIn(0.0f, 1.0f)
            val finalVal = (alpha * prevVal + (1.0f - alpha) * diffVal).coerceIn(0.05f, 1.0f)
            updated[domain] = (finalVal * 100f).toInt() / 100f
        }
        return updated
    }

    fun computeSystemStress(weights: Map<String, Float>): Float {
        var stressAcc = 0.0f
        weights.values.forEach { w ->
            if (w < 0.5f) {
                stressAcc += (0.5f - w) * 2.0f
            }
        }
        return (stressAcc / DOMAIN_KEYS.size).coerceIn(0.0f, 1.0f)
    }

    fun computeStateCommitment(weights: Map<String, Float>, sigma: Float, timestamp: Long): String {
        val payload = "AKGE-8D-$timestamp-${weights.entries.sortedBy { it.key }.joinToString { "${it.key}:${it.value}" }}-SIGMA:$sigma"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray())
        return digest.take(8).joinToString("") { "%02X".format(it) }
    }

    /**
     * Provede hromadnou aktualizaci vah pro více domén najednou.
     */
    fun updateDomainWeights(newWeightsMap: Map<String, Float>) {
        val current = _kernelState.value
        val updatedWeights = current.domainWeights.toMutableMap()
        newWeightsMap.forEach { (domain, w) ->
            updatedWeights[domain] = w.coerceIn(0.0f, 1.0f)
        }

        val diffusedWeights = propagateTensorField(updatedWeights, current.csrMatrix)
        val domainStatuses = diffusedWeights.mapValues { if (it.value >= 0.5f) "STABLE" else "STRESSED" }

        val topoMetrics = TopologicalInferenceEngine.evaluateTopologicalMetrics(
            domainScores = diffusedWeights,
            customNodesCount = 8,
            customEdgesCount = current.csrMatrix.values.size / 2,
            customFacesCount = 4
        )
        val percolation = TopologicalInferenceEngine.evalCascadeRisk(domainStatuses)
        val sigma = computeSystemStress(diffusedWeights)
        val hash = computeStateCommitment(diffusedWeights, sigma, System.currentTimeMillis())

        _kernelState.value = AkgeKernelSnapshot(
            timestamp = System.currentTimeMillis(),
            domainWeights = diffusedWeights,
            csrMatrix = current.csrMatrix,
            topologicalMetrics = topoMetrics,
            percolationStatus = percolation,
            systemStressSigma = sigma,
            stateCommitmentHash = hash,
            activeAnomaliesCount = if (percolation.isCascadeRiskActive) 1 else 0
        )
    }

    /**
     * Provede krok simulace nebo aplikaci nového doménového podnětu.
     */
    fun updateDomainWeight(domain: String, newWeight: Float) {
        updateDomainWeights(mapOf(domain to newWeight))
    }

    /**
     * Injektuje poruchu (např. výpadek SEC a SYS) pro zátěžové testování kaskád.
     */
    fun injectCascadeStress(affectedDomains: List<String>) {
        val current = _kernelState.value
        val updatedWeights = current.domainWeights.toMutableMap()
        for (domain in affectedDomains) {
            updatedWeights[domain] = 0.15f
        }

        val domainStatuses = updatedWeights.mapValues { if (it.value >= 0.5f) "STABLE" else "STRESSED" }
        val topoMetrics = TopologicalInferenceEngine.evaluateTopologicalMetrics(
            domainScores = updatedWeights,
            customNodesCount = 8,
            customEdgesCount = current.csrMatrix.values.size / 2,
            customFacesCount = 4
        )
        val percolation = TopologicalInferenceEngine.evalCascadeRisk(domainStatuses)
        val sigma = computeSystemStress(updatedWeights)
        val hash = computeStateCommitment(updatedWeights, sigma, System.currentTimeMillis())

        _kernelState.value = AkgeKernelSnapshot(
            timestamp = System.currentTimeMillis(),
            domainWeights = updatedWeights,
            csrMatrix = current.csrMatrix,
            topologicalMetrics = topoMetrics,
            percolationStatus = percolation,
            systemStressSigma = sigma,
            stateCommitmentHash = hash,
            activeAnomaliesCount = percolation.failingDomains
        )
    }

    /**
     * Resetuje kernel na výchozí nominální stav.
     */
    fun resetToEquilibrium() {
        _kernelState.value = createDefaultSnapshot()
    }
}
