package com.android.automation.utils;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import java.util.*;

/**
 * Method availability detection utility
 */
public class MethodDetector {
    private static final String TAG = "MethodDetector";
    
    private final Context context;
    private final Map<String, MethodInfo> methodCache;
    
    public MethodDetector(Context context) {
        this.context = context;
        this.methodCache = new HashMap<>();
    }
    
    /**
     * Detect Shizuku availability
     */
    public boolean detectShizuku() {
        try {
            // Check if Shizuku is installed
            boolean shizukuInstalled = isPackageInstalled("moe.shizuku.privileged.api");
            if (!shizukuInstalled) {
                Logger.d(TAG, "Shizuku not installed");
                return false;
            }
            
            // Check if Shizuku is running
            boolean shizukuRunning = rikka.shizuku.Shizuku.pingBinder();
            Logger.d(TAG, "Shizuku running: " + shizukuRunning);
            
            return shizukuRunning;
            
        } catch (Exception e) {
            Logger.e(TAG, "Shizuku detection failed", e);
            return false;
        }
    }
    
    /**
     * Detect Shizuku ADB availability
     */
    public boolean detectShizukuADB() {
        try {
            // First check basic Shizuku availability
            if (!detectShizuku()) {
                return false;
            }
            
            // Check Shizuku permission
            if (rikka.shizuku.Shizuku.checkSelfPermission() != rikka.shizuku.Shizuku.PERMISSION_GRANTED) {
                Logger.d(TAG, "Shizuku permission not granted");
                return false;
            }
            
            // Check if we can execute shell commands
            String result = executeShizukuCommand("echo test");
            boolean canExecute = "test".equals(result.trim());
            Logger.d(TAG, "Shizuku ADB available: " + canExecute);
            
            return canExecute;
            
        } catch (Exception e) {
            Logger.e(TAG, "Shizuku ADB detection failed", e);
            return false;
        }
    }
    
    /**
     * Detect root availability
     */
    public boolean detectRoot() {
        try {
            // Check for common root indicators
            boolean hasSuBinary = checkSuBinary();
            boolean hasRootPackages = checkRootPackages();
            boolean canExecuteSu = testSuExecution();
            
            boolean isRooted = hasSuBinary || hasRootPackages || canExecuteSu;
            Logger.d(TAG, "Root detected: " + isRooted);
            
            return isRooted;
            
        } catch (Exception e) {
            Logger.e(TAG, "Root detection failed", e);
            return false;
        }
    }
    
    /**
     * Detect accessibility service availability
     */
    public boolean detectAccessibility() {
        try {
            // Check if accessibility is enabled
            int accessibilityEnabled = Settings.Secure.getInt(
                context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            );
            
            boolean isEnabled = accessibilityEnabled == 1;
            Logger.d(TAG, "Accessibility enabled: " + isEnabled);
            
            return isEnabled;
            
        } catch (Exception e) {
            Logger.e(TAG, "Accessibility detection failed", e);
            return false;
        }
    }
    
    /**
     * Detect specific accessibility service
     */
    public boolean detectAccessibilityService(String serviceName) {
        try {
            String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            
            if (enabledServices != null) {
                boolean containsService = enabledServices.contains(serviceName);
                Logger.d(TAG, "Accessibility service " + serviceName + " enabled: " + containsService);
                return containsService;
            }
            
            return false;
            
        } catch (Exception e) {
            Logger.e(TAG, "Accessibility service detection failed", e);
            return false;
        }
    }
    
    /**
     * Get all available methods
     */
    public Map<String, Boolean> getAllMethods() {
        Map<String, Boolean> methods = new HashMap<>();
        
        methods.put("shizuku_adb", detectShizukuADB());
        methods.put("shizuku", detectShizuku());
        methods.put("root", detectRoot());
        methods.put("accessibility", detectAccessibility());
        
        return methods;
    }
    
    /**
     * Get method info
     */
    public MethodInfo getMethodInfo(String method) {
        if (methodCache.containsKey(method)) {
            MethodInfo cached = methodCache.get(method);
            if (cached.isValid()) {
                return cached;
            }
        }
        
        boolean available = false;
        String description = "";
        
        switch (method) {
            case "shizuku_adb":
                available = detectShizukuADB();
                description = "Shizuku with ADB shell access";
                break;
            case "shizuku":
                available = detectShizuku();
                description = "Shizuku framework";
                break;
            case "root":
                available = detectRoot();
                description = "Root access";
                break;
            case "accessibility":
                available = detectAccessibility();
                description = "Accessibility service";
                break;
        }
        
        MethodInfo info = new MethodInfo(method, available, description);
        methodCache.put(method, info);
        
        return info;
    }
    
    /**
     * Check if package is installed
     */
    private boolean isPackageInstalled(String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Execute command via Shizuku
     */
    private String executeShizukuCommand(String command) {
        try {
            android.os.ParcelFileDescriptor[] pipes = android.os.ParcelFileDescriptor.createPipe();
            
            int result = rikka.shizuku.Shizuku.newProcess(
                new String[]{"sh", "-c", command},
                null,
                null,
                pipes[1]
            ).waitFor();
            
            if (result != 0) {
                return "";
            }
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new android.os.ParcelFileDescriptor.AutoCloseInputStream(pipes[0])))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            return output.toString().trim();
            
        } catch (Exception e) {
            Logger.e(TAG, "Shizuku command execution failed", e);
            return "";
        }
    }
    
    /**
     * Check for su binary
     */
    private boolean checkSuBinary() {
        String[] paths = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su"
        };
        
        for (String path : paths) {
            if (new java.io.File(path).exists()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check for root packages
     */
    private boolean checkRootPackages() {
        String[] 
