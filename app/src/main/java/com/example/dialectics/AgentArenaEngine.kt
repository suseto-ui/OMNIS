package com.example.dialectics

import kotlinx.coroutines.delay
import java.security.MessageDigest
import kotlin.math.abs

object AgentArenaEngine {

    val all8DDomains = listOf("SYS", "ECON", "PSYCH", "ECO", "LAW", "SEC", "PHYS", "SOC")

    suspend fun executeArenaDebate(
        problemStatement: String,
        agentA: AgentArchetype,
        agentB: AgentArchetype,
        onProgressUpdate: suspend (ArenaDebateTurn) -> Unit = {}
    ): ArenaDebateResult {
        val turns = mutableListOf<ArenaDebateTurn>()

        // Calculate baseline 8D scores based on agent archetypes & problem complexity
        val problemHash = abs(problemStatement.hashCode())
        val agentAScores = generate8DScores(agentA, problemHash, isA = true)
        val agentBScores = generate8DScores(agentB, problemHash, isA = false)

        // Turn 1: Agent A Opening Stance
        delay(400)
        val turn1 = ArenaDebateTurn(
            turnNumber = 1,
            roundName = "Kolo 1: Primární Teze",
            agentId = agentA.id,
            agentName = agentA.name,
            argumentText = generateOpeningArgument(agentA, problemStatement),
            highlightedDomains = agentA.primaryDomains,
            domainScores = agentAScores
        )
        turns.add(turn1)
        onProgressUpdate(turn1)

        // Turn 2: Agent B Counter-Stance & Antidote
        delay(500)
        val turn2 = ArenaDebateTurn(
            turnNumber = 2,
            roundName = "Kolo 1: Protiteze & Antiteze",
            agentId = agentB.id,
            agentName = agentB.name,
            argumentText = generateOpeningArgument(agentB, problemStatement),
            highlightedDomains = agentB.primaryDomains,
            domainScores = agentBScores
        )
        turns.add(turn2)
        onProgressUpdate(turn2)

        // Turn 3: Agent A Cross-Examination
        delay(500)
        val turn3 = ArenaDebateTurn(
            turnNumber = 3,
            roundName = "Kolo 2: Křížový Výslech & Námitka",
            agentId = agentA.id,
            agentName = agentA.name,
            argumentText = generateCrossArgument(agentA, agentB, problemStatement),
            highlightedDomains = listOf(agentB.primaryDomains.firstOrNull() ?: "LAW", "SYS"),
            domainScores = agentAScores
        )
        turns.add(turn3)
        onProgressUpdate(turn3)

        // Turn 4: Agent B Rebuttal & Stress Test
        delay(500)
        val turn4 = ArenaDebateTurn(
            turnNumber = 4,
            roundName = "Kolo 2: Replikace & Stresový Test",
            agentId = agentB.id,
            agentName = agentB.name,
            argumentText = generateRebuttalArgument(agentB, agentA, problemStatement),
            highlightedDomains = listOf(agentA.primaryDomains.firstOrNull() ?: "SEC", "ECON"),
            domainScores = agentBScores
        )
        turns.add(turn4)
        onProgressUpdate(turn4)

        // Calculate synthesized scores & metrics
        val synthesizedScores = mutableMapOf<String, Float>()
        var totalDivergence = 0f
        all8DDomains.forEach { domain ->
            val scoreA = agentAScores[domain] ?: 0f
            val scoreB = agentBScores[domain] ?: 0f
            val diff = abs(scoreA - scoreB)
            totalDivergence += diff
            // Weighted average toward higher epistemic rigor
            val weightA = agentA.epistemicRigor
            val weightB = agentB.epistemicRigor
            val synth = (scoreA * weightA + scoreB * weightB) / (weightA + weightB)
            synthesizedScores[domain] = (synth * 100f).toInt() / 100f
        }

        val avgDivergence = (totalDivergence / all8DDomains.size).coerceIn(0.1f, 0.95f)
        val consensusIndex = ((1.0f - avgDivergence) * 100f).toInt() / 100f

        val hashString = "OMNIS-ARENA-${System.currentTimeMillis()}-$problemStatement-${agentA.id}-${agentB.id}"
        val digest = MessageDigest.getInstance("SHA-256").digest(hashString.toByteArray())
        val zkCommitmentHash = digest.take(8).joinToString("") { "%02x".format(it) }.uppercase()

        val synthesisSummary = "Syntéza souboje mezi [${agentA.name}] a [${agentB.name}]: Dosrženo consensus korigovaného skóre ${(consensusIndex * 100).toInt()}%. Vytvořeno tenzorové přemostění eliminující kritická rizika v doménách ${agentA.primaryDomains.joinToString()} a ${agentB.primaryDomains.joinToString()}."

        val actionableGuidelines = listOf(
            "Aplikovat kompromisní bezpečnostní práh v doméně SEC na úrovni ${synthesizedScores["SEC"] ?: 0.5f}.",
            "Inkorporovat auditní brány G1–G6 z návrhu ${agentA.name} do výpočetního toku.",
            "Zabezpečit sledování Shannonovy entropie pro eliminaci divergencí nad 0.60.",
            "Zapsat kryptografický potvrzovací otisk $zkCommitmentHash do ZK-SNARK řetězce."
        )

        val result = ArenaDebateResult(
            problemStatement = problemStatement,
            agentA = agentA,
            agentB = agentB,
            turns = turns,
            agentA8DScores = agentAScores,
            agentB8DScores = agentBScores,
            synthesized8DScores = synthesizedScores,
            divergenceIndex = avgDivergence,
            consensusIndex = consensusIndex,
            synthesisSummary = synthesisSummary,
            actionableGuidelines = actionableGuidelines,
            zkCommitmentHash = zkCommitmentHash
        )

        // Save to active history state
        debateHistoryState.value = listOf(result) + debateHistoryState.value

        return result
    }

