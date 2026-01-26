# Integration Guide

## Quick Start

### 1. Add Dependencies

Add the required dependencies to your `build.gradle`:

```gradle
dependencies {
    // Shizuku dependencies
    implementation 'dev.rikka.shizuku:api:13.1.5'
    implementation 'dev.rikka.shizuku:provider:13.1.5'
    
    // Other required dependencies
    implementation 'androidx.core:core:1.12.0'
}
```

### 2. Initialize the System

```java
public class MyApplication extends Application {
    private ActionExecutor actionExecutor;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize the action executor
        actionExecutor = new ActionExecutor(this);
        
        // Configure logging
        Logger.setLogLevel(true, true, true, true);
    }
    
    public ActionExecutor getActionExecutor() {
        return actionExecutor;
    }
}
```

### 3. Basic Usage

```java
// Create action request
Map<String, Object> parameters = new HashMap<>();
parameters.put("x", 500);
parameters.put("y", 1000);

ActionRequest request = new ActionRequest("ADB_TAP", parameters);

// Execute action
CompletableFuture<ExecutionResult> result = actionExecutor.executeAction(request);

// Handle result
result.thenAccept(executionResult -> {
    if (executionResult.isSuccess()) {
        Log.i("Action", "Tap executed successfully");
    } else {
        Log.e("Action", "Tap failed: " + executionResult.getError());
    }
});
```

## Detailed Integration

### Method Configuration

#### JSON Configuration
Create `method-config.json` in your assets folder:

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

#### Programmatic Configuration
```java
// Create custom method configuration
MethodConfig config = new MethodConfig();
config.setEnabled(true);
config.setPriority(1000);
config.setTimeout(30000);
config.setRetryCount(2);
config.setFallbackMethods(Arrays.asList("shizuku", "root", "accessibility"));

// Apply configuration
actionExecutor.configureMethod("shizuku_adb", config);
```

### Permission Handling

#### Check Permissions
```java
PermissionManager permissionManager = new PermissionManager(context);

// Check specific permission
boolean hasPermission = permissionManager.isPermissionGranted(
    "moe.shizuku.manager.permission.API_V23"
);

// Check multiple permissions
List<String> permissions = Arrays.asList(
    "moe.shizuku.manager.permission.API_V23",
    "android.permission.WRITE_SECURE_SETTINGS"
);
Map<String, Boolean> results = permissionManager.checkPermissions(permissions);
```

#### Request Permissions
```java
// Request Shizuku permission
if (!permissionManager.checkShizukuPermission()) {
    permissionManager.requestShizukuPermission();
}

// Handle permission result in your activity
@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    permissionManager.handlePermissionResult(requestCode, permissions, grantResults);
}
```

### Custom Fallback Strategies

#### Implement Custom Strategy
```java
public class CustomFallbackStrategy implements FallbackStrategy {
    @Override
    public String getName() {
        return "Custom";
    }
    
    @Override
    public boolean shouldRetry() {
        return true;
    }
    
    @Override
    public ExecutionResult retry(ExecutionMethod method, ActionRequest request) {
        // Custom retry logic
        Logger.d("CustomFallback", "Attempting custom fallback");
        
        // Modify request parameters
        Map<String, Object> newParams = new HashMap<>(request.getParameters());
        newParams.put("custom_param", "custom_value");
        
        ActionRequest newRequest = new ActionRequest(request.getActionType(), newParams);
        return method.execute(newRequest);
    }
    
    @Override
    public long getRetryDelay() {
        return 3000; // 3 seconds
    }
    
    @Override
    public int getMaxRetries() {
        return 2;
    }
}
```

#### Register Custom Strategy
```java
FallbackChain fallbackChain = new FallbackChain();
fallbackChain.addStrategy("custom_error", new CustomFallbackStrategy());

// Apply to executor
actionExecutor.setFallbackChain(fallbackChain);
```

### Custom Execution Methods

#### Implement Custom Method
```java
public class CustomMethod implements ExecutionMethod {
    private final Context context;
    private final Set<String> supportedActions;
    
    public CustomMethod(Context context) {
        this.context = context;
        this.supportedActions = initializeSupportedActions();
    }
    
    @Override
    public String getName() {
        return "Custom";
    }
    
    @Override
    public int getPriority() {
        return 500; // Custom priority
    }
    
    @Override
    public boolean isAvailable() {
        // Check if custom method is available
        return checkCustomAvailability();
    }
    
    @Override
    public boolean canHandle(ActionRequest request) {
        return isAvailable() && supportedActions.contains(request.getActionType());
    }
    
    @Override
    public ExecutionResult execute(ActionRequest request) {
        try {
            // Custom execution logic
            String result = performCustomAction(request);
            
            return ExecutionResult.success()
                    .message("Custom action completed")
                    .data("result", result)
                    .methodUsed(getName());
                    
        } catch (Exception e) {
            return ExecutionResult.failure("Custom action failed: " + e.getMessage(),
                    ActionExecutor.ErrorCode.EXECUTION_FAILED);
        }
    }
    
    @Override
    public void refreshAvailability() {
        // Refresh custom method availability
    }
    
    @Override
    public Set<String> getSupportedActions() {
        return new HashSet<>(supportedActions);
    }
    
    @Override
    public Set<String> getRequiredPermissions() {
        Set<String> permissions = new HashSet<>();
        permissions.add("android.permission.CUSTOM_PERMISSION");
        return permissions;
    }
    
    @Override
    public String getDescription() {
        return "Custom execution method";
    }
    
    @Override
    public Map<String, String> getLimitations() {
        Map<String, String> limitations = new HashMap<>();
        limitations.put("custom_limitation", "Custom method limitation");
        return limitations;
    }
}
```

#### Register Custom Method
```java
// Register custom method
CustomMethod customMethod = new CustomMethod(context);
actionExecutor.registerMethod("custom", customMethod);
```

## Advanced Features

### Asynchronous Execution with Timeout

```java
// Execute with custom timeout (10 seconds)
CompletableFuture<ExecutionResult> result = actionExecutor.executeAction(request, 10000);

// Handle timeout
result.exceptionally(throwable -> {
    if (throwable instanceof TimeoutException) {
        Log.e("Action", "Action timed out");
    }
    return ExecutionResult.failure("Execution failed", ActionExecutor.ErrorCode.TIMEOUT);
});
```

### Batch Execution

```java
// Create batch of actions
List<ActionRequest> batch = Arrays.asList(
    new ActionRequest("ADB_TAP", tapParams1),
    new ActionRequest("ADB_TAP", tapParams2),
    new ActionRequest("ADB_SWIPE", swipeParams)
);

// Execute batch
List<CompletableFuture<ExecutionResult>> results = batch.stream()
    .map(request -> actionExecutor.executeAction(request))
    .collect(Collectors.toList());

// Wait for all to complete
CompletableFuture<Void> allResults = CompletableFuture.allOf(
    results.toArray(new CompletableFuture[0])
);
```

### Method Availability Monitoring

```java
// Get available methods
List<ExecutionMethod> availableMethods = actionExecutor.getAvailableMethods();

// Monitor method availability
actionExecutor.setAvailabilityListener(new MethodAvailabilityListener() {
    @Override
    public void onMethodAvailable(String methodName) {
        Log.i("Met
