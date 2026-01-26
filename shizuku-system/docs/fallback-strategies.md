# Fallback Strategies Documentation

## Overview

The fallback system provides intelligent error handling and recovery mechanisms for the multi-method action execution system. When a primary execution method fails, the system automatically attempts recovery steps and falls back to alternative methods based on the error type.

## Fallback Strategy Types

### 1. Permission Fallback Strategy

**Purpose:** Handle permission-related errors

**Applicable Errors:**
- `permission denied`
- `requires permission`
- `not granted`
- `security exception`

**Recovery Steps:**
1. **Request Permissions:** Attempt to request missing permissions
2. **Check Permission Settings:** Verify permission settings in system
3. **Fallback to Lower Method:** Use method with fewer permission requirements

**Configuration:**
```json
{
  "permission_denied": {
    "enabled": true,
    "retry_count": 2,
    "retry_delay": 2000,
    "recovery_steps": [
      "request_permissions",
      "check_permission_settings",
      "fallback_to_lower_method"
    ]
  }
}
```

**Example Implementation:**
```java
public class PermissionFallbackStrategy implements FallbackStrategy {
    @Override
    public ExecutionResult retry(ExecutionMethod method, ActionRequest request) {
        // Check if we can request permissions
        if (canRequestPermissions(method)) {
            requestPermissions(method);
            // Retry after permission request
            return method.execute(request);
        }
        
        // Fallback to method with fewer permissions
        return fallbackToLowerPermissionMethod(request);
    }
}
```

### 2. Service Availability Fallback Strategy

**Purpose:** Handle service connection and availability issues

**Applicable Errors:**
- `service not available`
- `binder died`
- `service disconnected`
- `connection lost`

**Recovery Steps:**
1. **Refresh Service Status:** Check if service is now available
2. **Wait for Service:** Wait for service to become available
3. **Restart Service:** Attempt to restart the service
4. **Fallback to Lower Method:** Use alternative execution method

**Configuration:**
```json
{
  "service_not_available": {
    "enabled": true,
    "retry_count": 3,
    "retry_delay": 1500,
    "recovery_steps": [
      "refresh_service_status",
      "wait_for_service",
      "restart_service",
      "fallback_to_lower_method"
    ]
  }
}
```

**Example Implementation:**
```java
public class ServiceAvailabilityFallbackStrategy implements FallbackStrategy {
    @Override
    public ExecutionResult retry(ExecutionMethod method, ActionRequest request) {
        // Refresh method availability
        method.refreshAvailability();
        
        if (method.isAvailable()) {
            // Retry execution
            return method.execute(request);
        }
        
        // Wait and check again
        try {
            Thread.sleep(getRetryDelay());
            method.refreshAvailability();
            
            if (method.isAvailable()) {
                return method.execute(request);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return ExecutionResult.failure("Service unavailable after recovery attempts",
                ActionExecutor.ErrorCode.METHOD_NOT_AVAILABLE);
    }
}
```

### 3. Timeout Fallback Strategy

**Purpose:** Handle timeout-related errors

**Applicable Errors:**
- `timeout`
- `operation timed out`
- `connection timeout`
- `execution timeout`

**Recovery Steps:**
1. **Increase Timeout:** Use longer timeout for retry
2. **Wait and Retry:** Wait before retrying
3. **Fallback to Lower Method:** Use faster alternative method

**Configuration:**
```json
{
  "timeout": {
    "enabled": true,
    "retry_count": 2,
    "retry_delay": 5000,
    "recovery_steps": [
      "increase_timeout",
      "wait_and_retry",
      "fallback_to_lower_method"
    ]
  }
}
```

**Example Implementation:**
```java
public class TimeoutFallbackStrategy implements FallbackStrategy {
    @Override
    public ExecutionResult retry(ExecutionMethod method, ActionRequest request) {
        // Retry with increased timeout
        if (request instanceof TimeoutableRequest) {
            ((TimeoutableRequest) request).setTimeout(getIncreasedTimeout());
        }
        
        // Wait before retry
        try {
            Thread.sleep(getRetryDelay());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return method.execute(request);
    }
    
    private long getIncreasedTimeout() {
        return 60000; // 60 seconds
    }
}
```

### 4. Connection Fallback Strategy

**Purpose:** Handle network and connection-related errors

**Applicable Errors:**
- `connection failed`
- `network unreachable`
- `connection refused`
- `network error`

**Recovery Steps:**
1. **Reconnect:** Attempt to re-establish connection
2. **Check Network:** Verify network connectivity
3. **Refresh Connection:** Reset connection parameters
4. **Fallback to Lower Method:** Use offline alternative

**Configuration:**
```json
{
  "connection_failed": {
    "enabled": true,
    "retry_count": 3,
    "retry_delay": 3000,
    "recovery_steps": [
      "reconnect",
      "check_network",
      "refresh_connection",
      "fallback_to_lower_method"
    ]
  }
}
```

### 5. Resource Fallback Strategy

**Purpose:** Handle resource exhaustion errors

**Applicable Errors:**
- `out of memory`
- `resource exhausted`
- `insufficient space`
- `resource temporarily unavailable`

**Recovery Steps:**
1. **Clear Cache:** Clear system caches
2. **Free Memory:** Attempt to free memory
3. **Wait for Resources:** Wait for resources to become available
4. **Fallback to Lower Method:** Use less resource-intensive method

**Configuration:**
```json
{
  "resource_error": {
    "enabled": true,
    "retry_count": 2,
    "retry_delay": 10000,
    "recovery_steps": [
      "clear_cache",
      "free_memory",
      "wait_for_resources",
      "fallback_to_lower_method"
    ]
  }
}
```

### 6. Command Fallback Strategy

**Purpose:** Handle command execution failures

**Applicable Errors:**
- `command failed`
- `exit code`
- `command not found`
- `invalid command`

**Recovery Steps:**
1. **Retry Command:** Retry with same parameters
2. **Try Alternative Command:** Use different command format
3. **Fallback to Lower Method:** Use alternative execution approach

**Configuration:**
```json
{
  "command_failed": {
    "enabled": true,
    "retry_count": 2,
    "retry_delay": 2000,
    "recovery_steps": [
      "retry_command",
      "try_alternative_command",
      "fallback_to_lower_method"
    ]
  }
}
```

## Recovery Steps

### 1. Wait Recovery Step

**Purpose:** Brief pause for temporary issues

**Applicable Errors:** Temporary failures, busy conditions

**Implementation:**
```java
public class WaitRecoveryStep implements ErrorRecovery {
    private static final long WAIT_TIME = 1000; // 1 second
    
    @Override
    public boolean execute() {
        try {
            Thread.sleep(WAIT_TIME);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
```

### 2. Refresh Service Recovery Step

**Purpose:** Refresh service availability status

**Applicable Errors:** Service-related failures

**Implementation:**
```java
public class RefreshServiceRecoveryStep implements ErrorRecovery {
    @Override
    public boolean execute() {
        try {
            // Refresh all method availability
            for (ExecutionMethod method : availableMethods) {
                method.refreshAvailability();
            }
            return true;
        } catch (Exception e) {
            Logger.e("Recovery", "Failed to refresh services", e);
            return false;
        }
    }
}
```

### 3. Clear Cache Recovery Step

**Purpose:** Clear system caches and free resources

**Applicable Errors:** Memory, cache, resource issues

**Implementation:**
```java
public class ClearCacheRecoveryStep implements ErrorR
