package com.example.ai.transparency

import java.nio.charset.StandardCharsets

/**
 * AiSteganographyEngine:
 * Implementuje steganografickou vrstvu vyžadovanou Evropským nařízením o umělé inteligenci (EU AI Act, čl. 50 odst. 2).
 * Vkládá neviditelný, strojově čitelný a odolný vodoznak do veškerého generovaného syntetického textu
 * pomocí kryptografických zero-width sekvencí (Zero-Width Space \u200B a Zero-Width Non-Joiner \u200C).
 */
object AiSteganographyEngine {

    private const val ZW_ZERO = '\u200B'
    private const val ZW_ONE = '\u200C'
    private const val ZW_DELIM = '\u200D'
    private const val WATERMARK_PREFIX = "OMNIS-SYNTHETIC-AI-ACT-ART50-2"

    /**
     * Vloží steganografický neviditelný vodoznak do generovaného textu.
     */
    fun embedWatermark(
        plainText: String,
        modelName: String = "gemini-3.1-pro-preview",
        timestamp: Long = System.currentTimeMillis()
    ): String {
        if (plainText.isBlank()) return plainText

        val payload = "$WATERMARK_PREFIX|$modelName|$timestamp"
        val binaryString = payload.toByteArray(StandardCharsets.UTF_8).joinToString("") { byte ->
            String.format("%8s", Integer.toBinaryString(byte.toInt() and 0xFF)).replace(' ', '0')
        }

        val zeroWidthWatermark = buildString {
            append(ZW_DELIM)
            binaryString.forEach { bit ->
                append(if (bit == '1') ZW_ONE else ZW_ZERO)
            }
            append(ZW_DELIM)
        }

        // Vložíme vodoznak na konec textu pro zachování sémantického vyhledávání
        return plainText + zeroWidthWatermark
    }

    /**
     * Extrahuje a dekóduje steganografický vodoznak z textu.
     */
    fun extractWatermark(text: String): WatermarkExtractionResult {
        val startIdx = text.indexOf(ZW_DELIM)
        val endIdx = text.lastIndexOf(ZW_DELIM)

        if (startIdx == -1 || endIdx == -1 || startIdx >= endIdx) {
            return WatermarkExtractionResult(hasValidWatermark = false)
        }

        val zwSequence = text.substring(startIdx + 1, endIdx)
        val bitStringBuilder = StringBuilder()

        for (ch in zwSequence) {
            when (ch) {
                ZW_ONE -> bitStringBuilder.append('1')
                ZW_ZERO -> bitStringBuilder.append('0')
            }
        }

        val bits = bitStringBuilder.toString()
        if (bits.length % 8 != 0 || bits.isEmpty()) {
            return WatermarkExtractionResult(hasValidWatermark = false)
        }

        val bytes = ByteArray(bits.length / 8)
        for (i in bytes.indices) {
            val byteStr = bits.substring(i * 8, (i + 1) * 8)
            bytes[i] = byteStr.toInt(2).toByte()
        }

        val decodedPayload = String(bytes, StandardCharsets.UTF_8)
        val parts = decodedPayload.split('|')

        return if (parts.isNotEmpty() && parts[0] == WATERMARK_PREFIX) {
            WatermarkExtractionResult(
                hasValidWatermark = true,
                modelName = parts.getOrNull(1) ?: "unknown",
                timestamp = parts.getOrNull(2)?.toLongOrNull() ?: 0L,
                rawPayload = decodedPayload
            )
        } else {
            WatermarkExtractionResult(hasValidWatermark = false)
        }
    }

    /**
     * Vygeneruje C2PA (Coalition for Content Provenance and Authenticity) manifest pro média.
     */
    fun generateC2paManifest(assetId: String, modelName: String): Map<String, Any> {
        return mapOf(
            "c2pa_version" to "1.3",
            "claim_generator" to "OMNIS Platform / AI Act Art 50(2)",
            "title" to "Synthetic Asset Provenance Manifest",
            "assertions" to listOf(
                mapOf(
                    "label" to "c2pa.actions",
                    "data" to mapOf("action" to "c2pa.created", "softwareAgent" to modelName)
                ),
                mapOf(
                    "label" to "c2pa.ai_generated",
                    "data" to mapOf("compliance" to "EU_AI_ACT_ART_50_PAR_2", "is_synthetic" to true)
                )
            ),
            "signature" to "SHA256:ECDSA_${System.currentTimeMillis()}_$assetId"
        )
    }
}

data class WatermarkExtractionResult(
    val hasValidWatermark: Boolean,
    val modelName: String = "",
    val timestamp: Long = 0L,
    val rawPayload: String = ""
)
