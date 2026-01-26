package com.android.automation.core;

import com.android.automation.methods.*;
import com.android.automation.fallback.*;
import com.android.automation.utils.*;
import java.util.*;
import java.util.concurrent.*;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * Central action executor that manages multiple execution methods with fallback chain
 */
public class ActionExecutor {
    private static final String TAG = "ActionExecutor";
    
    private final Context context;
    private final FallbackChain fallbackChain;
    private final PermissionManager permissionManager;
    private final MethodDetector methodDetector;
    private final Handler mainHandler;
    private final ExecutorService executorService;
    
    // Available execution methods
    private final Map<MethodPriority, ExecutionMethod> executionMethods;
    private final List<ExecutionMethod> orderedMethods;
    
    public ActionExecutor(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newCachedThreadPool();
        
        // Initialize components
        this.permissionManager = new PermissionManager(context);
        this.methodDetector = new MethodDetector(context);
        this.fallbackChain = new FallbackChain();
        
        // Initialize execution methods
        this.executionMethods = initializeMethods();
        this.orderedMethods = sortMethodsByPriority();
        
        Logger.i(TAG, "ActionExecutor initialized with " + orderedMethods.size() + " methods");
    }
    
    /**
     * Execute an action using the best available method with fallback chain
     */
    public CompletableFuture<ExecutionResult> executeAction(ActionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Logger.i(TAG, "Executing action: " + request.getActionType());
                
                // Try each method in priority order
                for (ExecutionMethod method : orderedMethods) {
                    if (method.isAvailable() && method.canHandle(request)) {
                        Logger.d(TAG, "Trying method: " + method.getName());
                        
                        ExecutionResult result = method.execute(request);
                        
                        if (result.isSuccess()) {
                            Logger.i(TAG, "Action succeeded with method: " + method.getName());
                            return result;
                        } else {
                            Logger.w(TAG, "Method " + method.getName() + " failed: " + result.getError());
                            
                            // Apply fallback strategy
                            FallbackStrategy strategy = fallbackChain.getStrategy(result.getError());
                            if (strategy.shouldRetry()) {
                                Logger.d(TAG, "Retrying with strategy: " + strategy.getName());
                                result = strategy.retry(method, request);
                                if (result.isSuccess()) {
                                    return result;
                                }
                            }
                        }
                    }
                }
                
                // All methods failed
                Logger.e(TAG, "All execution methods failed for action: " + request.getActionType());
                return ExecutionResult.failure("All methods failed", ErrorCode.ALL_METHODS_FAILED);
                
            } catch (Exception e) {
                Logger.e(TAG, "Exception during action execution", e);
                return ExecutionResult.failure("Execution exception: " + e.getMessage(), ErrorCode.EXCEPTION);
            }
        }, executorService);
    }
    
    /**
     * Execute action with timeout
     */
    public CompletableFuture<ExecutionResult> executeAction(ActionRequest request, long timeoutMs) {
        CompletableFuture<ExecutionResult> future = executeAction(request);
        
        return future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        Logger.e(TAG, "Action execution timed out");
                        return ExecutionResult.failure("Execution timeout", ErrorCode.TIMEOUT);
                    }
                    return ExecutionResult.failure("Execution failed: " + throwable.getMessage(), 
                            ErrorCode.EXCEPTION);
                });
    }
    
    /**
     * Get available execution methods
     */
    public List<ExecutionMethod> getAvailableMethods() {
        List<ExecutionMethod> available = new ArrayList<>();
        for (ExecutionMethod method : orderedMethods) {
            if (method.isAvailable()) {
                available.add(method);
            }
        }
        return available;
    }
    
    /**
     * Get method by priority
     */
    public ExecutionMethod getMethod(MethodPriority priority) {
        return executionMethods.get(priority);
    }
    
    /**
     * Refresh method availability
     */
    public void refreshMethods() {
        Logger.d(TAG, "Refreshing method availability");
        for (ExecutionMethod method : executionMethods.values()) {
            method.refreshAvailability();
        }
        orderedMethods.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }
    
    /**
     * Initialize execution methods
     */
    private Map<MethodPriority, ExecutionMethod> initializeMethods() {
        Map<MethodPriority, ExecutionMethod> methods = new HashMap<>();
        
        // Shizuku ADB method (highest priority)
        methods.put(MethodPriority.SHIZUKU_ADB, new ShizukuADBMethod(context));
        
        // Shizuku method
        methods.put(MethodPriority.SHIZUKU, new ShizukuMethod(context));
        
        // Root method
        methods.put(MethodPriority.ROOT, new RootMethod(context));
        
        // Accessibility service method (lowest priority)
        methods.put(MethodPriority.ACCESSIBILITY, new AccessibilityMethod(context));
        
        return methods;
    }
    
    /**
     * Sort methods by priority (highest first)
     */
    private List<ExecutionMethod> sortMethodsByPriority() {
        List<ExecutionMethod> sorted = new ArrayList<>(executionMethods.values());
        sorted.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        return sorted;
    }
    
    /**
     * Shutdown the executor
     */
    public void shutdown() {
        Logger.d(TAG, "Shutting down ActionExecutor");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Action request class
     */
    public static class ActionRequest {
        private final String actionType;
        private final Map<String, Object> parameters;
        private final long timestamp;
        
        public ActionRequest(String actionType, Map<String, Object> parameters) {
            this.actionType = actionType;
            this.parameters = new HashMap<>(parameters);
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getActionType() { return actionType; }
        public Map<String, Object> getParameters() { return parameters; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Error codes
     */
    public enum ErrorCode {
        SUCCESS,
        PERMISSION_DENIED,
        METHOD_NOT_AVAILABLE,
        EXECUTION_FAILED,
        TIMEOUT,
        EXCEPTION,
        ALL_METHODS_FAILED
    }
}
