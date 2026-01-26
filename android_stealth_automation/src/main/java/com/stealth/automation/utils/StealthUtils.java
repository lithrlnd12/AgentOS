package com.stealth.automation.utils;

import com.stealth.automation.core.StealthConfig;
import java.util.Random;

public class StealthUtils {
    
    private static final Random random = new Random();
    
    /**
     * Generate realistic human reaction time
     */
    public static long generateReactionTime() {
        // Human reaction time typically ranges from 150-300ms for simple tasks
        // But can be much longer for complex decisions
        double baseTime = 200; // 200ms base
        double variance = StealthConfig.randomDouble(0.5, 2.0); // 50% to 200% variation
        
        return (long) (baseTime * variance);
    }
    
    /**
     * Generate realistic reading speed delay
     */
    public static long generateReadingDelay(int wordCount) {
        // Average reading speed: 200-300 words per minute
        double wordsPerMinute = StealthConfig.randomDouble(200, 300);
        double millisecondsPerWord = (60.0 * 1000.0) / wordsPerMinute;
        
        return (long) (wordCount * millisecondsPerWord);
    }
    
    /**
     * Calculate attention span based on content complexity
     */
    public static long calculateAttentionSpan(boolean isComplexContent) {
        if (isComplexContent) {
            // Complex content gets more attention
            return StealthConfig.randomInt(30000, 120000); // 30s to 2min
        } else {
            // Simple content, shorter attention
            return StealthConfig.randomInt(5000, 30000); // 5s to 30s
        }
    }
    
    /**
     * Generate realistic scroll velocity based on content type
     */
    public static double generateScrollVelocity(String contentType) {
        switch (contentType.toLowerCase()) {
            case "social_media":
                return StealthConfig.randomDouble(1.0, 1.5); // Fast scrolling
            case "news":
                return StealthConfig.randomDouble(0.5, 1.0); // Medium scrolling
            case "technical":
                return StealthConfig.randomDouble(0.2, 0.6); // Slow scrolling
            default:
                return StealthConfig.randomDouble(0.5, 1.2);
        }
    }
    
    /**
     * Simulate human memory limitations (occasional forgetting)
     */
    public static boolean shouldForgetInformation(double complexity, double timeSinceLearned) {
        // Higher complexity and more time increase forgetting probability
        double forgetProbability = complexity * 0.1 + (timeSinceLearned / 3600000.0) * 0.05;
        return StealthConfig.randomBoolean(Math.min(forgetProbability, 0.3));
    }
    
    /**
     * Generate realistic multi-tasking behavior
     */
    public static boolean shouldSwitchTask(double currentTaskDuration) {
        // Humans tend to switch tasks every 2-15 minutes
        double switchProbability = currentTaskDuration / (10.0 * 60.0 * 1000.0); // 10 minutes base
        return StealthConfig.randomBoolean(Math.min(switchProbability, 0.8));
    }
    
    /**
     * Calculate fatigue factor based on session duration
     */
    public static double calculateFatigueFactor(long sessionDuration) {
        // Fatigue increases with session duration
        double hours = sessionDuration / (60.0 * 60.0 * 1000.0);
        return Math.min(1.0, hours / 2.0); // Full fatigue after 2 hours
    }
    
    /**
     * Generate realistic pause duration based on context
     */
    public static long generateContextualPause(String context) {
        switch (context.toLowerCase()) {
            case "decision_making":
                return StealthConfig.randomInt(2000, 8000);
            case "error_recovery":
                return StealthConfig.randomInt(1000, 5000);
            case "content_switching":
                return StealthConfig.randomInt(500, 2000);
            case "waiting":
                return StealthConfig.randomInt(3000, 15000);
            default:
                return StealthConfig.randomInt(800, 3000);
        }
    }
    
    /**
     * Simulate learning curve (improvement over time)
     */
    public static double calculateLearningFactor(int repetitionCount) {
        // Learning curve: rapid improvement initially, then plateau
        return 1.0 - (0.5 * Math.exp(-repetitionCount / 10.0));
    }
    
    /**
     * Generate realistic hesitation before critical actions
     */
    public static long generateHesitationDelay(boolean isCriticalAction) {
        if (!isCriticalAction) {
            return 0;
        }
        
        // Critical actions get longer hesitation
        return StealthConfig.randomInt(500, 3000);
    }
    
    /**
     * Calculate distraction probability based on environment
     */
    public static double calculateDistractionProbability(String environment) {
        switch (environment.toLowerCase()) {
            case "noisy":
                return StealthConfig.randomDouble(0.3, 0.6);
            case "quiet":
                return StealthConfig.randomDouble(0.05, 0.15);
            case "office":
                return StealthConfig.randomDouble(0.1, 0.3);
            default:
                return StealthConfig.randomDouble(0.1, 0.4);
        }
    }
}
