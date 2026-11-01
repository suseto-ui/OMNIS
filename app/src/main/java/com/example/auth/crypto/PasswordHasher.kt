package com.example.auth.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Bezpečné hashování uživatelských hesel pomocí PBKDF2 s HMAC-SHA256.
 *
 * Parametry:
 * - Algoritmus: PBKDF2WithHmacSHA256
 * - Iterace: 10 000
 * - Délka soli: 16 bajtů (128 bitů) kryptograficky bezpečného náhodného generátoru (SecureRandom)
 * - Délka odvozeného klíče: 256 bitů (32 bajtů)
 * - Porovnání v konstantním čase (MessageDigest.isEqual) k prevenci útoků časováním (timing attacks).
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    const val ITERATIONS = 10_000
    const val KEY_LENGTH_BITS = 256
    const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    data class HashResult(
        val hashHex: String,
        val saltHex: String,
        val iterations: Int = ITERATIONS
    )

    /**
     * Vygeneruje novou náhodnou kryptografickou sůl.
     */
    fun generateSalt(length: Int = SALT_LENGTH_BYTES): ByteArray {
        val salt = ByteArray(length)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Vypočte solený PBKDF2 hash pro zadané heslo a sůl.
     */
    fun hashPassword(password: String, salt: ByteArray = generateSalt()): HashResult {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded
        spec.clearPassword() // Vyčištění paměti
        return HashResult(
            hashHex = bytesToHex(hashBytes),
            saltHex = bytesToHex(salt),
            iterations = ITERATIONS
        )
    }

    /**
     * Ověří zadané heslo vůči uloženému hashi a soli v konstantním čase.
     */
    fun verifyPassword(password: String, storedHashHex: String, storedSaltHex: String): Boolean {
        return try {
            val salt = hexToBytes(storedSaltHex)
            val expectedHashBytes = hexToBytes(storedHashHex)
            val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val computedHashBytes = factory.generateSecret(spec).encoded
            spec.clearPassword()
            MessageDigest.isEqual(computedHashBytes, expectedHashBytes)
        } catch (_: Exception) {
            false
        }
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }

    fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.trim().lowercase()
        require(cleanHex.length % 2 == 0) { "Hex řetězec musí mít sudý počet znaků." }
        val result = ByteArray(cleanHex.length / 2)
        for (i in result.indices) {
            val index = i * 2
            val byteVal = cleanHex.substring(index, index + 2).toInt(16)
            result[i] = byteVal.toByte()
        }
        return result
    }
}
