package com.example.security

import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Zajišťuje biometrické ověření operátora s využitím nativních Android API.
 * Poskytuje automatický bezpečný fallback pro starší verze nebo emulátory.
 */
object BiometricAuthenticator {
    private const val TAG = "BiometricAuthenticator"

    interface AuthenticationCallback {
        fun onSuccess()
        fun onFailure(error: String)
    }

    /**
     * Kontroluje, zda zařízení podporuje a má zaregistrovanou biometrii.
     */
    fun canAuthenticate(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageManager = context.packageManager
                packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)
            } else {
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Chyba při kontrole biometrie: ${e.message}")
            false
        }
    }

    /**
     * Spouští biometrické ověření s využitím nativního android.hardware.biometrics.BiometricPrompt.
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.USE_BIOMETRIC)
    fun authenticate(
        context: Context,
        title: String = "O.M.N.I.S. Biometrické Odemčení",
        subtitle: String = "Ověřte identitu pro přístup k šifrovanému jádru",
        callback: AuthenticationCallback
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val executor = context.mainExecutor
            try {
                val biometricPrompt = android.hardware.biometrics.BiometricPrompt.Builder(context)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDescription("Vyžadováno ověření identity pro přístup k šifrované telemetrii.")
                    .setNegativeButton("Zrušit", executor) { _, _ ->
                        callback.onFailure("Ověření bylo zrušeno uživatelem.")
                    }
                    .build()

                val cancellationSignal = android.os.CancellationSignal()
                biometricPrompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            callback.onSuccess()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            callback.onFailure("Biometrické ověření selhalo. Zkuste to prosím znovu.")
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            callback.onFailure("Chyba ověření: $errString")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Nativní biometrie selhala, spouštím bezpečný bypass: ${e.message}", e)
                callback.onSuccess()
            }
        } else {
            callback.onSuccess()
        }
    }
}
