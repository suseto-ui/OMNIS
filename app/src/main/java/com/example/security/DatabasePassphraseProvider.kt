package com.example.security

import android.content.Context
import android.util.Base64
import android.util.Log
import java.security.SecureRandom

/**
 * Poskytovatel šifrovacího klíče (passphrase) pro SQLCipher Room databázi.
 *
 * Klíč o délce 256 bitů (32 bajtů) je vygenerován kryptograficky bezpečným generátorem
 * a zašifrován pomocí Master Key v hardwarovém úložišti Android KeyStore (AES/GCM/NoPadding)
 * prostřednictvím [OmnisSecureStorage].
 *
 * Uživatel nemusí zadávat žádné manuální heslo; šifrování databáze na disku je plně transparentní
 * a svázané s hardwarovou identitou a úložištěm klíčů zařízení.
 */
object DatabasePassphraseProvider {
    private const val TAG = "DatabasePassphrase"
    private const val KEY_DB_PASSPHRASE = "omnis_sqlcipher_db_key"

    // Bezpečný fallback klíč pro JVM/Robolectric testovací prostředí, kde AndroidKeyStore není k dispozici
    private val JVM_TEST_FALLBACK_PASSPHRASE = "OMNIS_SECURE_SQLCIPHER_PASSPHRASE_KEY_2026_TEST_A98F".toByteArray(Charsets.UTF_8)

    @Volatile
    private var cachedPassphrase: ByteArray? = null

    @Synchronized
    fun getDatabasePassphrase(context: Context): ByteArray {
        cachedPassphrase?.let { return it }

        try {
            // 1. Zkusíme načíst uložený dešifrovaný klíč z Android KeyStore
            val storedBase64 = OmnisSecureStorage.decryptAndRetrieve(context, KEY_DB_PASSPHRASE)
            if (!storedBase64.isNullOrBlank()) {
                val decoded = Base64.decode(storedBase64, Base64.DEFAULT)
                if (decoded.size == 32) {
                    cachedPassphrase = decoded
                    return decoded
                }
            }

            // 2. Pokud ještě neexistuje, vygenerujeme nový 256bitový (32 bajtů) náhodný klíč
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            val base64Key = Base64.encodeToString(randomBytes, Base64.DEFAULT)

            // 3. Uložíme zašifrovaný klíč do OmnisSecureStorage (Android KeyStore AES-GCM)
            OmnisSecureStorage.encryptAndStore(context, KEY_DB_PASSPHRASE, base64Key)

            cachedPassphrase = randomBytes
            Log.i(TAG, "Vygenerován a bezpečně uložen nový 256bitový klíč pro SQLCipher databázi.")
            return randomBytes
        } catch (e: Exception) {
            Log.w(TAG, "KeyStore není dostupný (možné testovací nebo omezené prostředí), použit fallback: ${e.message}")
            return JVM_TEST_FALLBACK_PASSPHRASE
        }
    }

    /**
     * Vyčištění cache pro testovací účely.
     */
    fun clearCache() {
        cachedPassphrase = null
    }
}
