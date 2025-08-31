package com.innovative.smis.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.innovative.smis.data.local.entity.EmptyingServiceFormEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmptyingServiceFormDao {

    @Upsert
    suspend fun upsert(form: EmptyingServiceFormEntity)

    @Query("SELECT * FROM emptying_service_forms WHERE applicationId = :applicationId LIMIT 1")
    suspend fun getFormByApplicationId(applicationId: Int): EmptyingServiceFormEntity?

    @Query("SELECT * FROM emptying_service_forms WHERE id = :formId LIMIT 1")
    suspend fun getFormById(formId: String): EmptyingServiceFormEntity?

    @Query("SELECT * FROM emptying_service_forms WHERE applicationId = :applicationId")
    fun getFormByApplicationIdFlow(applicationId: Int): Flow<EmptyingServiceFormEntity?>

    @Query("SELECT COUNT(*) FROM emptying_service_forms WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedCount(): Int

    @Query("SELECT * FROM emptying_service_forms WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedForms(): List<EmptyingServiceFormEntity>

    @Query("UPDATE emptying_service_forms SET syncStatus = 'SYNCED', updatedAt = :timestamp WHERE id = :formId")
    suspend fun markAsSynced(formId: String, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE emptying_service_forms 
        SET syncStatus = :status, 
            updatedAt = :timestamp
        WHERE id = :formId
    """)
    suspend fun updateSyncStatus(
        formId: String, 
        status: String, 
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM emptying_service_forms WHERE applicationId = :applicationId")
    suspend fun deleteByApplicationId(applicationId: Int)

    @Query("DELETE FROM emptying_service_forms")
    suspend fun clearAll()
}