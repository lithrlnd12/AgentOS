package com.android.automation.utils;

import android.util.Log;

/**
 * Centralized logging utility
 */
public class Logger {
    private static final String TAG_PREFIX = "ShizukuSystem";
    private static boolean DEBUG = true;
    private static boolean INFO = true;
    private static boolean WARN = true;
    private static boolean ERROR = true;
    
    /**
     * Set log levels
     */
    public static void setLogLevel(boolean debug, boolean info, boolean warn, boolean error) {
        DEBUG = debug;
        INFO = info;
        WARN = warn;
        ERROR = error;
    }
    
    /**
     * Debug log
     */
    public static void d(String tag, String message) {
        if (DEBUG) {
            Log.d(TAG_PREFIX + "_" + tag, message);
        }
    }
    
    /**
     * Debug log with exception
     */
    public static void d(String tag, String message, Throwable throwable) {
        if (DEBUG) {
            Log.d(TAG_PREFIX + "_" + tag, message, throwable);
        }
    }
    
    /**
     * Info log
     */
    public static void i(String tag, String message) {
        if (INFO) {
            Log.i(TAG_PREFIX + "_" + tag, message);
        }
    }
    
    /**
     * Info log with exception
     */
    public static void i(String tag, String message, Throwable throwable) {
        if (INFO) {
            Log.i(TAG_PREFIX + "_" + tag, message, throwable);
        }
    }
    
    /**
     * Warning log
     */
    public static void w(String tag, String message) {
        if (WARN) {
            Log.w(TAG_PREFIX + "_" + tag, message);
        }
    }
    
    /**
     * Warning log with exception
     */
    public static void w(String tag, String message, Throwable throwable) {
        if (WARN) {
            Log.w(TAG_PREFIX + "_" + tag, message, throwable);
        }
    }
    
    /**
     * Error log
     */
    public static void e(String tag, String message) {
        if (ERROR) {
            Log.e(TAG_PREFIX + "_" + tag, message);
        }
    }
    
    /**
     * Error log with exception
     */
    public static void e(String tag, String message, Throwable throwable) {
        if (ERROR) {
            Log.e(TAG_PREFIX + "_" + tag, message, throwable);
        }
    }
    
    /**
     * Log method execution result
     */
    public static void logExecutionResult(String method, String action, boolean success, long duration, String error) {
        String message = String.format("Method: %s, Action: %s, Success: %s, Duration: %dms, Error: %s",
                method, action, success, duration, error != null ? error : "none");
        
        if (success) {
            i("Execution", message);
        } else {
            e("Execution", message);
        }
    }
    
    /**
     * Log method availability
     */
    public static void logMethodAvailability(String method, boolean available, String details) {
        String message = String.format("Method: %s, Available: %s, Details: %s",
                method, available, details != null ? details : "none");
        
        i("MethodAvailability", message);
    }
    
    /**
     * Log fallback action
     */
    public static void logFallback(String fromMethod, String toMethod, String reason) {
        String message = String.format("Fallback from %s to %s, Reason: %s",
                fromMethod, toMethod, reason);
        w("Fallback", message);
    }
}
