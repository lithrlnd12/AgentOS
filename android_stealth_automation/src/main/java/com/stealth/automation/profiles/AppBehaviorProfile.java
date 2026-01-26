package com.stealth.automation.profiles;

import com.stealth.automation.core.StealthConfig;
import java.util.HashMap;
import java.util.Map;

public abstract class AppBehaviorProfile {
    
    protected String appPackage;
    protected String appName;
    protected Map<String, Object> behaviorParams;
    
    public AppBehaviorProfile(String appPackage, String appName) {
        this.appPackage = appPackage;
        this.appName = appName;
        this.behaviorParams = new HashMap<>();
        initializeProfile();
    }
    
    protected abstract void initializeProfile();
    
    public String getAppPackage() {
        return appPackage;
    }
    
    public String getAppName() {
        return appName;
    }
    
    public Object getBehaviorParam(String key) {
        return behaviorParams.get(key);
    }
    
    public int getTypingSpeed() {
        return (int) behaviorParams.getOrDefault("typing_speed", 180);
    }
    
    public double getSwipeVelocity() {
        return (double) behaviorParams.getOrDefault("swipe_velocity", 1.0);
    }
    
    public int getScrollFrequency() {
        return (int) behaviorParams.getOrDefault("scroll_frequency", 5);
    }
    
    public double getErrorRate() {
        return (double) behaviorParams.getOrDefault("error_rate", 0.02);
    }
    
    public boolean shouldSimulateReading() {
        return (boolean) behaviorParams.getOrDefault("simulate_reading", true);
    }
    
    public long getSessionDuration() {
        return (long) behaviorParams.getOrDefault("session_duration", 300000L);
    }
}
