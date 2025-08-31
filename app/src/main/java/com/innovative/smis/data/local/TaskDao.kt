package com.innovative.smis.data.local

import androidx.room.*
import com.innovative.smis.domain.model.Task

@Dao
interface TaskDao {
    
    @Query("SELECT * FROM tasks WHERE assignedToUserId = :userId ORDER BY scheduledDate ASC")
    suspend fun getTasksByUserId(userId: String): List<Task>
    
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<Task>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): Task?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)
    
    @Update
    suspend fun updateTask(task: Task)
    
    @Delete
    suspend fun deleteTask(task: Task)
    
    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
}

/**
 * Temporary implementation for this demo
 * In production, this would be properly implemented with Room
 */
class MemoryTaskDao : TaskDao {
    private val tasks = mutableListOf<Task>()
    
    override suspend fun getTasksByUserId(userId: String): List<Task> {
        return tasks.filter { it.assignedToUserId == userId }
            .sortedBy { it.scheduledDate }
    }
    
    override suspend fun getAllTasks(): List<Task> = tasks.toList()
    
    override suspend fun getTaskById(taskId: String): Task? {
        return tasks.find { it.id == taskId }
    }
    
    override suspend fun insertTask(task: Task) {
        tasks.removeIf { it.id == task.id }
        tasks.add(task)
    }
    
    override suspend fun insertTasks(tasks: List<Task>) {
        this.tasks.clear()
        this.tasks.addAll(tasks)
    }
    
    override suspend fun updateTask(task: Task) {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            tasks[index] = task
        }
    }
    
    override suspend fun deleteTask(task: Task) {
        tasks.removeIf { it.id == task.id }
    }
    
    override suspend fun clearAllTasks() {
        tasks.clear()
    }
}