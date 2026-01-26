package com.android.automation.fallback;

import com.android.automation.core.*;
import java.util.*;

/**
 * Fallback chain for handling execution failures
 */
public class FallbackChain {
    private static final String TAG = "FallbackChain";
    
    private final Map<String, FallbackStrategy> strategies;
    private final List<ErrorRecovery> recoverySteps;
    
    public FallbackChain() {
        this.strategies = initializeStrategies();
        this.recoverySteps = initializeRecoverySteps();
    }
    
    /**
     * Get fallback strategy for specific error
     */
    public FallbackStrategy getStrategy(String error) {
        // Find matching strategy based on error pattern
        for (Map.Entry<String, FallbackStrategy> entry : strategies.entrySet()) {
            if (error != null && error.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        
        // Default strategy
        return new DefaultFallbackStrategy();
    }
    
    /**
     * Get recovery steps for error
     */
    public List<ErrorRecovery> getRecoverySteps(String error) {
        List<ErrorRecovery> applicableSteps = new ArrayList<>();
        
        for (ErrorRecovery step : recoverySteps) {
            if (step.appliesTo(error)) {
                applicableSteps.add(step);
            }
        }
        
        return applicableSteps;
    }
    
    /**
     * Execute recovery steps
     */
    public boolean executeRecovery(String error) {
        List<ErrorRecovery> steps = getRecoverySteps(error);
        
        for (ErrorRecovery step : steps) {
            try {
                Logger.d(TAG, "Executing recovery step: " + step.getName());
                if (step.execute()) {
                    Logger.d(TAG, "Recovery step succeeded: " + step.getName());
                    return true;
                }
            } catch (Exception e) {
                Logger.e(TAG, "Recovery step failed: " + step.getName(), e);
            }
        }
        
        return false;
    }
    
    /**
     * Initialize fallback strategies
     */
    private Map<String, FallbackStrategy> initializeStrategies() {
        Map<String, FallbackStrategy> strategies = new HashMap<>();
        
        // Permission-related errors
        strategies.put("permission", new PermissionFallbackStrategy());
        strategies.put("denied", new PermissionFallbackStrategy());
        strategies.put("not granted", new PermissionFallbackStrategy());
        
        // Service availability errors
        strategies.put("not available", new ServiceAvailabilityFallbackStrategy());
        strategies.put("service not connected", new ServiceAvailabilityFallbackStrategy());
        strategies.put("binder died", new ServiceAvailabilityFallbackStrategy());
        
        // Timeout errors
        strategies.put("timeout", new TimeoutFallbackStrategy());
        strategies.put("timed out", new TimeoutFallbackStrategy());
        
        // Connection errors
        strategies.put("connection", new ConnectionFallbackStrategy());
        strategies.put("network", new ConnectionFallbackStrategy());
        strategies.put("unreachable", new ConnectionFallbackStrategy());
        
        // Resource errors
        strategies.put("resource", new ResourceFallbackStrategy());
        strategies.put("memory", new ResourceFallbackStrategy());
        strategies.put("space", new ResourceFallbackStrategy());
        
        // Command execution errors
        strategies.put("command failed", new CommandFallbackStrategy());
        strategies.put("exit code", new CommandFallbackStrategy());
        strategies.put("not found", new CommandFallbackStrategy());
        
        return strategies;
    }
    
    /**
     * Initialize recovery steps
     */
    private List<ErrorRecovery> initializeRecoverySteps() {
        List<ErrorRecovery> steps = new ArrayList<>();
        
        // General recovery steps
        steps.add(new WaitRecoveryStep());
        steps.add(new RefreshServiceRecoveryStep());
        steps.add(new ClearCacheRecoveryStep());
        steps.add(new RestartServiceRecoveryStep());
        steps.add(new ReconnectRecoveryStep());
        
        return steps;
    }
    
    /**
     * Default fallback strategy
     */
    private static class DefaultFallbackStrategy implements FallbackStrategy {
        @Override
        public String getName() {
            return "Default";
        }
        
        @Override
        public boolean shouldRetry() {
            return false; // Don't retry by default
        }
        
        @Override
        public ExecutionResult retry(ExecutionMethod method, ActionExecutor.ActionRequest request) {
            // Don't retry, just return failure
            return ExecutionResult.failure("No fallback strategy available", 
                    ActionExecutor.ErrorCode.EXECUTION_FAILED);
        }
        
        @Override
        public long getRetryDelay() {
            return 0;
        }
        
        @Override
        public int getMaxRetries() {
            return 0;
        }
    }
    
    /**
     * Permission fallback strategy
     */
    private static class PermissionFallbackStrategy implements FallbackStrategy {
        private static final int MAX_RETRIES = 2;
        private static final long RETRY_DELAY = 2000; // 2 seconds
        
        @Override
        public String getName() {
            return "Permission";
        }
        
        @Override
        public boolean shouldRetry() {
            return true;
        }
        
        @Override
        public ExecutionResult retry(ExecutionMethod method, ActionExecutor.ActionRequest request) {
            // Try to request permissions or use alternative method
            Logger.d(TAG, "Attempting permission fallback strategy");
            
            // This could attempt to request permissions or use a different method
            // For now, just return a specific error
            return ExecutionResult.failure("Permission denied - try granting required permissions",
                    ActionExecutor.ErrorCode.PERMISSION_DENIED);
        }
        
        @Override
        public long getRetryDelay() {
            return RETRY_DELAY;
        }
        
        @Override
        public int getMaxRetries() {
            return MAX_RETRIES;
        }
    }
    
    /**
     * Service availability fallback strategy
     */
    private static class ServiceAvailabilityFallbackStrategy implements FallbackStrategy {
        private static final int MAX_RETRIES = 3;
        private static final long RETRY_DELAY = 1000; // 1 second
        
        @Override
        public String getName() {
            return "ServiceAvailability";
        }
        
        @Override
        public boolean shouldRetry() {
            return true;
        }
        
        @Override
        public ExecutionResult retry(ExecutionMethod method, ActionExecutor.ActionRequest request) {
            Logger.d(TAG, "Attempting service availability fallback strategy");
            
            // Refresh method availability and retry
            method.refreshAvailability();
            
            if (method.isAvailable()) {
                return method.execute(request);
            }
            
            return ExecutionResult.failure("Service still not available after refresh",
                    ActionExecutor.ErrorCode.METHOD_NOT_AVAILABLE);
        }
        
        @Override
        public long getRetryDelay() {
            return RETRY_DELAY;
        }
        
        @Override
        public int getMaxRetries() {
            return MAX_RETRIES;
        }
    }
    
    /**
     * Timeout fallback strategy
     */
    private static class TimeoutFallbackStrategy implements FallbackStrategy {
        private static final int MAX_RETRIES = 2;
        private static final long RETRY_DELAY = 5000; // 5 seconds
        
        @Override
        public String getName() {
            return "Timeout";
  
