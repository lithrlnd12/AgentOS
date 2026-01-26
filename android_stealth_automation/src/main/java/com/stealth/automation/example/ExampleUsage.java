package com.stealth.automation.example;

import com.stealth.automation.core.StealthAutomationEngine;
import com.stealth.automation.profiles.SocialMediaProfile;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.URL;
import java.util.concurrent.TimeUnit;

public class ExampleUsage {
    
    public static void main(String[] args) {
        try {
            // Setup Appium capabilities
            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
            capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "emulator-5554");
            capabilities.setCapability(MobileCapabilityType.APP_PACKAGE, "com.instagram.android");
            capabilities.setCapability(MobileCapabilityType.APP_ACTIVITY, ".activity.MainTabActivity");
            capabilities.setCapability(MobileCapabilityType.NO_RESET, true);
            capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "UiAutomator2");
            
            // Additional stealth capabilities
            capabilities.setCapability("disableAndroidWatchers", true);
            capabilities.setCapability("skipDeviceInitialization", true);
            capabilities.setCapability("ignoreUnimportantViews", false);
            
            // Initialize driver
            AndroidDriver driver = new AndroidDriver(new URL("http://localhost:4723/wd/hub"), capabilities);
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
            
            // Create stealth automation engine
            StealthAutomationEngine stealthEngine = new StealthAutomationEngine(driver);
            
            // Set behavior profile for social media app
            stealthEngine.setBehaviorProfile(new SocialMediaProfile());
            
            // Example automation flow with stealth features
            demonstrateStealthAutomation(stealthEngine);
            
            // Print final stealth report
            stealthEngine.printStealthReport();
            
            // Cleanup
            driver.quit();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void demonstrateStealthAutomation(StealthAutomationEngine engine) {
        try {
            // Wait for app to load
            Thread.sleep(5000);
            
            // Example 1: Natural scrolling behavior
            System.out.println("Performing natural scrolling...");
            for (int i = 0; i < 5; i++) {
                engine.stealthScroll(By.xpath("//androidx.recyclerview.widget.RecyclerView"), 400);
                Thread.sleep(1000);
            }
            
            // Example 2: Stealth tapping
            System.out.println("Performing stealth clicks...");
            engine.stealthClick(By.xpath("//android.widget.ImageView[@content-desc='Like']"));
            Thread.sleep(2000);
            
            // Example 3: Natural text input
            System.out.println("Performing stealth text input...");
            engine.stealthClick(By.id("com.instagram.android:id/comment_composer_editor"));
            engine.stealthType(By.id("com.instagram.android:id/comment_composer_editor"), 
                             "Great post! Really love the composition.");
            
            // Example 4: Long press
            System.out.println("Performing stealth long press...");
            engine.stealthLongPress(By.xpath("//android.widget.ImageView[@content-desc='More options']"));
            
            // Example 5: Swipe gestures
            System.out.println("Performing natural swipe...");
            engine.stealthSwipe(500, 1000, 200, 1000, 800);
            
        } catch (Exception e) {
            System.err.println("Error during demonstration: " + e.getMessage());
        }
    }
}
