package com.android.automation.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;

import java.util.*;

/**
 * Permission management utility
 */
public class PermissionManager {
    private static final String TAG = "PermissionManager";
    
    private final Context context;
    private final Map<String, PermissionStatus> permissionCache;
    
    public PermissionManager(Context context) {
        this.context = context;
        this.permissionCache = new HashMap<>();
    }
    
    /**
     * Check if permission is granted
     */
    public boolean isPermissionGranted(String permission) {
        // Check cache first
        if (permissionCache.containsKey(permission)) {
            PermissionStatus status = permissionCache.get(permission);
            if (status.isValid()) {
                return status.isGranted();
            }
        }
        
        // Check actual permission
        boolean granted = ContextCompat.checkSelfPermission(context, permission) == 
                         PackageManager.PERMISSION_GRANTED;
        
        // Update cache
        permissionCache.put(permission, new PermissionStatus(granted));
        
        return granted;
    }
    
    /**
     * Check multiple permissions
     */
    public Map<String, Boolean> checkPermissions(List<String> permissions) {
        Map<String, Boolean> results = new HashMap<>();
        
        for (String permission : permissions) {
            results.put(permission, isPermissionGranted(permission));
        }
        
        return results;
    }
    
    /**
     * Get missing permissions
     */
    public List<String> getMissingPermissions(List<String> permissions) {
        List<String> missing = new ArrayList<>();
        
        for (String permission : permissions) {
            if (!isPermissionGranted(permission)) {
                missing.add(permission);
            }
        }
        
        return missing;
    }
    
    /**
     * Request permissions
     */
    public void requestPermissions(String[] permissions, int requestCode) {
        // This would typically be called from an Activity
        // Implementation depends on how permissions are requested in the app
        Logger.d(TAG, "Requesting permissions: " + Arrays.toString(permissions));
    }
    
    /**
     * Handle permission result
     */
    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            permissionCache.put(permissions[i], new PermissionStatus(granted));
            
            Logger.d(TAG, "Permission " + permissions[i] + " " + (granted ? "granted" : "denied"));
        }
    }
    
    /**
     * Clear permission cache
     */
    public void clearCache() {
        permissionCache.clear();
    }
    
    /**
     * Check Shizuku permission
     */
    public boolean checkShizukuPermission() {
        try {
            return rikka.shizuku.Shizuku.checkSelfPermission() == rikka.shizuku.Shizuku.PERMISSION_GRANTED;
        } catch (Exception e) {
            Logger.e(TAG, "Failed to check Shizuku permission", e);
            return false;
        }
    }
    
    /**
     * Request Shizuku permission
     */
    public void requestShizukuPermission() {
        try {
            rikka.shizuku.Shizuku.requestPermission(0);
        } catch (Exception e) {
            Logger.e(TAG, "Failed to request Shizuku permission", e);
        }
    }
    
    /**
     * Check root permission
     */
    public boolean checkRootPermission() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.getOutputStream().write("exit\n".getBytes());
            process.getOutputStream().flush();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            Logger.e(TAG, "Root permission check failed", e);
            return false;
        }
    }
    
    /**
     * Permission status class
     */
    private static class PermissionStatus {
        private final boolean granted;
        private final long timestamp;
        private static final long VALIDITY_PERIOD = 60000; // 1 minute
        
        public PermissionStatus(boolean granted) {
            this.granted = granted;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isGranted() {
            return granted;
        }
        
        public boolean isValid() {
            return System.currentTimeMillis() - timestamp < VALIDITY_PERIOD;
        }
    }
}
