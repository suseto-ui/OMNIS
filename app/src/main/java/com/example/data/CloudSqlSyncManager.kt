package com.example.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Statement

object CloudSqlSyncManager {
    private const val TAG = "CloudSqlSyncManager"

    private val syncScope = CoroutineScope(Dispatchers.IO)

    init {
        // Run database initialization on startup asynchronously
        syncScope.launch {
            initDatabase()
        }
    }

    private fun getConnection(): Connection? {
        return DatabaseConfig.getPostgresConnection()
    }

    suspend fun initDatabase() = withContext(Dispatchers.IO) {
        val conn = getConnection() ?: return@withContext
        try {
            val stmt: Statement = conn.createStatement()
            val createTableSql = """
                CREATE TABLE IF NOT EXISTS omnis_messages (
                    id BIGINT PRIMARY KEY,
                    role VARCHAR(50) NOT NULL,
                    content TEXT NOT NULL,
                    cognitive_process TEXT,
                    follow_up_questions TEXT,
                    val_sys REAL,
                    val_econ REAL,
                    val_psych REAL,
                    val_eco REAL,
                    val_law REAL,
                    val_sec REAL,
                    val_phys REAL,
                    val_soc REAL,
                    composite_score REAL,
                    domain VARCHAR(100),
                    attached_image_path TEXT,
                    timestamp BIGINT,
                    defense_tier VARCHAR(50),
                    defense_notes TEXT
                )
            """.trimIndent()
            stmt.execute(createTableSql)
            Log.i(TAG, "Google Cloud SQL database tables initialized successfully.")
            stmt.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing Cloud SQL tables (Offline local Room storage is active): ${e.message}")
        } finally {
            try {
                conn.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun syncRecordAsync(record: OmnisRecord) {
        syncScope.launch {
            val conn = getConnection() ?: run {
                Log.w(TAG, "Cloud SQL connection parameters missing or database offline. Record persisted locally only.")
                return@launch
            }
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

                val pstmt: PreparedStatement = conn.prepareStatement(insertSql)
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
                pstmt.close()
                Log.i(TAG, "Successfully synced record #${record.id} to Google Cloud SQL.")
            } catch (e: java.sql.SQLException) {
                Log.w(TAG, "SQLException during record sync to Google Cloud SQL (local fallback is active): ${e.message}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync record #${record.id} to Google Cloud SQL (local fallback is active): ${e.message}")
            } finally {
                try {
                    conn.close()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }
}
