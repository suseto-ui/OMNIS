package com.example.auth

import android.content.Context
import com.example.auth.crypto.PasswordHasher
import com.example.auth.jwt.JwtTokenManager
import com.example.data.OmnisDatabase
import com.example.data.UserAuthDao
import com.example.data.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class RegistrationResult {
    data class Success(val user: UserEntity, val token: String) : RegistrationResult()
    data class Failure(val reason: String) : RegistrationResult()
}

/**
 * Centrální služba pro registraci, přihlášení a odhlášení uživatelů
 * s nativní JWT tokenizací a soleným PBKDF2 hashováním uloženým v Room.
 */
class UserAuthService(
    private val userAuthDaoProvider: (Context) -> UserAuthDao = { context ->
        OmnisDatabase.getDatabase(context).userAuthDao()
    }
) {
    companion object {
        val instance: UserAuthService by lazy { UserAuthService() }

        // Výchozí povolené bootstrap přihlašovací údaje pro automatický seed
        private val BOOTSTRAP_ACCOUNTS = mapOf(
            "admin" to Pair("Omnis@Admin2025", UserRole.ADMIN_OPERATOR),
            "operator" to Pair("Operator2025", UserRole.ADMIN_OPERATOR),
            "user" to Pair("User2025", UserRole.STANDARD_USER)
        )
    }

    /**
     * Registrace nového uživatele s unikátním jménem, soleným PBKDF2 heslem
     * a vydáním prvotního JWT tokenu.
     */
    suspend fun register(
        context: Context,
        username: String,
        password: String,
        role: UserRole = UserRole.STANDARD_USER
    ): RegistrationResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        if (cleanUser.length < 3) {
            return@withContext RegistrationResult.Failure("Uživatelské jméno musí mít alespoň 3 znaky.")
        }
        if (password.length < 6) {
            return@withContext RegistrationResult.Failure("Heslo musí mít alespoň 6 znaků.")
        }

        val dao = userAuthDaoProvider(context)
        val existing = dao.findByUsername(cleanUser)
        if (existing != null) {
            return@withContext RegistrationResult.Failure("Uživatel '$cleanUser' již existuje.")
        }

        // Kryptografické hashování PBKDF2 s náhodnou 16-bajtovou solí
        val hashResult = PasswordHasher.hashPassword(password)

        val newUser = UserEntity(
            username = cleanUser,
            passwordHash = hashResult.hashHex,
            passwordSalt = hashResult.saltHex,
            role = role.name,
            createdAt = System.currentTimeMillis()
        )

        dao.insertUser(newUser)

        // Vystavení podepsaného JWT tokenu (RFC 7519)
        val jwtToken = JwtTokenManager.createToken(
            subject = cleanUser,
            role = role.name
        )

        RegistrationResult.Success(user = newUser, token = jwtToken)
    }

    /**
     * Přihlášení uživatele s ověřením PBKDF2 hashe v Room a vydáním JWT tokenu.
     */
    suspend fun login(
        context: Context,
        username: String,
        password: String,
        roleRequested: UserRole? = null
    ): AuthResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        if (cleanUser.isBlank()) {
            return@withContext AuthResult.Failure("Uživatelské jméno nesmí být prázdné.")
        }
        if (password.isBlank()) {
            return@withContext AuthResult.Failure("Heslo nesmí být prázdné.")
        }

        val dao = userAuthDaoProvider(context)
        var user = dao.findByUsername(cleanUser)

        // Bootstrap on-the-fly seed pro výchozí účty, pokud uživatel ještě není v DB
        if (user == null && BOOTSTRAP_ACCOUNTS.containsKey(cleanUser.lowercase())) {
            val (defaultPass, defaultRole) = BOOTSTRAP_ACCOUNTS[cleanUser.lowercase()]!!
            if (password == defaultPass) {
                val hashResult = PasswordHasher.hashPassword(defaultPass)
                val seededUser = UserEntity(
                    username = cleanUser,
                    passwordHash = hashResult.hashHex,
                    passwordSalt = hashResult.saltHex,
                    role = defaultRole.name
                )
                dao.insertUser(seededUser)
                user = seededUser
            }
        }

        if (user == null) {
            return@withContext AuthResult.Failure("Uživatel '$cleanUser' nebyl nalezen.")
        }

        // Ověření hesla vůči PBKDF2 hashi v konstantním čase
        val isPasswordCorrect = PasswordHasher.verifyPassword(
            password = password,
            storedHashHex = user.passwordHash,
            storedSaltHex = user.passwordSalt
        )

        if (!isPasswordCorrect) {
            return@withContext AuthResult.Failure("Neplatné heslo pro uživatele '$cleanUser'.")
        }

        val userRole = try {
            UserRole.valueOf(user.role)
        } catch (_: Exception) {
            UserRole.STANDARD_USER
        }

        // Kontrola vyžádané role, pokud je specifikována
        if (roleRequested != null && roleRequested == UserRole.ADMIN_OPERATOR && userRole != UserRole.ADMIN_OPERATOR) {
            return@withContext AuthResult.Failure("Uživatel '$cleanUser' nedisponuje oprávněním ADMIN_OPERATOR.")
        }

        // Generování nového podepsaného JWT tokenu
        val effectiveRole = roleRequested ?: userRole
        val jwtToken = JwtTokenManager.createToken(
            subject = user.username,
            role = effectiveRole.name
        )

        val session = AuthSession(
            token = jwtToken,
            username = user.username,
            role = effectiveRole,
            createdAt = System.currentTimeMillis()
        )

        // Uložení relace
        saveSessionToPrefs(context, session)

        AuthResult.Success(session)
    }

    /**
     * Odhlášení uživatele.
     */
    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(AuthenticationManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * Inicializace a předvyplnění výchozích účtů pokud je tabulka prázdná.
     */
    suspend fun seedDefaultUsersIfEmpty(dao: UserAuthDao) = withContext(Dispatchers.IO) {
        if (dao.getUserCount() == 0) {
            for ((uname, creds) in BOOTSTRAP_ACCOUNTS) {
                val (pass, role) = creds
                val hashResult = PasswordHasher.hashPassword(pass)
                val entity = UserEntity(
                    username = uname,
                    passwordHash = hashResult.hashHex,
                    passwordSalt = hashResult.saltHex,
                    role = role.name
                )
                dao.insertUser(entity)
            }
        }
    }

    private fun saveSessionToPrefs(context: Context, session: AuthSession) {
        val prefs = context.getSharedPreferences(AuthenticationManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(AuthenticationManager.KEY_TOKEN, session.token)
            .putString(AuthenticationManager.KEY_USERNAME, session.username)
            .putString(AuthenticationManager.KEY_ROLE, session.role.name)
            .apply()
    }
}
