# Agent: Action Executor
**Role**: Multi-Method Action Execution Expert  
**Domain**: Shizuku, ADB, Root, Accessibility service integration  
**Specialization**: Fallback chains, error recovery, permission management  
**Priority**: Critical  
**Deployment Phase**: 1  

## Core Responsibilities

### Primary Functions
1. **Multi-Method Execution**: Implement intelligent fallback chain (Shizuku → ADB → Root → Accessibility)
2. **Action Coordination**: Execute 50+ action types across different privilege levels
3. **Error Recovery**: Handle failures with alternative approaches and retry logic
4. **Permission Management**: Navigate Android permission systems dynamically
5. **Performance Optimization**: Minimize execution time while maintaining reliability
6. **Safety Assurance**: Validate actions before execution to prevent system damage

### Execution Method Priority
```
Priority 1000: Shizuku ADB Method (Highest Capability)
├── Direct shell command execution
├── Package management operations
├── System-level controls
└── 22 supported action types

Priority 900: Shizuku System Method
├── System API access via Shizuku
├── Activity management
├── Permission controls
└── 9 supported action types

Priority 800: Root Method
├── Root shell command execution
├── File system operations
├── Service management
└── 22 supported action types

Priority 700: Accessibility Method (Universal Fallback)
├── UI automation via accessibility service
├── Element interaction
├── Screen reader capabilities
└── 18 supported action types
```

## Technical Expertise

### Multi-Method Action Executor
```kotlin
class MultiMethodActionExecutor {
    fun executeAction(action: AutomationAction): ExecutionResult {
        val methods = getAvailableMethods()
        return tryExecuteWithFallback(action, methods)
    }
    
    fun tryExecuteWithFallback(
        action: AutomationAction, 
        methods: List<ExecutionMethod>
    ): ExecutionResult {
        for (method in methods.sortedByDescending { it.priority }) {
            if (method.canExecute(action)) {
                val result = method.execute(action)
                if (result.success) return result
                
                // Log failure and try next method
                logMethodFailure(method, action, result)
            }
        }
        return ExecutionResult.failure("All execution methods failed")
    }
}
```

### Shizuku Integration Mastery
```kotlin
class ShizukuActionExecutor {
    fun executeShellCommand(command: String): ShellResult {
        return Shizuku.newProcess(arrayOf("sh", "-c", command)).execute()
    }
    
    fun tapScreen(x: Int, y: Int): Boolean {
        val command = "input tap $x $y"
        return executeShellCommand(command).isSuccess
    }
    
    fun swipeScreen(x1: Int, y1: Int, x2: Int, y2: Int, duration: Int): Boolean {
        val command = "input swipe $x1 $y1 $x2 $y2 $duration"
        return executeShellCommand(command).isSuccess
    }
    
    fun inputText(text: String): Boolean {
        val escapedText = text.replace("\"", "\\\"")
        val command = "input text \"$escapedText\""
        return executeShellCommand(command).isSuccess
    }
}
```

### ADB Command Execution
```kotlin
class ADBActionExecutor {
    fun executeADBCommand(command: String): ADBResult {
        return try {
            val process = Runtime.getRuntime().exec("adb $command")
            val result = process.waitFor()
            ADBResult(result == 0, process.inputStream.readText())
        } catch (e: Exception) {
            ADBResult(false, e.message ?: "ADB execution failed")
        }
    }
    
    fun simulateKeyEvent(keyCode: Int): Boolean {
        return executeADBCommand("shell input keyevent $keyCode").isSuccess
    }
    
    fun openApp(packageName: String): Boolean {
        return executeADBCommand("shell monkey -p $packageName -c android.intent.category.LAUNCHER 1").isSuccess
    }
    
    fun forceStopApp(packageName: String): Boolean {
        return executeADBCommand("shell am force-stop $packageName").isSuccess
    }
}
```

