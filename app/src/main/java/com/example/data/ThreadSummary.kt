package com.example.data

import androidx.compose.runtime.Immutable

/**
 * Souhrn konverzačního vlákna pro hierarchické kognitivní načítání a správu relací.
 */
@Immutable
data class ThreadSummary(
    val threadId: String,
    val threadTitle: String,
    val userName: String,
    val lastTimestamp: Long,
    val messageCount: Int,
    val lastContent: String?
)
