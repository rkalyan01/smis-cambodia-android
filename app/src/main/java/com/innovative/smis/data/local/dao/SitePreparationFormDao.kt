package com.innovative.smis.data.local.dao

import androidx.room.*
import com.innovative.smis.data.local.entity.SitePreparationFormEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SitePreparationFormDao {

    @Upsert
    suspend fun upsert(form: SitePreparationFormEntity)

    @Query("SELECT * FROM site_preparation_forms WHERE applicationId = :applicationId LIMIT 1")
    suspend fun getFormByApplicationId(applicationId: Int): SitePreparationFormEntity?

    @Query("SELECT * FROM site_preparation_forms WHERE id = :formId LIMIT 1")
    suspend fun getFormById(formId: String): SitePreparationFormEntity?

    @Query("SELECT * FROM site_preparation_forms WHERE applicationId = :applicationId")
    fun getFormByApplicationIdFlow(applicationId: Int): Flow<SitePreparationFormEntity?>

    @Query("SELECT COUNT(*) FROM site_preparation_forms WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedCount(): Int

    @Query("SELECT * FROM site_preparation_forms WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedForms(): List<SitePreparationFormEntity>

    @Query("UPDATE site_preparation_forms SET syncStatus = 'SYNCED', updatedAt = :timestamp WHERE id = :formId")
    suspend fun markAsSynced(formId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE site_preparation_forms SET syncStatus = 'PENDING', updatedAt = :timestamp WHERE id = :formId")
    suspend fun markAsPending(formId: String, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE site_preparation_forms 
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

    @Query("DELETE FROM site_preparation_forms WHERE applicationId = :applicationId")
    suspend fun deleteByApplicationId(applicationId: Int)

    @Query("DELETE FROM site_preparation_forms")
    suspend fun clearAll()
    
    @Query("SELECT * FROM site_preparation_forms WHERE syncStatus = :status")
    suspend fun getFormsByStatus(status: String): List<SitePreparationFormEntity>
}