package com.example.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement
import kotlin.random.Random

object CloudSqlSyncManager {
    private const val TAG = "CloudSqlSyncManager"
    private const val MAX_RETRY_ATTEMPTS = 3

    sealed interface SyncStatus {
        object Idle : SyncStatus
        object Connecting : SyncStatus
        data class Syncing(val pendingCount: Int) : SyncStatus
        data class Success(val syncedCount: Int, val lastSyncTime: Long) : SyncStatus
        data class Offline(val reason: String, val pendingCount: Int) : SyncStatus
    }

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val syncScope = CoroutineScope(com.example.api.OmnisGeminiClient.ioDispatcher)

    init {
        if (!DatabaseConfig.isTesting) {
            // Run database initialization on startup asynchronously
            syncScope.launch {
                initDatabase()
            }
        }
    }

    private fun getConnection(): Connection? {
        return DatabaseConfig.getPostgresConnection()
    }

    /**
     * Ověří síťové a databázové spojení s Cloud SQL PostgreSQL instancí.
     */
    suspend fun testConnection(): Pair<Boolean, String> = withContext(com.example.api.OmnisGeminiClient.ioDispatcher) {
        if (DatabaseConfig.isTesting) {
            _syncStatus.value = SyncStatus.Success(0, System.currentTimeMillis())
            return@withContext Pair(true, "Testovací sandbox: Emulace Cloud SQL aktivní.")
        }
        _syncStatus.value = SyncStatus.Connecting
        val conn = getConnection()
        if (conn == null) {
            val reason = "Nelze navázat spojení se serverem Cloud SQL (${DatabaseConfig.host}:${DatabaseConfig.port}). Zkontrolujte síť a přístupové údaje."
            _syncStatus.value = SyncStatus.Offline(reason, 0)
            return@withContext Pair(false, reason)
        }
        try {
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery("SELECT 1")
            var ok = false
            if (rs.next()) {
                ok = rs.getInt(1) == 1
            }
            rs.close()
            stmt.close()
            _syncStatus.value = SyncStatus.Success(0, System.currentTimeMillis())
            Pair(ok, "Spojení s Cloud SQL (${DatabaseConfig.host}) úspěšně navázáno a ověřeno.")
        } catch (e: Exception) {
            val msg = "Chyba ověření dotazu: ${e.message}"
            _syncStatus.value = SyncStatus.Offline(msg, 0)
            Pair(false, msg)
        } finally {
            try {
                conn.close()
            } catch (ignored: Exception) {}
        }
    }

