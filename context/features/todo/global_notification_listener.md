Plan: Notification Listener for Agent Auto-React

Context

ClawDroid currently reacts to commands from two sources: in-app chat and Telegram messages. The agent has no awareness of device notifications (WhatsApp messages, LinkedIn alerts, etc.).
This feature adds a NotificationListenerService that captures device notifications, stores them, and auto-triggers the agent to process and react to them. The agent's reactions are
forwarded to Telegram so the user stays informed remotely. The feature is off by default and must be enabled in settings.

Architecture Overview

Notification arrives → ClawdroidNotificationListenerService
→ saves to Room DB
→ emits via SharedFlow
→ NotificationReactor (if enabled + Telegram running)
→ constructs prompt from notification
→ runs through AgentExecutor
→ sends agent response to Telegram

The agent also gets a new getRecentNotifications() tool for on-demand queries.

 ---
Step 1: Domain Layer — Models, Repository Interface, Use Cases

New Files

domain/model/DeviceNotification.kt
data class DeviceNotification(
val id: Long = 0,
val packageName: String,
val appName: String,
val title: String?,
val text: String?,
val timestamp: Long,
val isOngoing: Boolean = false,
val category: String? = null
)

domain/repository/notification/NotificationRepository.kt — Interface with:
- val incomingNotifications: SharedFlow<DeviceNotification> — emits each captured notification
- suspend fun saveNotification(notification: DeviceNotification)
- suspend fun getRecentNotifications(limit: Int = 50, packageFilter: String? = null): List<DeviceNotification>
- suspend fun clearOldNotifications(olderThanMs: Long)

domain/usecase/notification/GetRecentNotificationsUseCase.kt — Wraps repository.getRecentNotifications(), injected into NotificationTools.

Modified Files

domain/preference/SharedPreferenceDataSource.kt — Add:
- fun getNotificationForwardingEnabled(): Boolean
- fun setNotificationForwardingEnabled(enabled: Boolean)
- fun getNotificationFilterPackages(): Set<String>
- fun setNotificationFilterPackages(packages: Set<String>)

 ---
Step 2: Data Layer — Entity, DAO, DB Migration, Repository Impl, Preferences

New Files

data/local/entity/NotificationEntity.kt — Room entity (notifications table) with: id (autoGenerate PK), packageName, appName, title, text, timestamp, isOngoing, category.

data/local/dao/notification/NotificationDao.kt — DAO with:
- insert(notification)
- getRecent(limit): List<NotificationEntity> — ordered by timestamp DESC
- getRecentByPackage(packageName, limit)
- deleteOlderThan(olderThan) — housekeeping

data/repository/NotificationRepositoryImpl.kt — Implements NotificationRepository:
- MutableSharedFlow<DeviceNotification>(extraBufferCapacity = 64) for incomingNotifications
- saveNotification() inserts to DB and emits to SharedFlow
- getRecentNotifications() delegates to DAO, maps entities to domain
- clearOldNotifications() delegates to DAO

Modified Files

data/local/AppDatabase.kt — Add NotificationEntity to entities, bump version 2→3, add notificationDao().

data/di/DataModule.kt — Add:
- MIGRATION_2_3: CREATE TABLE notifications with indexes on timestamp and packageName
- .addMigrations(MIGRATION_1_2, MIGRATION_2_3) in provideDatabase
- provideNotificationDao() in DatabaseModule
- bindNotificationRepository() in abstract DataModule

data/local/preferences/SharedPreferenceDataSourceImpl.kt — Implement the 4 new preference methods using SharedPreferences with keys notification_forwarding_enabled and
notification_filter_packages.

 ---
Step 3: NotificationListenerService

New Files

framework/notification/NotificationListenerServiceState.kt — Singleton @Inject class with MutableStateFlow<Boolean> for isConnected. Follows TelegramServiceState pattern.

framework/notification/ClawdroidNotificationListenerService.kt — Extends NotificationListenerService:
- Uses EntryPointAccessors for Hilt DI (system-bound service)
- onListenerConnected() / onListenerDisconnected() → update NotificationListenerServiceState
- onNotificationPosted(sbn):
  - Skip own package (com.aiassistant)
  - Skip ongoing notifications (FLAG_ONGOING_EVENT)
  - Skip empty notifications (no title and no text)
  - Dedup via hash of (package + title + text + 2-second time bucket), LRU set of 100
  - Extract: packageName, appName (via PackageManager), title (EXTRA_TITLE), text (EXTRA_TEXT), postTime, category
  - Build DeviceNotification and call repository.saveNotification()
