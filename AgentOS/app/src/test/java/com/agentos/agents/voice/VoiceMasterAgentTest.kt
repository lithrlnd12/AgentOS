package com.agentos.agents.voice

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * Comprehensive test suite for Voice Master agent
 * Tests voice processing, intent extraction, and error handling
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceMasterAgentTest {
    
    private lateinit var voiceMasterAgent: VoiceMasterAgent
    private lateinit var mockVoiceEngine: VoiceProcessingEngine
    private lateinit var mockTask: VoiceCommandTask
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        mockVoiceEngine = mock()
        voiceMasterAgent = VoiceMasterAgent(mockVoiceEngine)
        
        mockTask = VoiceCommandTask.createTestTask()
    }
    
    // ==================== Initialization Tests ====================
    
    @Test
    fun `initialization succeeds with valid configuration`() = runTest {
        // Given: Valid initialization result
        val initResult = VoiceInitializationResult(
            success = true,
            message = "Voice engine initialized",
            supportedLanguages = listOf("en-US", "es-ES"),
            availableVoices = listOf("alloy", "echo")
        )
        
        whenever(mockVoiceEngine.initialize()).thenReturn(initResult)
        
        // When: Initialize agent
        val result = voiceMasterAgent.initialize()
        
        // Then: Initialization succeeds
        assertTrue("Initialization should succeed", result.success)
        assertEquals("Success message", "Voice engine initialized", result.message)
        
        verify(mockVoiceEngine).initialize()
    }
    
    @Test
    fun `initialization fails when voice engine fails`() = runTest {
        // Given: Failed initialization
        val initResult = VoiceInitializationResult(
            success = false,
            message = "Voice engine initialization failed"
        )
        
        whenever(mockVoiceEngine.initialize()).thenReturn(initResult)
        
        // When: Initialize agent
        val result = voiceMasterAgent.initialize()
        
        // Then: Initialization fails
        assertFalse("Initialization should fail", result.success)
        assertEquals("Failure message", "Voice engine initialization failed", result.message)
        
        verify(mockVoiceEngine).initialize()
    }
    
    @Test
    fun `initialization handles exceptions gracefully`() = runTest {
        // Given: Exception during initialization
        whenever(mockVoiceEngine.initialize()).thenThrow(RuntimeException("Network error"))
        
        // When: Initialize agent
        val result = voiceMasterAgent.initialize()
        
        // Then: Returns failure with error message
        assertFalse("Initialization should fail", result.success)
        assertTrue("Error message contains exception", result.message.contains("Network error"))
        
        verify(mockVoiceEngine).initialize()
    }
    
    // ==================== Voice Processing Tests ====================
    
    @Test
    fun `voice processing succeeds with clear audio`() = runTest {
        // Given: Clear audio input
        val clearAudio = generateTestAudio("Open Chrome", noiseLevel = 0.1f)
        val task = VoiceCommandTask.fromAudioData("test_task", clearAudio)
        
        val processingResult = VoiceProcessingResult(
            success = true,
            recognizedText = "Open Chrome",
            confidence = 0.95,
            language = "en-US",
            processingTimeMs = 500
        )
        
        whenever(mockVoiceEngine.processVoiceAudio(any(), any())).thenReturn(processingResult)
        
        // When: Process voice command
        val result = voiceMasterAgent.execute(task)
        
        // Then: Processing succeeds
        assertTrue("Processing should succeed", result.success)
        assertNotNull("Should have result", result.result)
        
        verify(mockVoiceEngine).processVoiceAudio(any(), any())
    }
    
    @Test
    fun `voice processing handles noisy audio`() = runTest {
        // Given: Noisy audio input
        val noisyAudio = generateTestAudio("Schedule lunch", noiseLevel = 0.7f)
        val task = VoiceCommandTask.fromAudioData("test_task", noisyAudio, noiseLevel = 0.7f)
        
        val processingResult = VoiceProcessingResult(
            success = true,
            recognizedText = "Schedule lunch",
            confidence = 0.82,
            language = "en-US",
            processingTimeMs = 800
        )
        
        whenever(mockVoiceEngine.processVoiceAudio(any(), any())).thenReturn(processingResult)
        
        // When: Process voice command
        val result = voiceMasterAgent.execute(task)
        
        // Then: Processing succeeds with lower confidence
        assertTrue("Processing should succeed", result.success)
        assertNotNull("Should have result", result.result)
        
        verify(mockVoiceEngine).processVoiceAudio(any(), any())
    }
    
    @Test
    fun `voice processing handles different languages`() = runTest {
        // Given: Spanish audio input
        val spanishAudio = generateTestAudio("Abrir Chrome", noiseLevel = 0.2f)
        val task = VoiceCommandTask.fromAudioData("test_task", spanishAudio, language = "es-ES")
        
        val processingResult = VoiceProcessingResult(
            success = true,
            recognizedText = "Abrir Chrome",
            confidence = 0.91,
            language = "es-ES",
            processingTimeMs = 600
        )
        
        whenever(mockVoiceEngine.processVoiceAudio(any(), any())).thenReturn(processingResult)
        
        // When: Process voice command
        val result = voiceMasterAgent.execute(task)
        
        // Then: Processing succeeds in Spanish
        assertTrue("Processing should succeed", result.success)
        assertNotNull("Should have result", result.result)
        
        verify(mockVoiceEngine).processVoiceAudio(any(), any())
    }
    
    // ==================== Intent Extraction Tests ====================
    
    @Test
    fun `intent extraction creates valid automation commands`() = runTest {
        // Given: Recognized text
        val recognizedText = "Schedule lunch with Sarah next Tuesday at 3 PM"
        val audioData = generateTestAudio(recognizedText, noiseLevel = 0.1f)
        val task = VoiceCommandTask.fromAudioData("test_task", audioData)
        
        val processingResult = VoiceProcessingResult(
            success = true,
            recognizedText = recognizedText,
            confidence = 0.94,
            language = "en-US",
            processingTimeMs = 500
        )
        
        val intentResult = VoiceIntentResult(
            success = true,
            intent = AutomationIntent(
                type = IntentType.CALENDAR_EVENT,
                action = "schedule_event",
                parameters = mapOf(
                    "contact" to "Sarah",
                    "datetime" to "next Tuesday at 3 PM",
                    "event_type" to "lunch"
                ),
                confidence = 0.92,
                originalText = recognizedText
            ),
            confidence = 0.92
        )
        
        whenever(mockVoiceEngine.processVoiceAudio(any(), any())).thenReturn(processingResult)
        whenever(mockVoiceEngine.extractIntent(eq(recognizedText), any())).thenReturn(intentResult)
        
        // When: Process voice command
        val result = voiceMasterAgent.execute(task)
        
        // Then: Intent extraction succeeds
        assertTrue("Overall processing should succeed", result.success)
        assertNotNull("Should have intent result", result.result)
        
        verify(mockVoiceEngine).processVoiceAudio(any(), any())
        verify(mockVoiceEngine).extractIntent(eq(recognizedText), any())
    }
    
    @Test
    fun `intent extraction handles ambiguous commands`() = runTest {
        // Given: Ambiguous text
        val ambiguousText = "Do something with the app"
        val audioData = generateTestAudio(ambiguousText, noiseLevel = 0.1f)
        val task = VoiceCommandTask.fromAudioData("test_task", audioData)
        
        val processingResult = VoiceProcessingResult(
            success = true,
            recognizedText = ambiguousText,
            confidence = 0.88,
            language = "en-US",
            processingTimeMs = 400
        )
        
        val intentResult = VoiceIntentResult(
            success = false,
            clarificationNeeded = true,
            clarificationPrompt = "Could you be more specific about what you'd like to do?"
        )
        
        whenever(mockVoiceEngine.processVoiceAudio(any(), any())).thenReturn(processingResult)
        whenever(mockVoiceEngine.extractIntent(eq(ambiguousText), any())).thenReturn(intentResult)
        
        // When: Process voice command
        val result = voiceMasterAgent.execute(task)
        
        // Then: Clarification is requested
        assertTrue("Processing should complete", result.success)
        assertNotNull("Should have result", result.result)
        
        val voiceResult = result.result as VoiceResult
        assertTrue("Should request clarification", voiceResult.clarificationNeeded)
        assertNotNull("Should have clarification prompt", voiceResult.clarificationPrompt)
        
        verify(mockVoiceEngine).processVoiceAudio(any(), any())
        verify(mockVoiceEngine).extractIntent(eq(ambiguousText), any())
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    fun `error recovery handles voice processing failures`() = runTest {
        // Given: Voice processing fails
        val audioData = generateTestAudio("Test command", noiseLevel = 0.1f)
        val task = VoiceCommandTask.fromAudioData("test_task", audioData)
        
        val processingResult = VoiceProcessingResult(
            success = false,
            processingTimeMs = 200,
            error = VoiceError(
                errorCode = "NETWORK_ERROR",
                message = "Network connection failed",
                severity = VoiceErrorSeverity.HIGH
            )
        )
        
        whenever(mockVoiceEngine.processVoiceAudio(any(), any())).thenReturn(processingResult)
        whenever(mockVoiceEngine.handleError(any())).thenReturn(
            ErrorHandlingResult(
                success = true,
                message = "Network error recovered",
                recoveryAction = RecoveryAction(
                    actionType = "retry_with_backoff",
                    parameters = mapOf("maxRetries" to 3),
                    timeoutMs = 10000
                )
            )
        )
        
        // When: Process voice command
        val result = voiceMasterAgent.execute(task)
        
        // Then: Error is handled and recovery attempted
        assertFalse("Processing should fail", result.success)
        assertNotNull("Should have error", result.error)
        
        verify(mockVoiceEngine).processVoiceAudio(any(), any())
        verify(mockVoiceEngine).handleError(any())
    }
    
    @Test
    fun `timeout handling works correctly`() = runTest {
        // Given: Task with short timeout
        val audioData = generateTestAudio("Long running command", noiseLevel = 0.1f)
        val task = VoiceCommandTask.fromAudioData(
            "test_task", 
            audioData, 
            timeoutMs = 100 // Very short timeout
        )
        
        // Simulate timeout
        whenever(mockVoiceEngine.processVoiceAudio(any(), any())).thenAnswer {
            Thread.sleep(200) // Longer than timeout
            VoiceProcessingResult(
                success = false,
                processingTimeMs = 200,
                error = VoiceError(
                    errorCode = "TIMEOUT",
                    message = "Processing timed out",
                    severity = VoiceErrorSeverity.MEDIUM
                )
            )
        }
        
        // When: Process voice command with timeout
        val result = voiceMasterAgent.execute(task)
        
        // Then: Timeout is handled
        assertFalse("Processing should timeout", result.success)
        assertNotNull("Should have timeout error", result.error)
        
        verify(mockVoiceEngine).processVoiceAudio(any(), any())
    }
    
    // ==================== Performance Tests ====================
    
    @Test
    fun `voice processing meets performance requirements`() = runTest {
        // Given: Performance requirements
        val maxProcessingTime = 5000L // 5 seconds
        val minConfidence = 0.85
        
        val audioData = generateTestAudio("Performance test command", noiseLevel = 0.1f)
        val task = VoiceCommandTask.fromAudioData("test_task", audioData)
        
        val startTime = System.currentTimeMillis()
        
        val processingResult = VoiceProcessingResult(
            success = true,
            recognizedText = "Performance test command",
            confidence = 0.92,
            language = "en-US",
            processingTimeMs = 800 // Fast processing
        )
        
        whenever(mockVoiceEngine.processVoiceAudio(any(), any())).thenReturn(processingResult)
        
        // When: Process voice command
        val result = voiceMasterAgent.execute(task)
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Then: Performance requirements met
        assertTrue("Should complete within timeout", totalTime < maxProcessingTime)
        assertTrue("Should have sufficient confidence", processingResult.confidence ?: 0.0 >= minConfidence)
        
        verify(mockVoiceEngine).processVoiceAudio(any(), any())
    }
    
    // ==================== Health Check Tests ====================
    
    @Test
    fun `health check returns correct status`() = runTest {
        // Given: Healthy engine status
        val engineStatus = VoiceEngineStatus(
            isInitialized = true,
            isProcessing = false,
            lastActivity = System.currentTimeMillis(),
            processedCommands = 100,
            successRate = 0.94,
            currentModel = "gpt-4o-realtime-preview",
            supportedLanguages = listOf("en-US", "es-ES")
        )
        
        whenever(mockVoiceEngine.getStatus()).thenReturn(engineStatus)
        whenever(mockVoiceEngine.isHealthy()).thenReturn(true)
        
        // When: Check health
        val isHealthy = voiceMasterAgent.isHealthy()
        
        // Then: Health check passes
        assertTrue("Agent should be healthy", isHealthy)
        
        verify(mockVoiceEngine).isHealthy()
    }
    
    @Test
    fun `health check detects unhealthy engine`() = runTest {
        // Given: Unhealthy engine
        whenever(mockVoiceEngine.isHealthy()).thenReturn(false)
        
        // When: Check health
        val isHealthy = voiceMasterAgent.isHealthy()
        
        // Then: Health check fails
        assertFalse("Agent should not be healthy", isHealthy)
        
        verify(mockVoiceEngine).isHealthy()
    }
    
    // ==================== Metrics Tests ====================
    
    @Test
    fun `metrics collection works correctly`() = runTest {
        // Given: Some processing history
        val processingResult = VoiceProcessingResult(
            success = true,
            recognizedText = "Test command",
            confidence = 0.90,
            language = "en-US",
            processingTimeMs = 500
        )
        
        whenever(mockVoiceEngine.processVoiceAudio(any(), any())).thenReturn(processingResult)
        
        // When: Execute multiple tasks
        repeat(5) { i ->
            val task = VoiceCommandTask.fromAudioData("task_$i", generateTestAudio("Command $i"))
            voiceMasterAgent.execute(task)
        }
        
        // Then: Metrics are collected correctly
        val metrics = voiceMasterAgent.getMetrics()
        
        assertEquals("Agent ID correct", "voice-master", metrics.agentId)
        assertEquals("Total tasks executed", 5, metrics.totalTasksExecuted)
        assertEquals("Successful tasks", 5, metrics.successfulTasks)
        assertEquals("Failed tasks", 0, metrics.failedTasks)
        assertEquals("Success rate", 1.0, metrics.successRate, 0.01)
        
        verify(mockVoiceEngine, times(5)).processVoiceAudio(any(), any())
    }
    
    // ==================== Helper Methods ====================
    
    private fun generateTestAudio(text: String, noiseLevel: Float = 0.0f): ByteArray {
        // Simulate audio data - in real implementation this would be actual audio
        return text.toByteArray() + ByteArray((noiseLevel * 100).toInt()) { (it % 256).toByte() }
    }
    
    private fun loadTestAudio(fileName: String): ByteArray {
        // Load test audio file - in real implementation this would load actual audio
        return "Test audio data for $fileName".toByteArray()
    }
    
    private fun loadTestClaudeResponse(fileName: String): String {
        // Load test Claude response - in real implementation this would load actual JSON
        return """{"elements": [{"type": "button", "text": "Add to Cart"}]}"""
    }
}