package com.stealth.automation.timing;

import com.stealth.automation.core.StealthConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HumanTimingPatterns {
    
    private static final double[] THINKING_TIME_DISTRIBUTION = {
        0.1, 0.2, 0.3, 0.2, 0.1, 0.05, 0.025, 0.025
    };
    
    private static final int[] THINKING_TIME_BUCKETS = {
        500, 1000, 1500, 2000, 2500, 3000, 4000, 5000
    };
    
    private static final double[] TYPING_RHYTHM_PATTERNS = {
        1.0, 1.1, 0.9, 1.2, 0.8, 1.0, 1.1, 0.95, 1.05, 1.0
    };
    
    private final List<Long> actionHistory = new ArrayList<>();
    private long lastActionTime = 0;
    private int consecutiveActions = 0;
    
    public void humanDelay(String actionType) {
        long delay = calculateHumanDelay(actionType);
        sleep(delay);
        updateActionHistory();
    }
    
    public void typingDelay(char character, boolean isError) {
        long baseDelay = calculateTypingDelay(character);
        
        if (isError) {
            baseDelay *= 2.5; // Longer pause on errors
            addMicroPause();
        }
        
        // Add rhythm variation
        int rhythmIndex = consecutiveActions % TYPING_RHYTHM_PATTERNS.length;
        baseDelay *= TYPING_RHYTHM_PATTERNS[rhythmIndex];
        
        sleep(baseDelay);
        consecutiveActions++;
    }
    
    public void thinkingPause() {
        long thinkingTime = sampleThinkingTime();
        sleep(thinkingTime);
        addMicroPause();
    }
    
    public void betweenActionPause() {
        // Reduce consecutive action detection
        if (consecutiveActions > StealthConfig.MAX_CONSECUTIVE_ACTIONS) {
            long cooldown = (long) (StealthConfig.randomDelay(2000, 0.5) * 
                                   StealthConfig.ACTIVITY_COOLDOWN_FACTOR);
            sleep(cooldown);
            consecutiveActions = 0;
        }
        
        // Random micro-pauses
        if (StealthConfig.randomBoolean(0.3)) {
            addMicroPause();
        }
        
        consecutiveActions++;
    }
    
    private long calculateHumanDelay(String actionType) {
        long baseDelay;
        
        switch (actionType.toLowerCase()) {
            case "click":
                baseDelay = StealthConfig.randomInt(200, 800);
                break;
            case "scroll":
                baseDelay = StealthConfig.randomInt(300, 1200);
                break;
            case "swipe":
                baseDelay = StealthConfig.randomInt(500, 1500);
                break;
            case "typetext":
                baseDelay = StealthConfig.randomInt(100, 400);
                break;
            default:
                baseDelay = StealthConfig.randomInt(StealthConfig.MIN_PAUSE_MS, 
                                                   StealthConfig.MAX_PAUSE_MS);
        }
        
        return StealthConfig.randomDelay(baseDelay, 0.4);
    }
    
    private long calculateTypingDelay(char character) {
        double baseSpeed = StealthConfig.BASE_TYPING_SPEED / 60.0; // per second
        long charDelay = (long) (1000.0 / baseSpeed);
        
        // Special character delays
        if (Character.isUpperCase(character)) {
            charDelay *= 1.3; // Slower for uppercase
        } else if (character == ' ' || character == '.' || character == ',') {
            charDelay *= 1.2; // Slight pause after punctuation
        }
        
        return StealthConfig.randomDelay(charDelay, StealthConfig.TYPING_VARIANCE);
    }
    
    private long sampleThinkingTime() {
        double rand = StealthConfig.randomDouble(0, 1);
        double cumulative = 0;
        
        for (int i = 0; i < THINKING_TIME_DISTRIBUTION.length; i++) {
            cumulative += THINKING_TIME_DISTRIBUTION[i];
            if (rand <= cumulative) {
                return StealthConfig.randomDelay(THINKING_TIME_BUCKETS[i], 0.3);
            }
        }
        
        return THINKING_TIME_BUCKETS[THINKING_TIME_BUCKETS.length - 1];
    }
    
    private void addMicroPause() {
        if (StealthConfig.randomBoolean(0.4)) {
            long microPause = StealthConfig.randomInt(StealthConfig.MICRO_PAUSE_MS, 
                                                     StealthConfig.MAX_MICRO_PAUSE_MS);
            sleep(microPause);
        }
    }
    
    private void updateActionHistory() {
        long currentTime = System.currentTimeMillis();
        actionHistory.add(currentTime);
        lastActionTime = currentTime;
        
        // Remove actions older than 5 minutes
        actionHistory.removeIf(time -> currentTime - time > TimeUnit.MINUTES.toMillis(5));
    }
    
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public double getActivityLevel() {
        long now = System.currentTimeMillis();
        long recentActions = actionHistory.stream()
            .filter(time -> now - time < TimeUnit.MINUTES.toMillis(1))
            .count();
        
        return Math.min(1.0, recentActions / 20.0); // Normalize to 0-1
    }
}
