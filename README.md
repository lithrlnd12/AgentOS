# AgentOS

> An autonomous AI agent that operates your Android phone like Claude Code operates a terminal.

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com)
[![Min SDK](https://img.shields.io/badge/minSdk-26-orange.svg)](https://developer.android.com/about/versions/oreo)

## Vision

AgentOS enables users to delegate complex multi-step phone tasks through natural language commands:

- *"Find the best price for AirPods Pro and buy them"*
- *"Schedule lunch with Sarah next Tuesday"*
- *"Pay my electric bill"*

Unlike traditional voice assistants (Siri, Google Assistant, Bixby), AgentOS:
- Executes multi-step workflows across multiple apps
- Understands screen context through hybrid AI vision
- Learns and adapts to user preferences
- Handles errors and recovers gracefully

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                 User Input (Voice / Chat)                   │
│         OpenAI Realtime API (Voice Transcription)           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Screen Capture Layer                       │
│  ┌─────────────────────┐    ┌─────────────────────────────┐ │
│  │ Accessibility Tree  │    │     Screenshot (Vision)     │ │
│  │ - Element labels    │    │     - Visual fallback       │ │
│  │ - Bounds/coords     │    │     - Custom views          │ │
│  └─────────────────────┘    └─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  Claude API (Decision Engine)                │
│  - Interprets user intent                                    │
│  - Plans multi-step actions                                  │
│  - Returns structured action sequences                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Action Executor                           │
│  - Tap, swipe, scroll, type                                 │
│  - App navigation                                            │
│  - Verification after each action                           │
└─────────────────────────────────────────────────────────────┘
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVI + Clean Architecture |
| Screen Reading | Android Accessibility Services |
| Vision | Claude API (multimodal) |
| Voice Input | OpenAI Realtime API |
| Decision Engine | Claude API |
| Min SDK | 26 (Android 8.0) |

## Features

### MVP (Phase 1-3)
- [x] Screen understanding via Accessibility Services
- [ ] Claude Vision integration for visual fallback
- [ ] Action execution (tap, swipe, type)
- [ ] Multi-step workflow engine
- [ ] Chat interface
- [ ] User confirmation for sensitive actions

### Post-MVP (Phase 4+)
- [ ] Voice commands via OpenAI Realtime API
- [ ] Proactive suggestions
- [ ] Learning & personalization
- [ ] Background monitoring

## Project Structure

```
AgentOS/
├── app/
│   ├── src/main/
│   │   ├── java/com/agentos/
│   │   │   ├── accessibility/     # AccessibilityService
│   │   │   ├── capture/           # Screen capture & UI parsing
│   │   │   ├── claude/            # Claude API client
│   │   │   ├── workflow/          # Multi-step task engine
│   │   │   ├── ui/                # Jetpack Compose UI
│   │   │   ├── voice/             # Voice input pipeline
│   │   │   └── overlay/           # Floating status UI
│   │   └── res/
│   └── build.gradle.kts
├── docs/
│   └── MEMORY.md                  # Development reference
└── README.md
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Kotlin 1.9+
- Claude API key (from [Anthropic](https://console.anthropic.com))
- OpenAI API key (for voice features)

### Setup

```bash
# Clone the repository
git clone https://github.com/lithrlnd12/AgentOS.git
cd AgentOS

# Open in Android Studio
# Add your API keys to local.properties:
# CLAUDE_API_KEY=your_key_here
# OPENAI_API_KEY=your_key_here

# Build and run
./gradlew assembleDebug
```

### Permissions Required

The app requires these permissions:
- **Accessibility Service** - For screen reading and action execution
- **Overlay Permission** - For floating status indicator
- **Microphone** - For voice commands (optional)

## Development

See [docs/MEMORY.md](docs/MEMORY.md) for detailed development notes including:
- Technical architecture decisions
- Code templates and patterns
- API references
- Development phases

## Security & Privacy

- Never stores sensitive data (passwords, payment info)
- All API calls encrypted (TLS)
- User confirmation required for purchases, messages, deletions
- Screen content processed locally when possible

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting PRs.

## License

MIT License - see [LICENSE](LICENSE) for details.

## Acknowledgments

- [Anthropic](https://anthropic.com) - Claude API
- [OpenAI](https://openai.com) - Realtime API for voice
- [mobile-mcp](https://github.com/mobile-next/mobile-mcp) - Architecture inspiration

---

**Status**: Early Development | **Owner**: Aaron | **Last Updated**: January 2026
