package com.aiassistant.agent.agent_strategy

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import com.aiassistant.agent.MobileAutomationTools
import com.aiassistant.agent.NotificationTools
import com.aiassistant.agent.QuickActionTools
import com.aiassistant.agent.RecurringTaskTools

class GeneralAgent(
  private val mobileAutomationTools: MobileAutomationTools,
  private val quickActionTools: QuickActionTools,
  private val notificationTools: NotificationTools,
  private val recurringTaskTools: RecurringTaskTools
) : AgentStrategy {

  override fun systemPrompt(): String {
    return SystemPrompts.GENERAL_AGENT
  }

  override fun toolRegistry() = ToolRegistry {
    tools(mobileAutomationTools)
    tools(quickActionTools)
    tools(notificationTools)
    tools(recurringTaskTools)
  }
}