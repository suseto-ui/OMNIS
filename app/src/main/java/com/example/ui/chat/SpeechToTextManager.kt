package com.example.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Správce rozpoznávání řeči (Speech-to-Text) pro O.M.N.I.S. ChatView.
 * Zajišťuje převod hlasového vstupu do textového dotazu chatboxu v české lokalizaci.
 */
class SpeechToTextManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onListeningStateChange: (Boolean) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        if (!isAvailable()) {
            onError("Hlasové rozpoznávání není na tomto zařízení k dispozici.")
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        onListeningStateChange(true)
                    }

                    override fun onBeginningOfSpeech() {
                        onListeningStateChange(true)
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        onListeningStateChange(false)
                    }

                    override fun onError(error: Int) {
                        onListeningStateChange(false)
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Chyba nahrávání zvuku"
                            SpeechRecognizer.ERROR_CLIENT -> "Klientská chyba hlasového vstupu"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Chybí oprávnění k mikrofonu"
                            SpeechRecognizer.ERROR_NETWORK -> "Síťová chyba při rozpoznávání"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Vypršel časový limit sítě"
                            SpeechRecognizer.ERROR_NO_MATCH -> "Nebyla rozpoznána žádná řeč, zkuste to znovu"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Služba rozpoznávání je vyťažena"
                            SpeechRecognizer.ERROR_SERVER -> "Chyba serveru rozpoznávání"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Žádný hlasový vstup nebyl zachycen"
                            else -> "Chyba rozpoznávání hlasu ($error)"
                        }
                        Log.w("SpeechToTextManager", errorMsg)
                        onError(errorMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        onListeningStateChange(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()
                        if (!spokenText.isNullOrBlank()) {
                            onResult(spokenText)
                        } else {
                            onError("Nebyl zachycen žádný text.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()
                        if (!spokenText.isNullOrBlank()) {
                            onResult(spokenText)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "cs-CZ")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "cs-CZ")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Mluvte do mikrofonu (O.M.N.I.S.)...")
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onListeningStateChange(false)
            onError("Chyba při spuštění rozpoznávače: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("SpeechToTextManager", "Error stopping speech recognizer", e)
        }
        speechRecognizer = null
        onListeningStateChange(false)
    }
}
