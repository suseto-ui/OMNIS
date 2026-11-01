package com.example.engine

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AkgeTensorKernelTest {

    @Test
    fun `CSR matrix multiplication calculates correct dot product`() {
        // Simple 3x3 matrix:
        // [ 2, 0, 1 ]
        // [ 0, 3, 0 ]
        // [ 1, 0, 4 ]
        val rowPointers = intArrayOf(0, 2, 3, 5)
        val colIndices = intArrayOf(0, 2, 1, 0, 2)
        val values = floatArrayOf(2.0f, 1.0f, 3.0f, 1.0f, 4.0f)
        val matrix = AkgeCsrMatrix(rowPointers, colIndices, values, dimension = 3)

        val vector = floatArrayOf(1.0f, 2.0f, 3.0f)
        val result = matrix.multiplyVector(vector)

        // y[0] = 2*1 + 1*3 = 5
        // y[1] = 3*2 = 6
        // y[2] = 1*1 + 4*3 = 13
        assertEquals(3, result.size)
        assertEquals(5.0f, result[0], 0.001f)
        assertEquals(6.0f, result[1], 0.001f)
        assertEquals(13.0f, result[2], 0.001f)
    }

    @Test
    fun `buildCsrMatrix creates valid non-empty structure for 8D domain connections`() {
        val edges = listOf(
            AkgeEdge("SYS", "SEC", 0.9f),
            AkgeEdge("SEC", "LAW", 0.8f),
            AkgeEdge("SYS", "PHYS", 0.7f)
        )
        val matrix = AkgeTensorKernel.buildCsrMatrix(edges, AkgeTensorKernel.DOMAIN_KEYS)

        assertEquals(AkgeTensorKernel.DOMAIN_KEYS.size, matrix.dimension)
        assertTrue(matrix.values.isNotEmpty())
        assertEquals(matrix.dimension + 1, matrix.rowPointers.size)
    }

    @Test
    fun `default snapshot initializes with valid topology and consensus hash`() {
        val snapshot = AkgeTensorKernel.createDefaultSnapshot()

        assertEquals(8, snapshot.domainWeights.size)
        assertTrue(snapshot.stateCommitmentHash.isNotEmpty())
        assertEquals(16, snapshot.stateCommitmentHash.length) // 8 bytes in hex
        assertTrue(snapshot.topologicalMetrics.isValid)
        assertFalse(snapshot.percolationStatus.isCascadeRiskActive)
        assertEquals(0, snapshot.activeAnomaliesCount)
    }

    @Test
    fun `injectCascadeStress triggers percolation alert and increases system stress sigma`() {
        AkgeTensorKernel.resetToEquilibrium()
        val initialSigma = AkgeTensorKernel.kernelState.value.systemStressSigma

        // Inject critical failure across 4 domains (50% >= 38%)
        AkgeTensorKernel.injectCascadeStress(listOf("SYS", "SEC", "PHYS", "LAW"))

        val stressedState = AkgeTensorKernel.kernelState.value
        assertTrue(stressedState.percolationStatus.isCascadeRiskActive)
        assertTrue(stressedState.systemStressSigma > initialSigma)
        assertTrue(stressedState.activeAnomaliesCount >= 4)

        // Reset to equilibrium
        AkgeTensorKernel.resetToEquilibrium()
        assertFalse(AkgeTensorKernel.kernelState.value.percolationStatus.isCascadeRiskActive)
    }

    @Test
    fun `updateDomainWeight recalculates state hash and propagates changes`() {
        AkgeTensorKernel.resetToEquilibrium()
        val oldHash = AkgeTensorKernel.kernelState.value.stateCommitmentHash

        AkgeTensorKernel.updateDomainWeight("SEC", 0.20f)
        val newHash = AkgeTensorKernel.kernelState.value.stateCommitmentHash

        assertNotEquals(oldHash, newHash)
        assertTrue(AkgeTensorKernel.kernelState.value.domainWeights.containsKey("SEC"))
    }
}
