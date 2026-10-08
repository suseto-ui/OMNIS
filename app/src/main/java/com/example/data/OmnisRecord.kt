package com.example.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "omnis_messages",
    indices = [
        Index(value = ["threadId", "timestamp"]),
        Index(value = ["userName", "timestamp"]),
        Index(value = ["isSyncedToPostgres"])
    ]
)
data class OmnisRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val role: String, // "user" or "assistant"
    val content: String,
    val cognitiveProcess: String = "",
    val followUpQuestions: String = "",
    val valSys: Float = 0.5f,
    val valEcon: Float = 0.5f,
    val valPsych: Float = 0.5f,
    val valEco: Float = 0.5f,
    val valLaw: Float = 0.5f,
    val valSec: Float = 0.5f,
    val valPhys: Float = 0.5f,
    val valSoc: Float = 0.5f,
    val compositeScore: Float = 0.5f,
    val domain: String = "SYSTEMS_INTELLIGENCE",
    val attachedImagePath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val defenseTier: String = "APPROVED",
    val defenseNotes: String = "",
    val isSyncedToPostgres: Boolean = false,
    val threadId: String = "thread_main",
    val threadTitle: String = "Hlavní vlákno",
    val userName: String = "operator"
)

fun OmnisRecord.to8DVector(): Map<String, Float> = mapOf(
    "Sys" to valSys,
    "Econ" to valEcon,
    "Psych" to valPsych,
    "Eco" to valEco,
    "Law" to valLaw,
    "Sec" to valSec,
    "Phys" to valPhys,
    "Soc" to valSoc
)

val OmnisRecord.adversarialScore: Float
    get() {
        val secRisk = (1.0f - valSec.coerceIn(0f, 1f)).coerceIn(0f, 1f)
        return when (defenseTier) {
            "BLOCKED" -> maxOf(0.85f, secRisk)
            "WARNING" -> maxOf(0.60f, secRisk)
            else -> secRisk
        }
    }

