package com.aiassistant.agent

data class AgentConfig(
    val provider: LLMProvider = LLMProvider.OPENAI,
    val model: String = "gpt-4o",
    val apiKey: String,
    val temperature: Double = 0.1,
    val maxIterations: Int = 50
)

enum class LLMProvider { OPENAI, ANTHROPIC, GOOGLE }