- Coroutine scope with SupervisorJob + Dispatchers.IO, cancelled on onDestroy

Modified Files

AndroidManifest.xml — Add service declaration after TelegramBotService:
<service
android:name=".framework.notification.ClawdroidNotificationListenerService"
android:exported="true"
android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
<intent-filter>
<action android:name="android.service.notification.NotificationListenerService" />
</intent-filter>
</service>

 ---
Step 4: Auto-React — NotificationReactor + Telegram Integration

New Files

framework/notification/NotificationReactor.kt — Singleton that auto-processes notifications through the agent:
- Injected with: NotificationRepository, AgentExecutor, SendResponseUseCase, SaveMessageUseCase, GetConversationHistoryUseCase, TelegramServiceState, SharedPreferenceDataSource
- startReacting(chatId: Long) — Collects from repository.incomingNotifications:
  - Checks if forwarding is enabled and Telegram is running
  - Checks package filter (empty = all apps)
  - Constructs a notification prompt for the agent:
    [NOTIFICATION] App: WhatsApp | From: John Doe | Message: Hey, are you coming tonight?

A new notification arrived on the device. Analyze it and decide how to react:
- If it's a message that needs a reply, open the app and respond appropriately
- If it's informational, briefly acknowledge it
- If it's not important (system update, etc.), say so briefly
  - Saves the prompt as a "user message" in Telegram conversation history
  - Runs agentExecutor.execute(prompt, conversationHistory, requireServiceConnection = false)
  - Saves agent response to conversation history
  - Sends agent response to Telegram via sendResponseUseCase
- stopReacting() — Cancels the collection job
- Rate limiting: Skip if last agent execution was less than 5 seconds ago (avoid flooding)

Modified Files

framework/telegram/TelegramBotService.kt — Add:
- @Inject lateinit var notificationReactor: NotificationReactor
- In processMessage() (after first message): call notificationReactor.startReacting(chatId) with the active chat ID
- In onDestroy(): call notificationReactor.stopReacting()

 ---
Step 5: Agent Tool — getRecentNotifications()

New Files

agent/NotificationTools.kt — ToolSet with a single tool:
- getRecentNotifications(limit: Int = 20, appPackage: String = "") → Returns formatted list of recent notifications (time, app, title, text)
- Follows DeviceTools/QuickActionTools pattern with @Tool and @LLMDescription annotations

Modified Files

agent/di/AgentModule.kt — Add:
- provideNotificationTools(GetRecentNotificationsUseCase): NotificationTools
- Update provideAndroidAgentFactory() to accept and pass NotificationTools

agent/AndroidAgentFactory.kt — Add notificationTools constructor parameter. Register in ToolRegistry { tools(notificationTools) }.

agent/SystemPrompts.kt — Add to RESPONSE STRATEGY between items 2 and 3:
2.5. NOTIFICATIONS — If the user asks about messages, notifications, or alerts, use getRecentNotifications() to check recent device notifications.
Add to NOTIFICATION TOOLS section:
- getRecentNotifications(limit?, appPackage?) — Read recent device notifications.
  Common packages: com.whatsapp, com.linkedin.android, com.google.android.gm, com.instagram.android
  Add to examples:
- "Any new WhatsApp messages?" → getRecentNotifications(20, "com.whatsapp")
- "What notifications do I have?" → getRecentNotifications()
  Add a section for auto-react behavior:
  NOTIFICATION ALERTS:
  When you receive a message starting with [NOTIFICATION], a real notification arrived on the device.
- For messages/chats: open the app and reply if appropriate
- For informational alerts: briefly acknowledge
- For unimportant notifications: say "Not important" briefly
  Always inform the user what you did via your response.

 ---
Step 6: Permission Flow — Settings UI

Modified Files

framework/permission/PermissionManager.kt — Add:
- isNotificationListenerEnabled(): Boolean — Check Settings.Secure.getString("enabled_notification_listeners") contains app package
- openNotificationListenerSettings(context) — Launch ACTION_NOTIFICATION_LISTENER_SETTINGS intent

presentation/settings/TelegramSettingsViewModel.kt — Add to state:
- isNotificationListenerEnabled: Boolean
- isNotificationForwardingEnabled: Boolean

