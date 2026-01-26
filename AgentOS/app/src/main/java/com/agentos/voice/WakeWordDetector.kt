package com.agentos.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

/**
 * Detects the wake word "AgentOS" and extracts the command that follows.
 *
 * Usage patterns:
 * - "AgentOS open settings"
 * - "AgentOS, find nearby coffee shops"
 * - "Hey AgentOS, send a text to Mom"
 */
class WakeWordDetector(private val context: Context) {

    companion object {
        private val WAKE_WORDS = listOf(
            "agent os",
            "agentos",
            "agent o s",
            "hey agent os",
            "hey agentos",
            "ok agent os",
            "ok agentos"
        )
    }

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    /**
     * Start continuous listening for the wake word.
     * Returns a flow of detected commands (text after the wake word).
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startListening(): Flow<WakeWordResult> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(WakeWordResult.Error("Speech recognition not available"))
            close()
            return@callbackFlow
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    trySend(WakeWordResult.Listening)
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    trySend(WakeWordResult.AudioLevel(rmsdB))
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        else -> "Recognition error: $error"
                    }

                    // For no match/timeout, restart listening
                    if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        restartListening()
                    } else {
                        trySend(WakeWordResult.Error(errorMessage))
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                    matches?.firstOrNull()?.let { spokenText ->
                        val command = extractCommand(spokenText.lowercase(Locale.getDefault()))
                        if (command != null) {
                            trySend(WakeWordResult.CommandDetected(command, spokenText))
                        } else {
                            // No wake word detected, restart listening
                            trySend(WakeWordResult.NoWakeWord(spokenText))
                        }
                    }

                    // Continue listening
                    restartListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { partial ->
                        trySend(WakeWordResult.PartialResult(partial))
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        startRecognition()

        awaitClose {
            stopListening()
        }
    }

    private fun startRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun restartListening() {
        speechRecognizer?.cancel()
        startRecognition()
    }

    fun stopListening() {
        _isListening.value = false
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    /**
     * Extract the command from spoken text if it contains a wake word.
     * Returns null if no wake word is detected.
     */
    private fun extractCommand(spokenText: String): String? {
        for (wakeWord in WAKE_WORDS) {
            val index = spokenText.indexOf(wakeWord)
            if (index != -1) {
                // Extract everything after the wake word
                val afterWakeWord = spokenText.substring(index + wakeWord.length).trim()

                // Remove common filler words that might follow
                val command = afterWakeWord
                    .removePrefix(",")
                    .removePrefix("please")
                    .removePrefix("can you")
                    .removePrefix("could you")
                    .trim()

                return if (command.isNotEmpty()) command else null
            }
        }
        return null
    }
}

sealed class WakeWordResult {
    object Listening : WakeWordResult()
    data class AudioLevel(val level: Float) : WakeWordResult()
    data class PartialResult(val text: String) : WakeWordResult()
    data class CommandDetected(val command: String, val fullText: String) : WakeWordResult()
    data class NoWakeWord(val spokenText: String) : WakeWordResult()
    data class Error(val message: String) : WakeWordResult()
}
