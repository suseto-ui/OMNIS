package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Reprezentuje autonomní cíl (Goal) systému O.M.N.I.S.
 */
@Entity(tableName = "omnis_goals")
data class OmnisGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val status: String, // ACTIVE, COMPLETED, FAILED, PAUSED
    val priority: Int, // 1-5
    val progress: Float, // 0.0 - 1.0
    val tasksJson: String, // Serializovaný seznam OmnisTask
    val timestamp: Long = System.currentTimeMillis(),
    val deadline: Long? = null
)

@Serializable
data class OmnisTask(
    val id: String,
    val title: String,
    val description: String,
    val status: String, // PENDING, IN_PROGRESS, DONE
    val assignedAgentId: String,
    val domain: String // Např. SYS, ECON, SEC
)
