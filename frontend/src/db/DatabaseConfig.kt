package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

/**
 * DatabaseConfig defines the secure connection and metadata layer for
 * O.M.N.I.S. database persistence, integrating Room with the Google Cloud SQL (PostgreSQL) engine.
 */
object DatabaseConfig {
    private const val TAG = "DatabaseConfig"

    private fun getBuildConfigValue(fieldName: String): String? {
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField(fieldName)
            field.get(null) as? String
        } catch (e: Exception) {
            null
        }
    }

    // Google Cloud SQL (PostgreSQL) Connection Parameters
    val host: String? by lazy { getBuildConfigValue("CLOUDSQL_HOST") }
    val dbName: String by lazy { getBuildConfigValue("CLOUDSQL_DB") ?: "omnis_db" }
    val user: String by lazy { getBuildConfigValue("CLOUDSQL_USER") ?: "postgres" }
    val password: String? by lazy { getBuildConfigValue("CLOUDSQL_PASSWORD") }

    val isCloudSqlConfigured: Boolean
        get() = !host.isNullOrBlank() && !password.isNullOrBlank()

    /**
     * Obtains a direct connection to the Google Cloud SQL database instance.
     */
    fun getPostgresConnection(): Connection? {
        val currentHost = host
        val currentPass = password
        if (currentHost.isNullOrBlank() || currentPass.isNullOrBlank()) {
            Log.w(TAG, "PostgreSQL host or password is not configured. Falling back to local Room persistence.")
            return null
        }

        val url = "jdbc:postgresql://$currentHost:5432/$dbName"
        return try {
            Class.forName("org.postgresql.Driver")
            DriverManager.getConnection(url, user, currentPass)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Google Cloud SQL PostgreSQL instance at $url", e)
            null
        }
    }
}

/**
 * CloudSqlRepository acts as the secure persistence and query translation layer
 * for synchronization of chat sessions and O.M.N.I.S. multi-dimensional impact matrices to PostgreSQL.
 */
class CloudSqlRepository {
    private const val TAG = "CloudSqlRepository"

    suspend fun saveRecord(record: OmnisRecord): Boolean = withContext(Dispatchers.IO) {
        val conn = DatabaseConfig.getPostgresConnection() ?: return@withContext false
        var pstmt: PreparedStatement? = null
        try {
            val insertSql = """
                INSERT INTO omnis_messages (
                    id, role, content, cognitive_process, follow_up_questions,
                    val_sys, val_econ, val_psych, val_eco, val_law, val_sec, val_phys, val_soc,
                    composite_score, domain, attached_image_path, timestamp, defense_tier, defense_notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    role = EXCLUDED.role,
                    content = EXCLUDED.content,
                    cognitive_process = EXCLUDED.cognitive_process,
                    follow_up_questions = EXCLUDED.follow_up_questions,
                    val_sys = EXCLUDED.val_sys,
                    val_econ = EXCLUDED.val_econ,
                    val_psych = EXCLUDED.val_psych,
                    val_eco = EXCLUDED.val_eco,
                    val_law = EXCLUDED.val_law,
                    val_sec = EXCLUDED.val_sec,
                    val_phys = EXCLUDED.val_phys,
                    val_soc = EXCLUDED.val_soc,
                    composite_score = EXCLUDED.composite_score,
                    domain = EXCLUDED.domain,
                    attached_image_path = EXCLUDED.attached_image_path,
                    timestamp = EXCLUDED.timestamp,
                    defense_tier = EXCLUDED.defense_tier,
                    defense_notes = EXCLUDED.defense_notes
            """.trimIndent()

            pstmt = conn.prepareStatement(insertSql)
            pstmt.setLong(1, record.id)
            pstmt.setString(2, record.role)
            pstmt.setString(3, record.content)
            pstmt.setString(4, record.cognitiveProcess)
            pstmt.setString(5, record.followUpQuestions)
            pstmt.setFloat(6, record.valSys)
            pstmt.setFloat(7, record.valEcon)
            pstmt.setFloat(8, record.valPsych)
            pstmt.setFloat(9, record.valEco)
            pstmt.setFloat(10, record.valLaw)
            pstmt.setFloat(11, record.valSec)
            pstmt.setFloat(12, record.valPhys)
            pstmt.setFloat(13, record.valSoc)
            pstmt.setFloat(14, record.compositeScore)
            pstmt.setString(15, record.domain)
            pstmt.setString(16, record.attachedImagePath)
            pstmt.setLong(17, record.timestamp)
            pstmt.setString(18, record.defenseTier)
            pstmt.setString(19, record.defenseNotes)

            pstmt.executeUpdate()
            Log.i(TAG, "Successfully saved record #${record.id} to Google Cloud SQL.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving record to Google Cloud SQL", e)
            false
        } finally {
            try { pstmt?.close() } catch (ex: Exception) {}
            try { conn.close() } catch (ex: Exception) {}
        }
    }

    suspend fun getAllRecords(): List<OmnisRecord> = withContext(Dispatchers.IO) {
        val records = mutableListOf<OmnisRecord>()
        val conn = DatabaseConfig.getPostgresConnection() ?: return@withContext records
        var stmt: Statement? = null
        var rs: ResultSet? = null
        try {
            stmt = conn.createStatement()
            rs = stmt.executeQuery("SELECT * FROM omnis_messages ORDER BY timestamp ASC")
            while (rs.next()) {
                records.add(
                    OmnisRecord(
                        id = rs.getLong("id"),
                        role = rs.getString("role"),
                        content = rs.getString("content"),
                        cognitiveProcess = rs.getString("cognitive_process") ?: "",
                        followUpQuestions = rs.getString("follow_up_questions") ?: "",
                        valSys = rs.getFloat("val_sys"),
                        valEcon = rs.getFloat("val_econ"),
                        valPsych = rs.getFloat("val_psych"),
                        valEco = rs.getFloat("val_eco"),
                        valLaw = rs.getFloat("val_law"),
                        valSec = rs.getFloat("val_sec"),
                        valPhys = rs.getFloat("val_phys"),
                        valSoc = rs.getFloat("val_soc"),
                        compositeScore = rs.getFloat("composite_score"),
                        domain = rs.getString("domain") ?: "SYSTEMS_INTELLIGENCE",
                        attachedImagePath = rs.getString("attached_image_path"),
                        timestamp = rs.getLong("timestamp"),
                        defenseTier = rs.getString("defense_tier") ?: "APPROVED",
                        defenseNotes = rs.getString("defense_notes") ?: ""
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching records from Google Cloud SQL", e)
        } finally {
            try { rs?.close() } catch (ex: Exception) {}
            try { stmt?.close() } catch (ex: Exception) {}
            try { conn.close() } catch (ex: Exception) {}
        }
        records
    }

    suspend fun clearAllRecords(): Boolean = withContext(Dispatchers.IO) {
        val conn = DatabaseConfig.getPostgresConnection() ?: return@withContext false
        var stmt: Statement? = null
        try {
            stmt = conn.createStatement()
            stmt.executeUpdate("DELETE FROM omnis_messages")
            Log.i(TAG, "Successfully truncated omnis_messages on Google Cloud SQL.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing records on Google Cloud SQL", e)
            false
        } finally {
            try { stmt?.close() } catch (ex: Exception) {}
            try { conn.close() } catch (ex: Exception) {}
        }
    }
}
