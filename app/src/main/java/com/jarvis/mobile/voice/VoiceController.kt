package com.jarvis.mobile.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceController(
    context: Context,
    private val onText: (String) -> Unit,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var speechReady = false
    private var pendingSpeech: Triple<String, Float, Float>? = null

    init {
        textToSpeech = TextToSpeech(appContext) { status ->
            speechReady = status == TextToSpeech.SUCCESS
            if (speechReady) {
                val result = textToSpeech?.setLanguage(Locale("pt", "BR"))
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    speechReady = false
                } else {
                    pendingSpeech?.let { (text, speed, volume) -> speakNow(text, speed, volume) }
                }
            }
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onError("O reconhecimento de voz não está disponível neste aparelho.")
            return
        }
        stopSpeaking()
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = onListeningChanged(true)
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = onListeningChanged(false)
                    override fun onError(error: Int) {
                        onListeningChanged(false)
                        if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                            error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        ) {
                            this@VoiceController.onError(recognitionError(error))
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        onListeningChanged(false)
                        val spokenText = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                        if (!spokenText.isNullOrBlank()) onText(spokenText)
                    }

                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }
                startListening(intent)
            }
        } catch (error: Exception) {
            onListeningChanged(false)
            onError(error.message ?: "Não foi possível iniciar o microfone.")
        }
    }

    fun speak(text: String, speed: Float, volume: Float) {
        if (text.isBlank()) return
        if (!speechReady) {
            pendingSpeech = Triple(text, speed, volume)
            return
        }
        speakNow(text, speed, volume)
    }

    private fun speakNow(text: String, speed: Float, volume: Float) {
        pendingSpeech = null
        textToSpeech?.apply {
            setLanguage(Locale("pt", "BR"))
            setSpeechRate(speed.coerceIn(0.7f, 1.4f))
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
            }
            speak(text, TextToSpeech.QUEUE_FLUSH, params, "jarvis-${System.nanoTime()}")
        }
    }

    fun cancelListening() {
        recognizer?.cancel()
        onListeningChanged(false)
    }

    fun stopSpeaking() {
        pendingSpeech = null
        textToSpeech?.stop()
    }

    fun release() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    private fun recognitionError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "O microfone não conseguiu capturar o áudio."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permita o acesso ao microfone nas configurações do Android."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        -> "O serviço de reconhecimento de voz está sem conexão."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "O reconhecimento de voz já está em uso."
        SpeechRecognizer.ERROR_SERVER -> "O serviço de reconhecimento de voz encontrou um erro."
        else -> "Não foi possível reconhecer a fala. Tente novamente."
    }
}
