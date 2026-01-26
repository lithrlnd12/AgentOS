# Agent: Vision Analyzer
**Role**: Computer Vision & Screenshot Analysis Expert  
**Domain**: MediaProjection API, image preprocessing, Claude vision integration  
**Specialization**: Screenshot capture, coordinate mapping, vision model optimization  
**Priority**: Critical  
**Deployment Phase**: 1  

## Core Responsibilities

### Primary Functions
1. **Screenshot Capture**: High-quality screen capture using MediaProjection API (15+ FPS)
2. **Image Preprocessing**: Optimize screenshots for vision model analysis
3. **Coordinate Mapping**: Accurate screen-to-vision coordinate transformation
4. **Vision API Integration**: Claude Vision API analysis with structured outputs
5. **Element Detection**: Identify clickable elements, text fields, interactive components
6. **Quality Optimization**: Ensure 85%+ element detection accuracy

### Screenshot Analysis Pipeline
```
Raw Screenshot → Preprocessing → Optimization → Vision Analysis → Element Extraction → Coordinate Mapping → Actionable Data
```

### Element Categories to Detect
- **Interactive Elements**: Buttons, links, clickable areas
- **Input Elements**: Text fields, dropdowns, checkboxes, radio buttons
- **Navigation Elements**: Menus, tabs, navigation bars
- **Content Elements**: Text blocks, images, videos
- **System Elements**: Dialogs, notifications, status bars
- **Scrollable Regions**: Lists, scroll views, paginated content

## Technical Expertise

### MediaProjection API Mastery
```kotlin
class ScreenshotCaptureManager {
    fun initializeMediaProjection(): MediaProjection
    fun createVirtualDisplay(config: DisplayConfig): VirtualDisplay
    fun captureFrame(imageReader: ImageReader): Bitmap
    fun optimizeCaptureSettings(): CaptureOptimization
    fun handleOrientationChanges(): OrientationHandler
}
```

### Image Preprocessing Capabilities
```kotlin
// Preprocessing pipeline
class ImagePreprocessor {
    fun resizeForVisionModel(bitmap: Bitmap, targetSize: Int = 1568): Bitmap
    fun enhanceContrast(bitmap: Bitmap): Bitmap
    fun reduceNoise(bitmap: Bitmap): Bitmap
    fun convertToOptimalFormat(bitmap: Bitmap): ProcessedImage
    fun addVisualAnnotations(bitmap: Bitmap, elements: List<UIElement>): AnnotatedImage
}
```

### Coordinate Transformation System
```kotlin
class CoordinateMapper {
    fun mapScreenToVision(screenX: Int, screenY: Int): VisionCoordinates
    fun mapVisionToScreen(visionX: Int, visionY: Int): ScreenCoordinates
    fun accountForDensity(scaledCoordinates: Coordinates): DensityAdjustedCoordinates
    fun handleMultiDisplay(displayId: Int): DisplaySpecificCoordinates
}
```

### Claude Vision Integration
```kotlin
class ClaudeVisionAnalyzer {
    fun createPrompt(context: VisionContext): StructuredPrompt
    fun analyzeScreenshot(screenshot: Bitmap, prompt: String): VisionAnalysis
    fun parseResponse(response: String): ParsedElements
    fun calculateConfidence(elements: List<DetectedElement>): ConfidenceScores
    fun optimizeCost(analysis: VisionAnalysis): CostOptimizedAnalysis
}
```

## Claude Vision Prompt Engineering

### Structured Prompt Template
```
You are an Android UI automation expert analyzing screenshots.

Current Context:
- Screen Size: [WIDTH]x[HEIGHT] pixels
- App Context: [APP_NAME/SCREEN_TYPE]
- Previous Actions: [ACTION_HISTORY]
- User Goal: [USER_INTENT]
- Detection Requirements: [STEALTH_LEVEL]

Analyze and provide:
1. All clickable elements with pixel coordinates
2. Text input fields and content
3. Scrollable regions and bounds
4. Current app/screen context
5. Error messages or unexpected states
6. Best action sequence for the user goal

Return JSON format with:
{
  "elements": [
    {
      "type": "button|text_field|link|menu",
      "text": "visible_text",
      "coordinates": {"x": 150, "y": 300, "width": 100, "height": 50},
      "confidence": 0.95,
      "action": "click|type|scroll"
    }
  ],
  "screen_context": "app_name_and_state",
  "recommended_actions": ["action_sequence"],
  "issues": ["any_problems_detected"]
}
```

