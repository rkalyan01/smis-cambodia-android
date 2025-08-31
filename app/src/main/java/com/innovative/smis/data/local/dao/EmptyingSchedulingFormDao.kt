package com.innovative.smis.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.innovative.smis.data.local.entity.EmptyingSchedulingFormEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmptyingSchedulingFormDao {

    /**
     * Inserts or updates a form's details in the database.
     * Updates the updatedAt timestamp automatically.
     */
    @Upsert
    suspend fun upsert(entity: EmptyingSchedulingFormEntity)

    /**
     * Inserts multiple forms (useful for bulk operations)
     */
    @Upsert
    suspend fun upsertAll(entities: List<EmptyingSchedulingFormEntity>)

    /**
     * Retrieves the details for a specific form by its application ID.
     * Returns the most recent form if multiple exist.
     */
    @Query("SELECT * FROM emptying_scheduling_forms WHERE applicationId = :applicationId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getFormByApplicationId(applicationId: Int): EmptyingSchedulingFormEntity?

    /**
     * Retrieves a form by its unique ID
     */
    @Query("SELECT * FROM emptying_scheduling_forms WHERE id = :formId")
    suspend fun getFormById(formId: String): EmptyingSchedulingFormEntity?

    /**
     * Get all forms for an application (useful for versioning/history)
     */
    @Query("SELECT * FROM emptying_scheduling_forms WHERE applicationId = :applicationId ORDER BY updatedAt DESC")
    fun getFormsForApplication(applicationId: Int): Flow<List<EmptyingSchedulingFormEntity>>

    /**
     * Get all forms that need to be synced to the server
     */
    @Query("SELECT * FROM emptying_scheduling_forms WHERE syncStatus IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getUnsyncedForms(): List<EmptyingSchedulingFormEntity>

    /**
     * Get forms by sync status
     */
    @Query("SELECT * FROM emptying_scheduling_forms WHERE syncStatus = :status ORDER BY updatedAt DESC")
    suspend fun getFormsByStatus(status: String): List<EmptyingSchedulingFormEntity>

    /**
     * Update sync status and related fields
     */
    @Query("""
        UPDATE emptying_scheduling_forms 
        SET syncStatus = :status, 
            syncAttempts = :attempts, 
            lastSyncAttempt = :lastAttempt,
            errorMessage = :error,
            updatedAt = :updatedAt
        WHERE id = :formId
    """)
    suspend fun updateSyncStatus(
        formId: String, 
        status: String, 
        attempts: Int, 
        lastAttempt: Long, 
        error: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * Mark form as successfully synced
     */
    @Query("UPDATE emptying_scheduling_forms SET syncStatus = 'SYNCED', errorMessage = NULL, updatedAt = :updatedAt WHERE id = :formId")
    suspend fun markAsSynced(formId: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * Mark form as pending sync (when user submits)
     */
    @Query("UPDATE emptying_scheduling_forms SET syncStatus = 'PENDING', updatedAt = :updatedAt WHERE id = :formId")
    suspend fun markAsPending(formId: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * Get count of unsynced forms
     */
    @Query("SELECT COUNT(*) FROM emptying_scheduling_forms WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedCount(): Int

    /**
     * Delete a specific form
     */
    @Query("DELETE FROM emptying_scheduling_forms WHERE id = :formId")
    suspend fun deleteForm(formId: String)

    /**
     * Delete all forms for an application
     */
    @Query("DELETE FROM emptying_scheduling_forms WHERE applicationId = :applicationId")
    suspend fun deleteFormsForApplication(applicationId: Int)

    /**
     * Clean up old synced forms (keep only latest per application)
     */
    @Query("""
        DELETE FROM emptying_scheduling_forms 
        WHERE id NOT IN (
            SELECT id FROM emptying_scheduling_forms 
            WHERE syncStatus = 'SYNCED' 
            GROUP BY applicationId 
            HAVING updatedAt = MAX(updatedAt)
        ) 
        AND syncStatus = 'SYNCED'
        AND updatedAt < :cutoffTime
    """)
    suspend fun cleanupOldSyncedForms(cutoffTime: Long)

    /**
     * Get all draft forms (user is editing)
     */
    @Query("SELECT * FROM emptying_scheduling_forms WHERE syncStatus = 'DRAFT' ORDER BY updatedAt DESC")
    suspend fun getDraftForms(): List<EmptyingSchedulingFormEntity>

    /**
     * Clear all forms from the table
     */
    @Query("DELETE FROM emptying_scheduling_forms")
    suspend fun clearAll()
}
