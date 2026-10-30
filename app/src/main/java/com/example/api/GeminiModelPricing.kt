package com.example.api

/**
 * Ceník a kalkulátor nákladů pro modely Gemini (v USD za 1M tokenů).
 * Poskytuje přesné stanovení ceny za vstupní i výstupní tokeny pro nejvýhodnější Flash modely
 * a vypočítává úspory z tokenového šetřiče (Eco Mode).
 */
data class ModelPricingSpec(
    val modelName: String,
    val inputCostPerMillionUsd: Double,
    val outputCostPerMillionUsd: Double
) {
    fun calculateCostUsd(inputTokens: Long, outputTokens: Long): Double {
        val inputCost = (inputTokens.toDouble() / 1_000_000.0) * inputCostPerMillionUsd
        val outputCost = (outputTokens.toDouble() / 1_000_000.0) * outputCostPerMillionUsd
        return inputCost + outputCost
    }
}

object GeminiModelPricing {

    // Převodní kurz CZK / USD pro lokální zobrazení v českých korunách
    const val CZK_PER_USD = 23.50

    private val PRICING_MAP = mapOf(
        "gemini-2.5-flash" to ModelPricingSpec("gemini-2.5-flash", 0.15, 0.60),
        "gemini-2.0-flash" to ModelPricingSpec("gemini-2.0-flash", 0.10, 0.40),
        "gemini-1.5-flash" to ModelPricingSpec("gemini-1.5-flash", 0.075, 0.30),
        "gemini-1.5-flash-8b" to ModelPricingSpec("gemini-1.5-flash-8b", 0.0375, 0.15),
        "gemini-2.5-flash-image" to ModelPricingSpec("gemini-2.5-flash-image", 0.15, 0.60),
        "gemini-flash-latest" to ModelPricingSpec("gemini-flash-latest", 0.10, 0.40)
    )

    private val DEFAULT_PRICING = ModelPricingSpec("gemini-flash-default", 0.10, 0.40)

    fun getPricing(modelName: String): ModelPricingSpec {
        return PRICING_MAP[modelName] ?: PRICING_MAP.entries.firstOrNull { modelName.contains(it.key) }?.value ?: DEFAULT_PRICING
    }

    /**
     * Vypočítá celkovou cenu dotazu v USD na základě aktivního modelu a spotřebovaných tokenů.
     */
    fun calculatePromptCostUsd(modelName: String, inputTokens: Long, outputTokens: Long): Double {
        val spec = getPricing(modelName)
        return spec.calculateCostUsd(inputTokens, outputTokens)
    }

    /**
     * Vypočítá odhadovanou finanční úsporu v USD při použití Eco Mode (1-Call Batched syntéza)
     * oproti 8 samostatným nezávislým voláním pro jednotlivé domény.
     */
    fun calculateEcoModeSavingsUsd(modelName: String, inputTokens: Long, outputTokens: Long): Double {
        val spec = getPricing(modelName)
        // Bez Eco Mode: 8 paralelních syntetických dotazů s duplicitním kontextem (cca 8x násobek částky za vstupní kontext)
        val nonEcoInputTokens = inputTokens * 6L
        val nonEcoOutputTokens = outputTokens * 3L
        val nonEcoCost = spec.calculateCostUsd(nonEcoInputTokens, nonEcoOutputTokens)
        val ecoCost = spec.calculateCostUsd(inputTokens, outputTokens)
        return (nonEcoCost - ecoCost).coerceAtLeast(0.0)
    }

    fun formatUsd(amountUsd: Double): String {
        return if (amountUsd < 0.0001) {
            String.format(java.util.Locale.US, "$%.6f", amountUsd)
        } else {
            String.format(java.util.Locale.US, "$%.4f", amountUsd)
        }
    }

    fun formatCzk(amountUsd: Double): String {
        val czk = amountUsd * CZK_PER_USD
        return if (czk < 0.01) {
            String.format(java.util.Locale.US, "%.4f Kč", czk)
        } else {
            String.format(java.util.Locale.US, "%.2f Kč", czk)
        }
    }
}
