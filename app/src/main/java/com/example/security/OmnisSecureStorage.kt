package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Zabezpečené šifrované úložiště klíčů a klientských konfigurací s hardwarovou ochranou (Android KeyStore).
 * Kód splňuje standardy OWASP Mobile Top 10 (M2: Insecure Data Storage).
 */
object OmnisSecureStorage {
    private const val TAG = "OmnisSecureStorage"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "OmnisMasterKey"
    private const val SHARED_PREFS_NAME = "omnis_secure_prefs"
    private const val AES_MODE = "AES/GCM/NoPadding"

    init {
        try {
            initMasterKey()
        } catch (e: Exception) {
            Log.e(TAG, "Chyba při inicializaci Master Key v KeyStore: ${e.message}", e)
        }
    }

    private fun initMasterKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val builder = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
            Log.i(TAG, "Zabezpečený Master Key byl úspěšně vygenerován v Android KeyStore.")
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * Zašifruje řetězec a uloží jej do SharedPreferences.
     */
    fun encryptAndStore(context: Context, key: String, value: String) {
        try {
            val cipher = Cipher.getInstance(AES_MODE)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            
            // Zakódování šifrovaného textu a IV do Base64
            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
            val ivBase64 = Base64.encodeToString(iv, Base64.DEFAULT)

            val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("${key}_encrypted", encryptedBase64)
                .putString("${key}_iv", ivBase64)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Chyba při šifrování hodnoty pro klíč $key: ${e.message}", e)
        }
    }

    /**
     * Načte a dešifruje řetězec ze SharedPreferences.
     */
    fun decryptAndRetrieve(context: Context, key: String): String? {
        try {
            val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            val encryptedBase64 = prefs.getString("${key}_encrypted", null) ?: return null
            val ivBase64 = prefs.getString("${key}_iv", null) ?: return null

            val encryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)

            val cipher = Cipher.getInstance(AES_MODE)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Chyba při dešifrování hodnoty pro klíč $key: ${e.message}", e)
            return null
        }
    }
}
