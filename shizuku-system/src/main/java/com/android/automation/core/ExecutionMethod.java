package com.android.automation.core;

import java.util.Map;
import java.util.Set;

/**
 * Base interface for all execution methods
 */
public interface ExecutionMethod {
    
    /**
     * Get method name
     */
    String getName();
    
    /**
     * Get method priority (higher is better)
     */
    int getPriority();
    
    /**
     * Check if method is currently available
     */
    boolean isAvailable();
    
    /**
     * Check if method can handle specific action type
     */
    boolean canHandle(ActionExecutor.ActionRequest request);
    
    /**
     * Execute the action
     */
    ExecutionResult execute(ActionExecutor.ActionRequest request);
    
    /**
     * Refresh method availability status
     */
    void refreshAvailability();
    
    /**
     * Get supported action types
     */
    Set<String> getSupportedActions();
    
    /**
     * Get required permissions
     */
    Set<String> getRequiredPermissions();
    
    /**
     * Get method description
     */
    String getDescription();
    
    /**
     * Get method limitations
     */
    Map<String, String> getLimitations();
}
