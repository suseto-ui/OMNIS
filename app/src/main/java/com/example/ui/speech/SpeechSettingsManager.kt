package com.example.ui.speech

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VoiceGender(val labelCs: String, val labelEn: String) {
    AUTO("Automatický", "Auto"),
    MALE("Mužský", "Male"),
    FEMALE("Ženský", "Female")
}

/**
 * Centrální správce uživatelského nastavení hlasového výstupu (TTS).
 * Ukládá nastavení do SharedPreferences a reaguje v reálném čase.
 */
object SpeechSettingsManager {
    private const val PREFS_NAME = "omnis_speech_prefs"
    private const val KEY_SPEECH_RATE = "speech_rate"
    private const val KEY_SPEECH_PITCH = "speech_pitch"
    private const val KEY_VOICE_GENDER = "voice_gender"

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.0f)
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _voiceGender = MutableStateFlow(VoiceGender.AUTO)
    val voiceGender: StateFlow<VoiceGender> = _voiceGender.asStateFlow()

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _speechRate.value = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        _speechPitch.value = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)
        val genderStr = prefs.getString(KEY_VOICE_GENDER, VoiceGender.AUTO.name) ?: VoiceGender.AUTO.name
        _voiceGender.value = try { VoiceGender.valueOf(genderStr) } catch (_: Exception) { VoiceGender.AUTO }
        isInitialized = true
    }

    fun setSpeechRate(context: Context?, rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        _speechRate.value = clamped
        context?.let { ctx ->
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putFloat(KEY_SPEECH_RATE, clamped).apply()
        }
    }

    fun setSpeechPitch(context: Context?, pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 1.5f)
        _speechPitch.value = clamped
        context?.let { ctx ->
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putFloat(KEY_SPEECH_PITCH, clamped).apply()
        }
    }

    fun setVoiceGender(context: Context?, gender: VoiceGender) {
        _voiceGender.value = gender
        context?.let { ctx ->
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_VOICE_GENDER, gender.name).apply()
        }
    }
}
