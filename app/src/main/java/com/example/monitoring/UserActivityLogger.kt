package com.example.monitoring

import android.content.Context
import android.util.Log
import com.example.data.OmnisDatabase
import com.example.data.UserActivityLogDao
import com.example.data.UserActivityLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Logovací a auditní služba sledování uživatelských interakcí v aplikaci O.M.N.I.S.
 * Ukládá veškeré události do lokální Room databáze pro pozdější audit a analýzu.
 */
class UserActivityLogger(
    private val logDao: UserActivityLogDao,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    /**
     * Zaznamená všeobecnou aktivity uživatele.
     */
    fun logActivity(
        userId: String,
        action: String,
        details: String,
        severity: String = "INFO"
    ) {
        val entry = UserActivityLogEntity(
            userId = userId,
            action = action,
            details = details,
            severity = severity,
            timestamp = System.currentTimeMillis()
        )
        coroutineScope.launch {
            try {
                logDao.insertLog(entry)
                Log.i("UserActivityLogger", "[$severity] $userId: $action - $details")
            } catch (e: Exception) {
                Log.e("UserActivityLogger", "Failed to insert audit log entry", e)
            }
        }
    }

    /**
     * Zaznamená pokus o přihlášení uživatele.
     */
    fun logUserLogin(userId: String, success: Boolean, details: String = "") {
        val severity = if (success) "INFO" else "WARN"
        val action = if (success) "LOGIN_SUCCESS" else "LOGIN_FAILURE"
        logActivity(
            userId = userId,
            action = action,
            details = details.ifBlank { "Pokus o přihlášení: $action" },
            severity = severity
        )
    }

    /**
     * Zaznamená navigaci na obrazovku nebo záložku v aplikaci.
     */
    fun logNavigation(userId: String, destination: String) {
        logActivity(
            userId = userId,
            action = "NAVIGATION",
            details = "Přechod na obrazovku: $destination",
            severity = "INFO"
        )
    }

    /**
     * Zaznamená interakci s prvkem UI.
     */
    fun logInteraction(userId: String, component: String, actionDetail: String) {
        logActivity(
            userId = userId,
            action = "UI_INTERACTION",
            details = "[$component] $actionDetail",
            severity = "INFO"
        )
    }

    /**
     * Zaznamená odeslání kognitivního dotazu / promptu.
     */
    fun logPromptSubmitted(userId: String, promptLength: Int, sanitized: Boolean) {
        logActivity(
            userId = userId,
            action = "PROMPT_SUBMITTED",
            details = "Odeslán dotaz (délka: $promptLength znaků, sanitizováno: $sanitized)",
            severity = "INFO"
        )
    }

    /**
     * Zaznamená bezpečnostní událost nebo varování.
     */
    fun logSecurityAlert(userId: String, alertType: String, message: String) {
        logActivity(
            userId = userId,
            action = "SECURITY_ALERT",
            details = "[$alertType] $message",
            severity = "CRITICAL"
        )
    }

    /**
     * Vrací reakční Flow všech auditních záznamů řazených od nejnovějších.
     */
    fun getAllLogs(): Flow<List<UserActivityLogEntity>> = logDao.getAllLogs()

    /**
     * Vrací reakční Flow posledních N auditních záznamů.
     */
    fun getRecentLogs(limit: Int = 100): Flow<List<UserActivityLogEntity>> = logDao.getRecentLogs(limit)

    /**
     * Vrací záznamy konkrétního uživatele.
     */
    fun getLogsByUserId(userId: String): Flow<List<UserActivityLogEntity>> = logDao.getLogsByUserId(userId)

    /**
     * Vyčistí všechny uložené auditní záznamy.
     */
    suspend fun clearLogs() {
        logDao.clearLogs()
    }

    companion object {
        @Volatile
        private var INSTANCE: UserActivityLogger? = null

        fun getInstance(context: Context): UserActivityLogger {
            return INSTANCE ?: synchronized(this) {
                val db = OmnisDatabase.getDatabase(context.applicationContext)
                val instance = UserActivityLogger(db.userActivityLogDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
