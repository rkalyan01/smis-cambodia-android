package com.innovative.smis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.innovative.smis.data.model.request.ContainmentRequest
import java.util.*

@Entity(tableName = "containment_forms")
data class ContainmentFormEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sanitationCustomerId: String,
    val applicationId: Int,
    
    // Containment Details
    val typeOfStorageTank: String?,
    val otherTypeOfStorageTank: String?,
    val storageTankConnection: String?,
    val otherStorageTankConnection: String?,
    val sizeOfStorageTankM3: String?,
    val constructionYear: String?,
    val accessibility: String?,
    val everEmptied: String?,
    val lastEmptiedYear: String?,
    
    // Sync Management
    val syncStatus: String = "PENDING", // PENDING, SYNCED, FAILED
    val syncAttempts: Int = 0,
    val lastSyncAttempt: Long? = null,
    val syncError: String? = null,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toApiRequest(): ContainmentRequest {
        return ContainmentRequest(
            sanitation_customer_id = sanitationCustomerId,
            type_of_storage_tank = typeOfStorageTank,
            other_type_of_storage_tank = otherTypeOfStorageTank,
            storage_tank_connection = storageTankConnection,
            other_storage_tank_connection = otherStorageTankConnection,
            size_of_storage_tank_m3 = sizeOfStorageTankM3,
            construction_year = constructionYear,
            accessibility = accessibility,
            ever_emptied = everEmptied,
            last_emptied_year = lastEmptiedYear
        )
    }
}