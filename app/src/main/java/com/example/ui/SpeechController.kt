package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class SttState {
    object Idle : SttState()
    object Listening : SttState()
    object Processing : SttState()
    data class Error(val message: String) : SttState()
    data class Result(val text: String) : SttState()
}

/**
 * Správce hlasového výstupu (Text-To-Speech) a hlasového vstupu (Speech-To-Text / Diktování) pro O.M.N.I.S.
 * Zajišťuje automatické očištění Markdown syntaxe (#, **, *, _, `, křížky, hvězdičky)
 * před předčítáním výstupů a převod řeči na text pro nadiktování zpráv.
 */
class SpeechController(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val _sttState = MutableStateFlow<SttState>(SttState.Idle)
    val sttState: StateFlow<SttState> = _sttState.asStateFlow()

    fun initialize() {
        if (tts == null) {
            tts = TextToSpeech(context, this)
        }
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            } catch (e: Exception) {
                Log.w("SpeechController", "SpeechRecognizer creation failed: ${e.message}")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.forLanguageTag("cs-CZ"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(context, "Český jazyk pro TTS není dostupný.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Inicializace TTS selhala.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Spustí diktování řeči na text (STT) pomoci Android SpeechRecognizer.
     */
    fun startListening(
        languageCode: String = "cs-CZ",
        onFinalResult: (String) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val errMsg = "Hlasové diktování není na tomto zařízení podporováno."
            _sttState.value = SttState.Error(errMsg)
            onError?.invoke(errMsg)
            Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _sttState.value = SttState.Listening
                }

                override fun onBeginningOfSpeech() {
                    _sttState.value = SttState.Listening
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _sttState.value = SttState.Processing
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Chyba nahrávání zvuku."
                        SpeechRecognizer.ERROR_CLIENT -> "Klientská chyba diktování."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Chybí oprávnění pro mikrofon."
                        SpeechRecognizer.ERROR_NETWORK -> "Síťová chyba při převodu řeči."
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Vypršel časový limit sítě."
                        SpeechRecognizer.ERROR_NO_MATCH -> "Řeč nebyla rozpoznána."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Rozpoznávač je řepracován."
                        SpeechRecognizer.ERROR_SERVER -> "Serverová chyba rozpoznání."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nebyla detekována žádná řeč."
                        else -> "Neznámá chyba rozpoznání ($error)."
                    }
                    _sttState.value = SttState.Error(message)
                    onError?.invoke(message)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        _sttState.value = SttState.Result(text)
                        onFinalResult(text)
                    } else {
                        _sttState.value = SttState.Idle
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        _sttState.value = SttState.Result(text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            stop() // Zastavíme probíhající TTS před začátkem odposlechu
            speechRecognizer?.startListening(intent)
            _sttState.value = SttState.Listening
        } catch (e: Exception) {
            val err = "Chyba spuštění diktování: ${e.message}"
            _sttState.value = SttState.Error(err)
            onError?.invoke(err)
        }
    }

    /**
     * Zastaví odposlech řeči.
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _sttState.value = SttState.Idle
    }

    /**
     * Očištění Markdownu a speciálních značek před předčítáním (TTS).
     * Odstraňuje nadpisy (#), tučné písmo (**), kurzívu (*), odrážky, kódové bloky, křížky, hvězdičky atd.,
     * tak aby syntéza řeči nepředčítala "hvězdička hvězdička nadpis křížek křížek".
     */
    fun cleanTextForSpeech(rawText: String): String {
        if (rawText.isBlank()) return ""

        var cleaned = rawText

        // 1. Odstranění kódových bloků (```code```) -> ponecháme text uvnitř
        cleaned = cleaned.replace(Regex("```[a-zA-Z]*\\n?([\\s\\S]*?)```")) { match ->
            match.groupValues.getOrNull(1)?.trim() ?: ""
        }

        // 2. Odstranění inline kódu (`code`)
        cleaned = cleaned.replace(Regex("`([^`]+)`"), "$1")

        // 3. Odkazy: [text](url) -> pouze "text"
        cleaned = cleaned.replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")

        // 4. Obrázky: ![alt](url) -> ""
        cleaned = cleaned.replace(Regex("!\\[([^\\]]*)\\]\\([^\\)]+\\)"), "")

        // 5. Nadpisy (#, ##, ###, atd.) na začátku řádků
        cleaned = cleaned.replace(Regex("(?m)^#{1,6}\\s+"), "")

        // 6. Tučný text a kurzíva (**text**, *text*, __text__, _text_)
        cleaned = cleaned.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        cleaned = cleaned.replace(Regex("\\*([^*]+)\\*"), "$1")
        cleaned = cleaned.replace(Regex("__([^_]+)__"), "$1")
        cleaned = cleaned.replace(Regex("_([^_]+)_"), "$1")

        // 7. Odrážky seznamu (* , - , + )
        cleaned = cleaned.replace(Regex("(?m)^\\s*[*+\\-]\\s+"), "")

        // 8. Citace (> )
        cleaned = cleaned.replace(Regex("(?m)^\\s*>\\s*"), "")

        // 9. Horizontální oddělovače (---, ***, ___)
        cleaned = cleaned.replace(Regex("(?m)^[-*_]{3,}\\s*$"), "")

        // 10. Odstranění zbylých markdown značek a symbolů (*, #, _, `, ~, |)
        cleaned = cleaned.replace(Regex("[*#_`~|]"), "")

        // 11. Plynulejší čtení ampersandu a zkratek
        cleaned = cleaned.replace(" & ", " a ")

        // 12. Normalizace prázdných řádků na větnou pauzu (tečku) a ořez mezer
        cleaned = cleaned.replace(Regex("\\n{2,}"), ". ")
        cleaned = cleaned.replace(Regex("\\n"), " ")
        cleaned = cleaned.replace(Regex("\\s{2,}"), " ").trim()

        return cleaned
    }

    fun speak(text: String) {
        val cleanText = cleanTextForSpeech(text)
        if (cleanText.isBlank()) return

        stop()

        // Aplikovat uživatelské nastavení rychlosti, výšky a hlasu
        try {
            val rate = com.example.ui.speech.SpeechSettingsManager.speechRate.value
            val pitch = com.example.ui.speech.SpeechSettingsManager.speechPitch.value
            val gender = com.example.ui.speech.SpeechSettingsManager.voiceGender.value

            tts?.setSpeechRate(rate)
            tts?.setPitch(pitch)

            if (gender != com.example.ui.speech.VoiceGender.AUTO && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val availableVoices = tts?.voices ?: emptySet()
                val targetNameKeywords = if (gender == com.example.ui.speech.VoiceGender.MALE) listOf("male", "man", "cz-m", "cs-cz-language", "m01", "m02") else listOf("female", "woman", "cz-f", "cs-cz-language", "f01", "f02")
                val matchedVoice = availableVoices.firstOrNull { voice ->
                    val nameLower = voice.name.lowercase()
                    targetNameKeywords.any { kw -> nameLower.contains(kw) }
                }
                if (matchedVoice != null) {
                    tts?.voice = matchedVoice
                }
            }
        } catch (_: Exception) {
            // Bezpečná záloha při nekompatibilitě TTS engine
        }

        if (cleanText.length >= TextToSpeech.getMaxSpeechInputLength()) {
            val chunks = cleanText.chunked(TextToSpeech.getMaxSpeechInputLength() - 1)
            chunks.forEachIndexed { index, chunk ->
                tts?.speak(
                    chunk,
                    if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                    null,
                    null
                )
            }
        } else {
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {
            // Safe cleanup ignore
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
            // Safe cleanup ignore
        } finally {
            tts = null
            speechRecognizer = null
        }
    }
}

