package com.agentos.agents.voice

import com.agentos.core.base.ExecutionResult

/**
 * Interface for voice processing engine that handles OpenAI Advanced Voice API integration
 */
interface VoiceProcessingEngine {
    
    /**
     * Initialize the voice processing engine
     */
    suspend fun initialize(): VoiceInitializationResult
    
    /**
     * Process voice audio data and extract text
     */
    suspend fun processVoiceAudio(audioData: ByteArray, config: VoiceProcessingConfig): VoiceProcessingResult
    
    /**
     * Extract intent from recognized text
     */
    suspend fun extractIntent(text: String, context: VoiceContext): VoiceIntentResult
    
    /**
     * Get current engine status
     */
    suspend fun getStatus(): VoiceEngineStatus
    
    /**
     * Cleanup and shutdown the engine
     */
    suspend fun shutdown(): VoiceShutdownResult
    
    /**
     * Check if engine is healthy and operational
     */
    suspend fun isHealthy(): Boolean
}

/**
 * Voice processing configuration
 */
data class VoiceProcessingConfig(
    val sampleRate: Int = 16000,
    val language: String = "en-US",
    val voice: String = "alloy",
    val speed: Double = 1.0,
    val temperature: Double = 0.3,
    val maxTokens: Int = 1000,
    val noiseReduction: Boolean = true,
    val echoCancellation: Boolean = true,
    val timeoutMs: Long = 5000
) {
    companion object {
        val DEFAULT = VoiceProcessingConfig()
        val FAST = VoiceProcessingConfig(speed = 1.5, timeoutMs = 3000)
        val ACCURATE = VoiceProcessingConfig(temperature = 0.1, noiseReduction = true)
    }
}

/**
 * Voice context for intent extraction
 */
data class VoiceContext(
    val previousCommands: List<String> = emptyList(),
    val currentApp: String? = null,
    val userPreferences: UserVoicePreferences = UserVoicePreferences(),
    val sessionId: String = "default_session"
)

/**
 * User voice preferences
 */
data class UserVoicePreferences(
    val preferredVoice: String = "alloy",
    val speechSpeed: Double = 1.0,
    val noiseTolerance: Float = 0.3f,
    val accentAdaptation: Boolean = true,
    val commandConfirmation: Boolean = true
)

/**
 * Voice initialization result
 */
data class VoiceInitializationResult(
    val success: Boolean,
    val message: String,
    val supportedLanguages: List<String> = emptyList(),
    val availableVoices: List<String> = emptyList()
)

/**
 * Voice processing result
 */
data class VoiceProcessingResult(
    val success: Boolean,
    val recognizedText: String? = null,
    val confidence: Double? = null,
    val language: String? = null,
    val processingTimeMs: Long,
    val error: VoiceError? = null,
    val alternatives: List<String> = emptyList()
)

/**
 * Voice intent result
 */
data class VoiceIntentResult(
    val success: Boolean,
    val intent: AutomationIntent? = null,
    val confidence: Double? = null,
    val parameters: Map<String, Any> = emptyMap(),
    val error: VoiceError? = null,
    val clarificationNeeded: Boolean = false,
    val clarificationPrompt: String? = null
)

/**
 * Automation intent extracted from voice
 */
data class AutomationIntent(
    val type: IntentType,
    val action: String,
    val parameters: Map<String, Any> = emptyMap(),
    val confidence: Double,
    val originalText: String
)

/**
 * Intent type classification
 */
enum class IntentType {
    APP_NAVIGATION,
    SYSTEM_CONTROL,
    CALENDAR_EVENT,
    COMMUNICATION,
    E_COMMERCE,
    INFORMATION_QUERY,
    MULTI_STEP_WORKFLOW,
    UNKNOWN
}

/**
 * Voice engine status
 */
data class VoiceEngineStatus(
    val isInitialized: Boolean,
    val isProcessing: Boolean,
    val lastActivity: Long,
    val processedCommands: Long,
    val successRate: Double,
    val currentModel: String,
    val supportedLanguages: List<String>
)

/**
 * Voice shutdown result
 */
data class VoiceShutdownResult(
    val success: Boolean,
    val message: String,
    val cleanupTimeMs: Long
)

/**
 * Voice processing error
 */
data class VoiceError(
    val errorCode: String,
    val message: String,
    val severity: VoiceErrorSeverity,
    val retryable: Boolean = true
)

/**
 * Voice error severity
 */
enum class VoiceErrorSeverity {
    LOW,      // Minor issues, can continue
    MEDIUM,   // Significant issues, may need retry
    HIGH,     // Major issues, system degraded
    CRITICAL  // System cannot function
}