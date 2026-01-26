package com.stealth.automation.monitoring;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class StealthMetrics {
    
    private final Map<String, AtomicLong> actionCounters;
    private final List<Long> actionTimestamps;
    private final Map<String, AtomicLong> suspiciousActivities;
    private final List<Double> gestureConsistencyScores;
    
    private long sessionStartTime;
    private long lastActionTime;
    private int consecutiveActions;
    
    public StealthMetrics() {
        this.actionCounters = new ConcurrentHashMap<>();
        this.actionTimestamps = Collections.synchronizedList(new ArrayList<>());
        this.suspiciousActivities = new ConcurrentHashMap<>();
        this.gestureConsistencyScores = Collections.synchronizedList(new ArrayList<>());
        
        this.sessionStartTime = System.currentTimeMillis();
        this.lastActionTime = sessionStartTime;
        this.consecutiveActions = 0;
    }
    
    public void recordAction(String actionType) {
        long currentTime = System.currentTimeMillis();
        
        // Update action counter
        actionCounters.computeIfAbsent(actionType, k -> new AtomicLong(0)).incrementAndGet();
        
        // Record timestamp
        actionTimestamps.add(currentTime);
        
        // Update consecutive actions
        if (currentTime - lastActionTime < 1000) {
            consecutiveActions++;
        } else {
            consecutiveActions = 1;
        }
        
        lastActionTime = currentTime;
        
        // Clean old timestamps (keep last 5 minutes)
        cleanupOldTimestamps();
    }
    
    public void recordGestureConsistency(double consistencyScore) {
        gestureConsistencyScores.add(consistencyScore);
        
        // Keep only last 100 scores
        if (gestureConsistencyScores.size() > 100) {
            gestureConsistencyScores.remove(0);
        }
    }
    
    public void incrementSuspiciousActivity(String activityType) {
        suspiciousActivities.computeIfAbsent(activityType, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    public void addNoiseToMetrics(double noiseLevel) {
        // Add controlled noise to avoid perfect patterns
        // This is a simplified implementation
    }
    
    public void resetSessionMetrics() {
        sessionStartTime = System.currentTimeMillis();
        consecutiveActions = 0;
        actionTimestamps.clear();
        gestureConsistencyScores.clear();
    }
    
    public double getActionFrequency() {
        if (actionTimestamps.isEmpty()) return 0.0;
        
        long now = System.currentTimeMillis();
        long oneSecondAgo = now - 1000;
        
        long recentActions = actionTimestamps.stream()
            .filter(timestamp -> timestamp > oneSecondAgo)
            .count();
        
        return (double) recentActions;
    }
    
    public double getTimingRegularity() {
        if (actionTimestamps.size() < 2) return 0.0;
        
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < actionTimestamps.size(); i++) {
            intervals.add(actionTimestamps.get(i) - actionTimestamps.get(i-1));
        }
        
        if (intervals.isEmpty()) return 0.0;
        
        // Calculate coefficient of variation
        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double variance = intervals.stream()
            .mapToDouble(interval -> Math.pow(interval - mean, 2))
            .average().orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        
        return mean > 0 ? 1.0 - (stdDev / mean) : 0.0;
    }
    
    public double getGestureConsistency() {
        if (gestureConsistencyScores.isEmpty()) return 0.0;
        
        double avgConsistency = gestureConsistencyScores.stream()
            .mapToDouble(Double::doubleValue)
            .average().orElse(0.0);
        
        return avgConsistency;
    }
    
    public double getPatternRepetition() {
        // Simplified pattern repetition detection
        Map<String, Integer> actionCounts = new HashMap<>();
        
        for (String action : actionCounters.keySet()) {
            actionCounts.put(action, actionCounters.get(action).intValue());
        }
        
        if (actionCounts.isEmpty()) return 0.0;
        
        int totalActions = actionCounts.values().stream().mapToInt(Integer::intValue).sum();
        int maxActionCount = actionCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        
        return totalActions > 0 ? (double) maxActionCount / totalActions : 0.0;
    }
    
    public Map<String, Object> getStealthMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("action_frequency", getActionFrequency());
        metrics.put("timing_regularity", getTimingRegularity());
        metrics.put("gesture_consistency", getGestureConsistency());
        metrics.put("pattern_repetition", getPatternRepetition());
        metrics.put("consecutive_actions", consecutiveActions);
        metrics.put("session_duration", System.currentTimeMillis() - sessionStartTime);
        metrics.put("total_actions", getTotalActions());
        metrics.put("suspicious_activities", getSuspiciousActivitiesSummary());
        
        return metrics;
    }
    
    public Map<String, Long> getActionSummary() {
        Map<String, Long> summary = new HashMap<>();
        actionCounters.forEach((key, value) -> summary.put(key, value.get()));
        return summary;
    }
    
    public Map<String, Long> getSuspiciousActivitiesSummary() {
        Map<String, Long> summary = new HashMap<>();
        suspiciousActivities.forEach((key, value) -> summary.put(key, value.get()));
        return summary;
    }
    
    private long getTotalActions() {
        return actionCounters.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();
    }
    
    private void cleanupOldTimestamps() {
        long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
        actionTimestamps.removeIf(timestamp -> timestamp < fiveMinutesAgo);
    }
    
    public boolean isStealthModeRecommended() {
        double riskScore = calculateRiskScore();
        return riskScore > 0.6;
    }
    
    private double calculateRiskScore() {
        double riskScore = 0.0;
        
        // High action frequency risk
        if (getActionFrequency() > 8.0) {
            riskScore += 0.3;
        }
        
        // High timing regularity risk
        if (getTimingRegularity() > 0.85) {
            riskScore += 0.3;
        }
        
        // High gesture consistency risk
        if (getGestureConsistency() > 0.95) {
            riskScore += 0.2;
        }
        
        // High pattern repetition risk
        if (getPatternRepetition() > 0.8) {
            riskScore += 0.2;
        }
        
        // Too many consecutive actions
        if (consecutiveActions > 20) {
            riskScore += 0.1;
        }
        
        return Math.min(1.0, riskScore);
    }
}
