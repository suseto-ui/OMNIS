package com.example.ai.transparency

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AiDisclosureManager:
 * Spravuje stav a ověření povinného pop-up upozornění informujícího uživatele před prvním předáním dat,
 * že komunikuje s algoritmickým kognitivním systémem (AI botem), nikoliv lidským operátorem (EU AI Act).
 */
object AiDisclosureManager {

    private const val PREFS_NAME = "omnis_ai_disclosure_prefs"
    private const val KEY_DISCLOSED = "ai_bot_transparency_acknowledged"

    private val _isDisclosureConfirmed = MutableStateFlow(true)
    val isDisclosureConfirmed: StateFlow<Boolean> = _isDisclosureConfirmed.asStateFlow()

    private val _showDisclosureDialog = MutableStateFlow(false)
    val showDisclosureDialog: StateFlow<Boolean> = _showDisclosureDialog.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val confirmed = prefs.getBoolean(KEY_DISCLOSED, false)
        _isDisclosureConfirmed.value = confirmed
    }

    /**
     * Zkontroluje, zda může proběhnout odeslání dat / promptu.
     * Pokud uživatel ještě nepotvrdil disclosure, zablokuje odeslání a otevře pop-up dialog.
     */
    fun checkOrPromptDisclosure(): Boolean {
        if (!_isDisclosureConfirmed.value) {
            _showDisclosureDialog.value = true
            return false
        }
        return true
    }

    /**
     * Potvrdí seznámení se s transparentním hlášením o AI povaze komunikačního kanálu.
     */
    fun confirmDisclosure(context: Context? = null) {
        context?.let {
            val prefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_DISCLOSED, true).apply()
        }
        _isDisclosureConfirmed.value = true
        _showDisclosureDialog.value = false
    }

    fun dismissDialog() {
        _showDisclosureDialog.value = false
    }

    fun resetForTesting() {
        _isDisclosureConfirmed.value = false
        _showDisclosureDialog.value = false
    }
}
