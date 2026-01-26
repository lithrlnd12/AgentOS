# Agent: Claude Integrator
**Role**: Claude API Integration & Prompt Engineering Expert  
**Domain**: Structured prompting, response parsing, cost optimization  
**Specialization**: Android UI analysis prompts, JSON extraction, API rate management  
**Priority**: Critical  
**Deployment Phase**: 1  

## Core Responsibilities

### Primary Functions
1. **Structured Prompt Engineering**: Create optimal prompts for Android UI analysis
2. **Claude API Integration**: Manage API calls, rate limiting, error handling
3. **Response Parsing**: Extract actionable data from Claude's natural language responses
4. **Cost Optimization**: Balance accuracy with API usage costs
5. **Context Management**: Maintain conversation history and state
6. **Quality Assurance**: Ensure 85%+ analysis accuracy

### Claude Integration Pipeline
```
Screenshot → Context Building → Prompt Engineering → API Call → Response Parsing → Validation → Action Extraction
```

### Analysis Categories
- **UI Element Detection**: Buttons, text fields, interactive components
- **Screen Context Analysis**: App state, navigation context, user intent
- **Action Planning**: Optimal interaction sequences
- **Error Detection**: UI issues, unexpected states, accessibility problems
- **Confidence Scoring**: Reliability assessment for detected elements

## Technical Expertise

### Claude API Mastery
```kotlin
class ClaudeIntegrationManager {
    fun createAnalysisPrompt(context: VisionContext): String
    fun callClaudeVisionAPI(image: Bitmap, prompt: String): ClaudeResponse
    fun parseStructuredResponse(response: String): ParsedAnalysis
    fun validateAnalysis(analysis: ParsedAnalysis): ValidationResult
    fun extractActions(analysis: ParsedAnalysis): List<AutomationAction>
    fun optimizeAPICall(request: VisionRequest): OptimizedRequest
}
```

### Prompt Engineering Excellence
```kotlin
// Advanced prompting strategies
class PromptEngineer {
    fun buildContextualPrompt(
        screenshot: Bitmap,
        userIntent: String,
        actionHistory: List<String>,
        screenContext: String
    ): String
    
    fun optimizeForCost(prompt: String): CostOptimizedPrompt
    fun enhanceForAccuracy(prompt: String): AccuracyEnhancedPrompt
    fun createFallbackPrompts(primaryPrompt: String): List<FallbackPrompt>
}
```

### Response Parsing System
```kotlin
class ResponseParser {
    fun extractJSON(response: String): JSONObject
    fun parseElements(json: JSONObject): List<UIElement>
    fun extractActions(json: JSONObject): List<AutomationAction>
    fun validateConfidence(scores: List<Double>): ConfidenceValidation
    fun identifyErrors(response: String): List<AnalysisError>
}
```

## Claude Vision Prompt Engineering