    // Dynamic History State Flow
    val debateHistoryState = kotlinx.coroutines.flow.MutableStateFlow<List<ArenaDebateResult>>(generatePresetHistory())

    private fun generatePresetHistory(): List<ArenaDebateResult> {
        val agent1 = AgentArenaLibrary.presetArchetypes[0] // Conservative Analyst
        val agent2 = AgentArenaLibrary.presetArchetypes[1] // Radical Innovator
        val agent3 = AgentArenaLibrary.presetArchetypes[2] // Skeptic Auditor
        val agent4 = AgentArenaLibrary.presetArchetypes[3] // Pragmatic Engineer

        return listOf(
            ArenaDebateResult(
                debateId = "hist-001",
                problemStatement = "Kvantová migrace bankovní infrastruktury: Okamžitý přechod vs. Postupné testování",
                agentA = agent1,
                agentB = agent2,
                turns = emptyList(),
                agentA8DScores = mapOf("SYS" to 0.4f, "ECON" to 0.3f, "PSYCH" to 0.5f, "ECO" to 0.6f, "LAW" to 0.9f, "SEC" to 0.95f, "PHYS" to 0.4f, "SOC" to 0.5f),
                agentB8DScores = mapOf("SYS" to 0.9f, "ECON" to 0.85f, "PSYCH" to 0.3f, "ECO" to 0.2f, "LAW" to 0.3f, "SEC" to 0.4f, "PHYS" to 0.9f, "SOC" to 0.4f),
                synthesized8DScores = mapOf("SYS" to 0.65f, "ECON" to 0.58f, "PSYCH" to 0.4f, "ECO" to 0.4f, "LAW" to 0.6f, "SEC" to 0.68f, "PHYS" to 0.65f, "SOC" to 0.45f),
                divergenceIndex = 0.52f,
                consensusIndex = 0.74f,
                synthesisSummary = "Dosrženo postupné fázové nasazení s bezpečnostním obalem ZK-SNARK pro zamezení rizika v doméně SEC.",
                actionableGuidelines = listOf("Nasadit kvantově odolné šifrování na hranici LAW a SEC", "Ponechat záložní CSR cluster pro rollback"),
                zkCommitmentHash = "9F8B2C4E"
            ),
            ArenaDebateResult(
                debateId = "hist-002",
                problemStatement = "Autonomní AI v řízení letového provozu: Bezvýhradný automat vs. Člověk v rozhodovací smyčce",
                agentA = agent3,
                agentB = agent4,
                turns = emptyList(),
                agentA8DScores = mapOf("SYS" to 0.7f, "ECON" to 0.2f, "PSYCH" to 0.8f, "ECO" to 0.5f, "LAW" to 0.85f, "SEC" to 0.9f, "PHYS" to 0.5f, "SOC" to 0.7f),
                agentB8DScores = mapOf("SYS" to 0.85f, "ECON" to 0.7f, "PSYCH" to 0.4f, "ECO" to 0.6f, "LAW" to 0.5f, "SEC" to 0.7f, "PHYS" to 0.85f, "SOC" to 0.5f),
                synthesized8DScores = mapOf("SYS" to 0.78f, "ECON" to 0.45f, "PSYCH" to 0.6f, "ECO" to 0.55f, "LAW" to 0.68f, "SEC" to 0.8f, "PHYS" to 0.68f, "SOC" to 0.6f),
                divergenceIndex = 0.38f,
                consensusIndex = 0.82f,
                synthesisSummary = "Hibridní model řízení letů s autonomní mikrosekundovou korekcí a lidským schválením u strategických manévrů.",
                actionableGuidelines = listOf("Garantovat výpočetní latenci pod 2ms v doméně PHYS", "Implementovat hradítka Refusal Ladder G1-G6"),
                zkCommitmentHash = "3A7D91B2"
            )
        )
    }

