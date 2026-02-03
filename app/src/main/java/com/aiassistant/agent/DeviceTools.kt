package com.aiassistant.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.aiassistant.data.mapper.UINodeFormatter
import com.aiassistant.domain.model.Action
import com.aiassistant.domain.model.ActionType
import com.aiassistant.domain.model.ScrollDirection
import com.aiassistant.domain.repository.ScreenRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

@LLMDescription("Android device automation tools")
class DeviceTools @Inject constructor(
    private val screenRepository: ScreenRepository,
    private val uiNodeFormatter: UINodeFormatter
) : ToolSet {

    @Tool
    @LLMDescription("Get current screen UI tree with element indices")
    suspend fun getScreen(): String {
        val nodes = screenRepository.captureScreen()
        return if (nodes.isEmpty()) "ERROR: Cannot read screen"
        else uiNodeFormatter.format(nodes)
    }

    @Tool
    @LLMDescription("Click element by index")
    suspend fun click(@LLMDescription("Element index") index: Int): String {
        val action = Action(type = ActionType.CLICK, index = index)
        val success = screenRepository.performAction(action)
        return if (success) "Clicked [$index]" else "FAILED: Click [$index]"
    }

    @Tool
    @LLMDescription("Type text into editable field")
    suspend fun setText(
        @LLMDescription("Field index") index: Int,
        @LLMDescription("Text to type") text: String
    ): String {
        val action = Action(type = ActionType.SET_TEXT, index = index, text = text)
        val success = screenRepository.performAction(action)
        return if (success) "Typed '$text' into [$index]" else "FAILED"
    }

    @Tool
    @LLMDescription("Scroll element in direction")
    suspend fun scroll(
        @LLMDescription("Element index") index: Int,
        @LLMDescription("up/down/left/right") direction: String
    ): String {
        val scrollDir = try {
            ScrollDirection.valueOf(direction.uppercase())
        } catch (e: IllegalArgumentException) {
            return "FAILED: Invalid direction '$direction'. Use: up, down, left, right"
        }
        val action = Action(type = ActionType.SCROLL, index = index, direction = scrollDir)
        return if (screenRepository.performAction(action)) "Scrolled $direction" else "FAILED"
    }

    @Tool
    @LLMDescription("Launch app by name")
    suspend fun launchApp(@LLMDescription("App name") appName: String): String {
        val action = Action(type = ActionType.LAUNCH, packageName = appName)
        return if (screenRepository.performAction(action)) "Launched $appName" else "FAILED: Could not launch $appName"
    }

    @Tool
    @LLMDescription("Press back button")
    suspend fun pressBack(): String {
        val action = Action(type = ActionType.BACK)
        return if (screenRepository.performAction(action)) "Pressed back" else "FAILED"
    }

    @Tool
    @LLMDescription("Press home button")
    suspend fun pressHome(): String {
        val action = Action(type = ActionType.HOME)
        return if (screenRepository.performAction(action)) "Pressed home" else "FAILED"
    }

    @Tool
    @LLMDescription("Wait for screen to settle after action")
    suspend fun waitForUpdate(@LLMDescription("Milliseconds") ms: Int = 1500): String {
        delay(ms.coerceIn(100, 5000).toLong())
        return "Waited ${ms}ms"
    }

    @Tool
    @LLMDescription("Signal task completion")
    fun taskComplete(@LLMDescription("Summary") summary: String): String {
        return "TASK_COMPLETE: $summary"
    }

    @Tool
    @LLMDescription("Signal task failure")
    fun taskFailed(@LLMDescription("Reason") reason: String): String {
        return "TASK_FAILED: $reason"
    }
}
