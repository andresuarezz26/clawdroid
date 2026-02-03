package com.aiassistant.agent

import ai.koog.agents.core.feature.message.FeatureMessage
import ai.koog.agents.core.feature.message.FeatureMessageProcessor
import ai.koog.agents.core.feature.model.events.AgentCompletedEvent
import ai.koog.agents.core.feature.model.events.AgentExecutionFailedEvent
import ai.koog.agents.core.feature.model.events.AgentStartingEvent
import ai.koog.agents.core.feature.model.events.ToolCallStartingEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

class AgentEventProcessor(override val isOpen: StateFlow<Boolean>) : FeatureMessageProcessor() {
    private val _progress = MutableSharedFlow<AgentProgress>(replay = 1)
    val progress: SharedFlow<AgentProgress> = _progress.asSharedFlow()

    override suspend fun processMessage(message: FeatureMessage) {
        when (message) {
            is AgentStartingEvent -> _progress.emit(AgentProgress.Started)
            is ToolCallStartingEvent -> _progress.emit(
                AgentProgress.ToolExecuting(message.toolName)
            )
            is AgentCompletedEvent -> _progress.emit(
                AgentProgress.Completed(message.result?.toString())
            )
            is AgentExecutionFailedEvent -> _progress.emit(
                AgentProgress.Failed(message.error?.message)
            )
            else -> { /* Ignore other messages */ }
        }
    }


    override suspend fun close() {
        TODO("Not yet implemented")
    }
}

sealed interface AgentProgress {
    data object Started : AgentProgress
    data class ToolExecuting(val toolName: String) : AgentProgress
    data class Completed(val result: String?) : AgentProgress
    data class Failed(val error: String?) : AgentProgress
}
