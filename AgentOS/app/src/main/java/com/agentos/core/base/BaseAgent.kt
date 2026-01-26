package com.agentos.core.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Abstract base class for AgentOS agents
 * Provides common functionality and state management
 */
abstract class BaseAgent : Agent {
    
    protected val scope = CoroutineScope(Dispatchers.Default)
    private val isInitialized = AtomicBoolean(false)
    private val isShuttingDown = AtomicBoolean(false)
    private val totalTasksExecuted = AtomicLong(0)
    private val successfulTasks = AtomicLong(0)
    private val failedTasks = AtomicLong(0)
    private val lastActivity = AtomicLong(System.currentTimeMillis())
    
    protected var currentState: AgentState = AgentState.OFFLINE
        private set
    
    override suspend fun initialize(): InitializationResult {
        return withContext(scope.coroutineContext) {
            try {
                updateState(AgentState.INITIALIZING)
                
                val result = performInitialization()
                
                if (result.success) {
                    isInitialized.set(true)
                    updateState(AgentState.READY)
                } else {
                    updateState(AgentState.ERROR)
                }
                
                result
            } catch (e: Exception) {
                updateState(AgentState.ERROR)
                InitializationResult.failure("Initialization failed: ${e.message}")
            }
        }
    }
    
    override suspend fun execute(task: Task): ExecutionResult {
        return withContext(scope.coroutineContext) {
            if (!isInitialized.get()) {
                return@withContext ExecutionResult.failure(
                    AgentError("NOT_INITIALIZED", "Agent not initialized", ErrorSeverity.CRITICAL)
                )
            }
            
            if (isShuttingDown.get()) {
                return@withContext ExecutionResult.failure(
                    AgentError("SHUTTING_DOWN", "Agent is shutting down", ErrorSeverity.WARNING)
                )
            }
            
            updateState(AgentState.PROCESSING)
            updateLastActivity()
            
            val startTime = System.currentTimeMillis()
            
            try {
                // Validate task
                val validationResult = task.validate()
                if (!validationResult.isValid) {
                    throw IllegalArgumentException("Task validation failed: ${validationResult.errors.joinToString(", ")}")
                }
                
                // Execute task
                val result = performExecution(task)
                
                // Update metrics
                totalTasksExecuted.incrementAndGet()
                if (result.success) {
                    successfulTasks.incrementAndGet()
                } else {
                    failedTasks.incrementAndGet()
                }
                
                val executionTime = System.currentTimeMillis() - startTime
                updateState(AgentState.READY)
                
                result.copy(executionTimeMs = executionTime)
                
            } catch (e: Exception) {
                failedTasks.incrementAndGet()
                val executionTime = System.currentTimeMillis() - startTime
                updateState(AgentState.ERROR)
                
                val error = AgentError(
                    errorCode = "EXECUTION_ERROR",
                    message = "Task execution failed: ${e.message}",
                    severity = ErrorSeverity.MAJOR,
                    stackTrace = e.stackTraceToString()
                )
                
                ExecutionResult.failure(error, executionTime)
            }
        }
    }
    
    override suspend fun getStatus(): AgentStatus {
        return withContext(scope.coroutineContext) {
            val total = totalTasksExecuted.get()
            val successful = successfulTasks.get()
            val failed = failedTasks.get()
            val successRate = if (total > 0) successful.toDouble() / total else 0.0
            
            AgentStatus(
                agentId = agentId,
                state = currentState,
                lastActivity = lastActivity.get(),
                activeTasks = getActiveTaskCount(),
                totalTasksExecuted = total,
                successRate = successRate,
                averageExecutionTime = calculateAverageExecutionTime().toLong(),
                health = determineHealthStatus()
            )
        }
    }
    
    override suspend fun handleError(error: AgentError): ErrorHandlingResult {
        return withContext(scope.coroutineContext) {
            try {
                val recoveryAction = determineRecoveryAction(error)
                val result = performErrorRecovery(error, recoveryAction)
                
                ErrorHandlingResult(
                    success = result,
                    recoveryAction = recoveryAction,
                    message = if (result) "Error recovered successfully" else "Error recovery failed"
                )
            } catch (e: Exception) {
                ErrorHandlingResult(
                    success = false,
                    message = "Error handling failed: ${e.message}"
                )
            }
        }
    }
    
