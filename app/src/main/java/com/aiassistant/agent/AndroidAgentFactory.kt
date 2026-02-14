package com.aiassistant.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.context.RollbackStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.agents.snapshot.providers.InMemoryPersistenceStorageProvider
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import android.util.Log
import com.aiassistant.domain.model.ChatMessage
import io.ktor.websocket.WebSocketDeflateExtension.Companion.install
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Agent"

@Singleton
class AndroidAgentFactory @Inject constructor(
    private val deviceTools: DeviceTools,
    private val quickActionTools: QuickActionTools,
    private val notificationTools: NotificationTools
) {
    fun createAgent(config: AgentConfig): AIAgent<String, String> {
        return createAgent(config, emptyList())
    }

    fun createAgent(config: AgentConfig, conversationHistory: List<ChatMessage>): AIAgent<String, String> {
        Log.i(TAG, "createAgent called with provider=${config.provider}, model=${config.model}, historySize=${conversationHistory.size}")
        val executor = when (config.provider) {
            LLMProvider.OPENAI -> simpleOpenAIExecutor(config.apiKey)
            LLMProvider.ANTHROPIC -> simpleAnthropicExecutor(config.apiKey)
            LLMProvider.GOOGLE -> simpleGoogleAIExecutor(config.apiKey)
        }

        val model = resolveModel(config)
        Log.i(TAG, "Resolved model: $model, maxIterations=${config.maxIterations}, temperature=${config.temperature}")

        val systemPromptWithHistory = buildSystemPrompt(conversationHistory)

        return AIAgent(
            promptExecutor = executor,
            llmModel = model,
            systemPrompt = systemPromptWithHistory,
            temperature = config.temperature,
            maxIterations = config.maxIterations,
            toolRegistry = ToolRegistry {
                tools(deviceTools)
                tools(quickActionTools)
                tools(notificationTools)
            }

        ){
            install(Persistence ) {
              storage = InMemoryPersistenceStorageProvider()

              // Automatically save state after each node
              enableAutomaticPersistence = true

              // KEY: This maintains conversation history but starts fresh execution each run
              rollbackStrategy = RollbackStrategy.MessageHistoryOnly
            }
        }
          .also {
            Log.i(TAG, "Agent created successfully")
        }
    }

    private fun buildSystemPrompt(history: List<ChatMessage>): String {
        if (history.isEmpty()) {
            return SystemPrompts.ANDROID_AUTOMATION
        }

        val historySection = buildString {
            appendLine("\n\n--- CONVERSATION HISTORY ---")
            appendLine("(Previous messages in this session for context)")
            history.takeLast(30).forEach { msg ->  // Limit to last 30 to manage tokens
                val role = if (msg.isUser) "User" else "Assistant"
                appendLine("$role: ${msg.content}")
            }
            appendLine("--- END HISTORY ---\n")
        }

        return SystemPrompts.ANDROID_AUTOMATION + historySection
    }

    private fun resolveModel(config: AgentConfig) = when (config.provider) {
        LLMProvider.OPENAI -> when (config.model) {
            "gpt-4o" -> OpenAIModels.Chat.GPT4o
            "gpt-4o-mini" -> OpenAIModels.Chat.GPT4oMini
            "gpt-4-turbo" -> OpenAIModels.Chat.GPT4_1Mini
            "gpt-5" -> OpenAIModels.Chat.GPT5
            "gpt-5-mini" -> OpenAIModels.Chat.GPT5Mini
            "gpt-5.2" -> OpenAIModels.Chat.GPT5_2
            else -> OpenAIModels.Chat.GPT4oMini
        }
        LLMProvider.ANTHROPIC -> when (config.model) {
            "claude-3-5-sonnet" -> AnthropicModels.Sonnet_3_5
            "claude-3-opus" -> AnthropicModels.Opus_3
            "claude-3-haiku" -> AnthropicModels.Haiku_3
            else -> AnthropicModels.Sonnet_3_5
        }
        LLMProvider.GOOGLE -> when (config.model) {
            "gemini-2.0-flash" -> GoogleModels.Gemini2_0Flash
            "gemini-2.5-pro" -> GoogleModels.Gemini2_5Pro
            else -> GoogleModels.Gemini3_Pro_Preview
        }
    }
}
