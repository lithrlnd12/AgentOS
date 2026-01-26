# Multi-Method Action Execution System with Fallback Chain

A comprehensive Android automation framework that provides multiple execution methods with intelligent fallback chains. The system prioritizes high-privilege methods like Shizuku with ADB access, falling back through root and accessibility services as needed.

## Features

- **Multi-Method Execution:** Shizuku ADB, Shizuku API, Root, and Accessibility Service
- **Intelligent Fallback:** Automatic fallback chain based on error types and method availability
- **Error Recovery:** Comprehensive recovery mechanisms before method fallback
- **Asynchronous Execution:** Non-blocking execution with timeout support
- **Permission Management:** Automated permission checking and requesting
- **Extensible Architecture:** Easy to add custom methods and fallback strategies
- **Comprehensive Logging:** Detailed logging and diagnostics
- **Configuration-Driven:** JSON-based configuration for methods and fallbacks

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    ActionExecutor                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Fallback Chain                        │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐  │   │
│  │  │ Permission  │ │ Service     │ │ Timeout     │  │   │
│  │  │ Strategy    │ │ Availability│ │ Strategy    │  │   │
│  │  └─────────────┘ └─────────────┘ └─────────────┘  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                         │
┌────────────────────────┴────────────────────────────────────┐
│              Execution Methods (Priority Order)            │
│  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐   │
│  │ Shizuku ADB    │ │ Shizuku API    │ │ Root           │   │
│  │ (Priority 1000)│ │ (Priority 900) │ │ (Priority 800) │   │
│  └────────┬───────┘ └────────┬───────┘ └────────┬───────┘   │
│           │                  │                  │           │
│  ┌────────┴───────┐ ┌────────┴───────┐ ┌────────┴───────┐   │
│  │ ADB Commands   │ │ System API     │ │ Root Shell     │   │
│  │ Touch/Swipe    │ │ Package Mgmt   │ │ File System    │   │
│  │ Package Ops    │ │ Settings       │ │ Properties     │   │
│  └────────────────┘ └────────────────┘ └────────────────┘   │
└──────────────────────────────────────────────────────────────┘
                         │
┌────────────────────────┴────────────────────────────────────┐
│              Accessibility Service (Priority 700)           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ UI Automation                                       │   │
│  │ Click, Swipe, Text Input                            │   │
│  │ Element Finding, Global Actions                     │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Add Dependencies

```gradle
dependencies {
    implementation 'dev.rikka.shizuku:api:13.1.5'
    implementation 'dev.rikka.shizuku:provider:13.1.5'
    implementation 'androidx.core:core:1.12.0'
}
```

### 2. Initialize the System

```java
ActionExecutor executor = new ActionExecutor(context);
```

### 3. Execute Actions

```java
// Create tap action
Map<String, Object> params = new HashMap<>();
params.put("x", 500);
params.put("y", 1000);

ActionRequest request = new ActionRequest("ADB_TAP", params);

// Execute with fallback
CompletableFuture<ExecutionResult> result = executor.executeAction(request);

// Handle result
result.thenAccept(result -> {
    if (result.isSuccess()) {
        Log.i("Action", "Tap executed successfully");
    } else {
        Log.e("Action", "Failed: " + result.getError());
    }
});
```

## Supported Actions

### Shizuku ADB Method
- `ADB_SHELL` - Execute shell commands
- `ADB_TAP` - Touch input at coordinates
- `ADB_SWIPE` - Swipe gesture
- `ADB_TEXT` - Text input
- `ADB_KEYEVENT` - Key events
- `ADB_PACKAGE_LIST` - List installed packages
- `ADB_PACKAGE_INFO` - Get package information
- `ADB_SCREENSHOT` - Capture screenshot
- `ADB_WIFI_CONNECT` - Connect to WiFi
- `ADB_BLUETOOTH_ENABLE/DISABLE` - Control Bluetooth
- `ADB_MOBILE_DATA_ENABLE/DISABLE` - Control mobile data
- `ADB_REBOOT` - Reboot device
- `ADB_PACKAGE_INSTALL/UNINSTALL` - Package management