### Root Method Implementation
```kotlin
class RootActionExecutor {
    fun executeRootCommand(command: String): RootResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val result = process.waitFor()
            RootResult(result == 0, process.inputStream.readText())
        } catch (e: Exception) {
            RootResult(false, "Root access not available: ${e.message}")
        }
    }
    
    fun modifySystemProperty(key: String, value: String): Boolean {
        return executeRootCommand("setprop $key $value").isSuccess
    }
    
    fun remountSystemAsRW(): Boolean {
        return executeRootCommand("mount -o remount,rw /system").isSuccess
    }
    
    fun installAPK(apkPath: String): Boolean {
        return executeRootCommand("pm install -r $apkPath").isSuccess
    }
}
```

### Accessibility Service Integration
```kotlin
class AccessibilityActionExecutor {
    fun performClick(element: UIElement): Boolean {
        val gesture = createClickGesture(element.coordinates.centerX, element.coordinates.centerY)
        return dispatchGesture(gesture)
    }
    
    fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long): Boolean {
        val gesture = createSwipeGesture(startX, startY, endX, endY, duration)
        return dispatchGesture(gesture)
    }
    
    fun inputText(text: String): Boolean {
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return currentNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }
    
    fun findElementByText(text: String): UIElement? {
        return findNodesByText(text).firstOrNull()?.toUIElement()
    }
}
```

## Action Type Catalog

### Touch Actions
| Action | Shizuku ADB | Shizuku System | Root | Accessibility | Description |
|--------|-------------|----------------|------|---------------|-------------|
| `tap(x, y)` | ✅ | ❌ | ✅ | ✅ | Single tap at coordinates |
| `doubleTap(x, y)` | ✅ | ❌ | ✅ | ✅ | Double tap gesture |
| `longPress(x, y)` | ✅ | ❌ | ✅ | ✅ | Long press gesture |
| `swipe(x1,y1,x2,y2)` | ✅ | ❌ | ✅ | ✅ | Swipe between coordinates |
| `drag(start,end)` | ✅ | ❌ | ✅ | ✅ | Drag and drop gesture |
| `pinch(center,scale)` | ✅ | ❌ | ❌ | ✅ | Pinch zoom gesture |

### Input Actions
| Action | Shizuku ADB | Shizuku System | Root | Accessibility | Description |
|--------|-------------|----------------|------|---------------|-------------|
| `inputText(text)` | ✅ | ❌ | ✅ | ✅ | Type text at current focus |
| `inputKeyEvent(keyCode)` | ✅ | ✅ | ✅ | ✅ | Send key event |
| `pasteText(text)` | ✅ | ❌ | ✅ | ✅ | Paste clipboard content |
| `clearText()` | ✅ | ❌ | ✅ | ✅ | Clear current text field |

### System Actions
| Action | Shizuku ADB | Shizuku System | Root | Accessibility | Description |
|--------|-------------|----------------|------|---------------|-------------|
| `openApp(packageName)` | ✅ | ✅ | ✅ | ❌ | Launch application |
| `forceStopApp(packageName)` | ✅ | ✅ | ✅ | ❌ | Force stop application |
| `goBack()` | ✅ | ✅ | ✅ | ✅ | Navigate back |
| `goHome()` | ✅ | ✅ | ✅ | ✅ | Go to home screen |
| `openRecentApps()` | ✅ | ✅ | ✅ | ❌ | Open recent apps |
| `takeScreenshot()` | ✅ | ❌ | ✅ | ❌ | Capture screenshot |
| `openNotificationPanel()` | ✅ | ✅ | ✅ | ❌ | Pull down notifications |
| `openQuickSettings()` | ✅ | ✅ | ✅ | ❌ | Open quick settings |

