package com.innovative.smis.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.innovative.smis.data.local.entity.AdditionalRepairingFormEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdditionalRepairingFormDao {

    @Upsert
    suspend fun upsert(form: AdditionalRepairingFormEntity)

    @Query("SELECT * FROM additional_repairing_forms WHERE emptyingId = :emptyingId LIMIT 1")
    suspend fun getFormByEmptyingId(emptyingId: Int): AdditionalRepairingFormEntity?

    @Query("SELECT * FROM additional_repairing_forms WHERE emptyingId = :emptyingId")
    fun getFormByEmptyingIdFlow(emptyingId: Int): Flow<AdditionalRepairingFormEntity?>

    @Query("SELECT COUNT(*) FROM additional_repairing_forms WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedCount(): Int

    @Query("SELECT * FROM additional_repairing_forms WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedForms(): List<AdditionalRepairingFormEntity>

    @Query("UPDATE additional_repairing_forms SET syncStatus = 'SYNCED', updatedAt = :timestamp WHERE emptyingId = :emptyingId")
    suspend fun markAsSynced(emptyingId: Int, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE additional_repairing_forms 
        SET syncStatus = :status, 
            updatedAt = :timestamp
        WHERE emptyingId = :emptyingId
    """)
    suspend fun updateSyncStatus(
        emptyingId: Int, 
        status: String, 
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM additional_repairing_forms WHERE emptyingId = :emptyingId")
    suspend fun deleteByEmptyingId(emptyingId: Int)

    @Query("DELETE FROM additional_repairing_forms")
    suspend fun clearAll()
}
