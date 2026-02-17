package com.aiassistant.agent.agent_strategy

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import com.aiassistant.agent.MobileAutomationTools
import com.aiassistant.agent.NotificationTools
import com.aiassistant.agent.QuickActionTools

class NotificationReactor(
  private val mobileAutomationTools: MobileAutomationTools,
  private val quickActionTools: QuickActionTools,
  private val notificationTools: NotificationTools,
) : AgentStrategy {
  override fun systemPrompt(): String {
    return SystemPrompts.GENERAL_AGENT
  }

  override fun toolRegistry() = ToolRegistry {
    tools(notificationTools)
    tools(mobileAutomationTools)
    tools(quickActionTools)
  }
}