### Shizuku Method
- `START_ACTIVITY` - Start activities
- `STOP_PACKAGE` - Stop packages
- `GRANT_PERMISSION` - Grant permissions
- `REVOKE_PERMISSION` - Revoke permissions
- `FORCE_STOP` - Force stop packages
- `SET_SYSTEM_SETTING` - Modify system settings
- `GET_SYSTEM_SETTING` - Read system settings
- `INJECT_INPUT_EVENT` - Input event injection
- `TAKE_SCREENSHOT` - Screenshot capture

### Root Method
- `ROOT_SHELL` - Execute root commands
- `ROOT_FILE_READ/WRITE/DELETE` - File operations
- `ROOT_FILE_CHMOD/CHOWN` - File permissions
- `ROOT_MOUNT_RW/RO` - Mount operations
- `ROOT_SET_PROP/GET_PROP` - System properties
- `ROOT_KILL_PROCESS` - Process management
- `ROOT_SYSTEMCTL_START/STOP/RESTART` - Service control
- `ROOT_REBOOT_RECOVERY/BOOTLOADER` - Advanced reboot

### Accessibility Method
- `CLICK` - UI element clicking
- `LONG_CLICK` - Long press gestures
- `SCROLL_FORWARD/BACKWARD` - Scroll operations
- `SWIPE` - Swipe gestures
- `TEXT_INPUT` - Text input
- `FIND_ELEMENT` - Element finding
- `BACK/HOME/RECENTS` - Global actions

## Method Priority and Fallback

The system automatically selects the best available method based on priority:

1. **Shizuku ADB (1000)** - Highest priority, most capable
2. **Shizuku API (900)** - System-level operations
3. **Root (800)** - Root-based operations
4. **Accessibility (700)** - UI automation (fallback)

### Fallback Chain Example
```
User requests: ADB_TAP(500, 1000)
    ↓
Try Shizuku ADB Method
    ↓
Permission denied → Permission Fallback Strategy
    ↓
Request permissions → Retry → Still fails
    ↓
Fallback to Shizuku Method
    ↓
Not supported → Fallback to Root Method
    ↓
Execute: input tap 500 1000
    ↓
Success → Return result
```

## Configuration

### Method Configuration
```json
{
  "methods": {
    "shizuku_adb": {
      "enabled": true,
      "priority": 1000,
      "timeout": 30000,
      "retry_count": 2,
      "fallback_methods": ["shizuku", "root", "accessibility"]
    }
  }
}
```

### Fallback Configuration
```json
{
  "fallback_strategies": {
    "permission_denied": {
      "enabled": true,
      "retry_count": 2,
      "retry_delay": 2000,
      "recovery_steps": ["request_permissions", "fallback_to_lower_method"]
    }
  }
}
```

## Error Handling

### Error Types
- **Permission Errors:** Access denied, not granted
- **Service Errors:** Not available, disconnected
- **Timeout Errors:** Operation timeout, connection timeout
- **Resource Errors:** Memory, space exhaustion
- **Command Errors:** Execution failure, invalid parameters

### Recovery Mechanisms
1. **Wait Recovery:** Brief pause for temporary issues
2. **Service Refresh:** Refresh service availability
3. **Permission Request:** Request missing permissions
4. **Cache Clearing:** Clear system caches
5. **Service Restart:** Restart system services
6. **Reconnection:** Attempt to reconnect

## Advanced Usage

### Custom Fallback Strategy
```java
public class CustomFallbackStrategy implements FallbackStrategy {
    @Override
    public ExecutionResult retry(ExecutionMethod method, ActionRequest request) {
        // Custom retry logic
        return method.execute(request);
    }
}

// Register custom strategy
FallbackChain fallbackChain = new FallbackChain();
fallbackChain.addStrategy("custom_error", new CustomFallbackStrategy());
```

### Custom Execution Method
```java
public class CustomMethod implements ExecutionMethod {
    @Override
    public ExecutionResult execute(ActionRequest request) {
        // Custom execution logic
        return ExecutionResult.success().methodUsed(getName());
    }
}

// Register custom method
actionExecutor.registerMethod("custom", new CustomMethod(context));
``
