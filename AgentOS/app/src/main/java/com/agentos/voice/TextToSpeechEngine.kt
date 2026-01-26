package com.agentos.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Text-to-Speech engine for voice feedback.
 */
class TextToSpeechEngine(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val pendingUtterances = mutableListOf<String>()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val result = engine.setLanguage(Locale.US)
                    isInitialized = result != TextToSpeech.LANG_MISSING_DATA &&
                            result != TextToSpeech.LANG_NOT_SUPPORTED

                    engine.setSpeechRate(1.1f) // Slightly faster
                    engine.setPitch(1.0f)

                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                            // Speak next pending utterance if any
                            if (pendingUtterances.isNotEmpty()) {
                                val next = pendingUtterances.removeAt(0)
                                speakInternal(next)
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _isSpeaking.value = false
                        }
                    })

                    // Speak any pending text
                    if (pendingUtterances.isNotEmpty()) {
                        val first = pendingUtterances.removeAt(0)
                        speakInternal(first)
                    }
                }
            }
        }
    }

    /**
     * Speak the given text. If already speaking, queues it.
     */
    fun speak(text: String) {
        if (!isInitialized) {
            pendingUtterances.add(text)
            return
        }

        if (_isSpeaking.value) {
            pendingUtterances.add(text)
        } else {
            speakInternal(text)
        }
    }

    /**
     * Speak immediately, interrupting any current speech.
     */
    fun speakImmediately(text: String) {
        pendingUtterances.clear()
        stop()
        if (isInitialized) {
            speakInternal(text)
        } else {
            pendingUtterances.add(text)
        }
    }

    private fun speakInternal(text: String) {
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Stop speaking.
     */
    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    /**
     * Release resources.
     */
    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