    override suspend fun shutdown(): ShutdownResult {
        return withContext(scope.coroutineContext) {
            if (isShuttingDown.get()) {
                return@withContext ShutdownResult.success("Already shutting down", 0)
            }
            
            isShuttingDown.set(true)
            updateState(AgentState.SHUTTING_DOWN)
            
            val startTime = System.currentTimeMillis()
            
            try {
                val result = performShutdown()
                val cleanupTime = System.currentTimeMillis() - startTime
                
                isInitialized.set(false)
                updateState(AgentState.OFFLINE)
                
                result.copy(cleanupTimeMs = cleanupTime)
                
            } catch (e: Exception) {
                val cleanupTime = System.currentTimeMillis() - startTime
                ShutdownResult.failure("Shutdown failed: ${e.message}", cleanupTime)
            }
        }
    }
    
    override suspend fun getMetrics(): AgentMetrics {
        return withContext(scope.coroutineContext) {
            val total = totalTasksExecuted.get()
            val successful = successfulTasks.get()
            val failed = failedTasks.get()
            val successRate = if (total > 0) successful.toDouble() / total else 0.0
            
            AgentMetrics(
                agentId = agentId,
                totalTasksExecuted = total,
                successfulTasks = successful,
                failedTasks = failed,
                averageExecutionTime = calculateAverageExecutionTime(),
                successRate = successRate,
                lastExecutionTime = lastActivity.get(),
                resourceUsage = getCurrentResourceUsage()
            )
        }
    }
    
    override suspend fun isHealthy(): Boolean {
        return withContext(scope.coroutineContext) {
            when (currentState) {
                AgentState.READY -> performHealthCheck()
                AgentState.PROCESSING -> true // Processing is healthy
                AgentState.WAITING -> true // Waiting is healthy
                else -> false
            }
        }
    }
    
    /**
     * Abstract method for agent-specific initialization
     */
    protected abstract suspend fun performInitialization(): InitializationResult
    
    /**
     * Abstract method for agent-specific task execution
     */
    protected abstract suspend fun performExecution(task: Task): ExecutionResult
    
    /**
     * Abstract method for agent-specific error recovery
     */
    protected abstract suspend fun performErrorRecovery(
        error: AgentError, 
        recoveryAction: RecoveryAction?
    ): Boolean
    
    /**
     * Abstract method for agent-specific shutdown
     */
    protected abstract suspend fun performShutdown(): ShutdownResult
    
    /**
     * Abstract method for agent-specific health check
     */
    protected abstract suspend fun performHealthCheck(): Boolean
    
    /**
     * Determine recovery action for an error
     */
    protected abstract suspend fun determineRecoveryAction(error: AgentError): RecoveryAction?
    
    /**
     * Get current active task count
     */
    protected abstract fun getActiveTaskCount(): Int
    
    /**
     * Calculate average execution time
     */
    private fun calculateAverageExecutionTime(): Double {
        val total = totalTasksExecuted.get()
        return if (total > 0) {
            // This is a simplified calculation - in production you'd track actual execution times
            1000.0 // Placeholder - implement actual tracking
        } else {
            0.0
        }
    }
    
    /**
     * Get current resource usage
     */
    private fun getCurrentResourceUsage(): ResourceUsage {
        // Simplified implementation - in production you'd track actual resource usage
        return ResourceUsage(
            cpuUsage = 0.1, // 10% placeholder
            memoryUsage = 0.2, // 20% placeholder
            networkUsage = 0.05, // 5% placeholder
            batteryUsage = 0.01 // 1% placeholder
        )
    }
    
    /**
     * Determine health status based on current state and metrics
     */
    private fun determineHealthStatus(): HealthStatus {
        return when {
            currentState == AgentState.ERROR -> HealthStatus.UNHEALTHY
            currentState == AgentState.OFFLINE -> HealthStatus.UNHEALTHY
            else -> {
                val successRate = if (totalTasksExecuted.get() > 0) {
                    successfulTasks.get().toDouble() / totalTasksExecuted.get()
                } else {
                    1.0
                }
                
                when {
                    successRate >= 0.95 -> HealthStatus.HEALTHY
                    successRate >= 0.80 -> HealthStatus.DEGRADED
                    else -> HealthStatus.UNHEALTHY
                }
            }
        }
    }
    
    /**
     * Update agent state
     */
    private fun updateState(newState: AgentState) {
        currentState = newState
        updateLastActivity()
    }
    
    /**
     * Update last activity timestamp
     */
    private fun updateLastActivity() {
        lastActivity.set(System.currentTimeMillis())
    }
}