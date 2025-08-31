package com.innovative.smis.data.local.dao

import androidx.room.*
import com.innovative.smis.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId AND isDeleted = 0")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE assignedTo = :userId AND isDeleted = 0")
    fun getTasksForUser(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status AND isDeleted = 0")
    fun getTasksByStatus(status: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET syncStatus = :status WHERE id = :taskId")
    suspend fun updateSyncStatus(taskId: String, status: String)

    @Query("UPDATE tasks SET isDeleted = 1, syncStatus = 'PENDING' WHERE id = :taskId")
    suspend fun markTaskAsDeleted(taskId: String)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Transaction
    @Query("SELECT * FROM tasks WHERE isDeleted = 0")
    fun getTasksWithSteps(): Flow<List<TaskWithSteps>>
}

@Dao
interface WorkflowStepDao {
    @Query("SELECT * FROM workflow_steps WHERE taskId = :taskId AND isDeleted = 0 ORDER BY stepOrder ASC")
    fun getStepsForTask(taskId: String): Flow<List<WorkflowStepEntity>>
    
    @Query("SELECT * FROM workflow_steps WHERE taskId = :taskId AND isDeleted = 0 ORDER BY stepOrder ASC")
    suspend fun getWorkflowStepsForTask(taskId: String): List<WorkflowStepEntity>

    @Query("SELECT * FROM workflow_steps WHERE id = :stepId AND isDeleted = 0")
    suspend fun getStepById(stepId: String): WorkflowStepEntity?

    @Query("SELECT * FROM workflow_steps WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsyncedSteps(): List<WorkflowStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: WorkflowStepEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<WorkflowStepEntity>)

    @Update
    suspend fun updateStep(step: WorkflowStepEntity)

    @Query("UPDATE workflow_steps SET syncStatus = :status WHERE id = :stepId")
    suspend fun updateSyncStatus(stepId: String, status: String)

    @Query("UPDATE workflow_steps SET isDeleted = 1, syncStatus = 'PENDING' WHERE id = :stepId")
    suspend fun markStepAsDeleted(stepId: String)
}

@Dao
interface BuildingSurveyDao {
    @Query("SELECT * FROM building_surveys WHERE isDeleted = 0 ORDER BY surveyDate DESC")
    fun getAllSurveys(): Flow<List<BuildingSurveyEntity>>

    @Query("SELECT * FROM building_surveys WHERE bin = :bin AND isDeleted = 0")
    suspend fun getSurveyByBin(bin: String): BuildingSurveyEntity?

    @Query("SELECT * FROM building_surveys WHERE sangkat = :sangkat AND isDeleted = 0")
    fun getSurveysBySangkat(sangkat: String): Flow<List<BuildingSurveyEntity>>

    @Query("SELECT * FROM building_surveys WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsyncedSurveys(): List<BuildingSurveyEntity>

    @Query("SELECT * FROM building_surveys WHERE surveyedBy = :userId AND isDeleted = 0")
    fun getSurveysByUser(userId: String): Flow<List<BuildingSurveyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurvey(survey: BuildingSurveyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurveys(surveys: List<BuildingSurveyEntity>)

    @Update
    suspend fun updateSurvey(survey: BuildingSurveyEntity)

    @Query("UPDATE building_surveys SET syncStatus = :status WHERE id = :surveyId")
    suspend fun updateSyncStatus(surveyId: String, status: String)

    @Query("UPDATE building_surveys SET isDeleted = 1, syncStatus = 'PENDING' WHERE id = :surveyId")
    suspend fun markSurveyAsDeleted(surveyId: String)

    @Transaction
    @Query("SELECT * FROM building_surveys WHERE isDeleted = 0")
    fun getSurveysWithLocation(): Flow<List<SurveyWithLocation>>
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isActive = 1")
    fun getAllActiveUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)
}

@Dao
interface SurveyDropdownDao {
    @Query("SELECT * FROM survey_dropdowns WHERE category = :category AND isActive = 1")
    suspend fun getDropdownsByCategory(category: String): List<SurveyDropdownEntity>

    @Query("SELECT * FROM survey_dropdowns WHERE type = :type AND isActive = 1")
    suspend fun getDropdownsByType(type: String): List<SurveyDropdownEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDropdown(dropdown: SurveyDropdownEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDropdowns(dropdowns: List<SurveyDropdownEntity>)

    @Query("DELETE FROM survey_dropdowns WHERE category = :category")
    suspend fun deleteDropdownsByCategory(category: String)

    @Query("UPDATE survey_dropdowns SET syncedAt = :syncTime WHERE category = :category")
    suspend fun updateSyncTime(category: String, syncTime: String)
}

@Dao
interface WfsBuildingDao {
    @Query("SELECT * FROM wfs_buildings")
    fun getAllBuildings(): Flow<List<WfsBuildingEntity>>

    @Query("SELECT * FROM wfs_buildings WHERE bin = :bin")
    suspend fun getBuildingByBin(bin: String): WfsBuildingEntity?

    @Query("SELECT * FROM wfs_buildings WHERE sangkat = :sangkat")
    suspend fun getBuildingsBySangkat(sangkat: String): List<WfsBuildingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuilding(building: WfsBuildingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildings(buildings: List<WfsBuildingEntity>)

    @Query("DELETE FROM wfs_buildings")
    suspend fun clearAllBuildings()

    @Query("UPDATE wfs_buildings SET syncedAt = :syncTime")
    suspend fun updateAllSyncTime(syncTime: String)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAllPendingSyncs(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType ORDER BY createdAt ASC")
    suspend fun getSyncsByEntityType(entityType: String): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE retryCount < 3 ORDER BY createdAt ASC")
    suspend fun getRetryableSyncs(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sync: SyncQueueEntity)

    @Update
    suspend fun updateSync(sync: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :syncId")
    suspend fun deleteSync(syncId: String)

    @Query("DELETE FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteSyncsByEntity(entityType: String, entityId: String)

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1, lastAttempt = :lastAttempt, errorMessage = :errorMessage WHERE id = :syncId")
    suspend fun updateRetryInfo(syncId: String, lastAttempt: Long, errorMessage: String?)
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE retryCount < 3")
    suspend fun getPendingCount(): Int
}


@Dao
interface TodoItemDao {

    @Upsert
    suspend fun upsertAll(applications: List<TodoItemEntity>)

    @Query("SELECT * FROM applications ORDER BY proposedEmptyingDate DESC")
    fun getAllApplications(): Flow<List<TodoItemEntity>>

    @Query("SELECT * FROM applications WHERE status = :status ORDER BY proposedEmptyingDate DESC")
    fun getApplicationsByStatus(status: String): Flow<List<TodoItemEntity>>

    @Query("SELECT * FROM applications WHERE cacheExpiry > :currentTime ORDER BY proposedEmptyingDate DESC")
    fun getValidCachedApplications(currentTime: Long = System.currentTimeMillis()): Flow<List<TodoItemEntity>>

    @Query("SELECT * FROM applications WHERE status = :status AND cacheExpiry > :currentTime ORDER BY proposedEmptyingDate DESC")
    fun getValidCachedApplicationsByStatus(status: String, currentTime: Long = System.currentTimeMillis()): Flow<List<TodoItemEntity>>

    @Query("DELETE FROM applications WHERE cacheExpiry <= :currentTime")
    suspend fun clearExpiredCache(currentTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM applications")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM applications WHERE status = :status")
    suspend fun getApplicationCountByStatus(status: String): Int

    @Query("SELECT DISTINCT status FROM applications WHERE status IS NOT NULL")
    suspend fun getAllCachedStatuses(): List<String>
    
    @Query("UPDATE applications SET status = :newStatus WHERE applicationId = :applicationId")
    suspend fun updateApplicationStatus(applicationId: Int, newStatus: String)
}