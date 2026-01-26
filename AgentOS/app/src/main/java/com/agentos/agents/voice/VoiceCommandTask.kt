package com.agentos.agents.voice

import com.agentos.core.base.Task
import com.agentos.core.base.TaskType
import com.agentos.core.base.Priority
import com.agentos.core.base.ValidationResult

/**
 * Voice command task for processing audio input and extracting intent
 */
data class VoiceCommandTask(
    override val taskId: String,
    override val taskType: TaskType = TaskType.VOICE_COMMAND,
    override val priority: Priority = Priority.CRITICAL,
    override val parameters: Map<String, Any>,
    override val timeoutMs: Long = 5000,
    override val retryCount: Int = 2
) : Task {
    
    /**
     * Audio data as ByteArray
     */
    val audioData: ByteArray by lazy {
        parameters["audioData"] as? ByteArray 
            ?: throw IllegalArgumentException("audioData parameter required")
    }
    
    /**
     * Expected command type for validation
     */
    val expectedCommandType: String? by lazy {
        parameters["expectedCommandType"] as? String
    }
    
    /**
     * Language for speech recognition
     */
    val language: String by lazy {
        parameters["language"] as? String ?: "en-US"
    }
    
    /**
     * Sample rate of audio data
     */
    val sampleRate: Int by lazy {
        parameters["sampleRate"] as? Int ?: 16000
    }
    
    /**
     * Background noise level (0.0 to 1.0)
     */
    val noiseLevel: Float by lazy {
        parameters["noiseLevel"] as? Float ?: 0.0f
    }
    
    override fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Validate audio data
        if (!parameters.containsKey("audioData")) {
            errors.add("audioData parameter is required")
        } else {
            val audioData = parameters["audioData"] as? ByteArray
            when {
                audioData == null -> errors.add("audioData must be a ByteArray")
                audioData.isEmpty() -> errors.add("audioData cannot be empty")
                audioData.size < 1024 -> warnings.add("audioData seems very short (< 1KB)")
                audioData.size > 1024 * 1024 -> warnings.add("audioData is very large (> 1MB)")
            }
        }
        
        // Validate sample rate
        if (sampleRate < 8000 || sampleRate > 48000) {
            errors.add("sampleRate must be between 8000 and 48000 Hz")
        }
        
        // Validate language
        if (!isSupportedLanguage(language)) {
            errors.add("Unsupported language: $language")
        }
        
        // Validate noise level
        if (noiseLevel < 0.0f || noiseLevel > 1.0f) {
            errors.add("noiseLevel must be between 0.0 and 1.0")
        }
        
        // Validate timeout
        if (timeoutMs < 1000 || timeoutMs > 30000) {
            errors.add("timeoutMs must be between 1000 and 30000 ms")
        }
        
        // Validate retry count
        if (retryCount < 0 || retryCount > 5) {
            errors.add("retryCount must be between 0 and 5")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    private fun isSupportedLanguage(language: String): Boolean {
        val supportedLanguages = setOf(
            "en-US", "en-GB", "en-AU", "en-CA",
            "es-ES", "es-MX", "es-US",
            "fr-FR", "fr-CA",
            "de-DE",
            "it-IT",
            "pt-BR", "pt-PT",
            "ja-JP",
            "ko-KR",
            "zh-CN", "zh-HK", "zh-TW",
            "hi-IN",
            "ar-SA",
            "ru-RU"
        )
        return supportedLanguages.contains(language)
    }
    
    companion object {
        /**
         * Create a voice command task from audio data
         */
        fun fromAudioData(
            taskId: String,
            audioData: ByteArray,
            language: String = "en-US",
            sampleRate: Int = 16000,
            noiseLevel: Float = 0.0f,
            expectedCommandType: String? = null
        ): VoiceCommandTask {
            val params = mutableMapOf<String, Any>(
                "audioData" to audioData,
                "language" to language,
                "sampleRate" to sampleRate,
                "noiseLevel" to noiseLevel
            )
            expectedCommandType?.let { params["expectedCommandType"] = it }
            return VoiceCommandTask(
                taskId = taskId,
                parameters = params
            )
        }
        
        /**
         * Create a voice command task for testing
         */
        fun createTestTask(
            taskId: String = "test_voice_task",
            audioData: ByteArray = "test audio data".toByteArray(),
            language: String = "en-US"
        ): VoiceCommandTask {
            return fromAudioData(taskId, audioData, language)
        }
    }
}