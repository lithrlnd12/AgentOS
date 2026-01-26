package com.agentos.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Low-latency audio player for streaming PCM audio.
 * Used for playing back TTS audio from OpenAI Realtime API.
 */
class AudioPlayer {

    private var audioTrack: AudioTrack? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val audioQueue = Channel<ByteArray>(Channel.UNLIMITED)
    private val isPlaying = AtomicBoolean(false)

    private val _state = MutableStateFlow(PlayerState.IDLE)
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    // Audio format matching OpenAI Realtime API output
    private val sampleRate = 24000 // 24kHz
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    /**
     * Initialize the audio player.
     */
    fun initialize() {
        if (audioTrack != null) return

        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        startPlaybackLoop()
    }

    /**
     * Queue an audio chunk for playback.
     */
    fun playChunk(audioData: ByteArray) {
        if (audioTrack == null) {
            initialize()
        }

        scope.launch {
            audioQueue.send(audioData)
        }

        if (!isPlaying.get()) {
            startPlayback()
        }
    }

    /**
     * Start playback.
     */
    private fun startPlayback() {
        if (isPlaying.compareAndSet(false, true)) {
            audioTrack?.play()
            _state.value = PlayerState.PLAYING
        }
    }

    /**
     * Stop playback and clear queue.
     */
    fun stop() {
        isPlaying.set(false)
        audioTrack?.pause()
        audioTrack?.flush()

        // Clear the queue
        while (audioQueue.tryReceive().isSuccess) {
            // Drain queue
        }

        _state.value = PlayerState.IDLE
    }

    /**
     * Pause playback.
     */
    fun pause() {
        if (isPlaying.compareAndSet(true, false)) {
            audioTrack?.pause()
            _state.value = PlayerState.PAUSED
        }
    }

    /**
     * Resume playback.
     */
    fun resume() {
        if (isPlaying.compareAndSet(false, true)) {
            audioTrack?.play()
            _state.value = PlayerState.PLAYING
        }
    }

    /**
     * Background loop that writes audio data to the track.
     */
    private fun startPlaybackLoop() {
        scope.launch {
            for (chunk in audioQueue) {
                if (!isPlaying.get()) {
                    // Wait until playback is resumed
                    continue
                }

                audioTrack?.let { track ->
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.write(chunk, 0, chunk.size)
                    }
                }
            }
        }
    }

    /**
     * Check if currently playing.
     */
    fun isPlaying(): Boolean = isPlaying.get()

    /**
     * Release resources.
     */
    fun release() {
        stop()
        audioTrack?.release()
        audioTrack = null
        scope.cancel()
    }

    /**
     * Get current playback position in milliseconds.
     */
    fun getPlaybackPositionMs(): Long {
        val track = audioTrack ?: return 0
        val framePosition = track.playbackHeadPosition
        return (framePosition * 1000L) / sampleRate
    }
}

/**
 * Playback states.
 */
enum class PlayerState {
    IDLE,
    PLAYING,
    PAUSED,
    BUFFERING
}
