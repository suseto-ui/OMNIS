package com.example.data

data class SemanticAnchor(
    val word: String,
    val weight: Float // 0.0 to 1.0
)

data class OmnisSemanticRecord(
    val id: Long = System.currentTimeMillis(),
    val role: String, // "user" or "assistant"
    val content: String,
    val anchors: Map<String, List<SemanticAnchor>> = emptyMap(), // Domain -> Anchors
    val timestamp: Long = System.currentTimeMillis()
)
