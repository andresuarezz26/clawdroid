Add Interval/Frequency Scheduling to Recurring Tasks

Context

The recurring tasks feature currently only supports fixed-time scheduling (hour + minute + daysOfWeek). This means tasks like "every 15 minutes" or "every 2 hours" cannot be expressed. Adding an
intervalMinutes field enables both modes: fixed-time ("every day at 9 AM") and interval-based ("every 30 minutes, 24/7"). Minimum interval: 15 minutes (WorkManager floor).

Approach

Add a nullable intervalMinutes: Int? field. When non-null and > 0, the task uses interval scheduling and hour/minute/daysOfWeek are ignored. When null or 0, current fixed-time behavior applies. DB
version bump 6→7 with destructive migration (already configured).

 ---
Changes

1. Data Model — add field

- domain/model/RecurringTask.kt — add val intervalMinutes: Int? = null
- data/local/entity/recurringtask/RecurringTaskEntity.kt — add val intervalMinutes: Int? = null
- data/local/AppDatabase.kt — bump version = 6 → version = 7
- data/repository/recurringtask/RecurringTaskRepositoryImpl.kt — add intervalMinutes to both toDomain() and toEntity() mappers

2. Scheduler — handle interval mode

- framework/scheduler/RecurringTaskScheduler.kt
    - Update scheduleTask(): if task.intervalMinutes != null && task.intervalMinutes > 0, call new scheduleInterval(task) instead
    - New scheduleInterval(): uses PeriodicWorkRequestBuilder(intervalMinutes, TimeUnit.MINUTES) with no initial delay
    - Update cancelTask(): interval tasks use WorkManager cancel (same as flexible)

3. Worker — skip day-of-week check for intervals

- framework/scheduler/RecurringTaskWorker.kt
    - Guard the day-of-week skip logic: only apply when task.intervalMinutes == null || task.intervalMinutes == 0
    - Interval tasks: no rescheduling needed after execution (WorkManager auto-repeats)

4. Agent Tools — add parameter

- agent/RecurringTaskTools.kt
    - createRecurringTask(): add intervalMinutes: Int = 0 param with @LLMDescription. When > 0, hour/minute/daysOfWeek are ignored.
    - updateRecurringTask(): add intervalMinutes: Int = -1 param (-1 = keep current)
    - Pass intervalMinutes through to domain model

5. System Prompt — document new param

- agent/SystemPrompts.kt (GENERAL_AGENT section)
    - Add intervalMinutes to tool signature docs
    - Add guidance: "For periodic tasks like 'every 30 minutes', use intervalMinutes=30 and leave hour/minute at 0"
    - Add examples: "Check cheap flights every 2 hours" → intervalMinutes=120

6. Detail Screen — interval mode UI

- presentation/recurring_task_detail/RecurringTaskDetailViewModel.kt
    - Add editIntervalMinutes: Int? = null to state
    - Add UpdateIntervalMinutes(value: Int?) intent
    - Update save(): when interval mode, set scheduleDisplay = "Every X minutes/hours", set hour=0, minute=0, daysOfWeek=emptyList()
    - Update loadTask(): populate editIntervalMinutes from task
- presentation/recurring_task_detail/RecurringTaskDetailScreen.kt
    - Add a mode toggle (two FilterChips: "Fixed Time" / "Interval") above the schedule section
    - Fixed Time mode: show current TimePicker + day chips (no change)
    - Interval mode: show single OutlinedTextField for minutes (number input) with label "Run every X minutes (min 15)"

7. List Screen — no changes

RecurringTaskListScreen.kt already displays task.scheduleDisplay, which will be set correctly at creation/save time.

 ---
Files Summary

┌────────────────────────────────────────────────────────────────────┬──────────────────────────────────────┐
│                                File                                │                Change                │
├────────────────────────────────────────────────────────────────────┼──────────────────────────────────────┤
│ domain/model/RecurringTask.kt                                      │ Add intervalMinutes: Int? = null     │
├────────────────────────────────────────────────────────────────────┼──────────────────────────────────────┤
│ data/local/entity/recurringtask/RecurringTaskEntity.kt             │ Add intervalMinutes: Int? = null     │
├────────────────────────────────────────────────────────────────────┼──────────────────────────────────────┤
│ data/local/AppDatabase.kt                                          │ Bump version 6 → 7                   │
├────────────────────────────────────────────────────────────────────┼──────────────────────────────────────┤
│ data/repository/recurringtask/RecurringTaskRepositoryImpl.kt       │ Add field to mappers                 │
├────────────────────────────────────────────────────────────────────┼──────────────────────────────────────┤
│ framework/scheduler/RecurringTaskScheduler.kt                      │ Add scheduleInterval() path          │
├────────────────────────────────────────────────────────────────────┼──────────────────────────────────────┤
│ framework/scheduler/RecurringTaskWorker.kt                         │ Guard day-of-week check              │
├────────────────────────────────────────────────────────────────────┼──────────────────────────────────────┤
│ agent/RecurringTaskTools.kt                                        │ Add intervalMinutes param            │
├────────────────────────────────────────────────────────────────────┼──────────────────────────────────────┤
│ agent/SystemPrompts.kt                                             │ Document param + examples            │
├────────────────────────────────────────────────────────────────────┼──────────────────────────────────────┤
│ presentation/recurring_task_detail/RecurringTaskDetailViewModel.kt │ Add interval state/intent/save logic │
├────────────────────────────────────────────────────────────────────┼──────────────────────────────────────┤
│ presentation/recurring_task_detail/RecurringTaskDetailScreen.kt    │ Add mode toggle + interval input     │
└────────────────────────────────────────────────────────────────────┴──────────────────────────────────────┘

Verification

1. Build: ./gradlew compileDebugKotlin — must pass
2. Chat creation (fixed-time): "Every day at 9am check WhatsApp" → creates task with intervalMinutes=null, schedules via existing path
3. Chat creation (interval): "Check my email every 30 minutes" → creates task with intervalMinutes=30, scheduleDisplay="Every 30 minutes"
4. Detail screen: open interval task → shows "Interval" chip selected, text field with "30"
5. Detail screen: open fixed-time task → shows "Fixed Time" chip selected, time picker + day chips
6. Toggle/delete via UI and chat commands still work for both modes
