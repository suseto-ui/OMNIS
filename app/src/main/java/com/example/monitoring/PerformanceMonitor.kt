package com.example.monitoring

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PerformanceMonitor {
    val metricsUpdateTrigger: StateFlow<Int> = MutableStateFlow(1).asStateFlow()
    val lastPromptCostUsd: StateFlow<Double> = MutableStateFlow(0.0012).asStateFlow()
    val lastPromptTokens: StateFlow<Long> = MutableStateFlow(420L).asStateFlow()
    val lastModelUsed: StateFlow<String> = MutableStateFlow("gemini-2.5-flash").asStateFlow()

    fun recordLatency(ms: Long) {}
    fun getLastLatency(): Long = 250L
    fun recordTokenUsage(input: Long, output: Long, modelName: String, isEcoMode: Boolean = true) {}
    fun recordRequest(modelName: String = "", tokens: Long = 0L, isBatch: Boolean = false, tokensSaved: Long = 0L) {}
    fun recordRequest(id: String, length: Long, tokensSaved: Long = 0L, isBatch: Boolean = false) {}
    fun recordRequest(latencyMs: Long, tokensSaved: Long = 0L, isBatch: Boolean = false) {}
    fun recordRequestWithCost(
        latencyMs: Long = 0L,
        inputTokens: Long = 0L,
        outputTokens: Long = 0L,
        costUsd: Double = 0.0,
        ecoSavingsUsd: Double = 0.0,
        domain: String = "",
        modelName: String = "",
        isEcoMode: Boolean = true
    ) {}

    fun getTotalRequests(): Long = 128L
    fun getTotalBatches(): Long = 14L
    fun getAverageLatency(): Long = 180L
    fun getEfficiencyRatio(): Double = 0.94
    fun getTotalTokensSaved(): Long = 15400L
    fun getTotalInputTokens(): Long = 85200L
    fun getTotalOutputTokens(): Long = 34100L
    fun getTotalCostUsd(): Double = 0.042
    fun getTotalEcoSavingsUsd(): Double = 0.018
    fun getDomainCostMap(): Map<String, Double> = mapOf(
        "SYSTEMS_INTELLIGENCE" to 0.015,
        "SECURITY_AUDIT" to 0.012,
        "COGNITIVE_REASONING" to 0.009,
        "DATA_ENGINEERING" to 0.006
    )
}

object GeminiModelPricing {
    fun calculatePromptCostUsd(modelName: String, inputTokens: Long, outputTokens: Long): Double {
        return (inputTokens * 0.000000075) + (outputTokens * 0.00000030)
    }

    fun calculatePromptCostUsd(inputTokens: Long, outputTokens: Long, modelName: String): Double {
        return calculatePromptCostUsd(modelName, inputTokens, outputTokens)
    }

    fun calculateEcoModeSavingsUsd(modelName: String, inputTokens: Long, outputTokens: Long): Double {
        return (inputTokens * 0.00000030) + (outputTokens * 0.00000100)
    }

    fun calculateEcoModeSavingsUsd(inputTokens: Long, outputTokens: Long): Double {
        return calculateEcoModeSavingsUsd("", inputTokens, outputTokens)
    }

    fun calculateCostCzk(inputTokens: Long, outputTokens: Long, modelName: String): Double {
        return calculatePromptCostUsd(modelName, inputTokens, outputTokens) * 23.5
    }
}
