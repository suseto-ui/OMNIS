package com.example.auth.jwt

import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Standardní RFC 7519 JSON Web Token (JWT) manažer pro O.M.N.I.S.
 *
 * Formát: header.payload.signature
 * Algoritmus: HS256 (HMAC-SHA256)
 * Kódování: Base64Url bez paddingu
 */
object JwtTokenManager {

    private const val ALGORITHM_NAME = "HmacSHA256"
    private const val HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}"
    private const val DEFAULT_EXPIRATION_SECONDS = 24 * 60 * 60L // 24 hodin

    // Tajný klíč pro podpis tokenů v aplikaci O.M.N.I.S.
    // Lze nastavit dynamicky nebo použít výchozí kryptografický klíč
    @Volatile
    var secretKey: String = "omnis_akge8d_secure_hmac_sha256_jwt_signing_key_v1"

    data class JwtClaims(
        val subject: String,
        val role: String,
        val issuedAt: Long,
        val expiresAt: Long,
        val jwtId: String
    ) {
        val isExpired: Boolean
            get() = (System.currentTimeMillis() / 1000L) >= expiresAt
    }

    sealed class JwtValidationResult {
        data class Valid(val claims: JwtClaims) : JwtValidationResult()
        data class Expired(val claims: JwtClaims) : JwtValidationResult()
        object InvalidSignature : JwtValidationResult()
        data class Malformed(val error: String) : JwtValidationResult()
    }

    /**
     * Vygeneruje a podepíše nový RFC 7519 JWT token.
     */
    fun createToken(
        subject: String,
        role: String,
        durationSeconds: Long = DEFAULT_EXPIRATION_SECONDS,
        key: String = secretKey
    ): String {
        val nowSeconds = System.currentTimeMillis() / 1000L
        val expSeconds = nowSeconds + durationSeconds
        val jti = UUID.randomUUID().toString()

        val payloadJson = JSONObject().apply {
            put("sub", subject)
            put("role", role)
            put("iat", nowSeconds)
            put("exp", expSeconds)
            put("jti", jti)
        }.toString()

        val headerEncoded = base64UrlEncode(HEADER_JSON.toByteArray(StandardCharsets.UTF_8))
        val payloadEncoded = base64UrlEncode(payloadJson.toByteArray(StandardCharsets.UTF_8))
        val signingInput = "$headerEncoded.$payloadEncoded"
        val signatureBytes = signHmacSha256(signingInput, key)
        val signatureEncoded = base64UrlEncode(signatureBytes)

        return "$signingInput.$signatureEncoded"
    }

    /**
     * Ověří integritu, podpis a platnost JWT tokenu.
     */
    fun validateToken(token: String, key: String = secretKey): JwtValidationResult {
        val parts = token.trim().split(".")
        if (parts.size != 3) {
            return JwtValidationResult.Malformed("Token musí obsahovat právě 3 části oddělené tečkou (header.payload.signature).")
        }

        val headerPart = parts[0]
        val payloadPart = parts[1]
        val signaturePart = parts[2]

        val signingInput = "$headerPart.$payloadPart"
        val expectedSignatureBytes = signHmacSha256(signingInput, key)
        val actualSignatureBytes = try {
            base64UrlDecode(signaturePart)
        } catch (e: Exception) {
            return JwtValidationResult.Malformed("Chyba při dekódování podpisu: ${e.message}")
        }

        if (!MessageDigest.isEqual(expectedSignatureBytes, actualSignatureBytes)) {
            return JwtValidationResult.InvalidSignature
        }

        val claims = try {
            val payloadJsonString = String(base64UrlDecode(payloadPart), StandardCharsets.UTF_8)
            val json = JSONObject(payloadJsonString)
            JwtClaims(
                subject = json.optString("sub", ""),
                role = json.optString("role", "STANDARD_USER"),
                issuedAt = json.optLong("iat", 0L),
                expiresAt = json.optLong("exp", 0L),
                jwtId = json.optString("jti", "")
            )
        } catch (e: Exception) {
            return JwtValidationResult.Malformed("Chyba při parsování claims: ${e.message}")
        }

        val nowSeconds = System.currentTimeMillis() / 1000L
        if (nowSeconds >= claims.expiresAt) {
            return JwtValidationResult.Expired(claims)
        }

        return JwtValidationResult.Valid(claims)
    }

    /**
     * Rychlé vyextrahování claims bez validace podpisu (pro UI účely).
     */
    fun parseClaimsUnverified(token: String): JwtClaims? {
        val parts = token.trim().split(".")
        if (parts.size < 2) return null
        return try {
            val payloadJsonString = String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8)
            val json = JSONObject(payloadJsonString)
            JwtClaims(
                subject = json.optString("sub", ""),
                role = json.optString("role", "STANDARD_USER"),
                issuedAt = json.optLong("iat", 0L),
                expiresAt = json.optLong("exp", 0L),
                jwtId = json.optString("jti", "")
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun signHmacSha256(data: String, key: String): ByteArray {
        val mac = Mac.getInstance(ALGORITHM_NAME)
        val secretKeySpec = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), ALGORITHM_NAME)
        mac.init(secretKeySpec)
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
    }

    fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun base64UrlDecode(str: String): ByteArray {
        return Base64.getUrlDecoder().decode(str)
    }
}