    suspend fun initDatabase() = withContext(com.example.api.OmnisGeminiClient.ioDispatcher) {
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
                    defense_notes TEXT,
                    thread_id VARCHAR(128) DEFAULT 'thread_main',
                    thread_title VARCHAR(255) DEFAULT 'Hlavní vlákno',
                    user_name VARCHAR(100) DEFAULT 'operator'
                )
            """.trimIndent()
            stmt.execute(createTableSql)

            // Safe idempotent migration if table existed before multi-thread columns
            try {
                stmt.execute("ALTER TABLE omnis_messages ADD COLUMN IF NOT EXISTS thread_id VARCHAR(128) DEFAULT 'thread_main'")
                stmt.execute("ALTER TABLE omnis_messages ADD COLUMN IF NOT EXISTS thread_title VARCHAR(255) DEFAULT 'Hlavní vlákno'")
                stmt.execute("ALTER TABLE omnis_messages ADD COLUMN IF NOT EXISTS user_name VARCHAR(100) DEFAULT 'operator'")
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_omnis_messages_thread_ts ON omnis_messages (thread_id, timestamp)")
            } catch (ignored: Exception) {
                // Table already has columns or engine is in strict mode
            }

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

    /**
     * Dávková synchronizace všech neodbavených zpráv z Room do Cloud SQL (PostgreSQL).
     */
    suspend fun syncPendingRecords(dao: OmnisDao): Result<Int> = withContext(com.example.api.OmnisGeminiClient.ioDispatcher) {
        if (DatabaseConfig.isTesting) {
            val unsynced = dao.getUnsyncedRecords(100)
            if (unsynced.isNotEmpty()) {
                dao.markRecordsAsSynced(unsynced.map { it.id })
            }
            _syncStatus.value = SyncStatus.Success(unsynced.size, System.currentTimeMillis())
            return@withContext Result.success(unsynced.size)
        }

        val unsynced = try {
            dao.getUnsyncedRecords(100)
        } catch (e: Exception) {
            Log.e(TAG, "Chyba při čtení neodbavených záznamů z Room DB", e)
            emptyList()
        }

        if (unsynced.isEmpty()) {
            _syncStatus.value = SyncStatus.Success(0, System.currentTimeMillis())
            return@withContext Result.success(0)
        }

        _syncStatus.value = SyncStatus.Syncing(unsynced.size)
        val conn = getConnection()
        if (conn == null) {
            val reason = "Cloud SQL nedostupný. Data bezpečně uložena v lokální paměti (Room DB)."
            _syncStatus.value = SyncStatus.Offline(reason, unsynced.size)
            return@withContext Result.failure(IllegalStateException(reason))
        }

        try {
            val insertSql = """
                INSERT INTO omnis_messages (
                    id, role, content, cognitive_process, follow_up_questions,
                    val_sys, val_econ, val_psych, val_eco, val_law, val_sec, val_phys, val_soc,
                    composite_score, domain, attached_image_path, timestamp, defense_tier, defense_notes,
                    thread_id, thread_title, user_name
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                    defense_notes = EXCLUDED.defense_notes,
                    thread_id = EXCLUDED.thread_id,
                    thread_title = EXCLUDED.thread_title,
                    user_name = EXCLUDED.user_name
            """.trimIndent()

            val pstmt: PreparedStatement = conn.prepareStatement(insertSql)
            for (record in unsynced) {
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
                pstmt.setString(20, record.threadId)
                pstmt.setString(21, record.threadTitle)
                pstmt.setString(22, record.userName)
                pstmt.addBatch()
            }

            pstmt.executeBatch()
            pstmt.close()

            val syncedIds = unsynced.map { it.id }
            dao.markRecordsAsSynced(syncedIds)

            _syncStatus.value = SyncStatus.Success(syncedIds.size, System.currentTimeMillis())
            Log.i(TAG, "Úspěšně dávkově synchronizováno ${syncedIds.size} záznamů do Google Cloud SQL.")
            Result.success(syncedIds.size)
        } catch (e: Exception) {
            val errorMsg = "Chyba dávkové synchronizace do Cloud SQL: ${e.message}"
            Log.w(TAG, errorMsg, e)
            _syncStatus.value = SyncStatus.Offline(errorMsg, unsynced.size)
            Result.failure(e)
        } finally {
            try {
                conn.close()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Asynchronní zápis nového záznamu s automatickou Room aktualizací při úspěchu.
     */
    fun syncRecordAsync(record: OmnisRecord, dao: OmnisDao? = null) {
        syncScope.launch {
            var attempt = 0
            var success = false

            while (attempt < MAX_RETRY_ATTEMPTS && !success) {
                attempt++
                val conn = getConnection()
                if (conn == null) {
                    Log.w(TAG, "Cloud SQL connection offline on attempt $attempt. Retrying with backoff...")
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        val backoffMs = (200L * (1L shl (attempt - 1))) + Random.nextLong(50, 150)
                        delay(backoffMs)
                    }
                    continue
                }

                try {
                    val insertSql = """
                        INSERT INTO omnis_messages (
                            id, role, content, cognitive_process, follow_up_questions,
                            val_sys, val_econ, val_psych, val_eco, val_law, val_sec, val_phys, val_soc,
                            composite_score, domain, attached_image_path, timestamp, defense_tier, defense_notes,
                            thread_id, thread_title, user_name
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                            defense_notes = EXCLUDED.defense_notes,
                            thread_id = EXCLUDED.thread_id,
                            thread_title = EXCLUDED.thread_title,
                            user_name = EXCLUDED.user_name
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
                    pstmt.setString(20, record.threadId)
                    pstmt.setString(21, record.threadTitle)
                    pstmt.setString(22, record.userName)

                    pstmt.executeUpdate()
                    pstmt.close()
                    Log.i(TAG, "Successfully synced record #${record.id} to Google Cloud SQL (attempt $attempt).")
                    success = true
                    
                    // Označit jako synchronizováno v lokální Room DB
                    dao?.markRecordsAsSynced(listOf(record.id))
                    _syncStatus.value = SyncStatus.Success(1, System.currentTimeMillis())
                } catch (e: java.sql.SQLException) {
                    Log.w(TAG, "SQLException during record sync to Google Cloud SQL (attempt $attempt): ${e.message}")
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        val backoffMs = (250L * (1L shl (attempt - 1))) + Random.nextLong(50, 150)
                        delay(backoffMs)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync record #${record.id} to Google Cloud SQL (attempt $attempt): ${e.message}")
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        val backoffMs = (250L * (1L shl (attempt - 1))) + Random.nextLong(50, 150)
                        delay(backoffMs)
                    }
                } finally {
                    try {
                        conn.close()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }

            if (!success) {
                Log.w(TAG, "Record #${record.id} reached max retry attempts. Safely persisted in local Room database.")
            }
        }
    }
}
