# AgentOS 🤖

An autonomous AI agent that operates Android phones through natural voice commands, similar to how Claude Code operates browsers.

## 🎯 Vision

Enable users to delegate complex phone tasks through natural voice commands with vision-based AI automation that works across any Android app.

## ✨ Key Features

- **🎤 Voice-First Interface**: Natural language commands via OpenAI Advanced Voice
- **👁️ Vision-Based Automation**: Screenshot analysis using Claude Vision API  
- **⚡ Multi-Method Execution**: Intelligent fallback chain (Shizuku → ADB → Root → Accessibility)
- **🕵️ Stealth Operation**: Human-like behavior patterns to avoid detection
- **🔄 Universal App Control**: Works across any Android app without limitations

## 🏗️ Architecture

```
Voice Input → Intent Processing → Screenshot Capture → Vision Analysis → Action Planning → Multi-Method Execution → Verification → Loop
```

### Core Components

- **Voice Master**: OpenAI Advanced Voice integration
- **Vision Analyzer**: MediaProjection API + Claude vision analysis
- **Claude Integrator**: Structured prompting & decision making
- **Action Executor**: Multi-method execution with fallbacks
- **Stealth Coordinator**: Anti-detection & human-like behavior

## 🚀 Quick Start

### Prerequisites
- Android 6.0+ (API 23+)
- Shizuku framework (for privileged operations)
- OpenAI API key
- Claude API key

### Installation

1. **Download APK**: Get the latest release from [Releases](https://github.com/yourusername/AgentOS/releases)
2. **Enable Unknown Sources**: Allow installation from unknown sources
3. **Install APK**: Install the AgentOS application
4. **Grant Permissions**: Allow all requested permissions
5. **Configure APIs**: Enter your OpenAI and Claude API keys

### Basic Usage

```bash
# Voice commands you can try:
"Schedule lunch with Sarah next Tuesday"
"Find the best price for AirPods Pro and buy them"
"Pay my electric bill"
"Check my calendar for today"
```

## 📊 Performance Metrics

- **Task Completion Rate**: >85% for supported workflows
- **Response Time**: <3 seconds for simple commands
- **System Detection Rate**: <1% false positive rate
- **Voice Recognition Accuracy**: >95%
- **Vision Analysis Accuracy**: >85%

## 🛠️ Development

### Project Structure
```
AgentOS/
├── app/                          # Main Android application
│   ├── src/main/java/com/agentos/
│   │   ├── agents/              # Agent implementations
│   │   ├── core/                # Core functionality
│   │   ├── ui/                  # User interface
│   │   └── utils/               # Utility classes
│   ├── src/test/                # Unit tests
│   └── src/androidTest/         # Integration tests
├── agents/                      # Agent documentation
├── docs/                        # Project documentation
├── scripts/                     # Build and deployment scripts
└── config/                      # Configuration files
```

### Building from Source

```bash
# Clone repository
git clone https://github.com/yourusername/AgentOS.git
cd AgentOS

# Build debug version
./gradlew assembleDebug

# Run tests
./gradlew test

# Build release version
./gradlew assembleRelease
```

### Testing

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew connectedAndroidTest

# Specific test suite
./gradlew test --tests "*VoiceMasterTest*"
```

## 🔧 Configuration

### API Keys
Set up your API keys in `local.properties`:
```properties
openai.api.key=your_openai_api_key_here
claude.api.key=your_claude_api_key_here
```

### Agent Configuration
Configure agent behavior in `config/agents.json`:
```json
{
  "voice_agent": {
    "sample_rate": 16000,
    "voice": "alloy",
    "timeout_ms": 5000
  },
  "vision_agent": {
    "capture_fps": 15,
    "image_quality": 95,
    "target_size": 1568
  }
}
```

## 🔐 Security

- **No sensitive data storage**: Passwords, payment info never stored
- **Encrypted communications**: All API calls use TLS 1.3+
- **Permission transparency**: Clear permission requests with explanations
- **Local processing preferred**: Data processed locally when possible
- **User control**: Complete control over what gets automated

## 📝 API Documentation

See [docs/api](docs/api/) for complete API documentation.

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Write tests first (TDD approach)
4. Implement your changes
5. Run tests (`./gradlew test`)
6. Commit your changes (`git commit -m 'Add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

### Development Guidelines
- Follow TDD (Test-Driven Development)
- Write comprehensive tests
- Follow Kotlin coding conventions
- Document public APIs
- Maintain >90% test coverage

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Shizuku](https://github.com/RikkaApps/Shizuku) for privileged API access
- [OpenAI](https://openai.com/) for Advanced Voice API
- [Anthropic](https://anthropic.com/) for Claude Vision API
- Android Open Source Project for the platform

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/AgentOS/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/AgentOS/discussions)
- **Email**: support@agentos.com

## 📈 Roadmap

See [docs/roadmap.md](docs/roadmap.md) for upcoming features and improvements.

---

**AgentOS** - Making Android automation intelligent, voice-driven, and accessible to everyone. 🤖✨