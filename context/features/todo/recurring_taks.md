Recurring Tasks Feature — Implementation Plan

Context

ClawDroid currently supports one-shot commands via chat, Telegram, and notification reactions. Users want to define tasks that execute automatically on a schedule (e.g., "every morning at 9am check my
WhatsApp and send me a summary"). This feature adds:

1. Data model for recurring tasks with execution history
2. Agent tools so the LLM can create/manage tasks via chat
3. Side drawer navigation replacing the current TopAppBar settings icon
4. Task list + detail screens for viewing and managing tasks
5. AlarmManager + WorkManager scheduling for precise time-of-day execution

Schedule format: hour + minute + days-of-week (not cron). Simple, LLM-friendly, maps to UI pickers.

 ---
Architecture Diagrams

High-Level Component Overview

┌─────────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                                 │
│                                                                             │
│  ModalNavigationDrawer                                                      │
│  ├── Chat (ChatScreen + hamburger menu)                                     │
│  ├── Recurring Tasks (RecurringTaskListScreen)                              │
│  │       └── RecurringTaskDetailScreen                                      │
│  └── Settings (TelegramSettingsScreen)                                      │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                            AGENT LAYER                                      │
│                                                                             │
│  AndroidAgentFactory ──► registers ──► RecurringTaskTools (new)             │
│       │                                    │                                │
│       ├── DeviceTools                      ├── createRecurringTask()        │
│       ├── QuickActionTools                 ├── listRecurringTasks()         │
│       ├── NotificationTools                ├── updateRecurringTask()        │
│       └── SystemPrompts (updated)          └── deleteRecurringTask()        │
│                                                    │                        │
│  AgentExecutor ◄───────────────────────────────────┘                        │
│       │                                                                     │
├───────┼─────────────────────────────────────────────────────────────────────┤
│       │                      DOMAIN LAYER                                   │
│       │                                                                     │
│       │  RecurringTask (model)     RecurringTaskRepository (interface)       │
│       │  TaskExecution (model)     Use Cases: Create, Update, Delete,       │
│       │                            Toggle, Observe, Execute, GetHistory     │
│       │                                                                     │
├───────┼─────────────────────────────────────────────────────────────────────┤
│       │                       DATA LAYER                                    │
│       │                                                                     │
│       │  RecurringTaskRepositoryImpl ──► RecurringTaskDao ──► AppDatabase   │
│       │                                                        (v5 → v6)   │
│       │  Tables: recurring_tasks, task_executions                           │
│       │                                                                     │
├───────┼─────────────────────────────────────────────────────────────────────┤
│       │                    FRAMEWORK LAYER                                  │
│       │                                                                     │
│       ▼                                                                     │
│  RecurringTaskCoordinator ──► RecurringTaskScheduler ──► AlarmManager       │
│       │                                                       │             │
│       │  (orchestrates DB + scheduling)                       │ fires       │
│       │                                                       ▼             │
│       │                                        RecurringTaskReceiver        │
│       │                                                       │             │
│       │                                                       │ enqueues    │
│       │                                                       ▼             │
│       └──────────────────────────────────── RecurringTaskWorker             │
│                                              (WorkManager CoroutineWorker)  │
│                                                  │                          │
│                                                  ├── AgentExecutor.execute()│
│                                                  ├── Save to DB             │
│                                                  ├── Notify Telegram        │
│                                                  └── Reschedule next alarm  │
└─────────────────────────────────────────────────────────────────────────────┘

Chat-Based Task Creation Flow

User: "Every day at 9am check my WhatsApp"
│
▼
ChatScreen ──► ChatViewModel.executeCommand()
│
▼
AIAgent.run(command)
│
│  LLM decides to call createRecurringTask
▼
RecurringTaskTools.createRecurringTask(
title="Morning WhatsApp Check",
prompt="Check WhatsApp notifications and summarize...",
hour=9, minute=0, daysOfWeek="",
scheduleDisplay="Every day at 9:00 AM",
relatedApps="com.whatsapp"
)
│
▼
RecurringTaskCoordinator.createAndSchedule()
│
├──► Repository.create(task) ──► Room DB INSERT
│
└──► Scheduler.scheduleTask(task)
│
├── calculateNextFireTime(9, 0, [])
└── AlarmManager.setExactAndAllowWhileIdle(tomorrow 9AM)
│
▼
Agent returns: "Done! Created 'Morning WhatsApp Check' — runs daily at 9:00 AM"

Scheduled Task Execution Flow

AlarmManager fires at 9:00 AM
│
▼
RecurringTaskReceiver (BroadcastReceiver)
│
│  enqueues OneTimeWorkRequest with taskId
▼
WorkManager ──► RecurringTaskWorker.doWork()
│
├── 1. Load task from DB (Repository.getById)
├── 2. Check task.enabled == true
├── 3. AgentExecutor.execute(task.prompt)
│       └── LLM processes prompt, uses tools, returns summary
├── 4. Record execution result in task_executions table
├── 5. Update lastRunAt/lastRunStatus on recurring_tasks
├── 6. Save summary to conversation history
├── 7. Send summary to Telegram (if connected)
└── 8. Reschedule alarm for next occurrence
└── Scheduler.scheduleTask(task) → AlarmManager

Navigation Structure

┌──────────────────────┐     ┌─────────────────────────────────┐
│   Side Drawer        │     │         App Screens              │
│                      │     │                                  │
│  [=] Chat ───────────┼────►│  ChatScreen (start destination)  │
│                      │     │    TopAppBar: hamburger icon      │
│  [↻] Recurring Tasks ┼────►│  RecurringTaskListScreen          │
│                      │     │    ├── task cards with toggles    │
│  [⚙] Settings ──────┼────►│    └── tap → detail               │
│                      │     │  RecurringTaskDetailScreen        │
└──────────────────────┘     │    ├── edit title/prompt/schedule │
│    ├── Run Now button             │
│    ├── execution history          │
│    └── delete button              │
│  TelegramSettingsScreen           │
└─────────────────────────────────┘

Data Model

recurring_tasks                          task_executions
┌──────────────────────────────┐        ┌────────────────────────────┐
│ id           LONG PK         │        │ id           LONG PK       │
│ title        TEXT             │        │ taskId       LONG FK ──────┤
│ prompt       TEXT             │        │ executedAt   LONG          │
│ hour         INT  (0-23)     │  1:N   │ status       TEXT          │
│ minute       INT  (0-59)     │◄───────│ summary      TEXT?         │
│ daysOfWeek   TEXT  ("1,2,3") │        │ durationMs   LONG?        │
│ scheduleDisplay TEXT         │        └────────────────────────────┘
│ relatedApps  TEXT             │           CASCADE DELETE on parent
│ enabled      BOOL             │
│ createdAt    LONG             │
│ lastRunAt    LONG?            │
│ lastRunStatus TEXT?           │
│ lastRunSummary TEXT?          │
└──────────────────────────────┘

 ---
Phase 1: Data Layer

1.1 RecurringTaskEntity

New file: data/local/entity/recurringtask/RecurringTaskEntity.kt

@Entity(tableName = "recurring_tasks")
data class RecurringTaskEntity(
@PrimaryKey(autoGenerate = true) val id: Long = 0,
val title: String,
val prompt: String,
val hour: Int,              // 0-23
val minute: Int,            // 0-59
val daysOfWeek: String,     // comma-separated: "1,2,3,4,5" (Mon-Fri), empty = every day
val scheduleDisplay: String, // human-readable: "Every day at 9:00 AM"
val relatedApps: String,    // comma-separated package names
val enabled: Boolean = true,
val createdAt: Long,
val lastRunAt: Long? = null,
val lastRunStatus: String? = null, // "success" | "failure"
val lastRunSummary: String? = null
)

Pattern: follows NotificationEntity

1.2 TaskExecutionEntity

New file: data/local/entity/recurringtask/TaskExecutionEntity.kt

@Entity(tableName = "task_executions",
foreignKeys = [ForeignKey(entity = RecurringTaskEntity::class,
parentColumns = ["id"], childColumns = ["taskId"], onDelete = CASCADE)],
indices = [Index("taskId")])
data class TaskExecutionEntity(
@PrimaryKey(autoGenerate = true) val id: Long = 0,
val taskId: Long,
val executedAt: Long,
val status: String,
val summary: String?,
val durationMs: Long?
)

1.3 RecurringTaskDao

New file: data/local/dao/recurringtask/RecurringTaskDao.kt

Methods: insert, update, observeAll(): Flow, getById, getEnabled, setEnabled, updateLastRun, deleteById, insertExecution, getRecentExecutions

1.4 Update AppDatabase

Modify: data/local/AppDatabase.kt
- Add both entities to @Database(entities = [...])
- Bump version 5 → 6 (destructive migration already configured)
- Add abstract fun recurringTaskDao(): RecurringTaskDao

1.5 Domain model

New file: domain/model/RecurringTask.kt

data class RecurringTask(
val id: Long = 0,
val title: String,
val prompt: String,
val hour: Int,
val minute: Int,
val daysOfWeek: List<Int>,  // 1=Mon..7=Sun, empty=every day
val scheduleDisplay: String,
val relatedApps: List<String>,
val enabled: Boolean = true,
val createdAt: Long,
val lastRunAt: Long? = null,
val lastRunStatus: String? = null,
val lastRunSummary: String? = null
)

data class TaskExecution(
val id: Long = 0,
val taskId: Long,
val executedAt: Long,
val status: String,
val summary: String?,
val durationMs: Long?
)

1.6 Repository interface

New file: domain/repository/recurringtask/RecurringTaskRepository.kt

Methods: observeAll(): Flow, getById, getEnabled, create(): Long, update, setEnabled, delete, recordExecution, getRecentExecutions

1.7 Repository implementation

New file: data/repository/recurringtask/RecurringTaskRepositoryImpl.kt

Maps entity <-> domain. Comma-separated strings for relatedApps and daysOfWeek.

1.8 Wire DI

Modify: data/di/DataModule.kt
- Add @Binds bindRecurringTaskRepository
- Add @Provides provideRecurringTaskDao

 ---
Phase 2: Use Cases

New files in domain/usecase/recurringtask/:

┌────────────────────────────────┬──────────────────────────────────────────────────────┐
│            Use Case            │                       Purpose                        │
├────────────────────────────────┼──────────────────────────────────────────────────────┤
│ ObserveRecurringTasksUseCase   │ Returns Flow<List<RecurringTask>> for UI             │
├────────────────────────────────┼──────────────────────────────────────────────────────┤
│ GetRecurringTaskByIdUseCase    │ Single task lookup                                   │
├────────────────────────────────┼──────────────────────────────────────────────────────┤
│ CreateRecurringTaskUseCase     │ Creates task, returns ID                             │
├────────────────────────────────┼──────────────────────────────────────────────────────┤
│ UpdateRecurringTaskUseCase     │ Updates task fields                                  │
├────────────────────────────────┼──────────────────────────────────────────────────────┤
│ ToggleRecurringTaskUseCase     │ Enable/disable                                       │
├────────────────────────────────┼──────────────────────────────────────────────────────┤
│ DeleteRecurringTaskUseCase     │ Delete by ID                                         │
├────────────────────────────────┼──────────────────────────────────────────────────────┤
│ ExecuteRecurringTaskUseCase    │ Loads prompt, runs via AgentExecutor, records result │
├────────────────────────────────┼──────────────────────────────────────────────────────┤
│ GetTaskExecutionHistoryUseCase │ Recent executions for detail screen                  │
└────────────────────────────────┴──────────────────────────────────────────────────────┘

All follow existing pattern: class Foo @Inject constructor(repo) { suspend operator fun invoke(...) }

 ---
Phase 3: Agent Tools + System Prompt

3.1 RecurringTaskTools

New file: agent/RecurringTaskTools.kt

@LLMDescription("Tools for managing recurring scheduled tasks")
class RecurringTaskTools @Inject constructor(
private val repository: RecurringTaskRepository
) : ToolSet {

     @Tool
     @LLMDescription("Create a new recurring task")
     suspend fun createRecurringTask(
         title: String, prompt: String,
         hour: Int, minute: Int,
         daysOfWeek: String,      // "1,2,3,4,5" or "" for every day
         scheduleDisplay: String,
         relatedApps: String = ""
     ): String

     @Tool
     @LLMDescription("List all recurring tasks with their status")
     suspend fun listRecurringTasks(): String

     @Tool
     @LLMDescription("Update an existing recurring task")
     suspend fun updateRecurringTask(taskId: Long, ...): String

     @Tool
     @LLMDescription("Delete a recurring task by ID")
     suspend fun deleteRecurringTask(taskId: Long): String
}

Note: The coordinator (Phase 5) will be injected later to wire scheduling. Initially tools just hit the repository.

3.2 Register tools in DI

Modify: agent/di/AgentModule.kt
- Add provideRecurringTaskTools(repository: RecurringTaskRepository)
- Update provideAndroidAgentFactory to accept and pass RecurringTaskTools

3.3 Update AndroidAgentFactory

Modify: agent/AndroidAgentFactory.kt
- Add recurringTaskTools constructor param
- Register in ToolRegistry { ... tools(recurringTaskTools) }

3.4 Update SystemPrompts

Modify: agent/SystemPrompts.kt

Add after NOTIFICATION TOOLS section:
RECURRING TASK TOOLS:
- createRecurringTask(title, prompt, hour, minute, daysOfWeek, scheduleDisplay, relatedApps)
- listRecurringTasks()
- updateRecurringTask(taskId, ...)
- deleteRecurringTask(taskId)

When a user says "every morning at 9am...", "remind me daily...", or "schedule a task to...",
use createRecurringTask(). Extract: a short title, a detailed prompt for what the agent
should do autonomously, the time (24h format), days of week (1=Mon..7=Sun, empty=daily),
and any related app packages.

 ---
Phase 4: Navigation — Side Drawer

4.1 Add navigation routes

Modify: presentation/navigation/NavigationScreen.kt
@Serializable object RecurringTaskList : NavigationScreen()
@Serializable data class RecurringTaskDetail(val taskId: Long) : NavigationScreen()

4.2 Create drawer content

New file: presentation/navigation/AppDrawerContent.kt

ModalDrawerSheet with NavigationDrawerItem entries: Chat, Recurring Tasks, Settings. Uses Material3 icons (Chat, Repeat, Settings).

4.3 Update NavigationStack

Modify: presentation/navigation/NavigationStack.kt

Wrap NavHost in ModalNavigationDrawer. Pass drawerState and onOpenDrawer lambda to screens. Add composable<RecurringTaskList> and composable<RecurringTaskDetail> routes.

4.4 Update ChatScreen

Modify: presentation/chat/ChatScreen.kt

- Change param from onNavigateToTelegramSettings to onOpenDrawer
- Replace actions = { IconButton(Settings) } with navigationIcon = { IconButton(Menu) }

 ---
Phase 5: Scheduling — AlarmManager + WorkManager

5.1 Add WorkManager dependency

Modify: gradle/libs.versions.toml — add workManager = "2.10.1", work-runtime-ktx library, hilt-work, hilt-compiler
Modify: app/build.gradle.kts — add implementation(libs.work.runtime.ktx), implementation(libs.hilt.work), ksp(libs.hilt.compiler)

5.2 RecurringTaskWorker

New file: framework/scheduler/RecurringTaskWorker.kt

@HiltWorker CoroutineWorker that:
1. Reads taskId from inputData
2. Loads task from repository, checks if enabled
3. Executes prompt via AgentExecutor.execute(task.prompt, requireServiceConnection = false)
4. Records execution result
5. Saves summary to conversation history
6. Sends to Telegram if connected
7. Reschedules the next alarm via RecurringTaskScheduler

5.3 RecurringTaskScheduler

New file: framework/scheduler/RecurringTaskScheduler.kt

Uses AlarmManager.setExactAndAllowWhileIdle() to fire at precise times. Calculates next fire time based on hour/minute/daysOfWeek using Calendar.

5.4 RecurringTaskReceiver

New file: framework/scheduler/RecurringTaskReceiver.kt

BroadcastReceiver that receives alarm, enqueues a OneTimeWorkRequest for RecurringTaskWorker.

5.5 RecurringTaskCoordinator

New file: framework/scheduler/RecurringTaskCoordinator.kt

Orchestrates repository + scheduler:
- createAndSchedule(task) — save to DB, schedule alarm
- toggleAndReschedule(taskId, enabled) — update DB, schedule or cancel alarm
- deleteAndCancel(taskId) — delete from DB, cancel alarm
- rescheduleAllEnabled() — called on app start / boot

5.6 Initialize WorkManager with HiltWorkerFactory

Modify: presentation/ClawdroidApp.kt
- Implement Configuration.Provider
- Inject HiltWorkerFactory + RecurringTaskCoordinator
- Call coordinator.rescheduleAllEnabled() in onCreate()

5.7 Update AndroidManifest

Modify: AndroidManifest.xml
- Add RECEIVE_BOOT_COMPLETED, SCHEDULE_EXACT_ALARM permissions
- Register RecurringTaskReceiver
- Add WorkManager initializer override <provider> block
- Register boot receiver to call rescheduleAllEnabled()

5.8 Wire coordinator into RecurringTaskTools

Modify: agent/RecurringTaskTools.kt
- Inject RecurringTaskCoordinator
- Use coordinator.createAndSchedule() instead of raw repository.create()
- Use coordinator.deleteAndCancel() instead of raw repository.delete()

 ---
Phase 6: Task Screens

6.1 RecurringTaskListViewModel

New file: presentation/recurringtask/RecurringTaskListViewModel.kt

State: tasks: List<RecurringTask>, isLoading: Boolean
Intents: ToggleTask(id, enabled), DeleteTask(id)
Observes observeRecurringTasksUseCase() flow.

6.2 RecurringTaskListScreen

New file: presentation/recurringtask/RecurringTaskListScreen.kt

- Scaffold + TopAppBar with back arrow
- Empty state: icon + "No recurring tasks yet" + hint to use chat
- LazyColumn of task cards showing: title, schedule, on/off Switch, last run status
- Card tap → navigate to detail

6.3 RecurringTaskDetailViewModel

New file: presentation/recurringtask/RecurringTaskDetailViewModel.kt

State: task data, executions list, edit fields, isRunning, showDeleteConfirmation
Intents: edit fields, save, toggle, run now, delete
Uses SavedStateHandle to get taskId from nav args.

6.4 RecurringTaskDetailScreen

New file: presentation/recurringtask/RecurringTaskDetailScreen.kt

LazyColumn with sections:
- Title — editable OutlinedTextField
- Prompt — multiline OutlinedTextField
- Schedule — time picker (hour/minute) + day-of-week FilterChips
- Related Apps — chips (display only for v1)
- Enable/Disable toggle
- "Run Now" button
- Execution History — last 10 runs with timestamp, status, summary
- Delete button with AlertDialog confirmation

 ---
Files Summary

New files (~20)

┌───────────┬──────────────────────────────────────────────────────────────┐
│   Layer   │                             File                             │
├───────────┼──────────────────────────────────────────────────────────────┤
│ Data      │ data/local/entity/recurringtask/RecurringTaskEntity.kt       │
├───────────┼──────────────────────────────────────────────────────────────┤
│ Data      │ data/local/entity/recurringtask/TaskExecutionEntity.kt       │
├───────────┼──────────────────────────────────────────────────────────────┤
│ Data      │ data/local/dao/recurringtask/RecurringTaskDao.kt             │
├───────────┼──────────────────────────────────────────────────────────────┤
│ Data      │ data/repository/recurringtask/RecurringTaskRepositoryImpl.kt │
├───────────┼──────────────────────────────────────────────────────────────┤
│ Domain    │ domain/model/RecurringTask.kt                                │
├───────────┼──────────────────────────────────────────────────────────────┤
│ Domain    │ domain/repository/recurringtask/RecurringTaskRepository.kt   │
├───────────┼──────────────────────────────────────────────────────────────┤
│ Domain    │ domain/usecase/recurringtask/ (8 use case files)             │
├───────────┼──────────────────────────────────────────────────────────────┤
│ Agent     │ agent/RecurringTaskTools.kt                                  │
├───────────┼──────────────────────────────────────────────────────────────┤
│ Framework │ framework/scheduler/RecurringTaskWorker.kt                   │
├───────────┼──────────────────────────────────────────────────────────────┤
│ Framework │ framework/scheduler/RecurringTaskScheduler.kt                │
├───────────┼──────────────────────────────────────────────────────────────┤
│ Framework │ framework/scheduler/RecurringTaskReceiver.kt                 │
├───────────┼──────────────────────────────────────────────────────────────┤
│ Framework │ framework/scheduler/RecurringTaskCoordinator.kt              │
├───────────┼──────────────────────────────────────────────────────────────┤
│ UI        │ presentation/navigation/AppDrawerContent.kt                  │
├───────────┼──────────────────────────────────────────────────────────────┤
│ UI        │ presentation/recurringtask/RecurringTaskListViewModel.kt     │
├───────────┼──────────────────────────────────────────────────────────────┤
│ UI        │ presentation/recurringtask/RecurringTaskListScreen.kt        │
├───────────┼──────────────────────────────────────────────────────────────┤
│ UI        │ presentation/recurringtask/RecurringTaskDetailViewModel.kt   │
├───────────┼──────────────────────────────────────────────────────────────┤
│ UI        │ presentation/recurringtask/RecurringTaskDetailScreen.kt      │
└───────────┴──────────────────────────────────────────────────────────────┘

Modified files (~10)

┌─────────────────────────────────────────────┬─────────────────────────────────────────────────┐
│                    File                     │                     Change                      │
├─────────────────────────────────────────────┼─────────────────────────────────────────────────┤
│ data/local/AppDatabase.kt                   │ Add entities, bump version to 6, add DAO        │
├─────────────────────────────────────────────┼─────────────────────────────────────────────────┤
│ data/di/DataModule.kt                       │ Add repository binding + DAO provider           │
├─────────────────────────────────────────────┼─────────────────────────────────────────────────┤
│ agent/di/AgentModule.kt                     │ Add RecurringTaskTools provider, update factory │
├─────────────────────────────────────────────┼─────────────────────────────────────────────────┤
│ agent/AndroidAgentFactory.kt                │ Accept + register RecurringTaskTools            │
├─────────────────────────────────────────────┼─────────────────────────────────────────────────┤
│ agent/SystemPrompts.kt                      │ Add recurring task tool docs + examples         │
├─────────────────────────────────────────────┼─────────────────────────────────────────────────┤
│ presentation/navigation/NavigationScreen.kt │ Add 2 routes                                    │
├─────────────────────────────────────────────┼─────────────────────────────────────────────────┤
│ presentation/navigation/NavigationStack.kt  │ Wrap in ModalNavigationDrawer, add routes       │
├─────────────────────────────────────────────┼─────────────────────────────────────────────────┤
│ presentation/chat/ChatScreen.kt             │ Hamburger menu replaces settings icon           │
├─────────────────────────────────────────────┼─────────────────────────────────────────────────┤
│ presentation/ClawdroidApp.kt                │ WorkManager init, reschedule on start           │
├─────────────────────────────────────────────┼─────────────────────────────────────────────────┤
│ app/build.gradle.kts + libs.versions.toml   │ WorkManager + hilt-work deps                    │
├─────────────────────────────────────────────┼─────────────────────────────────────────────────┤
│ AndroidManifest.xml                         │ Permissions, receiver, WorkManager provider     │
└─────────────────────────────────────────────┴─────────────────────────────────────────────────┘

 ---
Build Order

1. Phase 1 → Data compiles, no visible changes
2. Phase 2 → Use cases ready
3. Phase 3 → Chat-based task CRUD works (tasks saved to DB, no scheduling yet)
4. Phase 4 → Side drawer visible, stubs for new screens
5. Phase 6 → Full task list + detail UI
6. Phase 5 → AlarmManager scheduling wired, tasks fire on time
7. Wire coordinator into tools (Phase 5.8)

 ---
Verification

1. Chat creation: Send "every day at 9am check my WhatsApp" → agent calls createRecurringTask → task appears in DB
2. Task list screen: Open drawer → Recurring Tasks → see the created task with title, schedule, toggle
3. Task detail: Tap card → see prompt, time picker, day chips, Run Now button
4. Run Now: Tap → agent executes the prompt → execution appears in history
5. Toggle: Disable → alarm cancelled; Enable → alarm rescheduled
6. Scheduled execution: Set task for 1 min from now → verify alarm fires → worker executes → result saved
7. Chat management: "Show my tasks" → listRecurringTasks() returns list; "Delete the WhatsApp task" → deleteRecurringTask() removes it
8. Side drawer: Hamburger icon opens drawer with Chat, Recurring Tasks, Settings items; navigation works between all screens
