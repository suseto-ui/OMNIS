package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Entita reprezentující záznam o interakci nebo aktivitě uživatele v aplikaci O.M.N.I.S.
 */
@Serializable
@Entity(tableName = "user_activity_logs")
data class UserActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val action: String,
    val details: String,
    val severity: String = "INFO",
    val timestamp: Long = System.currentTimeMillis()
)
