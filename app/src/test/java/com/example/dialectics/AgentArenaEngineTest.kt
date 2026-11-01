package com.example.dialectics

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgentArenaEngineTest {

    @Test
    fun `preset archetypes are loaded with distinct epistemics and primary domains`() {
        val archetypes = AgentArenaLibrary.presetArchetypes
        assertTrue(archetypes.size >= 4)

        val conservative = archetypes.find { it.id == "conservative_analyst" }
        assertNotNull(conservative)
        assertEquals("Konzervativní Analytik", conservative!!.name)
        assertTrue(conservative.epistemicRigor > 0.8f)

        val radical = archetypes.find { it.id == "radical_innovator" }
        assertNotNull(radical)
        assertEquals("Radikální Inovátor", radical!!.name)
        assertTrue(radical.riskTolerance > 0.7f)
    }

    @Test
    fun `preset debate history contains cryptographic commitments and valid synthesized scores`() {
        val history = AgentArenaEngine.debateHistoryState.value
        assertTrue(history.isNotEmpty())

        val firstItem = history.first()
        assertNotNull(firstItem.zkCommitmentHash)
        assertTrue(firstItem.zkCommitmentHash.isNotEmpty())
        assertTrue(firstItem.consensusIndex in 0.0f..1.0f)
        assertTrue(firstItem.synthesized8DScores.containsKey("SYS"))
        assertTrue(firstItem.actionableGuidelines.isNotEmpty())
    }

    @Test
    fun `executeArenaDebate generates turns, synthesis and cryptographic proof of necessity`() = runBlocking {
        val agentA = AgentArenaLibrary.presetArchetypes[0] // Conservative Analyst
        val agentB = AgentArenaLibrary.presetArchetypes[1] // Radical Innovator
        val problem = "Autonomní energetické přerozdělování v mikro-sítích"

        val recordedTurns = mutableListOf<ArenaDebateTurn>()
        val result = AgentArenaEngine.executeArenaDebate(
            problemStatement = problem,
            agentA = agentA,
            agentB = agentB,
            onProgressUpdate = { turn ->
                recordedTurns.add(turn)
            }
        )

        assertNotNull(result)
        assertEquals(problem, result.problemStatement)
        assertEquals(agentA.id, result.agentA.id)
        assertEquals(agentB.id, result.agentB.id)
        assertTrue(result.turns.size >= 4)
        assertEquals(result.turns.size, recordedTurns.size)

        // Verifikace 8D skóre
        AgentArenaEngine.all8DDomains.forEach { domain ->
            assertTrue("Should contain domain $domain", result.synthesized8DScores.containsKey(domain))
        }

        // Verifikace divergence a konsenzu
        assertTrue(result.consensusIndex in 0.0f..1.0f)
        assertTrue(result.divergenceIndex in 0.0f..1.0f)

        // Verifikace ZK otisku a důkazu nutnosti
        assertNotNull(result.zkCommitmentHash)
        assertEquals(16, result.zkCommitmentHash.length) // 8 bajtů v hex = 16 znaků
        assertNotNull(result.proofOfNecessity)
        assertTrue(result.proofOfNecessity!!.contains("AUDIT-NECESSITY"))

        // Verifikace přidání do historie
        val updatedHistory = AgentArenaEngine.debateHistoryState.value
        assertEquals(result.debateId, updatedHistory.first().debateId)
    }
}