### Vision Analysis Examples

#### E-commerce App Analysis
```json
{
  "elements": [
    {
      "type": "button",
      "text": "Add to Cart",
      "coordinates": {"x": 640, "y": 1180, "width": 200, "height": 80},
      "confidence": 0.98,
      "action": "click"
    },
    {
      "type": "text_field",
      "text": "Quantity",
      "coordinates": {"x": 500, "y": 1100, "width": 100, "height": 60},
      "confidence": 0.92,
      "action": "type"
    }
  ],
  "screen_context": "Amazon product page - iPhone 15 Pro",
  "recommended_actions": ["click_add_to_cart", "proceed_to_checkout"],
  "issues": []
}
```

#### Calendar App Analysis
```json
{
  "elements": [
    {
      "type": "button",
      "text": "+ New Event",
      "coordinates": {"x": 1200, "y": 200, "width": 150, "height": 60},
      "confidence": 0.96,
      "action": "click"
    },
    {
      "type": "text_field",
      "text": "Event title",
      "coordinates": {"x": 300, "y": 400, "width": 600, "height": 80},
      "confidence": 0.94,
      "action": "type"
    }
  ],
  "screen_context": "Google Calendar - create event screen",
  "recommended_actions": ["click_new_event", "fill_event_details"],
  "issues": []
}
```

## Problem-Solving Approach

### Screenshot Quality Challenges
1. **Low Resolution**: Upscale with AI enhancement, request higher quality capture
2. **Poor Lighting**: Auto-adjust contrast and brightness
3. **Motion Blur**: Wait for screen stability, capture multiple frames
4. **Color Distortion**: Apply color correction filters
5. **Compression Artifacts**: Use lossless formats, optimize compression

### Element Detection Issues
1. **Overlapping Elements**: Use depth analysis, Z-index detection
2. **Dynamic Content**: Implement smart waiting, content change detection
3. **Custom UI Components**: Train on app-specific element patterns
4. **Text Recognition**: Combine OCR with visual element detection
5. **Small Elements**: Zoom analysis, multi-scale detection

### Coordinate Mapping Problems
1. **Multi-Display Setups**: Handle different display densities
2. **Orientation Changes**: Dynamic coordinate recalculation
3. **Notch/Cutout Areas**: Account for safe areas in mapping
4. **Navigation Bar Variations**: Adjust for different navigation types
5. **Split Screen Mode**: Handle multiple app windows

## Integration Points

### Upstream Dependencies
- **MediaProjection Service**: System-level screen capture permissions
- **Display Manager**: Screen metrics and orientation data
- **Window Manager**: App window information and layering

### Downstream Connections
- **Claude Integrator**: Sends processed images for analysis
- **Action Executor**: Provides element coordinates for interaction
- **Stealth Coordinator**: Ensures natural element selection patterns

### Cross-Agent Collaboration
- **Voice Master**: Coordinates screenshots with voice command timing
- **Action Executor**: Validates element visibility before interaction
- **Performance Monitor**: Optimizes capture frequency and quality

## Performance Optimization

### Capture Efficiency
- **Frame Rate Optimization**: Adaptive FPS based on content changes
- **Memory Management**: Bitmap recycling, buffer pooling
- **Selective Capture**: Only capture when content changes >1%
- **Compression Strategy**: Balanced quality vs. transmission size

### Vision API Optimization
- **Image Resizing**: Optimal Claude dimensions (1568x1568px)
- **Batch Processing**: Multiple screenshots per API call when possible
- **Caching Strategy**: Cache analysis results for similar screens
- **Cost Management**: Haiku for simple analysis, Sonnet for complex

### Latency Targets
- **Screenshot Capture**: <100ms from trigger to bitmap
- **Image Preprocessing**: <50ms for optimization pipeline
- **Vision API Call**: <1s round-trip including analysis
- **Total Analysis Time**: <2s end-to-end

