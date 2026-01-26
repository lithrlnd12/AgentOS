package com.agentos.voice

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Audio recorder that captures PCM16 audio at 24kHz mono.
 * This format is required by OpenAI Realtime API.
 */
class AudioRecorder {

    companion object {
        const val SAMPLE_RATE = 24000 // 24kHz required by OpenAI
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val CHUNK_DURATION_MS = 100 // Send audio every 100ms
        val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val CHUNK_SIZE = (SAMPLE_RATE * 2 * CHUNK_DURATION_MS) / 1000 // bytes per chunk
    }

    private var audioRecord: AudioRecord? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    /**
     * Start recording and emit audio chunks.
     * Audio is PCM16, 24kHz, mono - ready for OpenAI Realtime API.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(): Flow<ByteArray> = flow {
        val bufferSize = maxOf(BUFFER_SIZE, CHUNK_SIZE)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord failed to initialize")
        }

        audioRecord?.startRecording()
        _isRecording.value = true

        val buffer = ByteArray(CHUNK_SIZE)

        try {
            while (coroutineContext.isActive && _isRecording.value) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (bytesRead > 0) {
                    // Emit a copy of the buffer
                    emit(buffer.copyOf(bytesRead))
                }
            }
        } finally {
            stopRecording()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Stop recording.
     */
    fun stopRecording() {
        _isRecording.value = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
