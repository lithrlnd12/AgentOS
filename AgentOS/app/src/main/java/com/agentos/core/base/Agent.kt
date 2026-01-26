package com.agentos.core.base

/**
 * Base interface for all AgentOS agents
 * Defines the contract that all agents must implement
 */
interface Agent {
    /**
     * Unique identifier for the agent
     */
    val agentId: String
    
    /**
     * Type of agent (VOICE_PROCESSING, VISION_PROCESSING, etc.)
     */
    val agentType: AgentType
    
    /**
     * Priority level for resource allocation and execution order
     */
    val priority: Priority
    
    /**
     * Initialize the agent and its dependencies
     */
    suspend fun initialize(): InitializationResult
    
    /**
     * Execute a specific task
     */
    suspend fun execute(task: Task): ExecutionResult
    
    /**
     * Get current agent status
     */
    suspend fun getStatus(): AgentStatus
    
    /**
     * Handle errors that occur during execution
     */
    suspend fun handleError(error: AgentError): ErrorHandlingResult
    
    /**
     * Cleanup resources and shutdown the agent
     */
    suspend fun shutdown(): ShutdownResult
    
    /**
     * Get agent performance metrics
     */
    suspend fun getMetrics(): AgentMetrics
    
    /**
     * Check if agent is healthy and operational
     */
    suspend fun isHealthy(): Boolean
}

/**
 * Agent type classification
 */
enum class AgentType {
    VOICE_PROCESSING,
    VISION_PROCESSING,
    AI_INTEGRATION,
    ACTION_EXECUTION,
    STEALTH_OPS,
    PERFORMANCE,
    SYSTEM_ARCHITECTURE,
    DEBUG_ANALYSIS,
    TESTING,
    MONITORING
}

/**
 * Agent priority levels
 */
enum class Priority {
    CRITICAL,    // Must be operational for system to function
    HIGH,        // Important for core functionality
    MEDIUM,      // Support functionality
    LOW          // Nice to have features
}

/**
 * Base task that agents can execute
 */
interface Task {
    val taskId: String
    val taskType: TaskType
    val priority: Priority
    val parameters: Map<String, Any>
    val timeoutMs: Long
    val retryCount: Int
    
    fun validate(): ValidationResult
}

/**
 * Task type classification
 */
enum class TaskType {
    VOICE_COMMAND,
    SCREEN_CAPTURE,
    VISION_ANALYSIS,
    AI_PROCESSING,
    ACTION_EXECUTION,
    ERROR_RECOVERY,
    PERFORMANCE_MONITORING,
    SYSTEM_CHECK,
    DEBUG_ANALYSIS,
    TEST_EXECUTION
}

/**
 * Validation result for tasks
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Agent initialization result
 */
data class InitializationResult(
    val success: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun success(message: String) = InitializationResult(true, message)
        fun failure(message: String) = InitializationResult(false, message)
    }
}

/**
 * Agent execution result
 */
data class ExecutionResult(
    val success: Boolean,
    val result: Any? = null,
    val error: AgentError? = null,
    val executionTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun success(result: Any? = null, executionTimeMs: Long = 0) = 
            ExecutionResult(true, result, null, executionTimeMs)
            
        fun failure(error: AgentError, executionTimeMs: Long = 0) = 
            ExecutionResult(false, null, error, executionTimeMs)
    }
}

/**
 * Agent status information
 */
data class AgentStatus(
    val agentId: String,
    val state: AgentState,
    val lastActivity: Long,
    val activeTasks: Int,
    val totalTasksExecuted: Long,
    val successRate: Double,
    val averageExecutionTime: Long,
    val health: HealthStatus
)

/**
 * Agent state enumeration
 */
enum class AgentState {
    INITIALIZING,
    READY,
    PROCESSING,
    WAITING,
    ERROR,
    SHUTTING_DOWN,
    OFFLINE
}

/**
* Health status enumeration
 */
enum class HealthStatus {
    HEALTHY,
    DEGRADED,
    UNHEALTHY,
    UNKNOWN
}

/**
 * Agent error information
 */
data class AgentError(
    val errorCode: String,
    val message: String,
    val severity: ErrorSeverity,
    val timestamp: Long = System.currentTimeMillis(),
    val stackTrace: String? = null,
    val context: Map<String, Any>? = null
)

/**
 * Error severity levels
 */
enum class ErrorSeverity {
    CRITICAL,    // System cannot continue
    MAJOR,       // Significant functionality affected
    MINOR,       // Minor functionality affected
    WARNING      // Potential issue, system can continue
}

/**
 * Error handling result
 */
data class ErrorHandlingResult(
    val success: Boolean,
    val recoveryAction: RecoveryAction? = null,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Recovery action for errors
 */
data class RecoveryAction(
    val actionType: String,
    val parameters: Map<String, Any> = emptyMap(),
    val timeoutMs: Long = 30000,
    val retryCount: Int = 3
)

/**
 * Agent shutdown result
 */
data class ShutdownResult(
    val success: Boolean,
    val message: String,
    val cleanupTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun success(message: String, cleanupTimeMs: Long = 0) = 
            ShutdownResult(true, message, cleanupTimeMs)
            
        fun failure(message: String, cleanupTimeMs: Long = 0) = 
            ShutdownResult(false, message, cleanupTimeMs)
    }
}

/**
 * Agent performance metrics
 */
data class AgentMetrics(
    val agentId: String,
    val totalTasksExecuted: Long,
    val successfulTasks: Long,
    val failedTasks: Long,
    val averageExecutionTime: Double,
    val successRate: Double,
    val lastExecutionTime: Long,
    val resourceUsage: ResourceUsage
)

/**
 * Resource usage information
 */
data class ResourceUsage(
    val cpuUsage: Double,
    val memoryUsage: Double,
    val networkUsage: Double,
    val batteryUsage: Double,
    val timestamp: Long = System.currentTimeMillis()
)