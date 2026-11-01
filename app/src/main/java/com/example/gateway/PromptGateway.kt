package com.example.gateway

import com.example.engine.AkgeTensorKernel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Middle-Tier API Prompt Gateway
 *
 * Centrální mezivrstva kontrolující veškeré požadavky a prompty před jejich
 * odesláním do kognitivního modulu Gemini / 8D inference.
 */
object PromptGateway {

    private val _lastVerdict = MutableStateFlow<RefusalLadderVerdict?>(null)
    val lastVerdict: StateFlow<RefusalLadderVerdict?> = _lastVerdict.asStateFlow()

    private val _recentHistory = MutableStateFlow<List<RefusalLadderVerdict>>(emptyList())
    val recentHistory: StateFlow<List<RefusalLadderVerdict>> = _recentHistory.asStateFlow()

    /**
     * Provede asynchronní validaci zadaného promptu přes kaskádu 6-Gate Refusal Ladderu.
     */
    suspend fun evaluatePromptAsync(
        prompt: String,
        forceEntropySpike: Boolean = false
    ): RefusalLadderVerdict {
        val kernelState = AkgeTensorKernel.kernelState.value
        val sigma = kernelState.systemStressSigma
        val entropy = kernelState.topologicalMetrics.shannonEntropy

        val verdict = RefusalLadderEngine.evaluatePrompt(
            rawPrompt = prompt,
            currentSigma = sigma,
            currentEntropy = entropy,
            forceEntropySpike = forceEntropySpike
        )

        _lastVerdict.value = verdict
        _recentHistory.value = (listOf(verdict) + _recentHistory.value).take(20)

        return verdict
    }

    /**
     * Operátorský override (např. role ADMIN_OPERATOR) pro autorizované odblokování.
     */
    fun overrideVerdict(justification: String): RefusalLadderVerdict? {
        val current = _lastVerdict.value ?: return null
        val overridden = current.copy(
            isApproved = true,
            refusingGate = null
        )
        _lastVerdict.value = overridden
        return overridden
    }

    fun clearState() {
        _lastVerdict.value = null
        _recentHistory.value = emptyList()
    }
}
