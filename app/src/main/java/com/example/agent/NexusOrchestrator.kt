package com.example.agent

import android.util.Log
import com.example.api.OmnisGeminiClient
import com.example.api.SynthesisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Orchestrátor pro multi-agentní spolupráci (NEXUS)
 */
object NexusOrchestrator {
    private const val TAG = "NexusOrchestrator"

    data class NexusMessage(
        val agentId: String,
        val text: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _nexusHistory = MutableStateFlow<List<NexusMessage>>(emptyList())
    val nexusHistory = _nexusHistory.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    suspend fun runCollaborativeSession(query: String, selectedAgentIds: List<String>): SynthesisResult? {
        if (_isProcessing.value) return null
        _isProcessing.value = true
        _nexusHistory.value = emptyList()

        try {
            val agents = selectedAgentIds.mapNotNull { AgentRegistry.getById(it) }
            val currentHistory = mutableListOf<NexusMessage>()

            // 1. KOLO: Každý agent vyjádří svůj primární pohled
            for (agent in agents) {
                val agentPrompt = "${agent.systemPrompt}\n\nDOTAZ UŽIVATELE: $query\nVYJÁDŘI SVŮJ POHLED (max 3 věty):"
                val result = OmnisGeminiClient.synthesize(agentPrompt, agent.domainFocus)
                
                val msg = NexusMessage(agent.id, result.answer)
                currentHistory.add(msg)
                _nexusHistory.value = currentHistory.toList()
                kotlinx.coroutines.delay(500)
            }

            // 2. KOLO: Syntéza (Final Synthesis by O.M.N.I.S. Core)
            val synthesisPrompt = """
                Jsi O.M.N.I.S. Core Synthesizer. Shromáždil jsi pohledy od specializovaných agentů na dotaz: "$query".
                
                POHLEDY AGENTŮ:
                ${currentHistory.joinToString("\n") { "${it.agentId}: ${it.text}" }}
                
                Úkol:
                Vytvoř finální harmonizovanou syntézu, která integruje tyto pohledy do jednoho robustního řešení.
            """.trimIndent()

            val finalResult = OmnisGeminiClient.synthesize(synthesisPrompt, "SYSTEM_INTEGRATION")
            _isProcessing.value = false
            return finalResult

        } catch (e: Exception) {
            Log.e(TAG, "Nexus session failed", e)
            _isProcessing.value = false
            return null
        }
    }
}
