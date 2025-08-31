package com.innovative.smis.data.repository

import com.innovative.smis.data.local.offline.OfflineManager
import com.innovative.smis.data.local.dao.*
import com.innovative.smis.data.model.response.*
import com.innovative.smis.data.local.offline.*
import com.innovative.smis.domain.model.Task
import com.innovative.smis.domain.model.WorkflowStep
import com.innovative.smis.domain.repository.TaskRepository
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class OfflineTaskRepositoryImpl(
    private val offlineManager: OfflineManager,
    private val taskDao: TaskDao,
    private val workflowStepDao: WorkflowStepDao,
    private val syncQueueDao: SyncQueueDao
) : TaskRepository {

    override suspend fun getTasks(userId: String): Flow<Resource<List<Task>>> = flow {
        emit(Resource.Loading())
        try {
            val cachedTasks = getCachedTasks()
            emit(Resource.Success(cachedTasks))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load tasks"))
        }
    }

    override suspend fun getTaskById(taskId: String): Flow<Resource<Task>> = flow {
        emit(Resource.Loading())
        try {
            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            val taskEntity = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                taskDao.getTaskById(taskId)
            }
            if (taskEntity != null) {
                emit(Resource.Success(taskEntity.toTask()))
            } else {
                emit(Resource.Error("Task not found"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load task"))
        }
    }

    override suspend fun updateTaskStatus(taskId: String, status: String): Flow<Resource<Task>> = flow {
        emit(Resource.Loading())
        try {
            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            val updatedTask = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val taskEntity = taskDao.getTaskById(taskId)
                if (taskEntity != null) {
                    val updated = taskEntity.copy(
                        status = status,
                        updatedAt = System.currentTimeMillis().toString(),
                        syncStatus = "PENDING"
                    )
                    taskDao.updateTask(updated)
                    updated.toTask()
                } else {
                    null
                }
            }

            if (updatedTask != null) {
                emit(Resource.Success(updatedTask))
            } else {
                emit(Resource.Error("Task not found"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update task"))
        }
    }

    override suspend fun getWorkflowSteps(taskId: String): Flow<Resource<List<WorkflowStep>>> = flow {
        emit(Resource.Loading())
        try {
            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            val steps = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                workflowStepDao.getWorkflowStepsForTask(taskId)
            }
            emit(Resource.Success(steps.map { it.toWorkflowStep() }))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load workflow steps"))
        }
    }

    override suspend fun createWorkflowStep(step: WorkflowStep): Flow<Resource<WorkflowStep>> = flow {
        emit(Resource.Loading())
        try {
            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                workflowStepDao.insertSteps(listOf(step.toEntity()))
            }
            emit(Resource.Success(step))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to create workflow step"))
        }
    }

    override suspend fun updateWorkflowStep(stepId: String, formData: Map<String, Any>): Flow<Resource<WorkflowStep>> = flow {
        emit(Resource.Loading())
        try {
            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            val updatedWorkflowStep = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val stepEntity = workflowStepDao.getStepById(stepId)
                if (stepEntity != null) {
                    val updatedStep = stepEntity.copy(
                        formData = formData.toString(),
                        isCompleted = true,
                        completedAt = System.currentTimeMillis().toString(),
                        syncStatus = "PENDING"
                    )
                    workflowStepDao.updateStep(updatedStep)
                    updatedStep.toWorkflowStep()
                } else {
                    null
                }
            }

            if (updatedWorkflowStep != null) {
                emit(Resource.Success(updatedWorkflowStep))
            } else {
                emit(Resource.Error("Workflow step not found"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update workflow step"))
        }
    }

    override suspend fun syncOfflineData(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val syncResult = offlineManager.syncAllData().first()
            when (syncResult) {
                is Resource.Success -> emit(Resource.Success(Unit))
                is Resource.Error -> emit(Resource.Error(syncResult.message ?: "Sync failed"))
                is Resource.Loading -> emit(Resource.Loading())
                is Resource.Idle -> emit(Resource.Success(Unit))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Sync failed"))
        }
    }

    override suspend fun cacheTasksLocally(tasks: List<Task>) {
        try {
            val taskEntities = tasks.map { it.toEntity() }
            taskDao.insertTasks(taskEntities)
        } catch (e: Exception) {
            //
        }
    }

    override suspend fun getCachedTasks(): List<Task> {
        return try {
            taskDao.getAllTasks().first().map { it.toTask() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun syncAllData(): Flow<Resource<String>> {
        return offlineManager.syncAllData()
    }

    suspend fun getPendingSyncCount(): Int {
        return try {
            syncQueueDao.getPendingCount()
        } catch (e: Exception) {
            0
        }
    }

    fun isOfflineMode(): Boolean {
        return !offlineManager.isNetworkAvailable()
    }
}