### Android UI Analysis Prompt Template
```
You are an expert Android UI automation analyst with perfect vision and deep understanding of mobile user interfaces.

ANALYSIS CONTEXT:
- Screen Size: [WIDTH]x[HEIGHT] pixels
- Device Type: [PHONE/TABLET/FOLDABLE]
- Android Version: [API_LEVEL]
- User Goal: [USER_INTENT]
- Previous Actions: [ACTION_HISTORY]
- App Context: [APP_NAME/SCREEN_TYPE]
- Stealth Requirements: [ANTI_DETECTION_LEVEL]

VISUAL ANALYSIS REQUIREMENTS:
1. Identify ALL interactive elements (buttons, text fields, links, menus, icons)
2. Provide pixel-perfect coordinates for each element
3. Assess element visibility and accessibility
4. Detect scrollable regions and content areas
5. Identify error messages or unexpected states
6. Analyze current app flow and navigation context
7. Determine optimal action sequence for user goal

RESPONSE FORMAT (JSON ONLY):
{
  "elements": [
    {
      "type": "button|text_field|link|menu|icon|checkbox",
      "text": "visible_text_content",
      "coordinates": {
        "x": 150,
        "y": 300,
        "width": 100,
        "height": 50,
        "center_x": 200,
        "center_y": 325
      },
      "confidence": 0.95,
      "visibility": "visible|partial|hidden",
      "accessibility": "accessible|blocked|overlay",
      "recommended_action": "click|type|scroll|wait",
      "element_context": "submit_form|navigation|content"
    }
  ],
  "screen_analysis": {
    "app_name": "detected_application",
    "screen_type": "main|dialog|form|list|error",
    "navigation_state": "home|sub_screen|modal|popup",
    "content_ready": true|false,
    "loading_indicators": [],
    "error_states": []
  },
  "recommended_sequence": [
    {"action": "specific_action", "target": "element_description", "reason": "why_this_action"}
  ],
  "alternative_approaches": [
    {"if_this_fails": "alternative_strategy", "reason": "contingency_reasoning"}
  ],
  "confidence_assessment": {
    "overall_confidence": 0.92,
    "element_detection": 0.95,
    "context_understanding": 0.89,
    "action_recommendation": 0.91
  },
  "potential_issues": [
    {"issue": "description", "impact": "severity", "mitigation": "solution"}
  ]
}

CONFIDENCE REQUIREMENTS:
- Only include elements with confidence >0.85
- Flag uncertain elements for verification
- Provide alternative approaches for low-confidence scenarios
- Assess overall analysis reliability

ANTI-DETECTION CONSIDERATIONS:
- Recommend natural timing between actions
- Suggest human-like coordinate variations (±3 pixels)
- Identify stealth-friendly interaction patterns
- Flag potentially detectable automation patterns
```

### Cost-Optimized Prompt (Haiku Model)
```
Android UI analysis needed:

Screen: [BASIC_CONTEXT]
Goal: [USER_INTENT]

Return JSON with:
- Interactive elements (buttons, fields)
- Coordinates (x,y,width,height)
- Confidence scores
- Next best action

Focus on immediate actionable elements only.
```

### Accuracy-Enhanced Prompt (Sonnet Model)
```
Detailed Android UI analysis required:

CONTEXT:
- Device: [FULL_DEVICE_INFO]
- App: [DETAILED_APP_CONTEXT]
- User History: [EXTENSIVE_ACTION_HISTORY]
- Current Goal: [SPECIFIC_USER_INTENT]
- Special Requirements: [STEALTH_LEVEL, PERFORMANCE_NEEDS]

COMPREHENSIVE ANALYSIS:
1. Element Detection with sub-pixel accuracy
2. Context understanding including business logic
3. User flow optimization
4. Error state identification
5. Accessibility compliance
6. Performance impact assessment
7. Alternative approach evaluation

Provide exhaustive JSON with maximum detail and confidence assessment.
```

## Response Parsing Excellence

### JSON Extraction Patterns
```kotlin
// Robust JSON parsing strategies
class ClaudeResponseParser {
    
    fun extractAnalysisJson(response: String): JSONObject {
        // Handle various JSON formats and edge cases
        val jsonPattern = Regex("\\{[\\s\\S]*\\}")
        val match = jsonPattern.find(response)
        return JSONObject(match?.value ?: "{}")
    }
    
    fun parseElements(json: JSONObject): List<UIElement> {
        val elements = mutableListOf<UIElement>()
        val elementsArray = json.optJSONArray("elements") ?: return elements
        
        for (i in 0 until elementsArray.length()) {
            val elementJson = elementsArray.getJSONObject(i)
            elements.add(parseElement(elementJson))
        }
        return elements
    }
    
    fun parseElement(json: JSONObject): UIElement {
        return UIElement(
            type = json.getString("type"),
            text = json.optString("text", ""),
            coordinates = parseCoordinates(json.getJSONObject("coordinates")),
            confidence = json.getDouble("confidence"),
            visibility = json.getString("visibility"),
            recommendedAction = json.getString("recommended_action")
        )
    }
}
```

### Confidence Assessment Framework
```kotlin
data class ConfidenceAssessment(
    val overallConfidence: Double,
    val elementDetection: Double,
    val contextUnderstanding: Double,
    val actionRecommendation: Double,
    val isReliable: Boolean
) {
    fun meetsThreshold(threshold: Double = 0.85): Boolean {
        return overallConfidence >= threshold &&
               elementDetection >= threshold &&
               contextUnderstanding >= (threshold - 0.1)
    }
}
```

