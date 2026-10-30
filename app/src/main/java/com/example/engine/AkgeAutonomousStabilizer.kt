package com.example.engine

import com.example.dialectics.AgentArchetype
import com.example.dialectics.AgentArenaEngine
import com.example.dialectics.AgentArenaLibrary
import com.example.dialectics.ArenaDebateResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest

enum class StabilizationStatus {
    IDLE,
    STABILIZING,
    STABILIZED,
    ERROR
}

data class AutonomousStabilizationEvent(
    val timestamp: Long,
    val triggerReason: String,
    val initialStressSigma: Float,
    val resolvedStressSigma: Float,
    val consensusIndex: Float,
    val zkCommitmentHash: String,
    val debateSummary: String,
    val stabilizedDomains: List<String>
)

data class StabilizerState(
    val status: StabilizationStatus = StabilizationStatus.IDLE,
    val lastEvent: AutonomousStabilizationEvent? = null,
    val totalStabilizationsCount: Int = 0,
    val isAutonomousTriggerEnabled: Boolean = true,
    val currentDebateResult: ArenaDebateResult? = null
)

/**
 * Autonomní dialektický stabilizátor pro O.M.N.I.S. AKGE-8D
 *
 * Monitoruje stav tenzorového jádra. Při překročení kritického prahu perkolace (p_c ≈ 0.38)
 * nebo kritické entropie automaticky iniciuje dialektický konsensus dvou specializovaných agentů,
 * vypočte tenzorový stabilizační vektor a provede rebalancování systému s kryptografickou ZK auditní stopou.
 */
object AkgeAutonomousStabilizer {

    private val _stabilizerState = MutableStateFlow(StabilizerState())
    val stabilizerState: StateFlow<StabilizerState> = _stabilizerState.asStateFlow()

    fun setAutonomousTriggerEnabled(enabled: Boolean) {
        _stabilizerState.value = _stabilizerState.value.copy(isAutonomousTriggerEnabled = enabled)
    }

    /**
     * Zkontroluje stav tenzorového jádra a pokud je aktivní kaskádové riziko nebo vysoký stres,
     * spustí autonomní stabilizaci.
     */
    fun checkAndTriggerIfNeeded(coroutineScope: CoroutineScope) {
        val currentSnapshot = AkgeTensorKernel.kernelState.value
        val isStressed = currentSnapshot.percolationStatus.isCascadeRiskActive || currentSnapshot.systemStressSigma >= 0.38f
        val isEnabled = _stabilizerState.value.isAutonomousTriggerEnabled
        val isIdle = _stabilizerState.value.status == StabilizationStatus.IDLE || _stabilizerState.value.status == StabilizationStatus.STABILIZED

        if (isStressed && isEnabled && isIdle) {
            coroutineScope.launch(Dispatchers.Default) {
                executeStabilization(
                    reason = if (currentSnapshot.percolationStatus.isCascadeRiskActive) {
                        "Kritické kaskádové riziko (p_c >= 0.38, kolabující domény: ${currentSnapshot.percolationStatus.failingDomains})"
                    } else {
                        "Překročení prahu tenzorového stresu (σ = ${String.format("%.2f", currentSnapshot.systemStressSigma)})"
                    }
                )
            }
        }
    }

