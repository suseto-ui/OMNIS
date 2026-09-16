package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.Locale

class SpeechController(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    fun initialize() {
        if (tts == null) {
            tts = TextToSpeech(context, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("cs", "CZ"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(context, "Český jazyk pro TTS není dostupný.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Inicializace TTS selhala.", Toast.LENGTH_SHORT).show()
        }
    }

    fun speak(text: String) {
        if (text.length >= TextToSpeech.getMaxSpeechInputLength()) {
            val chunks = text.chunked(TextToSpeech.getMaxSpeechInputLength() - 1)
            chunks.forEachIndexed { index, chunk ->
                tts?.speak(
                    chunk,
                    if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                    null,
                    null
                )
            }
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
