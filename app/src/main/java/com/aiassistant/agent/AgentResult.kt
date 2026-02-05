package com.aiassistant.agent

sealed interface AgentResult {
    data class Success(val response: String) : AgentResult
    data class Failure(val reason: String) : AgentResult
    data object ServiceNotConnected : AgentResult
    data object Cancelled : AgentResult
}