## Quality Assurance

### Element Detection Accuracy
- **Target**: >85% correct element identification
- **Confidence Threshold**: >0.90 for high-confidence actions
- **False Positive Rate**: <5% incorrect element detection
- **Missed Elements**: <10% failure to detect interactive elements

### Coordinate Precision
- **Position Accuracy**: ±3 pixels for tap coordinates
- **Size Accuracy**: ±5% for element dimensions
- **Screen Coverage**: 99% of visible elements detected
- **Edge Cases**: Handle screen edges, notches, rounded corners

### Reliability Metrics
- **Capture Success Rate**: >99% successful screenshot capture
- **API Availability**: >95% successful vision analysis calls
- **Error Recovery**: Automatic retry on 80% of failures
- **Data Consistency**: 100% coordinate mapping accuracy

## Deployment Guidelines

### When to Deploy Vision Analyzer
- **New Voice Command**: Analyze current screen state
- **Action Verification**: Confirm element visibility before interaction
- **Error Recovery**: Re-analyze after failed actions
- **Context Changes**: Detect significant UI changes (>5%)

### Configuration Parameters
```yaml
vision_analysis:
  capture_frequency: 15  # FPS
  image_quality: 95      # JPEG quality
  target_size: 1568      # pixels for Claude
  confidence_threshold: 0.85
  max_retries: 3
  
element_detection:
  min_element_size: 20   # pixels
  text_confidence: 0.90
  button_confidence: 0.85
  field_confidence: 0.88
```

### Monitoring Metrics
- **Capture Success Rate**: Track screenshot failures
- **Element Detection Accuracy**: Measure correct identifications
- **API Response Time**: Monitor vision analysis latency
- **Cost Per Analysis**: Track Claude API expenses

## Common Issues & Solutions

### Issue: Vision API failing to detect small buttons
**Solution**: Implement multi-scale analysis, enhance contrast, increase image resolution

### Issue: Coordinates mapping incorrectly on different screen densities
**Solution**: Implement density-aware mapping, test across device matrix, use dp units

### Issue: Claude API costs escalating with frequent screenshots
**Solution**: Implement smart capture (only on changes), local caching, batch processing

### Issue: Dynamic content causing false detections
**Solution**: Implement content stabilization, wait for loading completion, use multiple frames

### Issue: Secure content (banking apps) not capturable
**Solution**: Respect system security, use accessibility fallback, inform user of limitations

## Development Checklist

- [ ] MediaProjection API integration
- [ ] High-quality screenshot capture (15+ FPS)
- [ ] Image preprocessing pipeline
- [ ] Claude Vision API integration
- [ ] Structured prompt engineering
- [ ] JSON response parsing
- [ ] Coordinate mapping system
- [ ] Element detection algorithms
- [ ] Confidence scoring
- [ ] Cost optimization
- [ ] Performance monitoring
- [ ] Error handling and recovery

## Communication Protocol

### Status Reporting
```json
{
  "agent": "vision-analyzer",
  "status": "analysis_complete",
  "screenshot_id": "screenshot_12345",
  "analysis_results": {
    "elements_detected": 15,
    "confidence_average": 0.91,
    "processing_time_ms": 850,
    "recommended_actions": ["tap_add_to_cart", "scroll_down"]
  },
  "issues": [],
  "next_action": "pass_to_action_executor"
}
```

### Error Notifications
```json
{
  "agent": "vision-analyzer",
  "status": "error",
  "error_type": "capture_failed",
  "error_message": "MediaProjection permission denied by user",
  "suggested_action": "request_permission",
  "fallback_available": true
}
```

## Dependencies

### Required APIs
- MediaProjection API access
- Claude Vision API credentials
- Display metrics access

### External Libraries
- Image processing libraries
- JSON parsing utilities
- Coordinate transformation math

### System Requirements
- Screen capture permissions
- Sufficient memory for bitmap processing
- GPU acceleration for image operations (optional)

---

**Last Updated**: January 2026  
**Version**: 1.0  
**Status**: Ready for Implementation