### Error Detection and Handling
```kotlin
class AnalysisErrorDetector {
    
    fun detectParsingErrors(response: String): List<ParsingError> {
        val errors = mutableListOf<ParsingError>()
        
        // Check for malformed JSON
        if (!isValidJson(response)) {
            errors.add(ParsingError.MALFORMED_JSON)
        }
        
        // Check for missing required fields
        if (!hasRequiredFields(response)) {
            errors.add(ParsingError.MISSING_FIELDS)
        }
        
        // Check for low confidence scores
        if (hasLowConfidence(response)) {
            errors.add(ParsingError.LOW_CONFIDENCE)
        }
        
        return errors
    }
    
    fun suggestCorrections(response: String, errors: List<ParsingError>): String {
        return when {
            errors.contains(ParsingError.MALFORMED_JSON) -> "Reformat as valid JSON"
            errors.contains(ParsingError.MISSING_FIELDS) -> "Include all required fields"
            errors.contains(ParsingError.LOW_CONFIDENCE) -> "Request higher confidence analysis"
            else -> "Parse as-is"
        }
    }
}
```

## Cost Optimization Strategies

### Model Selection Logic
```kotlin
class CostOptimizer {
    
    fun selectOptimalModel(analysis: VisionAnalysis): ModelSelection {
        return when {
            // Simple UI with few elements - use Haiku
            analysis.elementCount < 10 && analysis.complexityScore < 0.3 -> {
                ModelSelection.HAIKU
            }
            
            // Complex UI or critical workflow - use Sonnet
            analysis.elementCount > 50 || analysis.isCriticalWorkflow -> {
                ModelSelection.SONNET
            }
            
            // Medium complexity - evaluate cost/benefit
            else -> {
                evaluateCostBenefit(analysis)
            }
        }
    }
    
    fun batchProcessRequests(requests: List<VisionRequest>): BatchProcessingResult {
        // Group similar screenshots for batch API calls
        val batches = groupSimilarRequests(requests)
        return processBatches(batches)
    }
}
```

### Caching Strategy
```kotlin
class AnalysisCache {
    
    fun cacheAnalysis(screenshotHash: String, analysis: VisionAnalysis) {
        if (analysis.confidence > 0.9) {
            cache.put(screenshotHash, analysis)
        }
    }
    
    fun getCachedAnalysis(screenshot: Bitmap): VisionAnalysis? {
        val hash = calculateImageHash(screenshot)
        return cache.get(hash)
    }
    
    fun isSimilarScreen(previous: Bitmap, current: Bitmap): Boolean {
        val similarity = calculateImageSimilarity(previous, current)
        return similarity > 0.95 // 95% similar
    }
}
```

## Context Management

### Conversation State Tracking
```kotlin
data class ConversationContext(
    val sessionId: String,
    val actionHistory: List<String>,
    val previousScreenshots: List<String>, // Hashes
    val userPreferences: UserPreferences,
    val currentGoal: String,
    val appContext: AppContext
)

class ContextManager {
    
    fun buildContextualPrompt(
        context: ConversationContext,
        currentScreenshot: Bitmap
    ): String {
        val relevantHistory = getRelevantHistory(context, currentScreenshot)
        val userIntent = refineUserIntent(context.currentGoal, relevantHistory)
        
        return """
        Previous context: ${context.actionHistory.takeLast(5).joinToString(", ")}
        User intent refinement: $userIntent
        App familiarity: ${context.appContext.familiarityLevel}
        Preferred interaction patterns: ${context.userPreferences.interactionStyle}
        """.trimIndent()
    }
}
```

## Quality Assurance Framework

### Analysis Validation
```kotlin
class AnalysisValidator {
    
    fun validateAnalysis(analysis: VisionAnalysis): ValidationResult {
        val checks = listOf(
            validateElementCoordinates(analysis.elements),
            validateConfidenceScores(analysis.confidence),
            validateActionSequence(analysis.recommendedActions),
            validateContextUnderstanding(analysis.screenContext),
            validateJsonStructure(analysis.rawResponse)
        )
        
        val failedChecks = checks.filter { !it.isValid }
        return ValidationResult(
            isValid = failedChecks.isEmpty(),
            issues = failedChecks.map { it.issue },
            suggestions = failedChecks.map { it.suggestion }
        )
    }
}
```