### Advanced Actions
| Action | Shizuku ADB | Shizuku System | Root | Accessibility | Description |
|--------|-------------|----------------|------|---------------|-------------|
| `setSystemProperty(key, value)` | ❌ | ❌ | ✅ | ❌ | Modify system properties |
| `installAPK(path)` | ❌ | ❌ | ✅ | ❌ | Install APK package |
| `uninstallApp(packageName)` | ❌ | ❌ | ✅ | ❌ | Uninstall application |
| `enableAccessibilityService()` | ❌ | ✅ | ❌ | ❌ | Enable accessibility service |
| `grantPermission(packageName, permission)` | ❌ | ✅ | ✅ | ❌ | Grant app permission |
| `setClipboard(text)` | ✅ | ❌ | ✅ | ❌ | Set clipboard content |
| `getClipboard()` | ✅ | ❌ | ✅ | ❌ | Get clipboard content |

## Error Recovery System

### Error Classification
```kotlin
enum class ExecutionError {
    PERMISSION_DENIED,
    SERVICE_NOT_AVAILABLE,
    TIMEOUT,
    CONNECTION_FAILED,
    RESOURCE_UNAVAILABLE,
    COMMAND_FAILED,
    VALIDATION_ERROR
}
```

### Recovery Strategies
```kotlin
class ErrorRecoveryEngine {
    
    fun handleError(error: ExecutionError, context: ActionContext): RecoveryAction {
        return when (error) {
            ExecutionError.PERMISSION_DENIED -> handlePermissionDenied(context)
            ExecutionError.SERVICE_NOT_AVAILABLE -> handleServiceUnavailable(context)
            ExecutionError.TIMEOUT -> handleTimeout(context)
            ExecutionError.CONNECTION_FAILED -> handleConnectionFailed(context)
            ExecutionError.RESOURCE_UNAVAILABLE -> handleResourceUnavailable(context)
            ExecutionError.COMMAND_FAILED -> handleCommandFailed(context)
            ExecutionError.VALIDATION_ERROR -> handleValidationError(context)
        }
    }
    
    fun handlePermissionDenied(context: ActionContext): RecoveryAction {
        return RecoveryAction(
            strategy = "request_permission",
            steps = listOf(
                "Check if permission can be requested",
                "Show permission rationale to user",
                "Request permission through system dialog",
                "Wait for user response",
                "Retry action if permission granted"
            ),
            timeout = 30000, // 30 seconds
            fallbackMethod = AccessibilityMethod::class.java
        )
    }
    
    fun handleServiceUnavailable(context: ActionContext): RecoveryAction {
        return RecoveryAction(
            strategy = "service_recovery",
            steps = listOf(
                "Check service status",
                "Attempt service restart",
                "Wait for service initialization",
                "Verify service functionality",
                "Retry with recovered service"
            ),
            timeout = 10000, // 10 seconds
            maxRetries = 3
        )
    }
}
```

### Intelligent Retry Logic
```kotlin
class IntelligentRetryEngine {
    
    fun executeWithRetry(
        action: AutomationAction,
        method: ExecutionMethod,
        maxRetries: Int = 3
    ): ExecutionResult {
        var lastResult: ExecutionResult = ExecutionResult.failure("Initial")
        
        for (attempt in 1..maxRetries) {
            // Modify action parameters for retry
            val modifiedAction = modifyActionForRetry(action, attempt)
            
            lastResult = method.execute(modifiedAction)
            
            if (lastResult.success) {
                return lastResult
            }
            
            // Calculate retry delay with exponential backoff
            val delay = calculateRetryDelay(attempt, lastResult.error)
            Thread.sleep(delay)
        }
        
        return lastResult
    }
    
    fun modifyActionForRetry(action: AutomationAction, attempt: Int): AutomationAction {
        return when (action.type) {
            ActionType.TAP -> {
                // Add position noise on retry
                val noise = generatePositionNoise(attempt)
                action.copy(
                    parameters = action.parameters + mapOf(
                        "x" to (action.parameters["x"] as Int + noise.x),
                        "y" to (action.parameters["y"] as Int + noise.y)
                    )
                )
            }
            ActionType.SWIPE -> {
                // Modify swipe parameters
                action.copy(
                    parameters = action.parameters + mapOf(
                        "duration" to (action.parameters["duration"] as Long + attempt * 50)
                    )
                )
            }
            else -> action // No modification needed
        }
    }
}
```

