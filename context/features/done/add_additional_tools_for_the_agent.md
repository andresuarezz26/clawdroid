Overview

Add remaining 11 ActionTypes to achieve full spec coverage. Currently at 35% (7/20 types implemented).

Files to Modify

1. app/src/main/java/com/aiassistant/domain/model/Action.kt - Add enum values + model fields
2. app/src/main/java/com/aiassistant/framework/accessibility/ActionPerformer.kt - Implement actions
3. app/src/main/java/com/aiassistant/agent/DeviceTools.kt - Expose tools to LLM
4. app/src/main/java/com/aiassistant/agent/SystemPrompts.kt - Document new capabilities

 ---
Phase 1: Global Actions (Easy - Direct API mapping)

ActionTypes: RECENT_APPS, NOTIFICATIONS, QUICK_SETTINGS

Action.kt changes:
enum class ActionType {
// Existing...
RECENT_APPS,      // NEW
NOTIFICATIONS,    // NEW
QUICK_SETTINGS,   // NEW
}

ActionPerformer.kt implementation:
ActionType.RECENT_APPS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
ActionType.NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
ActionType.QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

DeviceTools.kt - New tools:
- openRecentApps() - Opens app switcher
- openNotifications() - Pulls down notification shade
- openQuickSettings() - Opens quick settings panel

 ---
Phase 2: Simple Node Actions (Medium - Single action per node)

ActionTypes: LONG_CLICK, FOCUS, CLEAR_TEXT

Action.kt changes:
enum class ActionType {
// Existing...
LONG_CLICK,    // NEW
FOCUS,         // NEW
CLEAR_TEXT,    // NEW
}

ActionPerformer.kt implementation:
ActionType.LONG_CLICK -> node.performAction(ACTION_LONG_CLICK)
ActionType.FOCUS -> node.performAction(ACTION_FOCUS)
ActionType.CLEAR_TEXT -> {
node.performAction(ACTION_FOCUS)
node.performAction(ACTION_SET_TEXT, Bundle().apply { putCharSequence(ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "") })
}

DeviceTools.kt - New tools:
- longClick(index: Int) - Long press for context menus
- focus(index: Int) - Focus element without clicking
- clearText(index: Int) - Clear text field content

 ---
Phase 3: Gesture Actions (Complex - Coordinate-based)

ActionTypes: SWIPE, DRAG

Action.kt changes:
data class Action(
// Existing fields...
val startX: Int? = null,    // NEW - For gestures
val startY: Int? = null,    // NEW
val endX: Int? = null,      // NEW
val endY: Int? = null,      // NEW
val duration: Long? = null, // NEW - Gesture duration ms
)

enum class ActionType {
// Existing...
SWIPE,  // NEW
DRAG,   // NEW
}

ActionPerformer.kt implementation:
Uses GestureDescription API (API 24+):
ActionType.SWIPE -> performSwipeGesture(startX, startY, endX, endY, duration)
ActionType.DRAG -> performDragGesture(startX, startY, endX, endY, duration)

Helper method:
private fun performSwipeGesture(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Long): Boolean {
val path = Path().apply {
moveTo(startX.toFloat(), startY.toFloat())
lineTo(endX.toFloat(), endY.toFloat())
}
val gesture = GestureDescription.Builder()
.addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
.build()
return service.dispatchGesture(gesture, null, null)
}

DeviceTools.kt - New tools:
- swipe(fromIndex: Int, direction: String, distance: String) - Swipe from element
- swipeCoordinates(startX, startY, endX, endY) - Raw coordinate swipe
- drag(fromIndex: Int, toIndex: Int) - Drag element to target

 ---

Implementation Order (Recommended)
┌──────────┬─────────┬────────────────────────────────────────────┬────────┬────────┐
│ Priority │  Phase  │                ActionTypes                 │ Effort │ Value  │
├──────────┼─────────┼────────────────────────────────────────────┼────────┼────────┤
│ 1        │ Phase 1 │ RECENT_APPS, NOTIFICATIONS, QUICK_SETTINGS │ Low    │ High   │
├──────────┼─────────┼────────────────────────────────────────────┼────────┼────────┤
│ 2        │ Phase 2 │ LONG_CLICK, FOCUS, CLEAR_TEXT              │ Low    │ High   │
├──────────┼─────────┼────────────────────────────────────────────┼────────┼────────┤ ├──────────┼─────────┼────────────────────────────────────────────┼────────┼────────┤ ├──────────┼─────────┼────────────────────────────────────────────┼────────┼────────┤
│ 5        │ Phase 3 │ SWIPE, DRAG                                │ High   │ Medium │
└──────────┴─────────┴────────────────────────────────────────────┴────────┴────────┘

 ---
SystemPrompts.kt Updates

Add to ACTION RULES section:
- Long click elements to access context menus (longClick)
- Clear text fields before typing new values when replacing (clearText)
- Use copy/paste for moving text between fields
- Open notifications to check alerts and messages
- Use swipe for dismiss actions (delete emails, clear notifications)

Add to COMMON PATTERNS section:
- Context menus: longClick the item → read menu options → click desired action
- Quick settings: openQuickSettings() → toggle WiFi/Bluetooth/etc
- Notification actions: openNotifications() → find notification → click or swipe away

 ---
Verification Plan

1. E2E agent tests:
- "Turn on WiFi" → should use openQuickSettings + click
- "Dismiss the top notification" → should use openNotifications + swipe
- "Copy the phone number and paste it in notes" → should use copy + paste

 ---
API Level Considerations
┌────────────────┬─────────┬────────────────────┐
│   ActionType   │ Min API │      Fallback      │
├────────────────┼─────────┼────────────────────┤
│ RECENT_APPS    │ 16      │ N/A                │
├────────────────┼─────────┼────────────────────┤
│ NOTIFICATIONS  │ 16      │ N/A                │
├────────────────┼─────────┼────────────────────┤
│ QUICK_SETTINGS │ 17      │ N/A                │
├────────────────┼─────────┼────────────────────┤
│ LONG_CLICK     │ 16      │ N/A                │
├────────────────┼─────────┼────────────────────┤
│ SWIPE/DRAG     │ 24      │ Shell: input swipe │
└────────────────┴─────────┴────────────────────┘
Current app minSdk should be checked; fallbacks may be needed for older devices.
