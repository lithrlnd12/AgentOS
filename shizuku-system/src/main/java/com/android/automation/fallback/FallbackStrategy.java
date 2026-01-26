package com.android.automation.fallback;

import com.android.automation.core.*;

/**
 * Interface for fallback strategies
 */
public interface FallbackStrategy {
    
    /**
     * Get strategy name
     */
    String getName();
    
    /**
     * Check if strategy should retry execution
     */
    boolean shouldRetry();
    
    /**
     * Retry execution with this strategy
     */
    ExecutionResult retry(ExecutionMethod method, ActionExecutor.ActionRequest request);
    
    /**
     * Get retry delay in milliseconds
     */
    long getRetryDelay();
    
    /**
     * Get maximum number of retries
     */
    int getMaxRetries();
    
    /**
     * Check if strategy applies to specific error
     */
    default boolean appliesTo(String error) {
        return true; // Apply to all errors by default
    }
}
