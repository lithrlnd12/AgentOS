package com.android.automation.methods;

import com.android.automation.core.*;
import com.android.automation.utils.*;
import rikka.shizuku.Shizuku;
import android.content.Context;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Shizuku ADB method for direct ADB command execution
 */
public class ShizukuADBMethod implements ExecutionMethod {
    private static final String TAG = "ShizukuADBMethod";
    private static final Pattern ADB_VERSION_PATTERN = Pattern.compile("Android Debug Bridge version (\d+\.\d+\.\d+)");
    
    private final Context context;
    private final Set<String> supportedActions;
    private boolean isAvailable = false;
    private String adbVersion = null;
    
    public ShizukuADBMethod(Context context) {
        this.context = context;
        this.supportedActions = initializeSupportedActions();
        refreshAvailability();
    }
    
    @Override
    public String getName() {
        return "ShizukuADB";
    }
    
    @Override
    public int getPriority() {
        return MethodPriority.SHIZUKU_ADB.getValue();
    }
    
    @Override
    public boolean isAvailable() {
        return isAvailable;
    }
    
    @Override
    public boolean canHandle(ActionExecutor.ActionRequest request) {
        return isAvailable() && supportedActions.contains(request.getActionType());
    }
    
    @Override
    public ExecutionResult execute(ActionExecutor.ActionRequest request) {
        Logger.d(TAG, "Executing ADB action: " + request.getActionType());
        long startTime = System.currentTimeMillis();
        
        try {
            String adbCommand = buildADBCommand(request);
            if (adbCommand == null) {
                return ExecutionResult.failure("Unsupported action type", 
                    ActionExecutor.ErrorCode.METHOD_NOT_AVAILABLE);
            }
            
            return executeADBCommand(adbCommand, request, startTime);
            
        } catch (Exception e) {
            Logger.e(TAG, "ADB execution failed", e);
            return ExecutionResult.failure("ADB execution failed: " + e.getMessage(),
                    ActionExecutor.ErrorCode.EXECUTION_FAILED)
                    .executionTime(System.currentTimeMillis() - startTime);
        }
    }
    
    @Override
    public void refreshAvailability() {
        isAvailable = checkADBAvailability();
        Logger.d(TAG, "ShizukuADB availability: " + isAvailable);
        
        if (isAvailable) {
            adbVersion = getADBVersion();
            Logger.d(TAG, "ADB version: " + adbVersion);
        }
    }
    
    @Override
    public Set<String> getSupportedActions() {
        return new HashSet<>(supportedActions);
    }
    
    @Override
    public Set<String> getRequiredPermissions() {
        Set<String> permissions = new HashSet<>();
        permissions.add("moe.shizuku.manager.permission.API_V23");
        permissions.add("android.permission.WRITE_SECURE_SETTINGS");
        return permissions;
    }
    
    @Override
    public String getDescription() {
        return "Direct ADB command execution via Shizuku";
    }
    
    @Override
    public Map<String, String> getLimitations() {
        Map<String, String> limitations = new HashMap<>();
        limitations.put("requires_shizuku", "Requires Shizuku with ADB shell access");
        limitations.put("shell_commands", "Limited to shell command availability");
        limitations.put("device_security", "Some commands may be restricted by device security");
        return limitations;
    }
    
    /**
     * Check if ADB is available through Shizuku
     */
    private boolean checkADBAvailability() {
        try {
            if (!Shizuku.pingBinder()) {
                return false;
            }
            
            // Check if we have shell access
            if (Shizuku.checkSelfPermission() != Shizuku.PERMISSION_GRANTED) {
                return false;
            }
            
            // Test ADB command execution
            return executeTestCommand();
            
        } catch (Exception e) {
            Logger.e(TAG, "ADB availability check failed", e);
            return false;
        }
    }
    
    /**
     * Execute test command to verify ADB functionality
     */
    private boolean executeTestCommand() {
        try {
            String result = executeShellCommand("echo test", 5000);
            return "test".equals(result.trim());
        } catch (Exception e) {
            Logger.e(TAG, "Test command failed", e);
            return false;
        }
    }
    
    /**
     * Get ADB version
     */
    private String getADBVersion() {
        try {
            String result = executeShellCommand("adb version", 5000);
            var matcher = ADB_VERSION_PATTERN.matcher(result);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            Logger.e(TAG, "Failed to get ADB version", e);
        }
        return "unknown";
    }
    
    /**
     * Build ADB command for action
     */
    private String buildADBCommand(ActionExecutor.ActionRequest request) {
        Map<String, Object> params = request.getParameters();
        
        switch (request.getActionType()) {
            case "ADB_SHELL":
                return (String) params.get("command");
                
            case "ADB_TAP":
                int x = (int) params.get("x");
                int y = (int) params.get("y");
                return String.format("input tap %d %d", x, y);
                
            case "ADB_SWIPE":
                int x1 = (int) params.get("x1");
                int y1 = (int) params.get("y1");
                int x2 = (int) params.get("x2");
                int y2 = (int) params.get("y2");
                int duration = (int) params.getOrDefault("duration", 300);
                return String.format("input swipe %d %d %d %d %d", x1, y1, x2, y2, duration);
                
            case "ADB_TEXT":
                String text = (String) params.get("text");
                return String.format("input text \"%s\"", escapeShellText(text));
                
            case "ADB_KEYEVENT":
                int keyCode = (int) params.get("keyCode");
                return String.format("input keyevent %d", keyCode);
                
            case "ADB_PACKAGE_LIST":
                return "pm list packages";
                
            case "ADB_PACKAGE_INFO":
                String packageName = (String) params.get("packageName");
                return String.format("dumpsys package %s", packageName);
                
            case "ADB_SCREENSHOT":
                String path = (String) params.getOrDefault("path", "/sdcard/screenshot.png");
                return String.format("screencap -p %s", path);
                
            case "ADB_SCREEN_RECORD":
                String recordPath = (String) params.get("path");
                int recordTime = (int) params.getOrDefault("time", 10);
                return String.format("screenrecord --time-limit %d %s", recordTime, recordPath);
                
            case "ADB_WIFI_CONNECT":
                String ssid = (String) params.get("ssid");
                String password = (String) params.get("password");
                return String.format("cmd wifi connect-network %s %s", ssid, password);
                
            case "ADB_BLUETOOTH_ENABLE":
                return "svc bluetooth enable";
                
            case "ADB_BLUETOOTH_DISABLE":
                return "svc bluetooth disable";
                
            case "ADB_MOBILE_DATA_ENABLE":
                return "svc data enable";
                
            case "ADB_MOBILE_DATA_DISABLE":
                return "svc data disable";
                
            case "ADB_AIRPLANE_MODE_ON":
                return "settings put global airplane_mode_on 1";
                
