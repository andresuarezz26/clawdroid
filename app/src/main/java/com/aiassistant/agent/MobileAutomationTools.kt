package com.aiassistant.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import android.util.Log
import com.aiassistant.data.mapper.UINodeFormatter
import com.aiassistant.domain.model.Action
import com.aiassistant.domain.model.ActionType
import com.aiassistant.domain.model.ScrollDirection
import com.aiassistant.domain.repository.ScreenRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

private const val TAG = "Agent"

@LLMDescription("Android device automation tools")
class MobileAutomationTools @Inject constructor(
  private val screenRepository: ScreenRepository,
  private val uiNodeFormatter: UINodeFormatter
) : ToolSet {

  @Tool
  @LLMDescription("Get current screen UI tree with element indices")
  suspend fun getScreen(): String {
    Log.i(TAG, "Tool: getScreen() called")
    val nodes = screenRepository.captureScreen()
    val result = if (nodes.isEmpty()) {
      "ERROR: Cannot read screen"
    } else {
      uiNodeFormatter.format(nodes)
    }
    Log.i(TAG, "Tool: getScreen() returned ${nodes.size} nodes. Result: ${if(nodes.isEmpty()) result else "" }")
    return result
  }

  @Tool
  @LLMDescription("Click element by index")
  suspend fun click(@LLMDescription("Element index") index: Int): String {
    Log.i(TAG, "Tool: click(index=$index) called")
    val action = Action(type = ActionType.CLICK, index = index)
    val success = screenRepository.performAction(action)
    val result = if (success) "Clicked [$index]" else "FAILED: Click [$index]"
    Log.i(TAG, "Tool: click(index=$index) -> ${if(success) "" else result}")
    return result
  }

  @Tool
  @LLMDescription("Type text into editable field")
  suspend fun setText(
    @LLMDescription("Field index") index: Int,
    @LLMDescription("Text to type") text: String
  ): String {
    Log.i(TAG, "Tool: setText(index=$index, text='$text') called")
    val action = Action(type = ActionType.SET_TEXT, index = index, text = text)
    val success = screenRepository.performAction(action)
    val result = if (success) "Typed '$text' into [$index]" else "FAILED"
    Log.i(TAG, "Tool: setText() -> $result")
    return result
  }

  @Tool
  @LLMDescription("Scroll element in direction")
  suspend fun scroll(
    @LLMDescription("Element index") index: Int,
    @LLMDescription("up/down/left/right") direction: String
  ): String {
    Log.i(TAG, "Tool: scroll(index=$index, direction='$direction') called")
    val scrollDir = try {
      ScrollDirection.valueOf(direction.uppercase())
    } catch (e: IllegalArgumentException) {
      Log.i(TAG, "Tool: scroll() -> FAILED: Invalid direction")
      return "FAILED: Invalid direction '$direction'. Use: up, down, left, right"
    }
    val action = Action(type = ActionType.SCROLL, index = index, direction = scrollDir)
    val result = if (screenRepository.performAction(action)) "Scrolled $direction" else "FAILED"
    Log.i(TAG, "Tool: scroll() -> $result")
    return result
  }

  @Tool
  @LLMDescription("Launch app by name")
  suspend fun launchApp(@LLMDescription("App name") appName: String): String {
    Log.i(TAG, "Tool: launchApp(appName='$appName') called")
    val action = Action(type = ActionType.LAUNCH, packageName = appName)
    val result =
      if (screenRepository.performAction(action)) "Launched $appName" else "FAILED: Could not launch $appName"
    Log.i(TAG, "Tool: launchApp() -> $result")
    return result
  }

  @Tool
  @LLMDescription("Press back button")
  suspend fun pressBack(): String {
    Log.i(TAG, "Tool: pressBack() called")
    val action = Action(type = ActionType.BACK)
    val result = if (screenRepository.performAction(action)) "Pressed back" else "FAILED"
    Log.i(TAG, "Tool: pressBack() -> $result")
    return result
  }

  @Tool
  @LLMDescription("Press home button")
  suspend fun pressHome(): String {
    Log.i(TAG, "Tool: pressHome() called")
    val action = Action(type = ActionType.HOME)
    val result = if (screenRepository.performAction(action)) "Pressed home" else "FAILED"
    Log.i(TAG, "Tool: pressHome() -> $result")
    return result
  }

  @Tool
  @LLMDescription("Wait for screen to settle after action")
  suspend fun waitForUpdate(@LLMDescription("Milliseconds") ms: Int = 1500): String {
    Log.i(TAG, "Tool: waitForUpdate(ms=$ms) called")
    delay(ms.coerceIn(100, 5000).toLong())
    Log.i(TAG, "Tool: waitForUpdate() -> Waited ${ms}ms")
    return "Waited ${ms}ms"
  }

  @Tool
  @LLMDescription("Signal task completion")
  fun taskComplete(@LLMDescription("Summary") summary: String): String {
    Log.i(TAG, "Tool: taskComplete(summary='$summary')")
    return "TASK_COMPLETE: $summary"
  }

  @Tool
  @LLMDescription("Signal task failure")
  fun taskFailed(@LLMDescription("Reason") reason: String): String {
    Log.i(TAG, "Tool: taskFailed(reason='$reason')")
    return "TASK_FAILED: $reason"
  }

  @Tool
  @LLMDescription(
    "Press the Enter/Search key on the keyboard. Use after typing text " +
        "in a search field to submit the search query."
  )
  suspend fun pressEnter(): String {
    Log.i(TAG, "Tool: pressEnter() called")
    val action = Action(type = ActionType.PRESS_ENTER)
    val result = if (screenRepository.performAction(action)) "Pressed Enter" else "FAILED"
    Log.i(TAG, "Tool: pressEnter() -> $result")
    return result
  }

  // Phase 1: Global Action Tools
  @Tool
  @LLMDescription("Open recent apps switcher to see and switch between recently used apps")
  suspend fun openRecentApps(): String {
    Log.i(TAG, "Tool: openRecentApps() called")
    val action = Action(type = ActionType.RECENT_APPS)
    val result = if (screenRepository.performAction(action)) "Opened recent apps" else "FAILED"
    Log.i(TAG, "Tool: openRecentApps() -> $result")
    return result
  }

  @Tool
  @LLMDescription("Pull down the notification shade to view notifications")
  suspend fun openNotifications(): String {
    Log.i(TAG, "Tool: openNotifications() called")
    val action = Action(type = ActionType.NOTIFICATIONS)
    val result = if (screenRepository.performAction(action)) "Opened notifications" else "FAILED"
    Log.i(TAG, "Tool: openNotifications() -> $result")
    return result
  }

  @Tool
  @LLMDescription("Open quick settings panel for toggles like WiFi, Bluetooth, etc.")
  suspend fun openQuickSettings(): String {
    Log.i(TAG, "Tool: openQuickSettings() called")
    val action = Action(type = ActionType.QUICK_SETTINGS)
    val result = if (screenRepository.performAction(action)) "Opened quick settings" else "FAILED"
    Log.i(TAG, "Tool: openQuickSettings() -> $result")
    return result
  }

  // Phase 2: Node Action Tools
  @Tool
  @LLMDescription("Long press on element to open context menu or trigger long-click action")
  suspend fun longClick(@LLMDescription("Element index") index: Int): String {
    Log.i(TAG, "Tool: longClick(index=$index) called")
    val action = Action(type = ActionType.LONG_CLICK, index = index)
    val success = screenRepository.performAction(action)
    val result = if (success) "Long clicked [$index]" else "FAILED: Long click [$index]"
    Log.i(TAG, "Tool: longClick(index=$index) -> ${if(success) "" else result}")
    return result
  }

  @Tool
  @LLMDescription("Focus on an element without clicking it")
  suspend fun focus(@LLMDescription("Element index") index: Int): String {
    Log.i(TAG, "Tool: focus(index=$index) called")
    val action = Action(type = ActionType.FOCUS, index = index)
    val success = screenRepository.performAction(action)
    val result = if (success) "Focused [$index]" else "FAILED: Focus [$index]"
    Log.i(TAG, "Tool: focus(index=$index) -> ${if(success) "" else result}")
    return result
  }

  @Tool
  @LLMDescription("Clear all text from an editable field")
  suspend fun clearText(@LLMDescription("Field index") index: Int): String {
    Log.i(TAG, "Tool: clearText(index=$index) called")
    val action = Action(type = ActionType.CLEAR_TEXT, index = index)
    val success = screenRepository.performAction(action)
    val result = if (success) "Cleared text at [$index]" else "FAILED: Clear text [$index]"
    Log.i(TAG, "Tool: clearText(index=$index) -> ${if(success) "" else result}")
    return result
  }

  // Phase 3: Gesture Action Tools
  @Tool
  @LLMDescription(
    "Swipe from one point to another. Use for dismissing notifications, " +
        "navigating carousels, or custom swipe gestures. Distance: short=100, medium=300, long=500 pixels."
  )
  suspend fun swipe(
    @LLMDescription("Start X coordinate") startX: Int,
    @LLMDescription("Start Y coordinate") startY: Int,
    @LLMDescription("End X coordinate") endX: Int,
    @LLMDescription("End Y coordinate") endY: Int,
    @LLMDescription("Duration in milliseconds (default 300)") duration: Long = 300
  ): String {
    Log.i(TAG, "Tool: swipe(startX=$startX, startY=$startY, endX=$endX, endY=$endY, duration=$duration) called")
    val action = Action(
      type = ActionType.SWIPE,
      startX = startX,
      startY = startY,
      endX = endX,
      endY = endY,
      duration = duration
    )
    val success = screenRepository.performAction(action)
    val result = if (success) "Swiped from ($startX,$startY) to ($endX,$endY)" else "FAILED: Swipe"
    Log.i(TAG, "Tool: swipe() -> $result")
    return result
  }

  @Tool
  @LLMDescription(
    "Drag from one point to another. Use for reordering items or moving elements. " +
        "Slower than swipe for precise control."
  )
  suspend fun drag(
    @LLMDescription("Start X coordinate") startX: Int,
    @LLMDescription("Start Y coordinate") startY: Int,
    @LLMDescription("End X coordinate") endX: Int,
    @LLMDescription("End Y coordinate") endY: Int,
    @LLMDescription("Duration in milliseconds (default 500)") duration: Long = 500
  ): String {
    Log.i(TAG, "Tool: drag(startX=$startX, startY=$startY, endX=$endX, endY=$endY, duration=$duration) called")
    val action = Action(
      type = ActionType.DRAG,
      startX = startX,
      startY = startY,
      endX = endX,
      endY = endY,
      duration = duration
    )
    val success = screenRepository.performAction(action)
    val result = if (success) "Dragged from ($startX,$startY) to ($endX,$endY)" else "FAILED: Drag"
    Log.i(TAG, "Tool: drag() -> $result")
    return result
  }
}
