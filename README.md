# 🦞 ClawDroid

**A standalone AI agent that runs entirely on Android. No server required.**

ClawDroid is an open-source AI agent that runs on any Android device. Text it on Telegram, and it does things: sends messages, makes calls, sets alarms, searches the web, and controls any app on your phone. No Mac Mini. No Gateway. No companion setup.

---

## Why ClawDroid?

[OpenClaw](https://github.com/openclaw/openclaw) proved that AI agents you can text from anywhere are the future. But OpenClaw requires a Mac/Linux/Windows machine running the Gateway 24/7. Their [Android app](https://github.com/openclaw/openclaw/tree/main/apps/android) is just a **companion node** — it connects to the Gateway for chat and camera, but can't run standalone.

**ClawDroid is different. The agent runs entirely on the Android device:**

| | OpenClaw | OpenClaw Android | ClawDroid |
|---|----------|------------------|-----------|
| **Standalone?** | ✓ (on desktop) | ✗ Needs Gateway | ✓ Fully standalone |
| **Hardware** | Mac/Linux/Windows | Phone + Desktop | Phone only |
| **Cost** | $500+ Mac Mini | Phone + Mac Mini | $50 used phone |
| **Connectivity** | WiFi/Ethernet | Needs Gateway connection | Cellular + WiFi |
| **UI Automation** | Browser (CDP) | None | Any Android app |
| **SMS/Calls** | Via node | Limited | Native |

Take any old Android phone, install ClawDroid, connect Telegram, put it in a drawer. You now have a personal AI agent you can text from anywhere — no other devices required.

---

## What Can It Do?

### 💬 Instant Actions (via Android Intents)
> *"Send a text to Mom saying I'll be late"*  
> *"Set an alarm for 7am tomorrow"*  
> *"Play some jazz on Spotify"*  
> *"Call the pizza place"*

These happen instantly — no screen automation needed.

### 🔍 Answer Questions (via Web Search)
> *"What's the weather in Tokyo?"*  
> *"Who won the game last night?"*  
> *"Find me a good Italian restaurant nearby"*

ClawDroid searches the web and gives you answers directly.

### 📱 Control Any App (via AccessibilityService)
> *"Open WhatsApp and send 'running late' to the work group"*  
> *"Check my bank balance in Chase"*  
> *"Post this to Instagram"*

When there's no API, ClawDroid reads the screen and taps buttons like a human would.

### 📞 Make AI Phone Calls (Coming Soon)
> *"Call the restaurant and make a reservation for 2 at 7pm"*

ClawDroid will call, speak with its AI voice, and report back what happened.

---

## Real Use Cases

**The "Second Phone" Setup**  
Buy a $50 used Android phone. Install ClawDroid. Connect it to your Telegram. Leave it plugged in on your desk. Now you have a personal agent that's always online, always connected, controlled from your main phone or laptop.

**The Traveling Assistant**  
Text your ClawDroid from anywhere: "What time is my flight?" → It checks your email. "Set an alarm for 5am" → Done. "Text Sarah I landed safely" → Sent.

**The Automation Hub**  
ClawDroid can interact with apps that have no API. Check prices on websites, monitor social media, automate repetitive tasks in any Android app.

**Accessibility**  
For users who have difficulty interacting with touchscreens, ClawDroid provides a text-based interface to control the entire phone.

---

## Demo

```
You: What's the weather today?
ClawDroid: Currently 72°F and sunny in San Francisco. 
           High of 78°F expected this afternoon.

You: Set a reminder to buy groceries at 5pm
ClawDroid: ✓ Alarm set for 5:00 PM today with label "Buy groceries"

You: Send a WhatsApp message to Mom saying I'll call her tonight
ClawDroid: ✓ Opening WhatsApp...
           ✓ Found chat with Mom
           ✓ Sent message: "I'll call you tonight"

You: Play something relaxing on Spotify
ClawDroid: ✓ Opening Spotify...
           ✓ Playing "Relaxing Music" playlist
```

---

## Installation

### Prerequisites
- Android 8.0+ device
- Telegram account
- API key from OpenAI, Anthropic, or other supported LLM provider

### Quick Start

1. **Download the APK** from [Releases](https://github.com/yourusername/clawdroid/releases)

2. **Install and grant permissions:**
    - Accessibility Service (for screen control)
    - Notification Access (for reading messages)
    - Phone, SMS, Contacts (for quick actions)

3. **Configure your LLM provider:**
    - Open ClawDroid → Settings → API Configuration
    - Enter your API key (OpenAI, Anthropic, or OpenRouter)

4. **Connect Telegram:**
    - Settings → Telegram → Enter your bot token
    - Get a bot token from [@BotFather](https://t.me/BotFather)

5. **Start the service** and send your first message!

---

## Architecture

ClawDroid follows a priority-based execution model:

```
User Message
     ↓
┌────────────────────────────────┐
│         LLM Decision           │
│    "What's the best approach?" │
└────────────────────────────────┘
     ↓
┌────┴────┬────────┬─────────────┐
↓         ↓        ↓             ↓
Direct   Web    Android      UI
Answer  Search  Intents   Automation
         ↓        ↓             ↓
      Brave    Native       Accessibility
      Search   Actions       Service
         ↓        ↓             ↓
      Fast!   Instant!       Slow but
                            works on
                            any app
```

**The agent tries the fastest approach first:**
1. **Direct answer** — If it knows the answer, just respond
2. **Web search** — For current information, search and summarize
3. **Android Intents** — For system actions (SMS, calls, alarms), use native APIs
4. **UI Automation** — Only when needed, read screen and tap buttons

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** Clean Architecture + MVI
- **DI:** Hilt
- **Agent Framework:** [Koog](https://github.com/JetBrains/koog) by JetBrains
- **LLM Support:** OpenAI, Anthropic, OpenRouter (any OpenAI-compatible API)
- **Screen Control:** Android AccessibilityService
- **Notifications:** NotificationListenerService
- **Interface:** Telegram Bot API

---

## Project Structure

```
clawdroid/
├── app/                    # Android app module
├── domain/                 # Business logic & use cases
├── data/                   # Repositories & data sources
├── framework/              # Android-specific implementations
│   ├── accessibility/      # Screen reading & control
│   ├── notifications/      # Notification listening
│   └── telegram/           # Telegram bot integration
└── agent/                  # AI agent implementation
    ├── tools/              # Device tools, web search, etc.
    ├── prompts/            # System prompts
    └── strategies/         # Execution strategies
```

---

## Supported Actions

### Quick Actions (Instant)
| Action | Example |
|--------|---------|
| Send SMS | "Text John: running late" |
| Make Call | "Call Mom" |
| Set Alarm | "Wake me up at 7am" |
| Play Music | "Play jazz on Spotify" |
| Open URL | "Open twitter.com" |
| Open App | "Open Instagram" |
| Set Timer | "Set a 10 minute timer" |

### UI Actions (Any App)
| Action | Description |
|--------|-------------|
| Click | Tap on any element |
| Long Press | Long press for context menus |
| Type Text | Enter text in any field |
| Scroll | Scroll up/down/left/right |
| Back/Home | Navigate the system |
| Read Screen | Get all visible elements |

---

## Roadmap

- [x] Core AccessibilityService integration
- [x] Koog agent framework integration
- [x] Multi-provider LLM support
- [x] Basic UI automation (click, type, scroll)
- [x] Web search tool
- [ ] **Telegram bot interface** ← Current focus
- [ ] Quick action intents (SMS, calls, alarms)
- [ ] WhatsApp message reading/sending
- [ ] Conversation persistence
- [ ] AI phone calls (via Twilio/Bland.ai)
- [ ] Skills/plugins system
- [ ] On-device LLM option (Gemini Nano)

---

## Comparison with Similar Projects

| Project | Platform | Standalone? | Approach | Status |
|---------|----------|-------------|----------|--------|
| **ClawDroid** | Android | ✓ Yes | Native agent + AI | Active |
| OpenClaw | Desktop | ✓ Yes | Node.js + Tools | Very Active |
| OpenClaw Android | Android | ✗ No (companion) | Connects to Gateway | Active |
| DroidRun | Android | ✓ Yes | ADB-based | Active |
| mobile-use | Android/iOS | ✗ No | Python + ADB | Active |

**ClawDroid's advantage:** The only fully standalone Android AI agent. No desktop required, no Gateway needed, works over cellular, controls any app via AccessibilityService.

---

## Security Considerations

ClawDroid requires significant permissions to function. Please be aware:

- **Accessibility Service** can see and interact with any app
- **Notification Access** can read all notifications
- **Phone/SMS** permissions enable call and text features
- **API keys** are stored locally using EncryptedSharedPreferences

**Recommendations:**
- Use on a dedicated device, not your primary phone
- Don't store sensitive credentials in apps ClawDroid accesses
- Review the source code before use
- Use a separate Telegram bot token

---

## Contributing

Contributions are welcome! Areas where help is needed:

- **Telegram integration** — Implementing the bot polling service
- **Quick actions** — Adding more Android Intent-based actions
- **UI automation** — Improving reliability of screen interactions
- **Testing** — Writing tests for agent behaviors
- **Documentation** — Improving setup guides

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

## Acknowledgments

- [OpenClaw](https://github.com/openclaw/openclaw) — For proving the concept
- [Koog](https://github.com/JetBrains/koog) — JetBrains' excellent Kotlin agent framework
- [AndroidWorld](https://github.com/google-research/android_world) — Google's benchmark that informed our approach

---

<p align="center">
  <b>ClawDroid</b> — The AI agent that doesn't need a Gateway.
  <br>
  <a href="https://github.com/yourusername/clawdroid">GitHub</a> •
  <a href="https://t.me/clawdroid">Telegram</a> •
  <a href="https://twitter.com/clawdroid">Twitter</a>
</p>