package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "omnis_messages",
    indices = [
        Index(value = ["threadId", "timestamp"]),
        Index(value = ["userName", "timestamp"])
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