Add intents:
- OpenNotificationListenerSettings
- ToggleNotificationForwarding

Add handlers:
- OpenNotificationListenerSettings → call permissionManager.openNotificationListenerSettings(context)
- ToggleNotificationForwarding → toggle preference via SharedPreferenceDataSource

Inject PermissionManager and poll isNotificationListenerEnabled on resume.

presentation/settings/TelegramSettingsScreen.kt — Add a "Notification Listener" card section with:
- Connection status (connected/not connected)
- "Enable" button → opens system notification access settings
- "Forward & auto-react to notifications" toggle (only shown when listener is connected)

 ---
Step 7: Housekeeping

In ClawdroidNotificationListenerService.onListenerConnected(), clean notifications older than 24 hours via repository.clearOldNotifications(24 * 60 * 60 * 1000L). The service that owns the
data cleans up its own data when it connects — no coupling to unrelated services.

 ---
Files Summary

New Files (8)
┌────────────────────────────────────────────────────────────────┬───────────────────────────────────────────────────┐
│                              File                              │                      Purpose                      │
├────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ domain/model/DeviceNotification.kt                             │ Notification data model                           │
├────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ domain/repository/notification/NotificationRepository.kt                    │ Repository interface                              │
├────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ domain/usecase/notification/GetRecentNotificationsUseCase.kt   │ Use case for agent tool                           │
├────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ data/local/entity/notification/NotificationEntity.kt                        │ Room entity                                       │
├────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ data/local/dao/notification/NotificationDao.kt                              │ Room DAO                                          │
├────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ data/repository/notification/NotificationRepositoryImpl.kt                  │ Repository impl with SharedFlow                   │
├────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ framework/notification/ClawdroidNotificationListenerService.kt │ Core notification capture service                 │
├────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ framework/notification/NotificationListenerServiceState.kt     │ Service state tracker                             │
├────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ framework/notification/NotificationReactor.kt                  │ Auto-react: processes notifications through agent │
├────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ agent/NotificationTools.kt                                     │ Agent tool for querying notifications             │
└────────────────────────────────────────────────────────────────┴───────────────────────────────────────────────────┘
Modified Files (10)
┌────────────────────────────────────────────────────────────────────────────────┬────────────────────────────────────────────────────────┐
│                                      File                                      │                         Change                         │
├────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ AndroidManifest.xml                                                            │ Add NotificationListenerService declaration            │
├────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ domain/preference/SharedPreferenceDataSource.kt                                │ Add notification preference methods                    │
├────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ data/local/preferences/SharedPreferenceDataSourceImpl.kt                       │ Implement notification preferences                     │
├────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ data/local/AppDatabase.kt                                                      │ Add entity, bump version, add DAO                      │
├────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ data/di/DataModule.kt                                                          │ Add migration, DAO provider, repository binding        │
├────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ agent/di/AgentModule.kt                                                        │ Add NotificationTools, update factory                  │
├────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ agent/AndroidAgentFactory.kt                                                   │ Add notificationTools to constructor and registry      │
├────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ agent/SystemPrompts.kt                                                         │ Add notification awareness and auto-react instructions │
├────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ framework/telegram/TelegramBotService.kt                                       │ Inject NotificationReactor, start/stop                 │
├────────────────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ presentation/settings/TelegramSettingsViewModel.kt + TelegramSettingsScreen.kt │ Add notification settings UI                           │
└────────────────────────────────────────────────────────────────────────────────┴────────────────────────────────────────────────────────┘
 ---
Verification

1. Build: ./gradlew assembleDebug — should compile without errors
2. Install: Deploy to device/emulator
3. Enable notification access: Go to Settings > Notification access > enable ClawDroid
4. Verify capture: Check Logcat for NotificationListener tag — should see "Notification from [app]: [title] - [text]" for each notification
5. Test agent tool: In the app chat, ask "What notifications do I have?" — agent should use getRecentNotifications() and list recent notifications
6. Test Telegram forwarding: Enable forwarding in settings, send a WhatsApp message to the device, verify the agent processes it and responds via Telegram
7. Test filtering: Set a package filter in settings, verify only matching notifications trigger auto-react
8. Test rate limiting: Send multiple notifications quickly, verify the reactor doesn't flood the agent
9. Test cleanup: Verify notifications older than 24h are cleaned on service start