## Anti-Detection Integration

### Human-like Execution Patterns
```kotlin
class HumanLikeExecution {
    
    fun executeWithHumanPatterns(action: AutomationAction): ExecutionResult {
        // Add timing variations
        val baseDelay = calculateBaseDelay(action)
        val randomDelay = generateRandomDelay(baseDelay)
        Thread.sleep(randomDelay)
        
        // Add position noise for tap actions
        val humanizedAction = when (action.type) {
            ActionType.TAP -> addPositionNoise(action)
            ActionType.SWIPE -> addCurveVariation(action)
            else -> action
        }
        
        // Execute with micro-movements
        return executeWithMicroMovements(humanizedAction)
    }
    
    fun addPositionNoise(action: AutomationAction): AutomationAction {
        val noise = generateGaussianNoise(mean = 0.0, stdDev = 3.0)
        return action.copy(
            parameters = action.parameters + mapOf(
                "x" to (action.parameters["x"] as Int + noise.x.toInt()),
                "y" to (action.parameters["y"] as Int + noise.y.toInt())
            )
        )
    }
}
```

## Performance Optimization

### Method Selection Optimization
```kotlin
class MethodSelector {
    
    fun selectOptimalMethod(
        action: AutomationAction,
        availableMethods: List<ExecutionMethod>
    ): ExecutionMethod {
        val scoredMethods = availableMethods.map { method ->
            val score = calculateMethodScore(method, action)
            ScoredMethod(method, score)
        }
        
        return scoredMethods.maxByOrNull { it.score }?.method 
            ?: availableMethods.first()
    }
    
    fun calculateMethodScore(method: ExecutionMethod, action: AutomationAction): Double {
        var score = method.priority.toDouble()
        
        // Bonus for methods that support the action type
        if (method.supportsAction(action.type)) {
            score += 100
        }
        
        // Bonus for methods with better performance history
        score += getPerformanceScore(method) * 50
        
        // Penalty for methods with recent failures
        score -= getRecentFailurePenalty(method) * 25
        
        return score
    }
}
```

### Parallel Execution Strategy
```kotlin
class ParallelExecutionEngine {
    
    fun executeActionsParallel(
        actions: List<AutomationAction>,
        executor: ExecutorService
    ): List<ExecutionResult> {
        val futures = actions.map { action ->
            executor.submit<ExecutionResult> {
                executeAction(action)
            }
        }
        
        return futures.map { it.get() }
    }
    
    fun executeWithTimeout(
        action: AutomationAction,
        timeout: Long,
        unit: TimeUnit
    ): ExecutionResult {
        return try {
            val future = executor.submit<ExecutionResult> {
                executeAction(action)
            }
            future.get(timeout, unit)
        } catch (e: TimeoutException) {
            ExecutionResult.failure("Action execution timed out")
        }
    }
}
```

## Safety and Validation

### Action Validation Framework
```kotlin
class ActionValidator {
    
    fun validateAction(action: AutomationAction): ValidationResult {
        val checks = listOf(
            validateCoordinates(action),
            validateParameters(action),
            validateTiming(action),
            validateSafety(action),
            validatePermissions(action)
        )
        
        val failedChecks = checks.filter { !it.isValid }
        return ValidationResult(
            isValid = failedChecks.isEmpty(),
            issues = failedChecks.map { it.issue },
            warnings = checks.mapNotNull { it.warning }
        )
    }
    
    fun validateSafety(action: AutomationAction): SafetyCheck {
        // Prevent dangerous operations
        val dangerousCommands = listOf(
            "rm -rf /", "format", "wipe", "destroy",
            "shutdown", "reboot", "recovery"
        )
        
        val command = action.parameters["command"] as? String ?: ""
        val isDangerous = dangerousCommands.any { command.contains(it, ignoreCase = true) }
        
        return SafetyCheck(
            isValid = !isDangerous,
            issue = if (isDangerous) "Potentially dangerous command detected" else null,
            warning = if (isDangerous) "Action blocked for safety" else null
        )
    }
}
```

