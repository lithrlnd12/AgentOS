# AgentOS Development Memory

> **Project**: AgentOS - Autonomous AI Agent for Android
> **Vision**: An AI that operates your Android phone like Claude Code operates a terminal
> **Created**: January 2026
> **Owner**: Aaron

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Technical Architecture Decision](#technical-architecture-decision)
3. [MCP Servers Reference](#mcp-servers-reference)
4. [Android Accessibility Services](#android-accessibility-services)
5. [Claude Computer Use Patterns](#claude-computer-use-patterns)
6. [UI/UX Patterns](#uiux-patterns)
7. [Voice Integration](#voice-integration)
8. [Development Phases](#development-phases)
9. [Code Templates](#code-templates)
10. [Open Questions & Decisions](#open-questions--decisions)

---

## Project Overview

### Core Value Proposition
Enable users to delegate complex multi-step phone tasks through natural language:
- "Find the best price for AirPods Pro and buy them"
- "Schedule lunch with Sarah next Tuesday"
- "Pay my electric bill"

### MVP Features (Android Only)
| Feature | Description |
|---------|-------------|
| F1: Screen Understanding | Read text/UI via Accessibility + Vision |
| F2: Action Execution | Tap, swipe, scroll, type across any app |
| F3: Claude Integration | Send context + intent, parse responses |
| F4: Multi-Step Workflows | Execute sequences, handle errors |
| F5: Chat Interface | Text input, status display, history |

### Success Criteria
- Task completion rate >85%
- Response time <3 seconds for simple commands
- Works across top 20 Android apps

---

## Technical Architecture Decision

### Hybrid Approach: Accessibility + Vision

We will use a **hybrid approach** combining Android Accessibility Services with Claude Vision:

```
┌─────────────────────────────────────────────────────────────┐
│                     User Input (Chat)                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Screen Capture Layer                       │
│  ┌─────────────────────┐    ┌─────────────────────────────┐ │
│  │ Accessibility Tree  │    │     Screenshot (PNG)        │ │
│  │ - Element labels    │    │     - Visual fallback       │ │
│  │ - Bounds/coords     │    │     - Custom views          │ │
│  │ - Clickable states  │    │     - Error recovery        │ │
│  └─────────────────────┘    └─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Claude API (Decision)                     │
│  - Receives: Accessibility tree + Screenshot + User intent   │
│  - Returns: Structured action sequence (JSON)                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Action Executor                           │
│  - Tap(x, y) or Tap(element_id)                             │
│  - Type(text), Swipe(direction), Press(BACK/HOME)           │
│  - Verification screenshot after each action                 │
└─────────────────────────────────────────────────────────────┘
```

### Why Hybrid?

| Approach | Speed | Reliability | Works on Custom Views |
|----------|-------|-------------|----------------------|
| Accessibility Only | Fast | Medium (poor labels) | No |
| Vision Only | Slow | High | Yes |
| **Hybrid** | **Fast w/ fallback** | **High** | **Yes** |

### Technology Stack

```yaml
Android App:
  language: Kotlin
  ui: Jetpack Compose
  architecture: MVI + Clean Architecture
  minSdk: 26 (Android 8.0)
  targetSdk: 34

Key APIs:
  - AccessibilityService (screen reading + actions)
  - MediaProjection (screenshots)
  - Claude API (decision making + vision)

Libraries:
  - Retrofit/OkHttp (API calls)
  - Room (local context storage)
  - Kotlin Coroutines + Flow (async/streaming)
  - Coil (image handling)
```

---

## MCP Servers Reference

### Available MCPs for Development/Testing

| MCP | Language | Best For | Key Features |
|-----|----------|----------|--------------|
| [mobile-mcp](https://github.com/mobile-next/mobile-mcp) | TypeScript | Reference implementation | Accessibility + screenshots, iOS & Android |
| [android-adb-mcp-server](https://github.com/landicefu/android-adb-mcp-server) | TypeScript | Basic control | Clipboard, image formats |
| [adb-mcp](https://github.com/srmorete/adb-mcp) | TypeScript | UI automation | UI hierarchy inspection, logcat |
| [android-mcp-server](https://github.com/minhalvp/android-mcp-server) | Python | Simplicity | Smart UI parsing, screenshot compression |

### mobile-mcp Architecture (Reference)

Key patterns to learn from:

```typescript
// 1. Robot Interface Abstraction
interface Robot {
  tap(x: number, y: number): Promise<void>;
  swipe(direction: SwipeDirection): Promise<void>;
  sendKeys(text: string): Promise<void>;
  getScreenshot(): Promise<Buffer>;
  getElementsOnScreen(): Promise<ScreenElement[]>;
}

// 2. ScreenElement Structure
interface ScreenElement {
  type: string;        // Button, TextField, etc.
  text?: string;       // Visible text
  label?: string;      // Accessibility label
  identifier?: string; // Resource ID
  rect: { x, y, width, height };
  focused?: boolean;
}

// 3. Retry Logic for Flaky Operations
async function getUiAutomatorDump(): Promise<string> {
  for (let i = 0; i < 10; i++) {
    const result = adb("shell", "uiautomator", "dump", "/dev/tty");
    if (!result.includes("null root node")) return result;
  }
  throw new Error("Failed to dump UI hierarchy");
}
```

---

## Android Accessibility Services

### Service Setup

**AndroidManifest.xml:**
```xml
<service
    android:name=".AgentAccessibilityService"
    android:label="@string/accessibility_service_label"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

**res/xml/accessibility_service_config.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_service_description"
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFlags="flagDefault|flagReportViewIds|flagIncludeNotImportantViews"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="true"
    android:canTakeScreenshot="true"
    android:settingsActivity=".SettingsActivity" />
```

### Key Operations

```kotlin
// Get UI Hierarchy
val rootNode = rootInActiveWindow

// Find by Resource ID
val nodes = rootNode.findAccessibilityNodeInfosByViewId("com.app:id/button")

// Find by Text
val nodes = rootNode.findAccessibilityNodeInfosByText("Submit")

// Perform Click
node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

// Type Text (API 21+)
val args = Bundle().apply {
    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Hello")
}
node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

// Gesture: Tap at coordinates (API 24+)
fun tap(x: Float, y: Float) {
    val path = Path().apply { moveTo(x, y) }
    val gesture = GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        .build()
    dispatchGesture(gesture, null, null)
}

// Gesture: Swipe
fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 300) {
    val path = Path().apply {
        moveTo(startX, startY)
        lineTo(endX, endY)
    }
    val gesture = GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        .build()
    dispatchGesture(gesture, null, null)
}

// Global Actions
performGlobalAction(GLOBAL_ACTION_BACK)
performGlobalAction(GLOBAL_ACTION_HOME)
performGlobalAction(GLOBAL_ACTION_RECENTS)

// Screenshot (API 30+)
takeScreenshot(Display.DEFAULT_DISPLAY, executor, object : TakeScreenshotCallback {
    override fun onSuccess(screenshot: ScreenshotResult) {
        val bitmap = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
        // Use bitmap
    }
    override fun onFailure(errorCode: Int) { /* Handle */ }
})
```

### Limitations to Remember

1. **Cannot bypass biometrics** - Fingerprint, face unlock not accessible
2. **Cannot start itself** - User must manually enable in Settings
3. **Node staleness** - Always call `node.refresh()` before actions
4. **Android 13+ sideloading restrictions** - May need Play Store distribution
5. **Some apps detect simulated clicks** - May need visual fallback

### API Availability Matrix

| Feature | Min API | Notes |
|---------|---------|-------|
| Basic AccessibilityService | 4 | Core |
| ACTION_SET_TEXT | 21 | Text input |
| dispatchGesture() | 24 | Tap, swipe |
| GLOBAL_ACTION_TAKE_SCREENSHOT | 28 | System screenshot |
| takeScreenshot() with callback | 30 | Bitmap access |

---

## Claude Computer Use Patterns

### Action Schema for Mobile

```json
{
    "tool": "android_device",
    "input": {
        "action": "tap",
        "coordinate": [500, 800]
    }
}
```

```json
{
    "tool": "android_device",
    "input": {
        "action": "tap",
        "element_id": "com.example:id/submit_button"
    }
}
```

### Supported Actions

| Action | Parameters | Description |
|--------|------------|-------------|
| `tap` | `coordinate` or `element_id` | Single tap |
| `long_press` | `coordinate`, `duration` | Long press |
| `double_tap` | `coordinate` | Double tap |
| `swipe` | `direction`, `distance` | Directional swipe |
| `type_text` | `text` | Type string |
| `press_key` | `key` (BACK, HOME, ENTER) | System keys |
| `screenshot` | - | Capture screen |
| `get_ui_hierarchy` | - | Get accessibility tree |
| `launch_app` | `package_name` | Open app |
| `terminate_app` | `package_name` | Close app |

### Agent Loop Pattern

```kotlin
suspend fun agentLoop(userIntent: String, maxIterations: Int = 10) {
    val messages = mutableListOf<Message>()
    messages.add(Message.user(userIntent))

    repeat(maxIterations) { iteration ->
        // 1. Capture current state
        val screenshot = captureScreenshot()
        val uiHierarchy = getAccessibilityTree()

        // 2. Send to Claude
        val response = claudeApi.createMessage(
            messages = messages,
            tools = androidTools,
            images = listOf(screenshot)
        )

        messages.add(Message.assistant(response))

        // 3. Check for tool use
        val toolUses = response.content.filterIsInstance<ToolUse>()
        if (toolUses.isEmpty()) {
            // Task complete
            return
        }

        // 4. Execute actions
        val results = toolUses.map { toolUse ->
            val result = executeAction(toolUse.input)
            ToolResult(
                toolUseId = toolUse.id,
                content = result.output,
                isError = result.error != null
            )
        }

        messages.add(Message.user(results))
    }
}
```

### Prompt Engineering Tips

1. **Explicit verification**: "After each step, take a screenshot and verify the action succeeded"
2. **Keyboard shortcuts**: Use for tricky UI elements (dropdowns, scrollbars)
3. **Structured credentials**: Use XML tags for sensitive data
4. **Chain actions**: Batch multiple tool calls when feasible

### Error Handling

```kotlin
sealed class ActionResult {
    data class Success(val output: String, val screenshot: Bitmap?) : ActionResult()
    data class Error(val message: String, val screenshot: Bitmap?) : ActionResult()
}

// Always include screenshot in error responses for Claude to understand what went wrong
```

---

## UI/UX Patterns

### Architecture: MVI

```kotlin
// State
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val currentTask: TaskState = TaskState.Idle,
    val isProcessing: Boolean = false
)

sealed class TaskState {
    object Idle : TaskState()
    data class InProgress(val description: String, val progress: Float) : TaskState()
    data class Completed(val result: String) : TaskState()
    data class Error(val message: String) : TaskState()
}

// Intent
sealed class ChatIntent {
    data class SendMessage(val content: String) : ChatIntent()
    object CancelTask : ChatIntent()
    data class ConfirmAction(val actionId: String) : ChatIntent()
}

// ViewModel
class ChatViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun processIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> handleSendMessage(intent.content)
            // ...
        }
    }
}
```

### LazyColumn for Chat

```kotlin
@Composable
fun ChatMessageList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = messages,
            key = { it.id }  // CRITICAL: stable keys
        ) { message ->
            ChatBubble(
                message = message,
                modifier = Modifier.animateItem()
            )
        }
    }

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
}
```

### Streaming Response

```kotlin
fun streamClaudeResponse(prompt: String): Flow<String> = callbackFlow {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://api.anthropic.com/v1/messages")
        .post(/* body with stream: true */)
        .build()

    val eventSource = EventSources.createFactory(client)
        .newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                val delta = parseContentDelta(data)
                if (delta != null) trySend(delta)
            }
            override fun onClosed(eventSource: EventSource) { close() }
        })

    awaitClose { eventSource.cancel() }
}
```

### Floating Overlay (for showing status while other apps run)

```kotlin
class OverlayService : Service() {
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    override fun onCreate() {
        super.onCreate()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
        }

        val composeView = ComposeView(this).apply {
            setContent {
                AgentStatusBubble(/* state */)
            }
        }
        windowManager.addView(composeView, params)
    }
}
```

**Required permission:**
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
```

---

## Voice Integration

### Multi-Model Architecture

We use a **multi-model approach** for voice commands:
- **OpenAI** handles voice-to-text (superior transcription with streaming)
- **Claude** handles decision-making and action planning

This leverages each model's strengths - ChatGPT Advanced Voice provides excellent real-time transcription, while Claude excels at reasoning and tool use.

```
┌─────────────────────────────────────────────────────────────┐
│                    Voice Input (Microphone)                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              OpenAI Realtime API / Whisper                   │
│  - Real-time transcription with streaming                    │
│  - Voice Activity Detection (VAD)                            │
│  - Noise reduction (near-field / far-field)                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ (Transcribed text)
┌─────────────────────────────────────────────────────────────┐
│                    Claude API (Decision)                     │
│  - Interprets user intent                                    │
│  - Plans multi-step actions                                  │
│  - Executes via tool use                                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Action Executor                           │
└─────────────────────────────────────────────────────────────┘
```

### Why ChatGPT for Voice?

| Feature | ChatGPT Advanced Voice | Gemini |
|---------|----------------------|--------|
| Real-time transcription | Yes (streaming) | Limited |
| Text output available | Yes | No (voice-only) |
| API access | Realtime API (GA Aug 2025) | Limited |
| Quality | Excellent | Good |

### OpenAI Transcription Options

#### Option 1: Realtime API (Recommended for Live Voice)

```kotlin
// WebSocket connection to OpenAI Realtime API
class RealtimeTranscriptionClient {
    private val client = OkHttpClient()

    fun connect(onTranscript: (String) -> Unit): WebSocket {
        val request = Request.Builder()
            .url("wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview")
            .header("Authorization", "Bearer $OPENAI_API_KEY")
            .header("OpenAI-Beta", "realtime=v1")
            .build()

        return client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val event = parseEvent(text)
                when (event.type) {
                    "conversation.item.input_audio_transcription.delta" -> {
                        // Streaming transcript
                        onTranscript(event.delta)
                    }
                    "conversation.item.input_audio_transcription.completed" -> {
                        // Final transcript
                        onTranscript(event.transcript)
                    }
                }
            }
        })
    }

    fun sendAudio(webSocket: WebSocket, audioBase64: String) {
        val event = """
        {
            "type": "input_audio_buffer.append",
            "audio": "$audioBase64"
        }
        """.trimIndent()
        webSocket.send(event)
    }
}
```

#### Option 2: Whisper API (For Recorded Audio)

```kotlin
// For transcribing recorded audio files
suspend fun transcribeAudio(audioFile: File): String {
    val client = OkHttpClient()

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("model", "whisper-1")
        .addFormDataPart("file", audioFile.name,
            audioFile.asRequestBody("audio/wav".toMediaType()))
        .build()

    val request = Request.Builder()
        .url("https://api.openai.com/v1/audio/transcriptions")
        .header("Authorization", "Bearer $OPENAI_API_KEY")
        .post(requestBody)
        .build()

    return withContext(Dispatchers.IO) {
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body?.string() ?: "")
        json.getString("text")
    }
}
```

#### Option 3: Offline Whisper (No API Costs)

For offline transcription, use [whisper_android](https://github.com/vilassn/whisper_android):

```kotlin
// TensorFlow Lite Whisper implementation
class OfflineWhisper(context: Context) {
    private val interpreter: Interpreter

    init {
        val model = loadModelFile(context, "whisper-tiny.tflite")
        interpreter = Interpreter(model)
    }

    fun transcribe(audioBuffer: ShortArray): String {
        // Process audio through TFLite model
        val input = preprocessAudio(audioBuffer)
        val output = Array(1) { FloatArray(OUTPUT_SIZE) }
        interpreter.run(input, output)
        return decodeOutput(output[0])
    }
}
```

### Transcription Models Comparison

| Model | Speed | Accuracy | Cost | Streaming |
|-------|-------|----------|------|-----------|
| `gpt-4o-transcribe` | Fast | Best | $0.006/min | Yes |
| `gpt-4o-mini-transcribe` | Fastest | Very Good | $0.003/min | Yes |
| `whisper-1` | Medium | Good | $0.006/min | No |
| Offline Whisper (tiny) | Fast | Fair | Free | No |

### Android Audio Capture

```kotlin
class AudioCapture(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startCapture(onAudioData: (ByteArray) -> Unit) {
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        ).apply {
            startRecording()
        }

        // Read audio in background
        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(bufferSize)
            while (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    onAudioData(buffer.copyOf(read))
                }
            }
        }
    }

    fun stopCapture() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
```

### Voice Activation Options

1. **Push-to-talk**: User holds button while speaking
2. **Wake word**: "Hey Agent" detection (on-device with Porcupine/Snowboy)
3. **Always listening**: VAD-triggered (uses more battery)

```kotlin
// Push-to-talk implementation
@Composable
fun VoiceButton(
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(72.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onStartListening()
                        tryAwaitRelease()
                        isPressed = false
                        onStopListening()
                    }
                )
            }
            .background(
                if (isPressed) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primaryContainer,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice input",
            tint = if (isPressed) MaterialTheme.colorScheme.onPrimary
                   else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
```

### Full Voice Pipeline

```kotlin
class VoicePipeline(
    private val audioCapture: AudioCapture,
    private val transcriptionClient: RealtimeTranscriptionClient,
    private val claudeClient: ClaudeApiClient,
    private val actionExecutor: ActionExecutor
) {
    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    fun startListening() {
        _state.value = VoiceState.Listening

        val webSocket = transcriptionClient.connect { transcript ->
            _state.value = VoiceState.Transcribing(transcript)
        }

        audioCapture.startCapture { audioData ->
            val base64 = Base64.encodeToString(audioData, Base64.NO_WRAP)
            transcriptionClient.sendAudio(webSocket, base64)
        }
    }

    suspend fun processTranscript(transcript: String) {
        _state.value = VoiceState.Processing

        // Send to Claude for action planning
        val response = claudeClient.createMessage(
            messages = listOf(Message.user(transcript)),
            tools = androidTools
        )

        // Execute actions
        response.toolUses.forEach { toolUse ->
            _state.value = VoiceState.Executing(toolUse.description)
            actionExecutor.execute(toolUse)
        }

        _state.value = VoiceState.Completed
    }
}

sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    data class Transcribing(val partialText: String) : VoiceState()
    object Processing : VoiceState()
    data class Executing(val action: String) : VoiceState()
    object Completed : VoiceState()
}
```

### Required Permissions

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
```

### Voice Integration Resources

- [OpenAI Realtime API Docs](https://platform.openai.com/docs/guides/realtime)
- [OpenAI Realtime Transcription](https://platform.openai.com/docs/guides/realtime-transcription)
- [whisper_android (Offline)](https://github.com/vilassn/whisper_android)
- [Picovoice Porcupine (Wake Word)](https://picovoice.ai/platform/porcupine/)

---

## Development Phases

### Phase 1: Foundation (Weeks 1-2)
- [ ] Project scaffolding (Gradle, dependencies)
- [ ] AccessibilityService implementation
- [ ] Basic screen reading (UI tree parsing)
- [ ] Simple action execution (tap, type)
- [ ] Claude API integration (non-streaming)

**Deliverable**: "Open Settings and enable dark mode"

### Phase 2: Intelligence Layer (Weeks 3-4)
- [ ] Screenshot capture + compression
- [ ] Hybrid context formatting (accessibility + vision)
- [ ] Action parsing from Claude responses
- [ ] Multi-step workflow engine
- [ ] Error handling + recovery

**Deliverable**: "Find a pizza place nearby and get directions"

### Phase 3: Polish & Safety (Weeks 5-6)
- [ ] User confirmation flows for sensitive actions
- [ ] Permission management UI
- [ ] Chat interface refinement
- [ ] Floating overlay for status
- [ ] Testing across common apps

**Deliverable**: MVP ready for beta testing

### Phase 4: Voice Integration (Weeks 7-8)
- [ ] OpenAI Realtime API integration
- [ ] Audio capture pipeline
- [ ] Push-to-talk UI component
- [ ] Voice → Claude → Action flow
- [ ] Optional: Offline Whisper fallback

**Deliverable**: Voice commands working end-to-end

### Phase 5: Post-MVP (Weeks 9+)
- [ ] Learning/personalization
- [ ] Wake word detection ("Hey Agent")
- [ ] Proactive suggestions
- [ ] Performance optimization
- [ ] Text-to-speech responses

---

## Code Templates

### Project Structure

```
AgentOS/
├── app/
│   ├── src/main/
│   │   ├── java/com/agentos/
│   │   │   ├── AgentOSApplication.kt
│   │   │   ├── accessibility/
│   │   │   │   ├── AgentAccessibilityService.kt
│   │   │   │   ├── ScreenReader.kt
│   │   │   │   └── ActionExecutor.kt
│   │   │   ├── capture/
│   │   │   │   ├── ScreenCapture.kt
│   │   │   │   └── UiHierarchyParser.kt
│   │   │   ├── claude/
│   │   │   │   ├── ClaudeApiClient.kt
│   │   │   │   ├── ActionParser.kt
│   │   │   │   └── ContextBuilder.kt
│   │   │   ├── workflow/
│   │   │   │   ├── WorkflowEngine.kt
│   │   │   │   ├── TaskState.kt
│   │   │   │   └── ActionQueue.kt
│   │   │   ├── ui/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── chat/
│   │   │   │   │   ├── ChatScreen.kt
│   │   │   │   │   ├── ChatViewModel.kt
│   │   │   │   │   └── components/
│   │   │   │   ├── onboarding/
│   │   │   │   └── settings/
│   │   │   ├── overlay/
│   │   │   │   ├── OverlayService.kt
│   │   │   │   └── StatusBubble.kt
│   │   │   ├── voice/
│   │   │   │   ├── AudioCapture.kt
│   │   │   │   ├── RealtimeTranscriptionClient.kt
│   │   │   │   ├── VoicePipeline.kt
│   │   │   │   └── VoiceButton.kt
│   │   │   └── di/
│   │   │       └── AppModule.kt
│   │   └── res/
│   │       ├── xml/
│   │       │   └── accessibility_service_config.xml
│   │       └── values/
│   └── build.gradle.kts
├── gradle/
└── build.gradle.kts
```

### Key Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")

    // JSON
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Image
    implementation("io.coil-kt:coil-compose:2.5.0")

    // DI
    implementation("io.insert-koin:koin-android:3.5.3")
    implementation("io.insert-koin:koin-androidx-compose:3.5.3")

    // Room (local storage)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Voice / Audio (Phase 4)
    // OpenAI Realtime uses WebSocket (already have OkHttp)
    // Optional: Offline Whisper
    // implementation("org.tensorflow:tensorflow-lite:2.14.0")
    // implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
}
```

---

## Open Questions & Decisions

### Decided
- [x] **Architecture**: MVI + Clean Architecture
- [x] **Screen capture**: Hybrid (Accessibility + Vision)
- [x] **Min SDK**: 26 (Android 8.0) for gesture support
- [x] **Voice input**: Multi-model (OpenAI Realtime for transcription → Claude for decisions)

### To Decide
- [ ] **API Key Model**: User provides own key vs. proxy?
- [ ] **Monetization**: Freemium vs. subscription vs. one-time?
- [ ] **Distribution**: Play Store vs. APK initially?
- [ ] **App Categories to Support First**: e-commerce, productivity, communication?

### Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Accessibility API limitations | High | Extensive testing, document app compatibility |
| Claude API costs | Medium | Caching, local processing where possible |
| App UI changes break workflows | High | Robust error recovery, vision fallback |
| Privacy concerns | High | Clear communication, local processing, minimal retention |
| Google Play policy violations | Critical | Review policies, potentially APK distribution first |

---

## Resources

### Official Documentation
- [Android Accessibility Service Guide](https://developer.android.com/guide/topics/ui/accessibility/service)
- [Claude API Documentation](https://docs.anthropic.com/claude/reference)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)

### Reference Implementations
- [mobile-mcp](https://github.com/mobile-next/mobile-mcp) - MCP for mobile automation
- [Jetchat](https://github.com/android/compose-samples/tree/main/Jetchat) - Official Compose chat sample
- [Claude Computer Use Demo](https://github.com/anthropics/anthropic-quickstarts/tree/main/computer-use-demo)

### MCP Servers for Development
- [android-adb-mcp-server](https://github.com/landicefu/android-adb-mcp-server)
- [adb-mcp](https://github.com/srmorete/adb-mcp)
- [android-mcp-server](https://github.com/minhalvp/android-mcp-server)

---

*Last Updated: January 2026*