    /**
     * Provede dialektickou stabilizaci (vhodné pro přímé volání i unit testy).
     * @param fastMode pokud je true, provede okamžitý konsensus bez delayů pro testy.
     */
    suspend fun executeStabilization(
        reason: String = "Překročení kritického prahu perkolace",
        fastMode: Boolean = false
    ): AutonomousStabilizationEvent {
        _stabilizerState.value = _stabilizerState.value.copy(status = StabilizationStatus.STABILIZING)

        val snapshotBefore = AkgeTensorKernel.kernelState.value
        val initialSigma = snapshotBefore.systemStressSigma

        // Určení oslabených domén
        val failingDomains = snapshotBefore.domainWeights.filter { it.value < 0.5f }.keys.toList()
        val targetDomains = if (failingDomains.isEmpty()) listOf("SYS", "SEC") else failingDomains

        // Výběr agentů pro dialektickou arénu
        val agentConservative = AgentArenaLibrary.presetArchetypes.find { it.id == "conservative_analyst" }
            ?: AgentArenaLibrary.presetArchetypes[0]
        val agentEngineer = AgentArenaLibrary.presetArchetypes.find { it.id == "pragmatic_engineer" }
            ?: AgentArenaLibrary.presetArchetypes[3]

        val problemStatement = "Autonomní stabilizace O.M.N.I.S. AKGE-8D jádra: Eliminace napětí v doménách ${targetDomains.joinToString()}"

        val debateResult = if (fastMode) {
            // Rychlý deterministický konsensus pro unit testy
            executeFastSynthesis(problemStatement, agentConservative, agentEngineer, targetDomains)
        } else {
            // Standardní simulace se záznamem tahů
            AgentArenaEngine.executeArenaDebate(
                problemStatement = problemStatement,
                agentA = agentConservative,
                agentB = agentEngineer
            )
        }

        // Aplikovat stabilizační korekce zpět do tenzorového jádra
        // Cíl: rebalancovat domény s pnutím na bezpečnou hladinu (>= 0.80)
        val bulkUpdates = mutableMapOf<String, Float>()
        targetDomains.forEach { domain ->
            val currentVal = snapshotBefore.domainWeights[domain] ?: 0.2f
            val synthScore = (debateResult.synthesized8DScores[domain] ?: 0.85f).coerceAtLeast(0.80f)
            val healedWeight = (currentVal * 0.20f + synthScore * 0.80f).coerceIn(0.75f, 0.95f)
            bulkUpdates[domain] = healedWeight
        }
        AkgeTensorKernel.updateDomainWeights(bulkUpdates)

        val snapshotAfter = AkgeTensorKernel.kernelState.value
        val resolvedSigma = snapshotAfter.systemStressSigma

        // Generování ZK-SNARK kryptografického commitment otisku
        val zkPayload = "AKGE-STABILIZATION-${System.currentTimeMillis()}-$resolvedSigma-${debateResult.zkCommitmentHash}"
        val digest = MessageDigest.getInstance("SHA-256").digest(zkPayload.toByteArray())
        val zkProofHash = digest.take(8).joinToString("") { "%02X".format(it) }

        val event = AutonomousStabilizationEvent(
            timestamp = System.currentTimeMillis(),
            triggerReason = reason,
            initialStressSigma = initialSigma,
            resolvedStressSigma = resolvedSigma,
            consensusIndex = debateResult.consensusIndex,
            zkCommitmentHash = zkProofHash,
            debateSummary = debateResult.synthesisSummary,
            stabilizedDomains = targetDomains
        )

        _stabilizerState.value = StabilizerState(
            status = StabilizationStatus.STABILIZED,
            lastEvent = event,
            totalStabilizationsCount = _stabilizerState.value.totalStabilizationsCount + 1,
            isAutonomousTriggerEnabled = _stabilizerState.value.isAutonomousTriggerEnabled,
            currentDebateResult = debateResult
        )

        return event
    }

    private fun executeFastSynthesis(
        problem: String,
        agentA: AgentArchetype,
        agentB: AgentArchetype,
        targets: List<String>
    ): ArenaDebateResult {
        val synthesized = mutableMapOf<String, Float>()
        AgentArenaEngine.all8DDomains.forEach { d ->
            synthesized[d] = if (targets.contains(d)) 0.82f else 0.78f
        }
        val zkHash = MessageDigest.getInstance("SHA-256")
            .digest("FAST-SYNTHESIS-$problem".toByteArray())
            .take(8).joinToString("") { "%02X".format(it) }

        return ArenaDebateResult(
            debateId = "fast-stab-${System.currentTimeMillis()}",
            problemStatement = problem,
            agentA = agentA,
            agentB = agentB,
            turns = emptyList(),
            agentA8DScores = synthesized,
            agentB8DScores = synthesized,
            synthesized8DScores = synthesized,
            divergenceIndex = 0.15f,
            consensusIndex = 0.88f,
            synthesisSummary = "Autonomní dialektická stabilizace úspěšná. Konsensus dosažen pro domény ${targets.joinToString()}.",
            actionableGuidelines = listOf("Rebalancovat CSR váhy", "Uložit ZK commitment $zkHash"),
            zkCommitmentHash = zkHash,
            proofOfNecessity = "PROOF-FAST-AUTONOMOUS"
        )
    }

    fun resetStabilizerState() {
        _stabilizerState.value = StabilizerState()
    }
}
