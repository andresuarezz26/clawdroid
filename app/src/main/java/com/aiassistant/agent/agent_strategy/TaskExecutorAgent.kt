package com.aiassistant.agent.agent_strategy

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import com.aiassistant.agent.MobileAutomationTools
import com.aiassistant.agent.QuickActionTools

class TaskExecutorAgent(
  private val mobileAutomationTools: MobileAutomationTools,
  private val quickActionTools: QuickActionTools,
) : AgentStrategy {
  override fun systemPrompt(): String {
    return SystemPrompts.TASK_EXECUTOR
  }

  override fun toolRegistry() = ToolRegistry {
    tools(mobileAutomationTools)
    tools(quickActionTools)
  }
}