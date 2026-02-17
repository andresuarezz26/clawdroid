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
No scripting. No Selenium. No Appium. No Zapier. No Make. No n8n. Just tell the agent what you want in plain English, and it figures out how to do it. Automate any task without writing a single line of code.

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

## Architecture

ClawDroid uses a multi-tier response strategy. The LLM decides the best approach for each request:
```
## Architecture
```
                         ┌──────────────────┐
                         │   INPUT SOURCES  │
                         └────────┬─────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
   ┌─────────────┐        ┌─────────────┐        ┌───────────────┐
   │  In-App     │        │  Telegram   │        │ Notifications │
   │  Chat UI    │        │  Bot API    │        │   Listener    │
   └──────┬──────┘        └──────┬──────┘        └───────┬───────┘
          │                      │                       │
          └──────────────────────┴───────────────────────┘
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │     LLM DECISION       │
                    │ "What's the best way?" │
                    └───────────┬────────────┘
                                │
        ┌───────────┬───────────┼───────────┬───────────┐
        ▼           ▼           ▼           ▼           ▼
   ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
   │ Direct  │ │  Web    │ │ Quick   │ │   UI    │ │Schedule │
   │ Answer  │ │ Search  │ │ Actions │ │ Automat.│ │  Task   │
   └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘
                   │           │           │           │
                Browser    Intents   Accessibility  WorkManager/AlarmManager
                   │       (instant)   Service     (recurring)
                Needs                 Works on
                parsing               any app
```

1. **Direct answer** — If the LLM knows the answer, just respond
2. **Web search** — Opens a browser search; the agent can read the screen afterward
3. **Quick actions** — For system actions (SMS, calls, alarms, navigation, calendar), use native intents
4. **UI automation** — Read the screen and interact with any app element

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

## Contributing

Contributions are welcome! You can open issues for suggest features/bugs or submit a Pull Request. 

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

## Acknowledgments

- [OpenClaw](https://github.com/openclaw/openclaw) — For proving the concept
- [Koog](https://github.com/JetBrains/koog) — JetBrains' Kotlin agent framework
- [AndroidWorld](https://github.com/google-research/android_world) — Google's benchmark that informed our approach
