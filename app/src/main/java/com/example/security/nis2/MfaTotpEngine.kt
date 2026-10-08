package com.example.security.nis2

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * MfaTotpEngine:
 * Plošná vícefaktorová autentizace (MFA) dle standardu RFC 6238 (TOTP) a NIS2 směrnice.
 * Poskytuje deterministické generování a validaci 6místných kódů s 30sekundovým časovým oknem.
 */
object MfaTotpEngine {

    private const val TIME_STEP_SECONDS = 30L
    private const val DIGITS = 6

    /**
     * Vygeneruje nový náhodný Base32/Hex tajný klíč pro MFA.
     */
    fun generateSecretKey(): String {
        val bytes = ByteArray(20)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Vygeneruje aktuální 6místný TOTP kód pro daný klíč a časové razítko.
     */
    fun generateTotpCode(secretHex: String, timestampMs: Long = System.currentTimeMillis()): String {
        val timeIndex = timestampMs / 1000L / TIME_STEP_SECONDS
        val keyBytes = hexStringToByteArray(secretHex)
        val data = ByteBuffer.allocate(8).putLong(timeIndex).array()

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA1"))
        val hash = mac.doFinal(data)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val otp = binary % 10.0.pow(DIGITS.toDouble()).toInt()
        return "%0${DIGITS}d".format(otp)
    }

    /**
     * Ověří zadaný kód s tolerancí +/- 1 časového kroku (tzv. clock drift compensation).
     */
    fun verifyTotpCode(secretHex: String, inputCode: String, timestampMs: Long = System.currentTimeMillis()): Boolean {
        if (inputCode.trim().length != DIGITS) return false
        val cleanInput = inputCode.trim()

        // Testujeme okno: aktuální čas, -30s a +30s
        val offsets = listOf(0L, -TIME_STEP_SECONDS * 1000L, TIME_STEP_SECONDS * 1000L)
        return offsets.any { offset ->
            val expected = generateTotpCode(secretHex, timestampMs + offset)
            expected == cleanInput
        }
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
