Make NotificationReactor Autonomous

Context

The NotificationReactor currently acts as a passive forwarder: it receives a device notification, asks the LLM to summarize it, and sends that summary to Telegram. The agent is explicitly
told to keep responses "short and informative" and runs with requireServiceConnection = false, meaning it can't even perform UI automation. This makes the feature nearly useless — the user
already sees the notification on their device.

The goal is to make the agent act first, inform after: when a notification arrives, the agent should autonomously decide what to do (reply to a message, dismiss, snooze, open the app,
etc.), execute that action, and then report what it did via Telegram and the app's chat UI.

Changes

1. Rewrite the notification prompt to instruct autonomous action

File: app/src/main/java/com/aiassistant/framework/notification/NotificationReactor.kt

Replace the current prompt-building block (lines 64-76) with a new prompt that tells the agent to:
- Analyze the notification and decide the best autonomous action
- Execute the action using available tools (QuickActions for SMS/calls/etc, UI automation for replying in apps)
- After acting, produce a short report of what was done

New prompt structure:
[NOTIFICATION from {appName}]
Title: {title}
Message: {text}

You received this device notification. Act on it autonomously:
- If it's a message (WhatsApp, Telegram, SMS, etc.), open the app and reply on behalf of the user. Identify yourself as the user's AI assistant (e.g., "Hi, this is [user]'s AI assistant.
  They'll get back to you shortly.").
- If it's a reminder or calendar event, acknowledge it.
- If it's a delivery or tracking update, no action needed — just note it.
- If it's spam or marketing, dismiss it.
- If it requires a specific action (e.g., approve a login, confirm something), try to handle it.

You have FULL AUTONOMY — you can reply to messages, send SMS, make calls, dismiss notifications, or perform any other action you deem appropriate.

After acting, respond with a brief summary of what you did, starting with [ACTION TAKEN] or [NO ACTION NEEDED].
Do NOT just summarize the notification — take action when possible.

2. Enable service connection for UI automation

File: app/src/main/java/com/aiassistant/framework/notification/NotificationReactor.kt

Change requireServiceConnection = false (line 90) to requireServiceConnection = true on the agentExecutor.execute() call. This unlocks DeviceTools (click, type, scroll, navigate apps) so
the agent can actually interact with apps to reply to messages, dismiss notifications, etc.

Add a fallback: if the result is AgentResult.ServiceNotConnected, re-execute with requireServiceConnection = false so the agent can still use QuickActions (send SMS, make call, etc.) even
without the accessibility service.

3. Handle the ServiceNotConnected fallback

File: app/src/main/java/com/aiassistant/framework/notification/NotificationReactor.kt

After the first agentExecutor.execute() call, if result is ServiceNotConnected, re-run with requireServiceConnection = false and a modified prompt that limits the agent to quick actions
only. This ensures the feature degrades gracefully.

4. Decouple notification processing from Telegram

File: app/src/main/java/com/aiassistant/framework/notification/NotificationReactor.kt

Critical fix: Remove the Telegram-running guard at line 50 (if (!telegramServiceState.isRunning.value) return@collect). Currently, if Telegram is not running, notifications are completely
ignored — the chat UI gets nothing.

The new flow should:
- Always process notifications when forwarding is enabled (the preference check at line 47 stays)
- Always save to the chat UI via saveMessageUseCase(DEFAULT_CHAT_ID, ...) (lines 81 and 101 — both the incoming notification and the agent's response)
- Conditionally send to Telegram — only call sendResponseTelegramUseCase if telegramServiceState.isRunning.value is true

This means startReacting no longer needs a telegramChatId parameter as a required dependency. Instead, we should:
- Change the method signature: startReacting() (no parameter)
- Store telegramChatId from preferences or pass it only when Telegram is active
- Wrap the Telegram send in an if (telegramServiceState.isRunning.value) check

However, since telegramChatId is still needed when Telegram IS running, the simplest approach is to keep the parameter but make the Telegram guard conditional only around the send call, not
the entire flow:

fun startReacting(telegramChatId: Long) {
// ... collect notifications ...
// Remove: if (!telegramServiceState.isRunning.value) return@collect

     // ... process notification, execute agent, get responseText ...

     // Always save to chat UI
     saveMessageUseCase(DEFAULT_CHAT_ID, responseText, isFromUser = false)

     // Send to Telegram only if running
     if (telegramServiceState.isRunning.value) {
         sendResponseTelegramUseCase(telegramChatId, responseText).onFailure { error ->
             Log.e(TAG, "Failed to send notification response to Telegram", error)
         }
     }
}

6. Update the system prompt's notification section

File: app/src/main/java/com/aiassistant/agent/SystemPrompts.kt

Replace the --- NOTIFICATION ALERTS --- section (lines 41-47) to instruct the agent to act autonomously:

--- NOTIFICATION ALERTS ---
Messages starting with [NOTIFICATION from ...] are automatic alerts from the device notification listener.
When you receive a [NOTIFICATION] message, ACT AUTONOMOUSLY — do NOT just summarize:

1. ASSESS — Determine if the notification requires action (message reply, dismissal, acknowledgment) or is just informational.
2. ACT — If action is needed, use your tools with FULL AUTONOMY:
    - For messaging apps (WhatsApp, Telegram, SMS): open the app with launchApp(), navigate to the conversation, and reply on behalf of the user. Always identify yourself as the user's AI
      assistant (e.g., "Hi, this is [user]'s AI assistant. They'll get back to you shortly.").
    - For quick responses: use sendSms() or other quick action tools when appropriate.
    - For dismissals: open notification shade with openNotifications() and swipe to dismiss.
    - You may make calls, send SMS, reply in apps, dismiss notifications, or take any action you deem appropriate.
3. REPORT — After acting, respond with what you did. Start with [ACTION TAKEN] if you performed an action, or [NO ACTION NEEDED] if the notification was informational only.

Examples:
- WhatsApp message from Mom "Are you coming for dinner?" → Open WhatsApp, reply "Hi, this is [user]'s AI assistant. They'll get back to you about dinner shortly." → report: [ACTION TAKEN]
  Replied to Mom on WhatsApp on your behalf.
- Uber Eats delivery update → [NO ACTION NEEDED] Your Uber Eats order is on its way.
- Marketing email notification → [NO ACTION NEEDED] Promotional notification from Gmail, dismissed.
- Missed call from unknown number → [ACTION TAKEN] Sent SMS to the number: "Hi, this is [user]'s assistant. They missed your call and will get back to you."

7. Increase rate limit for autonomous actions

File: app/src/main/java/com/aiassistant/framework/notification/NotificationReactor.kt

Change RATE_LIMIT_MS from 5_000L to 15_000L (line 22). Autonomous actions take longer than summaries, and we don't want overlapping agent executions trying to control the UI simultaneously.
15 seconds gives enough time for the agent to complete a UI automation task before the next notification triggers.

Files to Modify

1. app/src/main/java/com/aiassistant/framework/notification/NotificationReactor.kt — Main changes: new autonomous prompt, decouple from Telegram (always save to chat UI, conditionally send
   to Telegram), requireServiceConnection = true with fallback, increased rate limit
2. app/src/main/java/com/aiassistant/agent/SystemPrompts.kt — Update the notification alerts section to instruct autonomous behavior

Verification

1. Build: Run ./gradlew assembleDebug to verify compilation
2. Manual test (with Telegram + Accessibility):
- Enable accessibility service and notification forwarding
- Send a WhatsApp/SMS message to the device
- Verify the agent opens the app and attempts to reply autonomously
- Verify the action report appears in both the chat UI and Telegram
3. Chat UI only test (Telegram off):
- Disable Telegram, keep notification forwarding enabled
- Send a notification to the device
- Verify the agent still processes it and the report appears in the chat UI (even without Telegram)
4. Fallback test: Disable the accessibility service, send a notification, verify the agent falls back to quick actions only (e.g., sendSms for SMS replies)
5. Rate limit test: Send multiple notifications rapidly, verify only one is processed per 15-second window
