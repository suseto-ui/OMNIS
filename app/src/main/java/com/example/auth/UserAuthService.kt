package com.example.auth

import android.content.Context
import com.example.auth.crypto.PasswordHasher
import com.example.auth.jwt.JwtTokenManager
import com.example.data.OmnisDatabase
import com.example.data.UserAuthDao
import com.example.data.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.api.OmnisGeminiClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

sealed class RegistrationResult {
    data class Success(val user: UserEntity, val token: String) : RegistrationResult()
    data class Failure(val reason: String) : RegistrationResult()
}

data class RemoteThreadSummary(
    val id: String,
    val title: String,
    val ontologyDomain: String,
    val messageCount: Int
)

/**
 * Centrální služba pro registraci, přihlášení a odhlášení uživatelů
 * s backendovou synchronizací přes FastAPI na Google Cloud Run a JWT.
 */
class UserAuthService {
    companion object {
        val instance: UserAuthService by lazy { UserAuthService() }
        const val BASE_URL = "https://ais-dev-ex6yxewfd4hwymxojrmgxr-291037164760.europe-west3.run.app"
    }

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private fun parseUserRole(role: String?): UserRole {
        if (role == null) return UserRole.STANDARD_USER
        return try {
            UserRole.valueOf(role)
        } catch (_: Exception) {
            if (role.contains("ADMIN", ignoreCase = true) || role.contains("OPERATOR", ignoreCase = true)) {
                UserRole.ADMIN_OPERATOR
            } else {
                UserRole.STANDARD_USER
            }
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String
    ): RegistrationResult = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = if (email.contains("@") && email.contains(".")) email.trim() else "$username@omnis.ai"
            val json = JSONObject().apply {
                put("username", username.trim())
                put("email", cleanEmail)
                put("password", password)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/api/auth/register")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val rawBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val resJson = try { JSONObject(rawBody) } catch (_: Exception) { JSONObject() }
                val token = resJson.optString("access_token", "token-$username")
                val role = resJson.optString("role", "STANDARD_USER")
                
                RegistrationResult.Success(
                    user = UserEntity(username = username, passwordHash = "", passwordSalt = "", role = role),
                    token = token
                )
            } else {
                val errorDetail = try {
                    val errObj = JSONObject(rawBody)
                    errObj.optString("detail", "Chyba při registraci (${response.code})")
                } catch (_: Exception) {
                    "Chyba při registraci (${response.code}). Zkontrolujte zadané údaje."
                }
                RegistrationResult.Failure(errorDetail)
            }
        } catch (e: Exception) {
            RegistrationResult.Failure("Komunikační chyba: ${e.localizedMessage ?: "Neznámá chyba sítě"}")
        }
    }

    suspend fun login(
        username: String,
        password: String
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("username", username.trim())
                put("password", password)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/api/auth/login")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val rawBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val resJson = try { JSONObject(rawBody) } catch (_: Exception) { JSONObject() }
                val token = resJson.optString("access_token")
                if (token.isEmpty()) {
                    AuthResult.Failure("Neplatná odpověď serveru.")
                } else {
                    val role = resJson.optString("role", "STANDARD_USER")
                    AuthResult.Success(
                        AuthSession(token = token, username = username, role = parseUserRole(role))
                    )
                }
            } else {
                val errorDetail = try {
                    val errObj = JSONObject(rawBody)
                    errObj.optString("detail", "Chyba při přihlášení (${response.code})")
                } catch (_: Exception) {
                    "Neplatné přihlašovací údaje nebo chyba serveru (${response.code})."
                }
                AuthResult.Failure(errorDetail)
            }
        } catch (e: Exception) {
            AuthResult.Failure("Komunikační chyba: ${e.localizedMessage ?: "Služba nedostupná."}")
        }
    }

    suspend fun loginWithGoogle(idToken: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("id_token", idToken)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/api/auth/google")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val rawBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val resJson = try { JSONObject(rawBody) } catch (_: Exception) { JSONObject() }
                val token = resJson.optString("access_token")
                if (token.isEmpty()) {
                    AuthResult.Failure("Neplatná odpověď serveru (Google Auth).")
                } else {
                    val username = resJson.optString("username", "Google User")
                    val role = resJson.optString("role", "STANDARD_USER")
                    
                    AuthResult.Success(
                        AuthSession(token = token, username = username, role = parseUserRole(role))
                    )
                }
            } else {
                val errorDetail = try {
                    val errObj = JSONObject(rawBody)
                    errObj.optString("detail", "Google přihlášení selhalo (${response.code})")
                } catch (_: Exception) {
                    "Google přihlášení selhalo (HTTP ${response.code})."
                }
                AuthResult.Failure(errorDetail)
            }
        } catch (e: Exception) {
            AuthResult.Failure("Komunikační chyba s backendem: ${e.localizedMessage ?: "Neznámá chyba"}")
        }
    }

    suspend fun getProfile(token: String): ProfileResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/user/profile")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val resJson = JSONObject(response.body?.string() ?: "{}")
                ProfileResult.Success(parseProfile(resJson))
            } else {
                ProfileResult.Failure("Nelze načíst profil: ${response.message}")
            }
        } catch (e: Exception) {
            ProfileResult.Failure(e.localizedMessage ?: "Chyba sítě")
        }
    }

    suspend fun updateProfile(token: String, update: UserProfileUpdate): ProfileResult = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                update.avatarUrl?.let { put("avatar_url", it) }
                update.bio?.let { put("bio", it) }
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/api/user/profile")
                .header("Authorization", "Bearer $token")
                .put(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val resJson = JSONObject(response.body?.string() ?: "{}")
                ProfileResult.Success(parseProfile(resJson))
            } else {
                ProfileResult.Failure("Nelze aktualizovat profil.")
            }
        } catch (e: Exception) {
            ProfileResult.Failure(e.localizedMessage ?: "Chyba sítě")
        }
    }

    /**
     * Získá seznam konverzačních vláken uložených v PostgreSQL na Cloud Runu.
     */
    suspend fun getRemoteThreads(token: String): List<RemoteThreadSummary> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/threads")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonArray = JSONArray(response.body?.string() ?: "[]")
                val list = mutableListOf<RemoteThreadSummary>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val msgs = obj.optJSONArray("messages")
                    list.add(
                        RemoteThreadSummary(
                            id = obj.optString("id", ""),
                            title = obj.optString("title", "Bez názvu"),
                            ontologyDomain = obj.optString("ontology_domain", "SYSTEMS_INTELLIGENCE"),
                            messageCount = msgs?.length() ?: 0
                        )
                    )
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Vytvoří nové konverzační vlákno v PostgreSQL na Cloud Runu.
     */
    suspend fun createRemoteThread(token: String, title: String, domain: String = "SYSTEMS_INTELLIGENCE"): String? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("title", title)
                put("ontology_domain", domain)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/api/threads")
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val resJson = JSONObject(response.body?.string() ?: "{}")
                resJson.optString("id", null)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseProfile(json: JSONObject): UserProfileResponse {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        return UserProfileResponse(
            id = json.optString("id", ""),
            userId = json.optString("user_id", ""),
            username = json.optString("username", ""),
            email = json.optString("email", ""),
            avatarUrl = if (json.isNull("avatar_url")) null else json.optString("avatar_url", null),
            birthDate = if (json.isNull("birth_date")) null else {
                try { sdf.parse(json.getString("birth_date")) } catch (_: Exception) { null }
            },
            bio = if (json.isNull("bio")) null else json.optString("bio", null),
            preferencesJson = emptyMap(),
            role = json.optString("role", "STANDARD_USER"),
            createdAt = try { sdf.parse(json.optString("created_at", "")) } catch (_: Exception) { java.util.Date() }
        )
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(AuthenticationManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
