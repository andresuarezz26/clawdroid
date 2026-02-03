package com.aiassistant.data.repository

import com.aiassistant.data.local.dao.TaskLogDao
import com.aiassistant.data.local.entity.TaskLogEntity
import com.aiassistant.domain.model.TaskLog
import com.aiassistant.domain.model.TaskResult
import com.aiassistant.domain.repository.TaskLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskLogRepositoryImpl @Inject constructor(
    private val taskLogDao: TaskLogDao
) : TaskLogRepository {

    override suspend fun saveLog(command: String, result: TaskResult, stepsUsed: Int) {
        val entity = TaskLogEntity(
            command = command,
            resultType = when (result) {
                is TaskResult.Success -> "success"
                is TaskResult.Failed -> "failed"
                is TaskResult.MaxStepsReached -> "max_steps"
                is TaskResult.Cancelled -> "cancelled"
            },
            resultSummary = when (result) {
                is TaskResult.Success -> result.summary
                is TaskResult.Failed -> result.reason
                else -> result.toString()
            },
            stepsUsed = stepsUsed,
            timestamp = System.currentTimeMillis()
        )
        taskLogDao.insert(entity)
    }

    override fun getRecentLogs(limit: Int): Flow<List<TaskLog>> {
        return taskLogDao.getRecent(limit).map { entities ->
            entities.map { entity ->
                TaskLog(
                    id = entity.id,
                    command = entity.command,
                    result = entity.resultType,
                    resultSummary = entity.resultSummary,
                    timestamp = entity.timestamp
                )
            }
        }
    }
}