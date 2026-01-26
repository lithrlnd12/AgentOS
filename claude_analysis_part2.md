
## 2. State Management and Context System

### Claude's Context Architecture

Claude maintains multi-layered context across browser automation sessions:

1. **Page State Context**: DOM structure, visible elements, content
2. **Action History Context**: Previous actions and their outcomes
3. **Goal Context**: Current objectives and sub-tasks
4. **Error Context**: Failed actions and recovery strategies

**Android Adaptation:**

```java
public class AndroidAutomationContext {
    private ScreenState currentScreen;
    private ActionHistory history;
    private AutomationGoal currentGoal;
    private ErrorRecoveryContext errorContext;
    private Map<String, Object> sessionData;
    
    public void updateContext(VisionAnalysis analysis, ActionResult result) {
        // Update screen state with detected UI elements
        currentScreen = analysis.getScreenState();
        
        // Add action to history
        history.addAction(result);
        
        // Update goal progress
        currentGoal.updateProgress(analysis);
        
        // Store any learned information
        sessionData.putAll(analysis.getExtractedData());
    }
    
    public ContextSnapshot createSnapshot() {
        return new ContextSnapshot(
            currentScreen.clone(),
            history.getRecentActions(10),
            currentGoal.getProgress(),
            errorContext.getRecoveryStrategies()
        );
    }
}
```

### State Persistence Across Steps:

```java
public class PersistentStateManager {
    private SharedPreferences preferences;
    private Gson gson = new Gson();
    
    public void saveAutomationState(String sessionId, AutomationState state) {
        String json = gson.toJson(state);
        preferences.edit()
            .putString("automation_state_" + sessionId, json)
            .putLong("last_update_" + sessionId, System.currentTimeMillis())
            .apply();
    }
    
    public AutomationState restoreAutomationState(String sessionId) {
        String json = preferences.getString("automation_state_" + sessionId, null);
        if (json != null) {
            return gson.fromJson(json, AutomationState.class);
        }
        return null;
    }
}
```

## 3. Vision Model Integration and Prompting Strategy

### Claude's Vision Prompting Pattern

Based on research, Claude uses structured prompts for browser vision analysis:

```
You are analyzing a screenshot for automation purposes.
Current goal: [GOAL_DESCRIPTION]
Previous actions: [ACTION_HISTORY]

Analyze the screenshot and provide:
1. Visible UI elements with coordinates
2. Current page state assessment
3. Recommended next actions
4. Confidence scores for each element
5. Potential error conditions

Respond in structured JSON format.
```

**Android Vision Integration:**

```java
public class AndroidVisionEngine {
    private VisionModel visionModel;
    private PromptTemplate promptTemplate;
    
    public VisionAnalysis analyze(Bitmap screenshot, AutomationGoal goal) {
        // Prepare context-rich prompt
        String prompt = promptTemplate.generatePrompt(
            goal,
            history.getRecentActions(),
            errorContext.getCurrentErrors()
        );
        
        // Analyze screenshot with vision model
        VisionResponse response = visionModel.analyzeImage(screenshot, prompt);
        
        return parseVisionResponse(response);
    }
    
    private VisionAnalysis parseVisionResponse(VisionResponse response) {
        VisionAnalysis analysis = new VisionAnalysis();
        
        // Extract detected elements
        for (DetectedElement element : response.getElements()) {
            analysis.addElement(new UIElement(
                element.getType(),
                element.getText(),
                new Rectangle(element.getX(), element.getY(), 
                             element.getWidth(), element.getHeight()),
                element.getConfidence()
            ));
        }
        
        // Extract recommended actions
        analysis.setRecommendedActions(response.getActions());
        analysis.setConfidence(response.getOverallConfidence());
        
        return analysis;
    }
}
```
