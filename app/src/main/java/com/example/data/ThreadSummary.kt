package com.example.data

/**
 * Souhrn konverzačního vlákna pro hierarchické kognitivní načítání a správu relací.
 */
data class ThreadSummary(
    val threadId: String,
    val threadTitle: String,
    val userName: String,
    val lastTimestamp: Long,
    val messageCount: Int,
    val lastContent: String?
)
