package com.example.ui.chat

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centrální správce uživatelských preferencí pro zobrazení konverzace a výstupů v chatu.
 * Umožňuje uživateli v Nastavení zvolit čistý nerušený režim nebo aktivovat
 * doplňkové kognitivní a právní prvky.
 */
object ChatDisplayPreferences {

    private const val PREFS_NAME = "omnis_chat_display_prefs"
    private const val KEY_LEGAL_BAR = "show_legal_bar"
    private const val KEY_REASONING = "show_reasoning"
    private const val KEY_AUDIT_MATRIX = "show_audit_matrix"
    private const val KEY_CONFIDENCE = "show_confidence"
    private const val KEY_NEXT_STEPS = "show_next_steps"
    private const val KEY_CYBER_GLASS = "cyber_glass_enabled"

    private val _showLegalStatusBarAboveInput = MutableStateFlow(true)
    val showLegalStatusBarAboveInput: StateFlow<Boolean> = _showLegalStatusBarAboveInput.asStateFlow()

    private val _showReasoningTrace = MutableStateFlow(false)
    val showReasoningTrace: StateFlow<Boolean> = _showReasoningTrace.asStateFlow()

    private val _showAuditMatrixGrid = MutableStateFlow(false)
    val showAuditMatrixGrid: StateFlow<Boolean> = _showAuditMatrixGrid.asStateFlow()

    private val _showConfidencePills = MutableStateFlow(false)
    val showConfidencePills: StateFlow<Boolean> = _showConfidencePills.asStateFlow()

    private val _showInteractiveNextSteps = MutableStateFlow(true)
    val showInteractiveNextSteps: StateFlow<Boolean> = _showInteractiveNextSteps.asStateFlow()

    private val _cyberGlassEffectEnabled = MutableStateFlow(true)
    val cyberGlassEffectEnabled: StateFlow<Boolean> = _cyberGlassEffectEnabled.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _showLegalStatusBarAboveInput.value = prefs.getBoolean(KEY_LEGAL_BAR, true)
        _showReasoningTrace.value = prefs.getBoolean(KEY_REASONING, false)
        _showAuditMatrixGrid.value = prefs.getBoolean(KEY_AUDIT_MATRIX, false)
        _showConfidencePills.value = prefs.getBoolean(KEY_CONFIDENCE, false)
        _showInteractiveNextSteps.value = prefs.getBoolean(KEY_NEXT_STEPS, true)
        _cyberGlassEffectEnabled.value = prefs.getBoolean(KEY_CYBER_GLASS, true)
    }

    fun setShowLegalStatusBarAboveInput(show: Boolean, context: Context? = null) {
        _showLegalStatusBarAboveInput.value = show
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_LEGAL_BAR, show)?.apply()
    }

    fun setShowReasoningTrace(show: Boolean, context: Context? = null) {
        _showReasoningTrace.value = show
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_REASONING, show)?.apply()
    }

    fun setShowAuditMatrixGrid(show: Boolean, context: Context? = null) {
        _showAuditMatrixGrid.value = show
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_AUDIT_MATRIX, show)?.apply()
    }

    fun setShowConfidencePills(show: Boolean, context: Context? = null) {
        _showConfidencePills.value = show
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_CONFIDENCE, show)?.apply()
    }

    fun setShowInteractiveNextSteps(show: Boolean, context: Context? = null) {
        _showInteractiveNextSteps.value = show
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_NEXT_STEPS, show)?.apply()
    }

    fun setCyberGlassEffectEnabled(enabled: Boolean, context: Context? = null) {
        _cyberGlassEffectEnabled.value = enabled
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_CYBER_GLASS, enabled)?.apply()
    }
}
