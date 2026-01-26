package com.stealth.automation.core;

import com.stealth.automation.detection.DetectionAvoidanceManager;
import com.stealth.automation.gestures.NaturalTouchGestures;
import com.stealth.automation.monitoring.StealthMetrics;
import com.stealth.automation.profiles.AppBehaviorProfile;
import com.stealth.automation.timing.HumanTimingPatterns;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.Map;
import java.util.logging.Logger;

public class StealthAutomationEngine {
    
    private static final Logger logger = Logger.getLogger(StealthAutomationEngine.class.getName());
    
    private final AndroidDriver driver;
    private final HumanTimingPatterns timingPatterns;
    private final NaturalTouchGestures touchGestures;
    private final DetectionAvoidanceManager detectionManager;
    private final StealthMetrics metrics;
    private AppBehaviorProfile currentProfile;
    
    public StealthAutomationEngine(AndroidDriver driver) {
        this.driver = driver;
        this.metrics = new StealthMetrics();
        this.timingPatterns = new HumanTimingPatterns();
        this.touchGestures = new NaturalTouchGestures(driver);
        this.detectionManager = new DetectionAvoidanceManager(metrics);
        this.currentProfile = null;
    }
    
    public void setBehaviorProfile(AppBehaviorProfile profile) {
        this.currentProfile = profile;
        logger.info("Switched to behavior profile: " + profile.getAppName());
    }
    
    public void stealthClick(By locator) {
        try {
            if (detectionManager.shouldAvoidDetection()) {
                detectionManager.randomizeBehavior();
            }
            
            timingPatterns.humanDelay("find_element");
            WebElement element = driver.findElement(locator);
            
            int x = element.getLocation().getX() + element.getSize().getWidth() / 2;
            int y = element.getLocation().getY() + element.getSize().getHeight() / 2;
            
            touchGestures.naturalTap(x, y);
            
            metrics.recordAction("click");
            timingPatterns.betweenActionPause();
            
            if (metrics.isStealthModeRecommended()) {
                detectionManager.cooldownPeriod();
            }
            
        } catch (Exception e) {
            logger.severe("Error during stealth click: " + e.getMessage());
            metrics.incrementSuspiciousActivity("click_error");
        }
    }
    
    public void stealthType(By locator, String text) {
        try {
            timingPatterns.humanDelay("typetext");
            
            WebElement element = driver.findElement(locator);
            element.click();
            
            for (int i = 0; i < text.length(); i++) {
                char character = text.charAt(i);
                
                boolean makeError = shouldMakeTypingError();
                
                if (makeError) {
                    char wrongChar = generateTypo(character);
                    element.sendKeys(String.valueOf(wrongChar));
                    timingPatterns.typingDelay(character, true);
                    
                    element.sendKeys("\b");
                    timingPatterns.typingDelay(character, false);
                }
                
                element.sendKeys(String.valueOf(character));
                timingPatterns.typingDelay(character, false);
                
                if (shouldPauseForThinking(i, text.length())) {
                    timingPatterns.thinkingPause();
                }
            }
            
            metrics.recordAction("typetext");
            timingPatterns.betweenActionPause();
            
        } catch (Exception e) {
            logger.severe("Error during stealth typing: " + e.getMessage());
            metrics.incrementSuspiciousActivity("type_error");
        }
    }
    
    public void stealthSwipe(int startX, int startY, int endX, int endY, int duration) {
        try {
            timingPatterns.humanDelay("swipe");
            touchGestures.naturalSwipe(startX, startY, endX, endY, duration);
            metrics.recordAction("swipe");
            timingPatterns.betweenActionPause();
        } catch (Exception e) {
            logger.severe("Error during stealth swipe: " + e.getMessage());
            metrics.incrementSuspiciousActivity("swipe_error");
        }
    }
    
    public void stealthScroll(By scrollableElement, int pixels) {
        try {
            timingPatterns.humanDelay("scroll");
            
            WebElement element = driver.findElement(scrollableElement);
            int startX = element.getLocation().getX() + element.getSize().getWidth() / 2;
            int startY = element.getLocation().getY() + element.getSize().getHeight() / 2;
            int endY = startY - pixels;
            
            touchGestures.naturalSwipe(startX, startY, startX, endY, 1000);
            
            metrics.recordAction("scroll");
            timingPatterns.betweenActionPause();
            
        } catch (Exception e) {
            logger.severe("Error during stealth scroll: " + e.getMessage());
            metrics.incrementSuspiciousActivity("scroll_error");
        }
    }
    
    public void stealthLongPress(By locator) {
        try {
            timingPatterns.humanDelay("longpress");
            
            WebElement element = driver.findElement(locator);
            int x = element.getLocation().getX() + element.getSize().getWidth() / 2;
            int y = element.getLocation().getY() + element.getSize().getHeight() / 2;
            
            touchGestures.naturalLongPress(x, y);
            
            metrics.recordAction("longpress");
            timingPatterns.betweenActionPause();
            
        } catch (Exception e) {
            logger.severe("Error during stealth long press: " + e.getMessage());
            metrics.incrementSuspiciousActivity("longpress_error");
        }
    }
    
    public Map<String, Object> getStealthMetrics() {
        return metrics.getStealthMetrics();
    }
    
    public void printStealthReport() {
        Map<String, Object> metrics = getStealthMetrics();
        logger.info("=== Stealth Automation Report ===");
        metrics.forEach((key, value) -> logger.info(key + ": " + value));
        logger.info("=================================");
    }
    
    private boolean shouldMakeTypingError() {
        if (currentProfile == null) return false;
        
        double errorRate = currentProfile.getErrorRate();
        return StealthConfig.randomBoolean(errorRate);
    }
    
    private char generateTypo(char original) {
        String nearbyKeys = "qwertyuiopasdfghjklzxcvbnm";
        int index = nearbyKeys.indexOf(Character.toLowerCase(original));
        
        if (index != -1 && StealthConfig.randomBoolean(0.7)) {
            int offset = StealthConfig.randomBoolean(0.5) ? 1 : -1;
            int newIndex = Math.max(0, Math.min(nearbyKeys.length() - 1, index + offset));
            char typoChar = nearbyKeys.charAt(newIndex);
            return Character.isUpperCase(original) ? Character.toUpperCase(typoChar) : typoChar;
        } else {
            return original;
        }
    }
    
    private boolean shouldPauseForThinking(int currentIndex, int totalLength) {
        if (currentProfile == null) return false;
        
        if (currentIndex > totalLength * 0.3 && currentIndex < totalLength * 0.7) {
            return StealthConfig.randomBoolean(0.1);
        }
        
        return false;
    }
}
