package com.innovative.smis.domain.repository

import com.innovative.smis.domain.model.Task
import com.innovative.smis.domain.model.WorkflowStep
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    suspend fun getTasks(userId: String): Flow<Resource<List<Task>>>
    suspend fun getTaskById(taskId: String): Flow<Resource<Task>>
    suspend fun updateTaskStatus(taskId: String, status: String): Flow<Resource<Task>>
    
    suspend fun getWorkflowSteps(taskId: String): Flow<Resource<List<WorkflowStep>>>
    suspend fun createWorkflowStep(step: WorkflowStep): Flow<Resource<WorkflowStep>>
    suspend fun updateWorkflowStep(stepId: String, formData: Map<String, Any>): Flow<Resource<WorkflowStep>>
    
    suspend fun syncOfflineData(): Flow<Resource<Unit>>
    suspend fun cacheTasksLocally(tasks: List<Task>)
    suspend fun getCachedTasks(): List<Task>
}