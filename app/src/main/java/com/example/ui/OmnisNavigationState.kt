package com.example.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OmnisTab {
    CHAT,
    NEXUS,
    DASHBOARD,
    GUIDE,
    GOALS,
    SCENARIOS,
    ARTIFACTS,
    MEMORY,
    ANALYTICS,
    ADMIN,
    NODES,
    MATRIX,
    TELEMETRY,
    TEST_SEMANTIC,
    DEV_PROMPT_LAB,
    CAUSAL_SIMULATOR,
    DIALECTICS,
    PRODUCTION_AUDIT,
    SETTINGS
}

class OmnisNavigationState {
    private val _showDeleteConfirm = MutableStateFlow(false)
    val showDeleteConfirm: StateFlow<Boolean> = _showDeleteConfirm.asStateFlow()

    fun setShowDeleteConfirm(show: Boolean) { 
        _showDeleteConfirm.value = show 
    }
}
