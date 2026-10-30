package com.example.engine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AkgeAutonomousStabilizerTest {

    @Before
    fun setUp() {
        AkgeTensorKernel.resetToEquilibrium()
        AkgeAutonomousStabilizer.resetStabilizerState()
    }

    @Test
    fun `executeStabilization dampens cascade stress and heals failing domains`() = runTest {
        // 1. Inject critical stress across 4 domains (4/8 = 50% >= 38% critical threshold)
        AkgeTensorKernel.injectCascadeStress(listOf("SYS", "SEC", "LAW", "PHYS"))
        val stressedSnapshot = AkgeTensorKernel.kernelState.value
        assertTrue(stressedSnapshot.percolationStatus.isCascadeRiskActive)
        assertTrue(stressedSnapshot.systemStressSigma >= 0.25f)

        // 2. Execute fast stabilization
        val event = AkgeAutonomousStabilizer.executeStabilization(
            reason = "Test cascade trigger",
            fastMode = true
        )

        // 3. Verify stabilization results
        assertNotNull(event)
        assertTrue(event.resolvedStressSigma < event.initialStressSigma)
        assertTrue(event.consensusIndex >= 0.70f)
        assertTrue(event.zkCommitmentHash.isNotEmpty())
        assertEquals(16, event.zkCommitmentHash.length) // 8 bytes = 16 hex chars

        // 4. Verify kernel snapshot after stabilization
        val healedSnapshot = AkgeTensorKernel.kernelState.value
        assertFalse(healedSnapshot.percolationStatus.isCascadeRiskActive)
        assertTrue(healedSnapshot.domainWeights["SYS"]!! >= 0.70f)
        assertTrue(healedSnapshot.domainWeights["SEC"]!! >= 0.70f)
        assertTrue(healedSnapshot.domainWeights["LAW"]!! >= 0.70f)
        assertTrue(healedSnapshot.domainWeights["PHYS"]!! >= 0.70f)

        // 5. Verify stabilizer state
        val state = AkgeAutonomousStabilizer.stabilizerState.value
        assertEquals(StabilizationStatus.STABILIZED, state.status)
        assertEquals(1, state.totalStabilizationsCount)
        assertNotNull(state.lastEvent)
    }

    @Test
    fun `toggle autonomous trigger updates configuration flag`() {
        AkgeAutonomousStabilizer.setAutonomousTriggerEnabled(false)
        assertFalse(AkgeAutonomousStabilizer.stabilizerState.value.isAutonomousTriggerEnabled)

        AkgeAutonomousStabilizer.setAutonomousTriggerEnabled(true)
        assertTrue(AkgeAutonomousStabilizer.stabilizerState.value.isAutonomousTriggerEnabled)
    }

    @Test
    fun `resetStabilizerState clears history and resets to idle`() = runTest {
        AkgeTensorKernel.injectCascadeStress(listOf("SYS", "PHYS"))
        AkgeAutonomousStabilizer.executeStabilization(fastMode = true)
        assertEquals(1, AkgeAutonomousStabilizer.stabilizerState.value.totalStabilizationsCount)

        AkgeAutonomousStabilizer.resetStabilizerState()
        assertEquals(StabilizationStatus.IDLE, AkgeAutonomousStabilizer.stabilizerState.value.status)
        assertEquals(0, AkgeAutonomousStabilizer.stabilizerState.value.totalStabilizationsCount)
        assertNull(AkgeAutonomousStabilizer.stabilizerState.value.lastEvent)
    }
}
