package com.stealth.automation.profiles;

public class GamingProfile extends AppBehaviorProfile {
    
    public GamingProfile() {
        super("com.supercell.clashofclans", "Clash of Clans");
    }
    
    @Override
    protected void initializeProfile() {
        // Gaming app behavior patterns
        behaviorParams.put("typing_speed", 160); // Slower typing for gaming
        behaviorParams.put("swipe_velocity", 0.8); // More deliberate swipes
        behaviorParams.put("scroll_frequency", 2); // Low scrolling
        behaviorParams.put("error_rate", 0.05); // Higher error rate (precision needed)
        behaviorParams.put("simulate_reading", false);
        behaviorParams.put("session_duration", 1800000L); // 30 min sessions
        behaviorParams.put("tap_precision", 0.95); // High precision required
        behaviorParams.put("multi_touch_frequency", 0.6); // Frequent multi-touch
        behaviorParams.put("pause_frequency", 0.3); // Frequent pauses for strategy
        behaviorParams.put("menu_navigation_rate", 0.8); // Lots of menu navigation
    }
}
