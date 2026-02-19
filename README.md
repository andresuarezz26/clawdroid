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
Access it through Telegram or from the built-in chat. It doesn't require an external backend. It runs directly the LLMs and store the conversations in a local database. The agent can schedule recurrent tasks, react to notifications and manage all the apps installed on the phone.

<br/>

🤖 **Live demo**: ClawDroid manages its own X account **[@clawdroidagent](https://x.com/clawdroidagent)** autonomously. It posts daily, replies to comments, and answers DMs. **[Go DM it →](https://x.com/clawdroidagent)**
<br/>

## How to use it? 

**[📱 Download APK](https://github.com/andresuarezz26/ClawDroid/releases/download/v1.0.0/app-release.apk)** · [View Releases](https://github.com/andresuarezz26/ClawDroid/releases)

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

## Screenshots

<div align="center">
<img width="300" height="668" alt="screenshot1" src="https://github.com/user-attachments/assets/92b706bb-6b56-4425-ae6d-b4390c6ba8d2" />
<img width="300" height="668" alt="screenshotnumber2clawdroid" src="https://github.com/user-attachments/assets/2f6fd3f0-3e21-4c71-a819-13b42a5fe6ec" />
<img width="300" height="668" alt="screenshot3" src="https://github.com/user-attachments/assets/e3082ba2-b834-4010-af45-dc51517892a7" />
</div>
## Installation

### Prerequisites
- Android 7.0+ device (API 24)
- API key from OpenAI, Anthropic, or Google
- Telegram account (Optional)

### Quick Start

1. **Build from source** or download the APK from [Releases](https://github.com/andresuarezz26/ClawDroid/releases)

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
```
                         ┌──────────────────┐
                         │   INPUT SOURCES  │
                         └────────┬─────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
   ┌─────────────┐        ┌─────────────┐        ┌───────────────┐
   │  In-App     │        │  Telegram   │        │ Notification  │
   │  Chat UI    │        │  Bot API    │        │   Alarms      │
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
                          (instant)   Service     (recurring)
                                       Works on
                                       any app
```

---

## LLM Providers

ClawDroid supports 3 providers with multiple models each:

| Provider | Status |
|----------|--------|
| **OpenAI** | ✅ Supported |
| **Anthropic** | ✅ Supported |
| **Google** | ✅ Supported |

---
## Tools 

ClawDroid's capabilities come from **tools** — functions the AI agent can call to interact with your device. When you send a message, the LLM decides which tools to use based on your request. The agent can chain multiple tools together to complete complex tasks.

**34 tools** across 4 categories:

---

### 📱 Mobile Automation (19 tools)
*UI automation via AccessibilityService. The agent reads the screen and interacts with elements just like a human would.*

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
| `waitForUpdate` | Wait for screen to settle after an action |
| `taskComplete` | Signal that the task is done |
| `taskFailed` | Signal that the task has failed |

---

### ⚡ Quick Actions (9 tools)
*Instant actions via Android Intents. No UI automation needed — these execute immediately.*

| Tool | Description |
|------|-------------|
| `sendSms` | Send an SMS message |
| `makeCall` | Start a phone call |
| `openUrl` | Open a URL in the default browser |
| `setAlarm` | Set an alarm (24-hour format) |
| `playMusic` | Play music by search query |
| `openSettings` | Open device settings (wifi, bluetooth, display, etc.) |
| `webSearch` | Open a web search in the browser |
| `createCalendarEvent` | Create a calendar event |
| `startNavigation` | Open navigation to an address |

---

### 🔔 Notifications (2 tools)
*Read and respond to any notification on the device.*

| Tool | Description |
|------|-------------|
| `getRecentNotifications` | Get recent notifications, filterable by app |
| `replyToNotification` | Reply directly via inline reply action |

---

### ⏰ Recurring Tasks (4 tools)
*Schedule tasks that run automatically. Create them by chatting naturally.*

| Tool | Description |
|------|-------------|
| `createRecurringTask` | Create a scheduled task (e.g., "Post daily at 9 AM") |
| `listRecurringTasks` | List all tasks with status and last run info |
| `updateRecurringTask` | Update schedule, prompt, or enable/disable |
| `deleteRecurringTask` | Delete a task permanently |
## Tech Stack

- **Language:** Kotlin
- **Agent Framework:** [Koog](https://github.com/JetBrains/koog) v0.6.1 by JetBrains
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Clean Architecture + MVI
- **DI:** Hilt
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
