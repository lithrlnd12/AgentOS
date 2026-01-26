package com.android.automation.methods;

import com.android.automation.core.*;
import com.android.automation.utils.*;
import android.content.Context;
import android.os.Build;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Root method for executing commands with root privileges
 */
public class RootMethod implements ExecutionMethod {
    private static final String TAG = "RootMethod";
    
    private final Context context;
    private final Set<String> supportedActions;
    private boolean isAvailable = false;
    private String suBinaryPath = null;
    
    public RootMethod(Context context) {
        this.context = context;
        this.supportedActions = initializeSupportedActions();
        refreshAvailability();
    }
    
    @Override
    public String getName() {
        return "Root";
    }
    
    @Override
    public int getPriority() {
        return MethodPriority.ROOT.getValue();
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
        Logger.d(TAG, "Executing root action: " + request.getActionType());
        long startTime = System.currentTimeMillis();
        
        try {
            String command = buildRootCommand(request);
            if (command == null) {
                return ExecutionResult.failure("Unsupported action type",
                    ActionExecutor.ErrorCode.METHOD_NOT_AVAILABLE);
            }
            
            return executeRootCommand(command, request, startTime);
            
        } catch (Exception e) {
            Logger.e(TAG, "Root execution failed", e);
            return ExecutionResult.failure("Root execution failed: " + e.getMessage(),
                    ActionExecutor.ErrorCode.EXECUTION_FAILED)
                    .executionTime(System.currentTimeMillis() - startTime);
        }
    }
    
    @Override
    public void refreshAvailability() {
        isAvailable = checkRootAvailability();
        Logger.d(TAG, "Root availability: " + isAvailable);
        
        if (isAvailable) {
            Logger.d(TAG, "SU binary path: " + suBinaryPath);
        }
    }
    
    @Override
    public Set<String> getSupportedActions() {
        return new HashSet<>(supportedActions);
    }
    
    @Override
    public Set<String> getRequiredPermissions() {
        Set<String> permissions = new HashSet<>();
        permissions.add("android.permission.ACCESS_SUPERUSER");
        return permissions;
    }
    
    @Override
    public String getDescription() {
        return "Root-based command execution";
    }
    
    @Override
    public Map<String, String> getLimitations() {
        Map<String, String> limitations = new HashMap<>();
        limitations.put("requires_root", "Requires device to be rooted");
        limitations.put("su_binary", "Requires working su binary");
        limitations.put("security", "May be blocked by security policies");
        limitations.put("selinux", "SELinux may restrict some operations");
        return limitations;
    }
    
    /**
     * Check if root is available
     */
    private boolean checkRootAvailability() {
        try {
            // Check for common su binary locations
            String[] suPaths = {
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/su/bin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su"
            };
            
            for (String path : suPaths) {
                if (checkSuBinary(path)) {
                    suBinaryPath = path;
                    return true;
                }
            }
            
            // Try to execute su command
            return testSuCommand();
            
        } catch (Exception e) {
            Logger.e(TAG, "Root availability check failed", e);
            return false;
        }
    }
    
    /**
     * Check if su binary exists and is executable
     */
    private boolean checkSuBinary(String path) {
        try {
            File suFile = new File(path);
            return suFile.exists() && suFile.canExecute();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Test su command execution
     */
    private boolean testSuCommand() {
        try {
            String result = executeRootCommand("id", 5000);
            return result != null && result.contains("uid=0");
        } catch (Exception e) {
            Logger.e(TAG, "SU command test failed", e);
            return false;
        }
    }
    
    /**
     * Build root command for action
     */
    private String buildRootCommand(ActionExecutor.ActionRequest request) {
        Map<String, Object> params = request.getParameters();
        
        switch (request.getActionType()) {
            case "ROOT_SHELL":
                return (String) params.get("command");
                
            case "ROOT_FILE_READ":
                String filePath = (String) params.get("filePath");
                return String.format("cat \"%s\"", filePath);
                
            case "ROOT_FILE_WRITE":
                String writeFilePath = (String) params.get("filePath");
                String content = (String) params.get("content");
                return String.format("echo \"%s\" \u003e \"%s\"", escapeShellText(content), writeFilePath);
                
            case "ROOT_FILE_DELETE":
                String deleteFilePath = (String) params.get("filePath");
                return String.format("rm -f \"%s\"", deleteFilePath);
                
            case "ROOT_FILE_CHMOD":
                String chmodFilePath = (String) params.get("filePath");
                String permissions = (String) params.get("permissions");
                return String.format("chmod %s \"%s\"", permissions, chmodFilePath);
                
            case "ROOT_FILE_CHOWN":
                String chownFilePath = (String) params.get("filePath");
                String owner = (String) params.get("owner");
                return String.format("chown %s \"%s\"", owner, chownFilePath);
                
            case "ROOT_MOUNT_RW":
                String mountPoint = (String) params.get("mountPoint");
                return String.format("mount -o remount,rw %s", mountPoint);
                
            case "ROOT_MOUNT_RO":
                String roMountPoint = (String) params.get("mountPoint");
                return String.format("mount -o remount,ro %s", roMountPoint);
                
            case "ROOT_SET_PROP":
                String propName = (String) params.get("property");
                String propValue = (String) params.get("value");
                return String.format("setprop %s %s", propName, escapeShellText(propValue));
                
            case "ROOT_GET_PROP":
                String getPropName = (String) params.get("property");
                return String.format("getprop %s", getPropName);
                
            case "ROOT_KILL_PROCESS":
                String processName = (String) params.get("processName");
                return String.format("pkill -f \"%s\"", processName);
                
            case "ROOT_SYSTEMCTL_START":
                String startService = (String) params.get("service");
                return String.format("systemctl start %s", startService);
                
            case "ROOT_SYSTEMCTL_STOP":
                String stopService = (String) params.get("service");
                return String.format("systemctl stop %s", stopService);
                
            case "ROOT_SYSTEMCTL_RESTART":
                String restartService = (String) params.get("service");
                return String.format("systemctl restart %s", restartService);
                
        
