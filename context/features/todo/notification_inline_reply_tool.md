Add Notification Inline Reply Tool

Context

The NotificationReactor currently uses UI automation (open app, navigate, type reply) to respond to notifications. This is fragile, slow, and disrupts the user's screen. Android
notifications with messaging apps (WhatsApp, SMS, Telegram, etc.) include inline reply actions via RemoteInput + PendingIntent. We can capture these when the notification arrives and let
the agent call a replyToNotification() tool to reply instantly — no UI navigation needed.

Approach

1. Capture the PendingIntent + RemoteInput from notifications in the listener service
2. Store them in an in-memory cache (they're not serializable to Room DB)
3. Expose a replyToNotification(notificationKey, replyText) tool to the agent
4. Update prompts so the agent prefers inline reply over UI automation

Changes

1. Create NotificationActionStore (NEW FILE)

File: app/src/main/java/com/aiassistant/framework/notification/NotificationActionStore.kt

In-memory @Singleton cache mapping notification keys to their reply PendingIntent + RemoteInput. Uses LinkedHashMap with FIFO eviction (max 50 entries) and @Synchronized methods for thread
safety across listener and agent threads.

Contains:
- NotificationReplyAction data class (holds PendingIntent, List<RemoteInput>, notificationKey, packageName, timestamp)
- storeReplyAction(key, action) — store with FIFO eviction
- getReplyAction(key) — retrieve for reply
- removeReplyAction(key) — cleanup after reply or notification dismissal
- evictOlderThan(maxAgeMs) — periodic cleanup

2. Add fields to DeviceNotification

File: app/src/main/java/com/aiassistant/domain/model/DeviceNotification.kt

Add two fields with defaults (no DB changes needed):
val notificationKey: String? = null,
val hasReplyAction: Boolean = false

The Room entity (NotificationEntity) is NOT modified — these are runtime-only fields.

3. Extract reply actions in ClawdroidNotificationListenerService

File: app/src/main/java/com/aiassistant/framework/notification/ClawdroidNotificationListenerService.kt

- Add notificationActionStore() to the Hilt EntryPoint interface
- In onNotificationPosted(): find the first action with RemoteInput, store it in NotificationActionStore, pass notificationKey and hasReplyAction to DeviceNotification
- Add onNotificationRemoved(): clean up the cache entry when a notification is dismissed
- On onListenerConnected(): call evictOlderThan() to clear stale entries

4. Add replyToNotification tool to NotificationTools

File: app/src/main/java/com/aiassistant/agent/NotificationTools.kt

Add NotificationActionStore and @ApplicationContext Context to constructor. Add new tool:

@Tool
@LLMDescription("Reply to a notification directly using inline reply...")
suspend fun replyToNotification(notificationKey: String, replyText: String): String

Implementation: look up cached NotificationReplyAction, build Intent with RemoteInput.addResultsToIntent(), call pendingIntent.send(context, 0, intent). Handle
PendingIntent.CanceledException gracefully. Remove from cache after successful reply.

5. Update DI wiring in AgentModule

File: app/src/main/java/com/aiassistant/agent/di/AgentModule.kt

Update provideNotificationTools to pass the new dependencies (NotificationActionStore, Context).

6. Update prompt in NotificationReactor

File: app/src/main/java/com/aiassistant/framework/notification/NotificationReactor.kt

When notification.hasReplyAction is true, include in the prompt:
NotificationKey: {key}
ReplyCapable: YES - You can use replyToNotification("{key}", "your reply text") to reply directly.

Also add a rule: "If the notification supports inline reply (ReplyCapable: YES), ALWAYS prefer replyToNotification() over UI automation."

7. Add tool documentation to SystemPrompts.kt

File: app/src/main/java/com/aiassistant/agent/SystemPrompts.kt

Add one line to the NOTIFICATION TOOLS section:
- replyToNotification(notificationKey, replyText) — Reply directly to a notification using its inline reply action. Instant, no app opening needed. Only works when the notification prompt
  includes "ReplyCapable: YES".

No changes to the NOTIFICATION ALERTS section — the NotificationReactor prompt already instructs the agent to prefer inline reply.

Files Summary

┌────────────────────────────────────────────────────────────────┬────────┬─────────────────────────────────────────────────────┐
│                              File                              │ Action │                     Description                     │
├────────────────────────────────────────────────────────────────┼────────┼─────────────────────────────────────────────────────┤
│ framework/notification/NotificationActionStore.kt              │ CREATE │ In-memory cache for reply PendingIntents            │
├────────────────────────────────────────────────────────────────┼────────┼─────────────────────────────────────────────────────┤
│ domain/model/DeviceNotification.kt                             │ MODIFY │ Add notificationKey and hasReplyAction              │
├────────────────────────────────────────────────────────────────┼────────┼─────────────────────────────────────────────────────┤
│ framework/notification/ClawdroidNotificationListenerService.kt │ MODIFY │ Extract reply actions, add onNotificationRemoved    │
├────────────────────────────────────────────────────────────────┼────────┼─────────────────────────────────────────────────────┤
│ agent/NotificationTools.kt                                     │ MODIFY │ Add replyToNotification() tool                      │
├────────────────────────────────────────────────────────────────┼────────┼─────────────────────────────────────────────────────┤
│ agent/di/AgentModule.kt                                        │ MODIFY │ Update DI wiring for new dependencies               │
├────────────────────────────────────────────────────────────────┼────────┼─────────────────────────────────────────────────────┤
│ framework/notification/NotificationReactor.kt                  │ MODIFY │ Include reply capability info in prompt             │
├────────────────────────────────────────────────────────────────┼────────┼─────────────────────────────────────────────────────┤
│ agent/SystemPrompts.kt                                         │ MODIFY │ Document new tool, update notification instructions │
