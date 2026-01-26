package com.android.automation.core;

/**
 * Execution method priorities (higher number = higher priority)
 */
public enum MethodPriority {
    SHIZUKU_ADB(1000, "Shizuku ADB", "Highest priority - Direct ADB commands via Shizuku"),
    SHIZUKU(900, "Shizuku", "High priority - System-level operations via Shizuku"),
    ROOT(800, "Root", "Medium priority - Root-based operations"),
    ACCESSIBILITY(700, "Accessibility", "Low priority - Accessibility service operations");
    
    private final int priority;
    private final String name;
    private final String description;
    
    MethodPriority(int priority, String name, String description) {
        this.priority = priority;
        this.name = name;
        this.description = description;
    }
    
    public int getValue() {
        return priority;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get priority by value
     */
    public static MethodPriority fromValue(int value) {
        for (MethodPriority priority : values()) {
            if (priority.priority == value) {
                return priority;
            }
        }
        return ACCESSIBILITY; // Default to lowest
    }
}