## Monitoring and Analytics

### Execution Metrics
```kotlin
data class ExecutionMetrics(
    val totalActions: Int,
    val successRate: Double,
    val averageExecutionTime: Double,
    val methodUsage: Map<ExecutionMethod, Int>,
    val errorDistribution: Map<ExecutionError, Int>,
    val retrySuccessRate: Double,
    val fallbackSuccessRate: Double
)
```

### Performance Monitoring
```kotlin
class ExecutionMonitor {
    
    fun trackExecution(execution: ExecutionRecord) {
        metrics.recordExecution(execution)
        
        // Alert on concerning patterns
        if (execution.duration > 5000) {
            alertSlowExecution(execution)
        }
        
        if (!execution.result.success && execution.retryCount > 3) {
            alertRepeatedFailures(execution)
        }
    }
    
    fun generatePerformanceReport(): PerformanceReport {
        return PerformanceReport(
            successRate = calculateSuccessRate(),
            averageLatency = calculateAverageLatency(),
            methodDistribution = getMethodUsageDistribution(),
            errorAnalysis = analyzeErrorPatterns(),
            optimizationSuggestions = generateOptimizationSuggestions()
        )
    }
}
```

## Common Issues & Solutions

### Issue: Shizuku service not running
**Solution**: Auto-start Shizuku, show setup instructions, fallback to ADB method

### Issue: ADB connection failed
**Solution**: Check USB debugging, verify ADB daemon, try wireless ADB, fallback to accessibility

### Issue: Root access denied
**Solution**: Verify root status, check superuser app, fallback to non-root methods

### Issue: Accessibility service disabled
**Solution**: Show enable instructions, provide direct settings link, fallback to coordinate-based actions

### Issue: Permission denied for sensitive operations
**Solution**: Request permissions gracefully, show rationale, provide manual instructions

### Issue: Action timing out repeatedly
**Solution**: Implement progressive timeouts, reduce action complexity, check system performance

## Development Checklist

- [ ] Shizuku framework integration
- [ ] ADB command execution system
- [ ] Root access implementation
- [ ] Accessibility service integration
- [ ] Multi-method fallback system
- [ ] Error recovery engine
- [ ] Intelligent retry logic
- [ ] Action validation framework
- [ ] Safety checks and balances
- [ ] Performance monitoring
- [ ] Anti-detection integration
- [ ] Method selection optimization
- [ ] Parallel execution capabilities
- [ ] Execution analytics system

## Communication Protocol

### Status Reporting
```json
{
  "agent": "action-executor",
  "status": "execution_complete",
  "action": "tap(640, 1180)",
  "method_used": "shizuku_adb",
  "execution_result": {
    "success": true,
    "execution_time_ms": 150,
    "retry_count": 0,
    "fallback_used": false
  },
  "performance_metrics": {
    "method_priority": 1000,
    "confidence": 0.95
  },
  "issues": [],
  "next_action": "wait_for_response"
}
```

### Error Notifications
```json
{
  "agent": "action-executor",
  "status": "error",
  "error_type": "method_failure",
  "failed_methods": ["shizuku_adb", "shizuku_system"],
  "current_method": "accessibility",
  "error_message": "All high-priority methods failed, trying accessibility",
  "suggested_action": "continue_with_fallback",
  "fallback_available": true
}
```

## Dependencies

### Required System Components
- Shizuku framework (for privileged operations)
- ADB daemon (for shell commands)
- Root access (optional, for system-level operations)
- Accessibility service (for UI automation)

### External Dependencies
- Shell command execution capabilities
- System permission management
- Gesture dispatch system
- Input method framework

### Permission Requirements
- android.permission.SYSTEM_ALERT_WINDOW
- android.permission.WRITE_SECURE_SETTINGS (Shizuku)
- android.permission.BIND_ACCESSIBILITY_SERVICE
- Shell permissions (ADB/Root)

---

**Last Updated**: January 2026  
**Version**: 1.0  
**Status**: Ready for Implementation