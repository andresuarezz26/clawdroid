<div align="center">

<img src="https://github.com/user-attachments/assets/c8d4f67d-1b94-440d-b128-6275ebfd34a7" width="150" height="150" alt="ClawDroid" />

# ClawDroid

[![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?style=flat&logo=telegram&logoColor=white)](https://telegram.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**An AI agent that runs entirely on Android.
Text it. It does things. No server required.**
</div>

ClawDroid is an open-source AI agent that runs on any Android device. 
Text it on **Telegram** or use the built-in chat, and it actually does things:

- 📱 **Control any app** — Post on Instagram, reply to WhatsApp, scroll through LinkedIn
- ⏰ **Run scheduled tasks** — "Apply to 10 jobs on LinkedIn every morning"
- 🔔 **React to notifications** — Auto-reply to emails, respond to DMs, triage messages
- 📅 **Access your data** — "What's on my calendar next week?"

Think ChatGPT/Claude, but with hands.

<br/>

🤖 **Live demo**: ClawDroid manages their own X account **[@clawdroidagent](https://x.com/clawdroidagent)** autonomously. It posts daily, replies to comments, and answers DMs. **[Go DM it →](https://x.com/clawdroidagent)**
<br/>

## How to use it? 

You can build the APK from source or download from Github [releases](https://github.com/andresuarezz26/ClawDroid/releases). 

---

## Why ClawDroid?

📱 **Access what desktop agents can't**  
Some apps only exist on mobile or don't have APIs. Instagram, WhatsApp, TikTok, banking apps, delivery apps — they block desktop automation or don't have web versions. 

🤖 **Automate anything without code**  
No scripting. No Selenium. No Appium. No Zapier. No Make. No n8n. Just tell the agent what you want in plain English, and it figures out how to do it. Automate any app without writing a single line of code.

🚀 **Fast setup**  
Install the APK, add your API key, connect Telegram. Done in 5 minutes.

💰 **Cheap to run**  
Works on any Android phone, old or new. Or use an emulator. No servers, no subscriptions.

🔒 **Privacy-first**  
Conversations and history stay on-device. Only LLM API calls go out.

⚡ **No backend overhead**  
Direct to LLM API. Fewer hops, faster responses.

---

## How it looks?

<img width="300" height="668" alt="screenshot1-clawdroid" src="https://github.com/user-attachments/assets/8e8d195e-6865-4873-8a17-84aab9ea60f3" />
<img width="300" height="668" alt="screenshotnumber2clawdroid" src="https://github.com/user-attachments/assets/2f6fd3f0-3e21-4c71-a819-13b42a5fe6ec" />
<img width="300" height="668" alt="screenshot3_model_selection" src="https://github.com/user-attachments/assets/8f3d0bd0-b176-443b-a1a0-f9e3a1309e3d" />

## Real Use Cases

**The "Second Phone" Setup**
Buy a $50 used Android phone. Install ClawDroid. Connect it to your Telegram. Leave it plugged in on your desk. Now you have a personal agent that's always online, always connected, controlled from your main phone or laptop.

**The Automation Hub**
ClawDroid can interact with apps that have no API. Check prices on websites, monitor social media, automate repetitive tasks in any Android app.

**Accessibility**
For users who have difficulty interacting with touchscreens, ClawDroid provides a text-based interface to control the entire phone.

---

## Installation

### Prerequisites
- Android 7.0+ device (API 24)
- Telegram account
- API key from OpenAI, Anthropic, or Google

### Quick Start

1. **Build from source** or download the APK from [Releases](https://github.com/yourusername/clawdroid/releases)

2. **Install and grant permissions:**
    - Accessibility Service (for screen control)
    - Phone, SMS (for quick actions)

3. **Configure your LLM provider:**
    - Open ClawDroid → Settings → API Configuration
    - Enter your API key

4. **Connect Telegram:**
    - Settings → Telegram → Enter your bot token
    - Get a bot token from [@BotFather](https://t.me/BotFather)

5. **Start the service** and send your first message!

---

## Architecture

ClawDroid uses a 4-tier response strategy. The LLM decides the best approach for each request:

```
User Message (via Telegram or in-app chat)
     |
     v
+----------------------------+
|       LLM Decision         |
|  "What's the best approach?" |
+----------------------------+
     |
     +--------+--------+-----------+
     v        v        v           v
  Direct    Web     Quick       UI
  Answer   Search  Actions   Automation
             |        |           |
          Opens    Android    Accessibility
          Browser  Intents     Service
             |        |           |
          Needs    Instant!    Slow but
          screen               works on
          reading              any app
          to parse
```

1. **Direct answer** — If the LLM knows the answer, just respond
2. **Web search** — Opens a browser search; the agent can read the screen afterward
3. **Quick actions** — For system actions (SMS, calls, alarms, navigation, calendar), use native intents
4. **UI automation** — Read the screen and interact with any app element

---

## Supported Tools

### Device Tools (19 tools — UI Automation)

| Tool | Description |
|------|-------------|
| `getScreen` | Get current screen UI tree with element indices |
| `click` | Tap on an element by index |
| `longClick` | Long press on an element |
| `setText` | Type text into an editable field |
| `clearText` | Clear all text from an editable field |
| `focus` | Focus on an element without clicking |
| `scroll` | Scroll an element (up/down/left/right) |
| `swipe` | Swipe from one point to another |
| `drag` | Drag from one point to another |
| `pressBack` | Press the back button |
| `pressHome` | Press the home button |
| `pressEnter` | Press Enter/Search key on keyboard |
| `openRecentApps` | Open the recent apps switcher |
| `openNotifications` | Pull down the notification shade |
| `openQuickSettings` | Open quick settings panel |
| `launchApp` | Launch any installed app by name |
| `waitForUpdate` | Wait for the screen to settle after an action |
| `taskComplete` | Signal that the task is done |
| `taskFailed` | Signal that the task has failed |

### Quick Action Tools (9 tools — Instant via Intents)

| Tool | Description |
|------|-------------|
| `sendSms` | Send an SMS message |
| `makeCall` | Start a phone call |
| `openUrl` | Open a URL in the default browser |
| `setAlarm` | Set an alarm (24-hour format) |
| `playMusic` | Play music by search query |
| `openSettings` | Open device settings (with optional section) |
| `webSearch` | Open a web search in the browser |
| `createCalendarEvent` | Create a calendar event |
| `startNavigation` | Open navigation to an address |

---

## LLM Providers

ClawDroid supports 3 providers with multiple models each:

| Provider | Models | Default |
|----------|--------|---------|
| **OpenAI** | gpt-4o, gpt-4o-mini, gpt-4-turbo, gpt-5, gpt-5-mini, gpt-5.2 | gpt-5-mini |
| **Anthropic** | claude-3-5-sonnet, claude-3-opus, claude-3-haiku | claude-3-5-sonnet |
| **Google** | gemini-2.0-flash, gemini-2.5-pro, gemini-3-pro-preview | gemini-3-pro-preview |

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Clean Architecture + MVI
- **DI:** Hilt
- **Agent Framework:** [Koog](https://github.com/JetBrains/koog) v0.6.1 by JetBrains
- **LLM Support:** OpenAI, Anthropic, Google
- **Screen Control:** Android AccessibilityService
- **Messaging:** Telegram Bot API (long-polling foreground service)
- **Database:** Room (conversation history, task logs)
- **Networking:** Ktor
- **Security:** EncryptedSharedPreferences for API keys
- **Compile/Target SDK:** 36
- **Min SDK:** 24 (Android 7.0)

---

## Project Structure

```
app/src/main/java/com/aiassistant/
├── agent/                          # AI agent implementation
│   ├── di/                         # Hilt module (AgentModule)
│   ├── AndroidAgentFactory.kt      # Creates Koog agent per LLM provider
│   ├── AgentConfig.kt              # Provider, model, temperature config
│   ├── AgentEventProcessor.kt      # Processes agent lifecycle events
│   ├── AgentExecutor.kt            # Runs agent tasks
│   ├── AgentResult.kt              # Task result model
│   ├── DeviceTools.kt              # 19 UI automation tools
│   ├── QuickActionTools.kt         # 9 quick action tools
│   └── SystemPrompts.kt            # System prompt for the agent
├── data/
│   ├── di/                         # Hilt modules (API keys, network, DB, Telegram)
│   ├── local/
│   │   ├── dao/                    # Room DAOs (TaskLog, TelegramConversation, TelegramMessage)
│   │   ├── entity/                 # Room entities
│   │   └── AppDatabase.kt         # Room database (v2)
│   ├── mapper/                     # ScreenParser, UINodeFormatter
│   ├── remote/
│   │   ├── ApiKeyProvider.kt       # Encrypted API key storage
│   │   └── telegram/               # Telegram API client (Ktor)
│   └── repository/                 # Repository implementations
├── domain/
│   ├── model/                      # Domain models (UINode, ChatMessage, TaskLog, etc.)
│   ├── repository/                 # Repository interfaces
│   └── usecase/                    # Use cases (screen capture, task history, Telegram ops)
├── framework/
│   ├── accessibility/              # AccessibilityService, ActionPerformer
│   ├── permission/                 # PermissionManager (suspendable permission requests)
│   └── telegram/                   # TelegramBotService (foreground), NotificationManager
└── presentation/
    ├── chat/                       # In-app chat UI (ChatScreen, ChatViewModel)
    ├── navigation/                 # Compose navigation
    ├── settings/                   # Telegram settings screen
    ├── theme/                      # Material 3 theme
    ├── ClawdroidApp.kt             # Hilt Application class
    └── MainActivity.kt             # Main activity
```

---

## Telegram Integration

ClawDroid runs a **foreground service** (`TelegramBotService`) that continuously polls the Telegram Bot API for new messages. Key details:

- **Long-polling** with exponential backoff (1s–30s)
- **Foreground service** with persistent notification (survives process death via `START_STICKY`)
- **Conversation history** stored in Room DB — the last 20 messages are included as context for each request
- **Per-chat conversations** tracked with foreign key relationships

---

## Roadmap

- [x] Core AccessibilityService integration
- [x] Koog agent framework integration
- [x] Multi-provider LLM support (OpenAI, Anthropic, Google)
- [x] UI automation (click, type, scroll, swipe, drag, long press, etc.)
- [x] Telegram bot interface (foreground service + conversation history)
- [x] Quick action intents (SMS, calls, alarms, music, navigation, calendar)
- [x] Suspendable runtime permission manager
- [ ] Web search API (currently browser-based, no results returned to agent)
- [ ] NotificationListenerService (read/act on notifications)
- [ ] WhatsApp integration
- [ ] AI phone calls
- [ ] On-device LLM

---

## Security Considerations

ClawDroid requires significant permissions to function. Please be aware:

- **Accessibility Service** can see and interact with any app
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

- **Web search API** — Returning search results directly to the agent instead of opening a browser
- **Notification listener** — Implementing NotificationListenerService for reading notifications
- **UI automation** — Improving reliability of screen interactions
- **Testing** — Writing tests for agent behaviors

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

## Acknowledgments

- [OpenClaw](https://github.com/openclaw/openclaw) — For proving the concept
- [Koog](https://github.com/JetBrains/koog) — JetBrains' Kotlin agent framework
- [AndroidWorld](https://github.com/google-research/android_world) — Google's benchmark that informed our approach
