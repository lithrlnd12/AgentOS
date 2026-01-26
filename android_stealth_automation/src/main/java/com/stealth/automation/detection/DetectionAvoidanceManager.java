package com.stealth.automation.detection;

import com.stealth.automation.core.StealthConfig;
import com.stealth.automation.monitoring.StealthMetrics;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DetectionAvoidanceManager {
    
    private final StealthMetrics metrics;
    private final Map<String, Long> lastActionTimes;
    private final Set<String> suspiciousPatterns;
    private final Random random;
    
    public DetectionAvoidanceManager(StealthMetrics metrics) {
        this.metrics = metrics;
        this.lastActionTimes = new ConcurrentHashMap<>();
        this.suspiciousPatterns = ConcurrentHashMap.newKeySet();
        this.random = new Random();
    }
    
    public boolean shouldAvoidDetection() {
        double riskScore = calculateRiskScore();
        
        if (riskScore > 0.7) {
            return random.nextDouble() < 0.9;
        } else if (riskScore > 0.4) {
            return random.nextDouble() < 0.6;
        }
        
        return false;
    }
    
    public void randomizeBehavior() {
        randomizeTiming();
        randomizeGestures();
        introduceNoise();
    }
    
    public void cooldownPeriod() {
        int cooldownDuration = StealthConfig.randomInt(5000, 15000);
        sleep(cooldownDuration);
        metrics.resetSessionMetrics();
    }
    
    public void mimicHumanInconsistency() {
        if (random.nextDouble() < 0.1) {
            makeIntentionalError();
        }
        
        if (random.nextDouble() < 0.05) {
            unexpectedPause();
        }
        
        if (random.nextDouble() < 0.15) {
            varyPrecision();
        }
    }
    
    private double calculateRiskScore() {
        double riskScore = 0.0;
        
        double actionFrequency = metrics.getActionFrequency();
        if (actionFrequency > 10.0) {
            riskScore += 0.3;
        }
        
        double timingRegularity = metrics.getTimingRegularity();
        if (timingRegularity > 0.8) {
            riskScore += 0.4;
        }
        
        double gestureConsistency = metrics.getGestureConsistency();
        if (gestureConsistency > 0.9) {
            riskScore += 0.3;
        }
        
        return Math.min(1.0, riskScore);
    }
    
    private void randomizeTiming() {
        int randomDelay = StealthConfig.randomInt(100, 2000);
        sleep(randomDelay);
    }
    
    private void randomizeGestures() {
        StealthConfig.POSITION_NOISE_PX = StealthConfig.randomInt(2, 5);
    }
    
    private void introduceNoise() {
        metrics.addNoiseToMetrics(StealthConfig.randomDouble(0.01, 0.05));
    }
    
    private void makeIntentionalError() {
        int errorType = StealthConfig.randomInt(0, 3);
        
        switch (errorType) {
            case 0:
                sleep(StealthConfig.randomInt(500, 1500));
                break;
            case 1:
                sleep(StealthConfig.randomInt(100, 300));
                break;
            case 2:
                sleep(StealthConfig.randomInt(200, 800));
                break;
            case 3:
                sleep(StealthConfig.randomInt(1000, 3000));
                break;
        }
    }
    
    private void unexpectedPause() {
        int pauseDuration = StealthConfig.randomInt(3000, 10000);
        sleep(pauseDuration);
        sleep(StealthConfig.randomInt(500, 1500));
    }
    
    private void varyPrecision() {
        int precisionDelay = StealthConfig.randomInt(50, 300);
        sleep(precisionDelay);
    }
    
    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public Set<String> getSuspiciousPatterns() {
        return new HashSet<>(suspiciousPatterns);
    }
    
    public void reportSuspiciousPattern(String pattern) {
        suspiciousPatterns.add(pattern);
        metrics.incrementSuspiciousActivity(pattern);
    }
}
