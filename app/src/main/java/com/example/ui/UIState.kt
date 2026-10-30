package com.example.ui

sealed interface UIState<out T> {
    object Idle : UIState<Nothing>
    object Loading : UIState<Nothing>
    data class Success<out T>(val data: T) : UIState<T>
    data class Error(val message: String, val throwable: Throwable? = null) : UIState<Nothing>
}

data class LlmErrorAnalysis(
    val component: String = "OmnisCore",
    val message: String = "",
    val analysisText: String = "",
    val recommendedSteps: List<String> = emptyList()
)
