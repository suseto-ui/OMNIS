package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entita pro ukládání vygenerovaných artefaktů (kód, konfigurace, reporty)
 */
@Entity(tableName = "omnis_artifacts")
data class OmnisArtifact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String, // CODE, CONFIG, REPORT, SCRIPT
    val language: String, // kotlin, python, json, markdown
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceRecordId: Long? = null,
    val metadata: String = "{}" // JSON metadata
)
