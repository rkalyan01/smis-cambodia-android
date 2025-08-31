package com.innovative.smis.data.local.dao

import androidx.room.*
import com.innovative.smis.data.local.entity.ContainmentFormEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContainmentFormDao {
    
    @Upsert
    suspend fun upsert(form: ContainmentFormEntity)
    
    @Query("SELECT * FROM containment_forms WHERE sanitationCustomerId = :sanitationCustomerId LIMIT 1")
    suspend fun getFormBySanitationCustomerId(sanitationCustomerId: String): ContainmentFormEntity?
    
    @Query("SELECT * FROM containment_forms WHERE applicationId = :applicationId LIMIT 1")
    suspend fun getFormByApplicationId(applicationId: Int): ContainmentFormEntity?
    
    @Query("SELECT * FROM containment_forms WHERE id = :formId LIMIT 1")
    suspend fun getFormById(formId: String): ContainmentFormEntity?
    
    @Query("SELECT * FROM containment_forms WHERE sanitationCustomerId = :sanitationCustomerId")
    fun getFormBySanitationCustomerIdFlow(sanitationCustomerId: String): Flow<ContainmentFormEntity?>
    
    @Query("SELECT * FROM containment_forms WHERE applicationId = :applicationId")
    fun getFormByApplicationIdFlow(applicationId: Int): Flow<ContainmentFormEntity?>
    
    @Query("SELECT * FROM containment_forms WHERE syncStatus = :syncStatus")
    suspend fun getFormsBySyncStatus(syncStatus: String): List<ContainmentFormEntity>
    
    @Query("SELECT * FROM containment_forms WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getPendingSyncForms(): List<ContainmentFormEntity>
    
    @Query("UPDATE containment_forms SET syncStatus = :status, syncAttempts = :attempts, lastSyncAttempt = :lastAttempt, syncError = :error, updatedAt = :updatedAt WHERE id = :formId")
    suspend fun updateSyncStatus(
        formId: String,
        status: String,
        attempts: Int,
        lastAttempt: Long,
        error: String?,
        updatedAt: Long
    )
    
    @Query("DELETE FROM containment_forms WHERE id = :formId")
    suspend fun deleteById(formId: String)
    
    @Query("SELECT COUNT(*) FROM containment_forms WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncCount(): Int
}