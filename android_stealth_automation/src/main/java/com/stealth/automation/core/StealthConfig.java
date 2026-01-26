package com.stealth.automation.core;

import java.util.concurrent.ThreadLocalRandom;

public class StealthConfig {
    
    // Human-like timing parameters
    public static final double BASE_TYPING_SPEED = 180; // chars per minute
    public static final double TYPING_VARIANCE = 0.3;
    public static final int MIN_PAUSE_MS = 800;
    public static final int MAX_PAUSE_MS = 3500;
    public static final int MICRO_PAUSE_MS = 50;
    public static final int MAX_MICRO_PAUSE_MS = 200;
    
    // Touch gesture parameters
    public static final double SWIPE_CURVE_VARIANCE = 0.15;
    public static final int TOUCH_DOWN_VARIANCE_MS = 25;
    public static final int TOUCH_UP_VARIANCE_MS = 30;
    public static final double PRESSURE_VARIANCE = 0.2;
    public static final int GESTURE_POINTS_MIN = 8;
    public static final int GESTURE_POINTS_MAX = 20;
    
    // Position noise parameters
    public static final int POSITION_NOISE_PX = 3;
    public static final int MICRO_MOVEMENT_PX = 1;
    public static final double POSITION_BIAS_DECAY = 0.95;
    
    // Detection avoidance
    public static final double ACTIVITY_COOLDOWN_FACTOR = 0.1;
    public static final int MAX_CONSECUTIVE_ACTIONS = 15;
    public static final int STEALTH_MODE_VARIANCE_MS = 5000;
    
    private static final ThreadLocalRandom random = ThreadLocalRandom.current();
    
    public static int randomInt(int min, int max) {
        return random.nextInt(min, max + 1);
    }
    
    public static double randomDouble(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }
    
    public static boolean randomBoolean(double probability) {
        return random.nextDouble() < probability;
    }
    
    public static long randomDelay(int baseDelay, double variance) {
        double factor = 1.0 + randomDouble(-variance, variance);
        return (long) (baseDelay * factor);
    }
}
