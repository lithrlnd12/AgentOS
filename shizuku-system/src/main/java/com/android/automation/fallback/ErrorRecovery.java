package com.android.automation.fallback;

/**
 * Error recovery step interface
 */
public interface ErrorRecovery {
    
    /**
     * Get recovery step name
     */
    String getName();
    
    /**
     * Check if this recovery step applies to the error
     */
    boolean appliesTo(String error);
    
    /**
     * Execute recovery step
     */
    boolean execute();
    
    /**
     * Get recovery step priority (higher = executed first)
     */
    int getPriority();
    
    /**
     * Check if recovery step can be retried
     */
    default boolean canRetry() {
        return true;
    }
    
    /**
     * Get maximum number of retries
     */
    default int getMaxRetries() {
        return 1;
    }
}

/**
 * Wait recovery step - wait for a short period
 */
class WaitRecoveryStep implements ErrorRecovery {
    private static final long WAIT_TIME = 1000; // 1 second
    
    @Override
    public String getName() {
        return "Wait";
    }
    
    @Override
    public boolean appliesTo(String error) {
        return error != null && (
            error.toLowerCase().contains("timeout") ||
            error.toLowerCase().contains("busy") ||
            error.toLowerCase().contains("temporary")
        );
    }
    
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
    
    @Override
    public int getPriority() {
        return 100;
    }
}

/**
 * Refresh service recovery step - refresh service availability
 */
class RefreshServiceRecoveryStep implements ErrorRecovery {
    @Override
    public String getName() {
        return "RefreshService";
    }
    
    @Override
    public boolean appliesTo(String error) {
        return error != null && (
            error.toLowerCase().contains("service") ||
            error.toLowerCase().contains("binder") ||
            error.toLowerCase().contains("disconnected")
        );
    }
    
    @Override
    public boolean execute() {
        // This would refresh service availability
        // Implementation would depend on specific services
        return true;
    }
    
    @Override
    public int getPriority() {
        return 90;
    }
}

/**
 * Clear cache recovery step - clear system caches
 */
class ClearCacheRecoveryStep implements ErrorRecovery {
    @Override
    public String getName() {
        return "ClearCache";
    }
    
    @Override
    public boolean appliesTo(String error) {
        return error != null && (
            error.toLowerCase().contains("memory") ||
            error.toLowerCase().contains("cache") ||
            error.toLowerCase().contains("resource")
        );
    }
    
    @Override
    public boolean execute() {
        try {
            // Clear system caches
            Runtime.getRuntime().gc();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public int getPriority() {
        return 80;
    }
}

/**
 * Restart service recovery step - restart system services
 */
class RestartServiceRecoveryStep implements ErrorRecovery {
    @Override
    public String getName() {
        return "RestartService";
    }
    
    @Override
    public boolean appliesTo(String error) {
        return error != null && (
            error.toLowerCase().contains("service") ||
            error.toLowerCase().contains("restart") ||
            error.toLowerCase().contains("crashed")
        );
    }
    
    @Override
    public boolean execute() {
        // This would restart system services
        // Implementation would depend on specific services and permissions
        return true;
    }
    
    @Override
    public int getPriority() {
        return 70;
    }
}

/**
 * Reconnect recovery step - attempt to reconnect
 */
class ReconnectRecoveryStep implements ErrorRecovery {
    @Override
    public String getName() {
        return "Reconnect";
    }
    
    @Override
    public boolean appliesTo(String error) {
        return error != null && (
            error.toLowerCase().contains("connection") ||
            error.toLowerCase().contains("network") ||
            error.toLowerCase().contains("timeout")
        );
    }
    
    @Override
    public boolean execute() {
        // This would attempt to reconnect
        // Implementation would depend on specific connection type
        return true;
    }
    
    @Override
    public int getPriority() {
        return 60;
    }
}
