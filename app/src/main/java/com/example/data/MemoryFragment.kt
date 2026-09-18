package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entita pro uložení zkonsolidované paměti (Context Fragments)
 */
@Entity(tableName = "omnis_memory_fragments")
data class MemoryFragment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceRecordCount: Int,
    val tags: String = "" // Čárkou oddělené štítky
)
