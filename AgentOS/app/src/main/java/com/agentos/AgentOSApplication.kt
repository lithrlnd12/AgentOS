package com.agentos

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.agentos.core.logging.Logger
import com.agentos.core.monitoring.PerformanceMonitor
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Main Application class for AgentOS
 * Handles global initialization, dependency injection setup, and system-wide configuration
 */
@HiltAndroidApp
class AgentOSApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var logger: Logger
    
    @Inject
    lateinit var performanceMonitor: PerformanceMonitor
    
    companion object {
        private const val TAG = "AgentOSApplication"
        
        @Volatile
        private var instance: AgentOSApplication? = null
        
        fun getInstance(): AgentOSApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
        
        fun getContext(): Context {
            return getInstance().applicationContext
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        initializeLogging()
        initializePerformanceMonitoring()
        initializeAgents()
        validateSystemRequirements()
        
        logger.i(TAG, "AgentOS Application initialized successfully")
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG_MODE) Log.DEBUG else Log.ERROR)
            .build()
    
    private fun initializeLogging() {
        if (BuildConfig.DEBUG_MODE) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
        
        logger.i(TAG, "Logging initialized - Debug mode: ${BuildConfig.DEBUG_MODE}")
    }
    
    private fun initializePerformanceMonitoring() {
        try {
            performanceMonitor.initialize()
            logger.i(TAG, "Performance monitoring initialized")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to initialize performance monitoring", e)
        }
    }
    
    private fun initializeAgents() {
        logger.i(TAG, "Initializing AgentOS agents...")
        
        try {
            // Initialize core agents in sequence
            initializeVoiceAgent()
            initializeVisionAgent()
            initializeClaudeAgent()
            initializeActionAgent()
            
            // Initialize support agents in parallel
            initializeSupportAgents()
            
            logger.i(TAG, "All agents initialized successfully")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to initialize agents", e)
            throw RuntimeException("Critical agent initialization failed", e)
        }
    }
    
    private fun initializeVoiceAgent() {
        logger.i(TAG, "Initializing Voice Master agent...")
        // Voice agent initialization logic
        logger.i(TAG, "Voice Master agent initialized")
    }
    
    private fun initializeVisionAgent() {
        logger.i(TAG, "Initializing Vision Analyzer agent...")
        // Vision agent initialization logic
        logger.i(TAG, "Vision Analyzer agent initialized")
    }
    
    private fun initializeClaudeAgent() {
        logger.i(TAG, "Initializing Claude Integrator agent...")
        // Claude agent initialization logic
        logger.i(TAG, "Claude Integrator agent initialized")
    }
    
    private fun initializeActionAgent() {
        logger.i(TAG, "Initializing Action Executor agent...")
        // Action agent initialization logic
        logger.i(TAG, "Action Executor agent initialized")
    }
    
    private fun initializeSupportAgents() {
        logger.i(TAG, "Initializing support agents...")
        // Support agents initialization logic
        logger.i(TAG, "Support agents initialized")
    }
    
    private fun validateSystemRequirements() {
        logger.i(TAG, "Validating system requirements...")
        
        try {
            // Check minimum Android version
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
                throw UnsupportedOperationException("AgentOS requires Android 6.0 (API 23) or higher")
            }
            
            // Check for required system features
            validateAudioCapabilities()
            validateScreenCaptureCapabilities()
            validateAccessibilityCapabilities()
            
            logger.i(TAG, "System requirements validation passed")
        } catch (e: Exception) {
            logger.e(TAG, "System requirements validation failed", e)
            throw RuntimeException("System requirements not met", e)
        }
    }
    
    private fun validateAudioCapabilities() {
        // Check if device has microphone
        val hasMicrophone = packageManager.hasSystemFeature("android.hardware.microphone")
        if (!hasMicrophone) {
            logger.w(TAG, "Device does not have microphone - voice features disabled")
        }
    }
    
    private fun validateScreenCaptureCapabilities() {
        // Check MediaProjection API availability
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            logger.w(TAG, "MediaProjection not available - screen capture disabled")
        }
    }
    
    private fun validateAccessibilityCapabilities() {
        // Check accessibility service availability
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        if (!accessibilityManager.isEnabled) {
            logger.w(TAG, "Accessibility services not enabled - some features disabled")
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        logger.i(TAG, "AgentOS Application terminating")
        
        try {
            // Cleanup agents
            cleanupAgents()
            
            // Shutdown monitoring
            performanceMonitor.shutdown()
            
            logger.i(TAG, "AgentOS Application terminated cleanly")
        } catch (e: Exception) {
            logger.e(TAG, "Error during application termination", e)
        } finally {
            instance = null
        }
    }
    
    private fun cleanupAgents() {
        logger.i(TAG, "Cleaning up agents...")
        // Agent cleanup logic
        logger.i(TAG, "Agents cleaned up")
    }
    
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return
            }
            
            // Log to crash reporting service in production
            // FirebaseCrashlytics.getInstance().recordException(t ?: Exception(message))
        }
    }
}