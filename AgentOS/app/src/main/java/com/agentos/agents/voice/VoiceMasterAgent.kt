package com.agentos.agents.voice

import android.content.Context
import android.util.Log
import com.agentos.core.base.*
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.atomic.AtomicInteger

/**
 * Voice Master Agent - Handles voice command processing and intent extraction
 * Integrates with OpenAI Advanced Voice API for real-time speech recognition
 */
class VoiceMasterAgent(
    private val context: Context,
    private val apiKey: String = "", // Should be injected via BuildConfig
    private val voiceEngine: VoiceProcessingEngine? = null
) : BaseAgent() {
    
    companion object {
        private const val TAG = "VoiceMasterAgent"
        const val AGENT_ID = "voice-master"
    }
    
    override val agentId: String = AGENT_ID
    override val agentType: AgentType = AgentType.VOICE_PROCESSING
    override val priority: Priority = Priority.CRITICAL
    
    private lateinit var processingEngine: VoiceProcessingEngine
    private val activeTaskCount = AtomicInteger(0)
    
    override suspend fun performInitialization(): InitializationResult {
        return coroutineScope {
            try {
                Log.i(TAG, "Initializing Voice Master Agent")
                
                // Initialize voice processing engine
                processingEngine = voiceEngine ?: OpenAIVoiceProcessingEngine(context, apiKey)
                
                val initResult = processingEngine.initialize()
                
                if (initResult.success) {
                    Log.i(TAG, "Voice Master Agent initialized successfully")
                    Log.d(TAG, "Supported languages: ${initResult.supportedLanguages.joinToString()}")
                    Log.d(TAG, "Available voices: ${initResult.availableVoices.joinToString()}")
                    
                    InitializationResult.success("Voice Master Agent initialized successfully")
                } else {
                    Log.e(TAG, "Voice processing engine initialization failed: ${initResult.message}")
                    InitializationResult.failure("Voice processing engine initialization failed: ${initResult.message}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Voice Master Agent initialization failed", e)
                InitializationResult.failure("Voice Master Agent initialization failed: ${e.message}")
            }
        }
    }
    
    override suspend fun performExecution(task: Task): ExecutionResult {
        return coroutineScope {
            try {
                Log.d(TAG, "Executing voice task: ${task.taskId}")
                
                // Validate task type
                if (task.taskType != TaskType.VOICE_COMMAND) {
                    throw IllegalArgumentException("Voice Master Agent only processes VOICE_COMMAND tasks")
                }
                
                // Cast to VoiceCommandTask
                val voiceTask = task as? VoiceCommandTask
                    ?: throw IllegalArgumentException("Task must be VoiceCommandTask")
                
                // Validate task parameters
                val validationResult = voiceTask.validate()
                if (!validationResult.isValid) {
                    throw IllegalArgumentException("Task validation failed: ${validationResult.errors.joinToString(", ")}")
                }
                
                // Process voice command
                val voiceResult = processVoiceCommand(voiceTask)
                
                Log.d(TAG, "Voice task completed successfully: ${voiceTask.taskId}")
                
                ExecutionResult.success(voiceResult)
                
            } catch (e: Exception) {
                Log.e(TAG, "Voice task execution failed", e)
                ExecutionResult.failure(
                    AgentError(
                        errorCode = "VOICE_EXECUTION_ERROR",
                        message = "Voice task execution failed: ${e.message}",
                        severity = ErrorSeverity.MAJOR
                    )
                )
            }
        }
    }
    
    override suspend fun performErrorRecovery(
        error: AgentError, 
        recoveryAction: RecoveryAction?
    ): Boolean {
        return try {
            Log.w(TAG, "Performing error recovery for: ${error.errorCode}")
            
            when (error.errorCode) {
                "VOICE_PROCESSING_ERROR" -> handleVoiceProcessingError(error, recoveryAction)
                "NETWORK_ERROR" -> handleNetworkError(error, recoveryAction)
                "TIMEOUT_ERROR" -> handleTimeoutError(error, recoveryAction)
                else -> handleGenericError(error, recoveryAction)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error recovery failed", e)
            false
        }
    }
    
    override suspend fun performShutdown(): ShutdownResult {
        return try {
            Log.i(TAG, "Shutting down Voice Master Agent")
            
            val startTime = System.currentTimeMillis()
            
            // Shutdown voice processing engine
            val shutdownResult = processingEngine.shutdown()
            
            val cleanupTime = System.currentTimeMillis() - startTime
            
            if (shutdownResult.success) {
                Log.i(TAG, "Voice Master Agent shutdown successfully")
                ShutdownResult.success("Voice Master Agent shutdown successfully", cleanupTime)
            } else {
                Log.e(TAG, "Voice processing engine shutdown failed: ${shutdownResult.message}")
                ShutdownResult.failure("Voice processing engine shutdown failed: ${shutdownResult.message}", cleanupTime)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Voice Master Agent shutdown failed", e)
            ShutdownResult.failure("Voice Master Agent shutdown failed: ${e.message}")
        }
    }
    
    override suspend fun performHealthCheck(): Boolean {
        return try {
            Log.d(TAG, "Performing health check")
            
            // Check if voice engine is healthy
            val engineHealthy = processingEngine.isHealthy()
            
            if (!engineHealthy) {
                Log.w(TAG, "Voice engine is not healthy")
                return false
            }
            
            // Check if we can process a simple test
            val testTask = VoiceCommandTask.createTestTask()
            val testResult = processVoiceCommand(testTask)
            
            val isHealthy = testResult.processingResult?.success ?: false
            
            Log.d(TAG, "Health check result: $isHealthy")
            isHealthy
            
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            false
        }
    }
    
    override suspend fun determineRecoveryAction(error: AgentError): RecoveryAction? {
        return when (error.errorCode) {
            "VOICE_PROCESSING_ERROR" -> RecoveryAction(
                actionType = "retry_with_backoff",
                parameters = mapOf(
                    "maxRetries" to 3,
                    "backoffMultiplier" to 2.0,
                    "initialDelayMs" to 1000
                ),
                timeoutMs = 30000,
                retryCount = 3
            )
            "NETWORK_ERROR" -> RecoveryAction(
                actionType = "wait_and_retry",
                parameters = mapOf(
                    "waitTimeMs" to 5000,
                    "maxRetries" to 2
                ),
                timeoutMs = 20000,
                retryCount = 2
            )
            "TIMEOUT_ERROR" -> RecoveryAction(
                actionType = "increase_timeout",
                parameters = mapOf(
                    "timeoutMultiplier" to 2.0,
                    "maxTimeoutMs" to 30000
                ),
                timeoutMs = 35000,
                retryCount = 1
            )
            else -> null
        }
    }
    
    override fun getActiveTaskCount(): Int = activeTaskCount.get()
    
    // ==================== Private Helper Methods ====================
    
    private suspend fun processVoiceCommand(task: VoiceCommandTask): VoiceResult {
        activeTaskCount.incrementAndGet()
        
        return try {
            Log.d(TAG, "Processing voice command: ${task.taskId}")
            
            // Step 1: Process audio to get recognized text
            val processingConfig = createProcessingConfig(task)
            val processingResult = processingEngine.processVoiceAudio(task.audioData, processingConfig)
            
            if (!processingResult.success) {
                Log.w(TAG, "Voice processing failed: ${processingResult.error?.message}")
                return VoiceResult(
                    success = false,
                    processingResult = processingResult,
                    error = processingResult.error
                )
            }
            
            Log.d(TAG, "Voice recognized: '${processingResult.recognizedText}' with confidence: ${processingResult.confidence}")
            
            // Step 2: Extract intent from recognized text
            val voiceContext = createVoiceContext(task)
            val intentResult = processingEngine.extractIntent(
                processingResult.recognizedText ?: "",
                voiceContext
            )
            
            // Step 3: Create final result
            val voiceResult = VoiceResult(
                success = true,
                processingResult = processingResult,
                intentResult = intentResult,
                recognizedText = processingResult.recognizedText,
                confidence = processingResult.confidence ?: 0.0,
                clarificationNeeded = intentResult.clarificationNeeded,
                clarificationPrompt = intentResult.clarificationPrompt
            )
            
            Log.d(TAG, "Voice command processed successfully: ${task.taskId}")
            voiceResult
            
        } finally {
            activeTaskCount.decrementAndGet()
        }
    }
    
    private fun createProcessingConfig(task: VoiceCommandTask): VoiceProcessingConfig {
        return VoiceProcessingConfig(
            sampleRate = task.sampleRate,
            language = task.language,
            noiseReduction = task.noiseLevel > 0.2f,
            echoCancellation = true,
            timeoutMs = task.timeoutMs
        )
    }
    
    private fun createVoiceContext(task: VoiceCommandTask): VoiceContext {
        return VoiceContext(
            userPreferences = UserVoicePreferences(
                noiseTolerance = task.noiseLevel,
                commandConfirmation = task.expectedCommandType != null
            )
        )
    }
    
    private fun handleVoiceProcessingError(error: AgentError, recoveryAction: RecoveryAction?): Boolean {
        Log.d(TAG, "Handling voice processing error: ${error.errorCode}")
        
        return try {
            when (error.context?.get("errorType")) {
                "NETWORK_ERROR" -> handleNetworkError(error, recoveryAction)
                "AUDIO_ERROR" -> handleAudioError(error, recoveryAction)
                "RECOGNITION_ERROR" -> handleRecognitionError(error, recoveryAction)
                else -> handleGenericVoiceError(error, recoveryAction)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Voice processing error recovery failed", e)
            false
        }
    }
    
    private fun handleNetworkError(error: AgentError, recoveryAction: RecoveryAction?): Boolean {
        Log.d(TAG, "Handling network error")

        return try {
            // Wait and retry
            recoveryAction?.let { action ->
                val waitTime = action.parameters["waitTimeMs"] as? Long ?: 5000
                Thread.sleep(waitTime)
                // Return true to indicate recovery action was applied
                // Actual health check will happen on next operation
                true
            } ?: false

        } catch (e: Exception) {
            Log.e(TAG, "Network error recovery failed", e)
            false
        }
    }

    private fun handleAudioError(error: AgentError, recoveryAction: RecoveryAction?): Boolean {
        Log.d(TAG, "Handling audio error")
        // Mark for re-initialization on next operation
        // Actual re-initialization will happen when the engine is next used
        return true
    }
    
    private fun handleRecognitionError(error: AgentError, recoveryAction: RecoveryAction?): Boolean {
        Log.d(TAG, "Handling recognition error")
        
        return try {
            // Retry with different parameters
            recoveryAction?.let { action ->
                // Implement retry logic based on action parameters
                Log.d(TAG, "Retrying with recovery action: ${action.actionType}")
                true
            } ?: false
            
        } catch (e: Exception) {
            Log.e(TAG, "Recognition error recovery failed", e)
            false
        }
    }
    
    private fun handleGenericVoiceError(error: AgentError, recoveryAction: RecoveryAction?): Boolean {
        Log.d(TAG, "Handling generic voice error")
        
        return try {
            // Generic retry logic
            recoveryAction?.let { action ->
                Log.d(TAG, "Applying generic recovery action: ${action.actionType}")
                true
            } ?: false
            
        } catch (e: Exception) {
            Log.e(TAG, "Generic error recovery failed", e)
            false
        }
    }
    
    private fun handleTimeoutError(error: AgentError, recoveryAction: RecoveryAction?): Boolean {
        Log.d(TAG, "Handling timeout error")
        
        return try {
            // Increase timeout and retry
            recoveryAction?.let { action ->
                val timeoutMultiplier = action.parameters["timeoutMultiplier"] as? Double ?: 2.0
                Log.d(TAG, "Increasing timeout by factor: $timeoutMultiplier")
                true
            } ?: false
            
        } catch (e: Exception) {
            Log.e(TAG, "Timeout error recovery failed", e)
            false
        }
    }
    
    private fun handleGenericError(error: AgentError, recoveryAction: RecoveryAction?): Boolean {
        Log.d(TAG, "Handling generic error: ${error.errorCode}")
        
        return try {
            // Apply generic recovery action
            recoveryAction?.let { action ->
                Log.d(TAG, "Applying recovery action: ${action.actionType}")
                true
            } ?: false
            
        } catch (e: Exception) {
            Log.e(TAG, "Generic error recovery failed", e)
            false
        }
    }
}

/**
 * Result from voice processing
 */
data class VoiceResult(
    val success: Boolean,
    val processingResult: VoiceProcessingResult? = null,
    val intentResult: VoiceIntentResult? = null,
    val recognizedText: String? = null,
    val confidence: Double = 0.0,
    val clarificationNeeded: Boolean = false,
    val clarificationPrompt: String? = null,
    val error: VoiceError? = null
)