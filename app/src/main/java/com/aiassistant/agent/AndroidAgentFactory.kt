package com.aiassistant.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAgentFactory @Inject constructor(
    private val deviceTools: DeviceTools
) {
    fun createAgent(config: AgentConfig): AIAgent<String, String> {
        val executor = when (config.provider) {
            LLMProvider.OPENAI -> simpleOpenAIExecutor(config.apiKey)
            LLMProvider.ANTHROPIC -> simpleAnthropicExecutor(config.apiKey)
            LLMProvider.GOOGLE -> simpleGoogleAIExecutor(config.apiKey)
        }

        val model = resolveModel(config)

        return AIAgent(
            promptExecutor = executor,
            llmModel = model,
            systemPrompt = SystemPrompts.ANDROID_AUTOMATION,
            temperature = config.temperature,
            maxIterations = config.maxIterations,
            toolRegistry = ToolRegistry {
                tools(deviceTools)
            }
        )
    }

    private fun resolveModel(config: AgentConfig) = when (config.provider) {
        LLMProvider.OPENAI -> when (config.model) {
            "gpt-4o" -> OpenAIModels.Chat.GPT4o
            "gpt-4o-mini" -> OpenAIModels.Chat.GPT4oMini
            "gpt-4-turbo" -> OpenAIModels.Chat.GPT4_1Mini
            else -> OpenAIModels.Chat.GPT4o
        }
        LLMProvider.ANTHROPIC -> when (config.model) {
            "claude-3-5-sonnet" -> AnthropicModels.Sonnet_3_5
            "claude-3-opus" -> AnthropicModels.Opus_3
            "claude-3-haiku" -> AnthropicModels.Haiku_3
            else -> AnthropicModels.Sonnet_3_5
        }
        LLMProvider.GOOGLE -> when (config.model) {
            "gemini-1.5-pro" -> GoogleModels.Gemini2_0Flash
            "gemini-1.5-flash" -> GoogleModels.Gemini2_5Pro
            else -> GoogleModels.Gemini3_Pro_Preview
        }
    }
}