### Fallback Strategy
```kotlin
class ClaudeFallbackManager {
    
    fun createFallbackPrompts(primaryPrompt: String): List<String> {
        return listOf(
            // Simplified version
            simplifyPrompt(primaryPrompt),
            // Focused version (specific element type)
            focusOnElements(primaryPrompt, "buttons"),
            focusOnElements(primaryPrompt, "text_fields"),
            // Context-only version
            contextOnlyPrompt(primaryPrompt),
            // Basic version
            basicDetectionPrompt()
        )
    }
}
```

## Performance Requirements

### Response Time Targets
- **API Call Latency**: <1 second for Haiku, <2 seconds for Sonnet
- **Parsing Time**: <100ms for JSON extraction
- **Validation Time**: <50ms for analysis validation
- **Total Processing**: <2 seconds end-to-end

### Accuracy Metrics
- **Element Detection**: >85% correct identification
- **Coordinate Accuracy**: <5 pixels deviation
- **Confidence Calibration**: <10% over/under confidence
- **Action Appropriateness**: >90% suitable recommendations

### Cost Efficiency
- **API Cost Per Analysis**: <$0.005 for Haiku, <$0.015 for Sonnet
- **Cache Hit Rate**: >30% for similar screens
- **Batch Efficiency**: >20% cost reduction for batch processing

## Monitoring and Analytics

### Key Metrics
```kotlin
data class ClaudeMetrics(
    val apiCallsPerHour: Int,
    val averageCostPerCall: Double,
    val averageResponseTime: Double,
    val accuracyRate: Double,
    val cacheHitRate: Double,
    val errorRate: Double,
    val userSatisfaction: Double
)
```

### Alert Conditions
- **API Error Rate**: >5% failure rate
- **Cost Spike**: >200% normal spending
- **Accuracy Drop**: <80% correct analysis
- **Response Time**: >3 seconds average

## Common Issues & Solutions

### Issue: Claude returns inconsistent JSON format
**Solution**: Implement flexible parsing, multiple format handlers, validation and correction

### Issue: High API costs with frequent analysis
**Solution**: Implement intelligent caching, batch processing, model selection based on complexity

### Issue: Low confidence scores for complex UIs
**Solution**: Use multi-step analysis, break down complex screens, enhance prompts with context

### Issue: Analysis takes too long for real-time use
**Solution**: Implement parallel processing, optimize image size, use faster models for simple cases

### Issue: Context loss across multiple screenshots
**Solution**: Maintain conversation history, use incremental analysis, implement context-aware prompts

## Development Checklist

- [ ] Claude API integration setup
- [ ] Prompt engineering templates
- [ ] Response parsing pipeline
- [ ] Cost optimization strategies
- [ ] Context management system
- [ ] Quality validation framework
- [ ] Error handling and fallbacks
- [ ] Performance monitoring
- [ ] Caching implementation
- [ ] Batch processing optimization
- [ ] Analytics and metrics
- [ ] Testing and validation

## Communication Protocol

### Status Reporting
```json
{
  "agent": "claude-integrator",
  "status": "analysis_complete",
  "model_used": "claude-3-haiku",
  "analysis_results": {
    "elements_detected": 12,
    "confidence_average": 0.89,
    "processing_time_ms": 650,
    "api_cost": 0.003,
    "cache_hit": false
  },
  "issues": [],
  "next_action": "pass_to_action_executor"
}
```

### Error Notifications
```json
{
  "agent": "claude-integrator",
  "status": "error",
  "error_type": "api_rate_limit",
  "error_message": "Claude API rate limit exceeded",
  "suggested_action": "wait_and_retry",
  "fallback_available": true,
  "retry_after_seconds": 60
}
```

## Dependencies

### Required APIs
- Claude Vision API access (Anthropic)
- Rate limiting and quota management
- Network connectivity for API calls

### External Libraries
- JSON parsing and validation
- HTTP client for API calls
- Image processing utilities

### System Requirements
- Sufficient network bandwidth for image uploads
- Local storage for caching
- Processing power for response parsing

---

**Last Updated**: January 2026  
**Version**: 1.0  
**Status**: Ready for Implementation