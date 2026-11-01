package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Robust Google Sign-In Manager utilizing Jetpack CredentialManager.
 * Strictly adheres to zero-trust invariants: interactive Google Sign-In exclusively,
 * single option per GetCredentialRequest, and explicit cancellation handling.
 */
object GoogleAuthManager {
    private const val TAG = "GoogleAuthManager"

    private val auth: FirebaseAuth get() = Firebase.auth

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private var authListener: FirebaseAuth.AuthStateListener? = null

    fun initialize(context: Context) {
        _currentUser.value = auth.currentUser
        if (authListener == null) {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                _currentUser.value = firebaseAuth.currentUser
                Log.d(TAG, "Auth state changed: user=${firebaseAuth.currentUser?.email}")
            }
            authListener = listener
            auth.addAuthStateListener(listener)
        }
    }

    /**
     * Silent auto-login attempt on app start.
     */
    fun attemptAutoSignIn(
        context: Context,
        scope: CoroutineScope,
        onSuccess: (FirebaseUser) -> Unit = {},
        onFailure: () -> Unit = {}
    ) {
        val user = auth.currentUser
        if (user != null) {
            _currentUser.value = user
            onSuccess(user)
            return
        }

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.w(TAG, "default_web_client_id not found: ${e.message}")
            onFailure()
            return
        }

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch(Dispatchers.Main) {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(token, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val signedInUser = authResult.user
                    _currentUser.value = signedInUser
                    if (signedInUser != null) {
                        onSuccess(signedInUser)
                    } else {
                        onFailure()
                    }
                } else {
                    onFailure()
                }
            } catch (e: Exception) {
                Log.d(TAG, "Silent sign-in not available: ${e.message}")
                onFailure()
            }
        }
    }

    /**
     * Interactive Google Sign-In flow triggered by user button tap.
     */
    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: (FirebaseUser) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            val err = "Konfigurace Google Sign-In chybí (default_web_client_id nenalezen)"
            _authError.value = err
            onError(err)
            return
        }

        _isAuthenticating.value = true
        _authError.value = null

        val credentialManager = CredentialManager.create(activity)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch(Dispatchers.Main) {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    _currentUser.value = user
                    _isAuthenticating.value = false
                    if (user != null) {
                        Log.i(TAG, "Přihlášení úspěšné: ${user.email} (${user.uid})")
                        onSuccess(user)
                    } else {
                        onError("Přihlášení se nezdařilo: prázdný uživatel")
                    }
                } else {
                    _isAuthenticating.value = false
                    val err = "Neočekávaný typ přihlašovacích údajů"
                    _authError.value = err
                    onError(err)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Uživatel zrušil dialog přihlášení Google: ${e.message}")
                _isAuthenticating.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In selhal: ${e.message}", e)
                _isAuthenticating.value = false
                val msg = e.localizedMessage ?: "Přihlášení přes Google selhalo"
                _authError.value = msg
                onError(msg)
            }
        }
    }

    /**
     * Odhlášení uživatele z Firebase i CredentialManageru.
     */
    fun signOut(context: Context, scope: CoroutineScope, onComplete: () -> Unit = {}) {
        auth.signOut()
        _currentUser.value = null
        val credentialManager = CredentialManager.create(context)
        scope.launch(Dispatchers.IO) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Chyba při mazání relace CredentialManageru: ${e.message}", e)
            } finally {
                scope.launch(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
}
