package com.aiassistant.domain.repository

import com.aiassistant.domain.model.TaskLog
import com.aiassistant.domain.model.TaskResult
import kotlinx.coroutines.flow.Flow

interface TaskLogRepository {
    suspend fun saveLog(command: String, result: TaskResult, stepsUsed: Int)
    fun getRecentLogs(limit: Int): Flow<List<TaskLog>>
}