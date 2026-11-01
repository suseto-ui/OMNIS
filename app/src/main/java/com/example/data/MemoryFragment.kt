package com.example.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entita pro uložení zkonsolidované paměti (Context Fragments)
 */
@Immutable
@Entity(
    tableName = "omnis_memory_fragments",
    indices = [
        Index(value = ["timestamp"])
    ]
)
data class MemoryFragment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceRecordCount: Int,
    val tags: String = "" // Čárkou oddělené štítky
)