    private fun generate8DScores(agent: AgentArchetype, problemHash: Int, isA: Boolean): Map<String, Float> {
        val scores = mutableMapOf<String, Float>()
        val signModifier = if (isA) 1.0f else -0.7f
        all8DDomains.forEachIndexed { idx, domain ->
            val isPrimary = agent.primaryDomains.contains(domain)
            val baseVal = if (isPrimary) {
                0.6f + (agent.riskTolerance * 0.35f)
            } else {
                0.1f + ((problemHash % (idx + 5)) / 10f) * 0.4f
            }
            val valAdjusted = (baseVal * if (isPrimary) 1.0f else signModifier).coerceIn(-0.9f, 0.95f)
            scores[domain] = (valAdjusted * 10f).toInt() / 10f
        }
        return scores
    }

    private fun generateOpeningArgument(agent: AgentArchetype, problem: String): String {
        return when (agent.id) {
            "conservative_analyst" ->
                "Při analýze problému '$problem' musíme striktně upřednostnit bezpečnost a normativní integritu. Navrhuji aktivovat brány Refusal Ladderu (G1–G6) a nepovolovat nekontrolované změny bez auditní stopy."
            "radical_innovator" ->
                "Pro zadanou výzvu '$problem' je konzervativní přístup brzdu vývoje! Je nutné okamžitě nasadit agresivní 8D tenzorovou optimalizaci v doménách SYS a PHYS pro 10x vyšší propustnost."
            "skeptic_auditor" ->
                "Předpoklady u tématiky '$problem' obsahují zásadní trhliny. Ignorujete možnost kaskádového selhání hraničních uzlů a nárůst Shannonovy entropie při vysoké zátěži!"
            "pragmatic_engineer" ->
                "Z technického hlediska u problému '$problem' potřebujeme čistý KSP/Kotlin 2.0 kód a paměťově úspornou CSR matici. Bez stabilní paměti je akademická debata bezpředmětná."
            "eco_socius" ->
                "Nesmíme zapomínat na širší systémové dopady problému '$problem' na lidskou společnost a energetické zdroje. Řešení musí být udržitelné v doménách ECO a SOC."
            else ->
                "Z pohledu role ${agent.roleTitle} navrhuji přístup s důrazem na domény ${agent.primaryDomains.joinToString()} a kontrolované riziko (${agent.riskTolerance})."
        }
    }

    private fun generateCrossArgument(agent: AgentArchetype, opponent: AgentArchetype, problem: String): String {
        return "Namítám vůči pozici [${opponent.name}]! Jejich zaměření na ${opponent.primaryDomains.joinToString()} zanedbává klíčové faktory v mé doménové oblasti. Můj přístup zaručuje vyšší kognitivní stabilitu bez nepředvídatelných výpadků."
    }

    private fun generateRebuttalArgument(agent: AgentArchetype, opponent: AgentArchetype, problem: String): String {
        return "Námitka [${opponent.name}] pramení z příliš úzkého pohledu. Pokud neaplikujeme mé principy (${agent.roleTitle}), vystavujeme systém riziku kolapsu. Trvám na kompromisním úpravení 8D matice!"
    }
}
