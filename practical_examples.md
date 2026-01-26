# Practical Implementation Examples

## Example 1: Complete Shizuku-based Automation System

```java
public class ShizukuAutomationService extends Service {
    private IInputManager inputManager;
    private boolean isShizukuReady = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        initializeShizuku();
    }
    
    private void initializeShizuku() {
        try {
            // Bind to Shizuku service
            Bundle args = new Bundle();
            args.putString("moe.shizuku.privileged.api.PACKAGE_NAME", getPackageName());
            
            IShizukuService shizukuService = IShizukuService.Stub.asInterface(
                ShizukuBinderWrapper.getSystemService("input")
            );
            
            inputManager = IInputManager.Stub.asInterface(
                shizukuService.getSystemService("input")
            );
            isShizukuReady = true;
        } catch (Exception e) {
            Log.e("ShizukuAutomation", "Failed to initialize Shizuku", e);
        }
    }
    
    public boolean performTap(int x, int y) {
        if (!isShizukuReady || inputManager == null) return false;
        
        try {
            long downTime = SystemClock.uptimeMillis();
            
            // Create down event
            MotionEvent downEvent = MotionEvent.obtain(
                downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0
            );
            
            // Create up event
            MotionEvent upEvent = MotionEvent.obtain(
                downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0
            );
            
            // Inject events
            inputManager.injectInputEvent(downEvent, 
                InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
            inputManager.injectInputEvent(upEvent, 
                InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
            
            downEvent.recycle();
            upEvent.recycle();
            
            return true;
        } catch (Exception e) {
            Log.e("ShizukuAutomation", "Failed to perform tap", e);
            return false;
        }
    }
}
```

## Example 2: ADB-based Automation System

```java
public class ADBAutomationEngine {
    private static final String ADB_HOST = "localhost";
    private static final int ADB_PORT = 5555;
    
    public boolean connectADB() {
        try {
            AdbConnection connection = AdbConnection.create(
                new InetSocketAddress(ADB_HOST, ADB_PORT)
            );
            connection.connect();
            return true;
        } catch (Exception e) {
            Log.e("ADBAutomation", "Failed to connect ADB", e);
            return false;
        }
    }
    
    public boolean performGesture(List<Point> points, int duration) {
        try {
            StringBuilder command = new StringBuilder("input swipe ");
            
            for (Point point : points) {
                command.append(point.x).append(" ").append(point.y).append(" ");
            }
            command.append(duration);
            
            executeShellCommand(command.toString());
            return true;
        } catch (Exception e) {
            Log.e("ADBAutomation", "Failed to perform gesture", e);
            return false;
        }
    }
    
    private String executeShellCommand(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );
        
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        
        process.waitFor();
        return output.toString();
    }
}
```

## Example 3: Root-based Input Injection

```java
public class RootAutomationEngine {
    private static final String INPUT_DEVICE = "/dev/input/event2";
    
    public boolean hasRootAccess() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean injectTap(int x, int y) {
        if (!hasRootAccess()) return false;
        
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            
            // Send input events as root
            String command = String.format(
                "sendevent %s 3 0 %d\n" +  // ABS_X
                "sendevent %s 3 1 %d\n" +  // ABS_Y
                "sendevent %s 1 330 1\n" + // BTN_TOUCH DOWN
                "sendevent %s 0 0 0\n" +   // SYN_REPORT
                "sendevent %s 1 330 0\n" + // BTN_TOUCH UP
                "sendevent %s 0 0 0\n",    // SYN_REPORT
                INPUT_DEVICE, x,
                INPUT_DEVICE, y,
                INPUT_DEVICE,
                INPUT_DEVICE,
                INPUT_DEVICE,
                INPUT_DEVICE
            );
            
            os.writeBytes(command);
            os.writeBytes("exit\n");
            os.flush();
            
            return process.waitFor() == 0;
        } catch (Exception e) {
            Log.e("RootAutomation", "Failed to inject tap", e);
            return false;
        }
    }
}
```

## Example 4: Hybrid Automation System

```java
public class HybridAutomationEngine {
    private enum Method {
        ROOT, SHIZUKU, ADB, ACCESSIBILITY, NONE
    }
    
    private Method currentMethod = Method.NONE;
    private RootAutomationEngine rootEngine;
    private ShizukuAutomationService shizukuService;
    private ADBAutomationEngine adbEngine;
    private AccessibilityService accessibilityService;
    
    public void initialize() {
        // Try methods in order of preference
        if (tryRootMethod()) {
            currentMethod = Method.ROOT;
        } else if (tryShizukuMethod()) {
            currentMethod = Method.SHIZUKU;
        } else if (tryADBMethod()) {
            currentMethod = Method.ADB;
        } else if (tryAccessibilityMethod()) {
            currentMethod = Method.ACCESSIBILITY;
        }
    }
    
    public boolean performAutomation(AutomationCommand command) {
        switch (currentMethod) {
            case ROOT:
                return rootEngine.executeCommand(command);
            case SHIZUKU:
                return shizukuService.executeCommand(command);
            case ADB:
                return adbEngine.executeCommand(command);
            case ACCESSIBILITY:
                return accessibilityService.executeCommand(command);
            default:
                return false;
        }
    }
    
    private boolean tryShizukuMethod() {
        try {
            if (Shizuku.pingBinder()) {
                shizukuService = new ShizukuAutomationService();
                return true;
            }
        } catch (Exception e) {
            Log.w("HybridAutomation", "Shizuku not available");
        }
        return false;
    }
    
    private boolean tryRootMethod() {
        rootEngine = new RootAutomationEngine();
        return rootEngine.hasRootAccess();
    }
    
    private boolean tryADBMethod() {
        adbEngine = new ADBAutomationEngine();
        return adbEngine.connectADB();
    }
    
    private boolean tryAccessibilityMethod() {
        // Check if accessibility service is enabled
        AccessibilityManager manager = (AccessibilityManager) 
            getSystemService(Context.ACCESSIBILITY_SERVICE);
        return manager.isEnabled();
    }
}
```

## Example 5: Anti-Detection Implementation

```java
public class AntiDetectionUtils {
    private static final Random random = new Random();
    
    public static void randomDelay(int minMs, int maxMs) {
        int delay = minMs + random.nextInt(maxMs - minMs);
     
