package com.stealth.automation.gestures;

import com.stealth.automation.core.StealthConfig;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class NaturalTouchGestures {
    
    private final AndroidDriver driver;
    private final TouchAction touchAction;
    
    public NaturalTouchGestures(AndroidDriver driver) {
        this.driver = driver;
        this.touchAction = new TouchAction(driver);
    }
    
    public void naturalTap(int x, int y) {
        // Add position noise
        int[] noisyPos = addPositionNoise(x, y);
        
        // Vary touch pressure and duration
        int pressDuration = StealthConfig.randomInt(80, 180);
        int releaseDelay = StealthConfig.randomInt(StealthConfig.TOUCH_UP_VARIANCE_MS, 
                                                   StealthConfig.TOUCH_UP_VARIANCE_MS + 50);
        
        // Add micro-movement during touch
        List<int[]> microMovements = generateMicroMovements(noisyPos[0], noisyPos[1]);
        
        touchAction.press(PointOption.point(microMovements.get(0)[0], microMovements.get(0)[1]))
                  .waitAction(WaitOptions.waitOptions(Duration.ofMillis(pressDuration)));
        
        // Add subtle movements during press
        for (int i = 1; i < microMovements.size(); i++) {
            int[] point = microMovements.get(i);
            touchAction.moveTo(PointOption.point(point[0], point[1]))
                      .waitAction(WaitOptions.waitOptions(Duration.ofMillis(10)));
        }
        
        touchAction.release()
                  .perform();
        
        // Natural pause after tap
        sleep(releaseDelay);
    }
    
    public void naturalSwipe(int startX, int startY, int endX, int endY, int duration) {
        // Add position noise to start and end points
        int[] noisyStart = addPositionNoise(startX, startY);
        int[] noisyEnd = addPositionNoise(endX, endY);
        
        // Generate curved path
        List<int[]> path = generateCurvedPath(noisyStart[0], noisyStart[1], 
                                             noisyEnd[0], noisyEnd[1], duration);
        
        // Vary swipe parameters
        int variedDuration = StealthConfig.randomDelay(duration, 0.3);
        int pointInterval = variedDuration / path.size();
        
        TouchAction swipe = new TouchAction(driver);
        swipe.press(PointOption.point(path.get(0)[0], path.get(0)[1]));
        
        // Execute curved path
        for (int i = 1; i < path.size(); i++) {
            int[] point = path.get(i);
            swipe.waitAction(WaitOptions.waitOptions(Duration.ofMillis(pointInterval)))
                 .moveTo(PointOption.point(point[0], point[1]));
        }
        
        swipe.release().perform();
        
        // Natural pause after swipe
        sleep(StealthConfig.randomInt(200, 500));
    }
    
    private List<int[]> generateCurvedPath(int startX, int startY, int endX, int endY, int duration) {
        List<int[]> path = new ArrayList<>();
        
        int numPoints = StealthConfig.randomInt(StealthConfig.GESTURE_POINTS_MIN, 
                                              StealthConfig.GESTURE_POINTS_MAX);
        
        // Generate control points for bezier curve
        double midX = (startX + endX) / 2.0;
        double midY = (startY + endY) / 2.0;
        
        // Add curve variance
        double curveIntensity = StealthConfig.randomDouble(-50, 50);
        double ctrlX1 = midX + curveIntensity + StealthConfig.randomDouble(-20, 20);
        double ctrlY1 = midY - curveIntensity + StealthConfig.randomDouble(-20, 20);
        double ctrlX2 = midX - curveIntensity + StealthConfig.randomDouble(-20, 20);
        double ctrlY2 = midY + curveIntensity + StealthConfig.randomDouble(-20, 20);
        
        // Generate bezier curve points
        for (int i = 0; i < numPoints; i++) {
            double t = (double) i / (numPoints - 1);
            
            // Cubic bezier curve calculation
            double x = Math.pow(1-t, 3) * startX +
                      3 * Math.pow(1-t, 2) * t * ctrlX1 +
                      3 * (1-t) * Math.pow(t, 2) * ctrlX2 +
                      Math.pow(t, 3) * endX;
                      
            double y = Math.pow(1-t, 3) * startY +
                      3 * Math.pow(1-t, 2) * t * ctrlY1 +
                      3 * (1-t) * Math.pow(t, 2) * ctrlY2 +
                      Math.pow(t, 3) * endY;
            
            // Add small random variations
            x += StealthConfig.randomDouble(-2, 2);
            y += StealthConfig.randomDouble(-2, 2);
            
            path.add(new int[]{(int) x, (int) y});
        }
        
        return path;
    }
    
    private int[] addPositionNoise(int x, int y) {
        int noiseX = x + StealthConfig.randomInt(-StealthConfig.POSITION_NOISE_PX, 
                                               StealthConfig.POSITION_NOISE_PX);
        int noiseY = y + StealthConfig.randomInt(-StealthConfig.POSITION_NOISE_PX, 
                                               StealthConfig.POSITION_NOISE_PX);
        
        return new int[]{noiseX, noiseY};
    }
    
    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
