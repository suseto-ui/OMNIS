package com.example.ui.localization

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Podporované jazyky v aplikaci O.M.N.I.S.
 */
enum class AppLanguage(val code: String, val label: String, val flag: String) {
    CS("cs", "Čeština", "🇨🇿"),
    EN("en", "English", "🇬🇧")
}

/**
 * Centrální správce jazyka aplikace (Runtime Locale Engine).
 * Umožňuje okamžité reaktivní přepnutí jazyka celého UI bez nutnosti restartu
 * a automaticky ukládá volbu do lokálního úložiště SharedPreferences.
 */
object AppLocaleManager {
    private const val PREFS_NAME = "omnis_locale_prefs"
    private const val KEY_LANGUAGE = "selected_app_language"

    private val _currentLanguage = MutableStateFlow(AppLanguage.CS)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private var isInitialized = false

    /**
     * Inicializuje jazyk z perzistentního úložiště zařízení.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs.getString(KEY_LANGUAGE, AppLanguage.CS.code) ?: AppLanguage.CS.code
        _currentLanguage.value = if (savedCode == AppLanguage.EN.code) AppLanguage.EN else AppLanguage.CS
        isInitialized = true
    }

    /**
     * Nastaví vybraný jazyk a uloží jej pro budoucí spuštění.
     */
    fun setLanguage(context: Context?, language: AppLanguage) {
        _currentLanguage.value = language
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        }
    }

    /**
     * Přepne mezi češtinou a angličtinou (Toggle).
     */
    fun toggleLanguage(context: Context?) {
        val next = if (_currentLanguage.value == AppLanguage.CS) AppLanguage.EN else AppLanguage.CS
        setLanguage(context, next)
    }

    /**
     * Zda je aktivní angličtina.
     */
    val isEnglish: Boolean
        get() = _currentLanguage.value == AppLanguage.EN
}
