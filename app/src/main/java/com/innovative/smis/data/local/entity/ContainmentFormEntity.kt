package com.innovative.smis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.innovative.smis.data.model.request.ContainmentRequest
import java.util.*

@Entity(tableName = "containment_forms")
data class ContainmentFormEntity(
    @PrimaryKey
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
        // Convert string values to API-expected types (Boolean, Int)
        // CRITICAL: API expects Boolean for accessibility/ever_emptied, Int for years
        
        val accessibilityBoolean = when (accessibility?.lowercase()) {
            "yes", "true", "1" -> true
            "no", "false", "0" -> false
            else -> null
        }
        
        val everEmptiedBoolean = when (everEmptied?.lowercase()) {
            "yes", "true", "1" -> true
            "no", "false", "0" -> false
            else -> null
        }
        
        val constructionYearInt = constructionYear?.toIntOrNull().also {
            if (it == null && !constructionYear.isNullOrBlank()) {
                println("WARNING: Invalid construction year: '$constructionYear' - will send null to API")
            }
        }
        
        val lastEmptiedYearInt = lastEmptiedYear?.toIntOrNull().also {
            if (it == null && !lastEmptiedYear.isNullOrBlank()) {
                println("WARNING: Invalid last emptied year: '$lastEmptiedYear' - will send null to API")
            }
        }
        
        return ContainmentRequest(
            sanitation_customer_id = sanitationCustomerId,
            type_of_storage_tank = typeOfStorageTank,
            other_type_of_storage_tank = otherTypeOfStorageTank,
            storage_tank_connection = storageTankConnection,
            other_storage_tank_connection = otherStorageTankConnection,
            size_of_storage_tank_m3 = sizeOfStorageTankM3,
            construction_year = constructionYearInt,
            accessibility = accessibilityBoolean,
            ever_emptied = everEmptiedBoolean,
            last_emptied_year = lastEmptiedYearInt
        )
    }
}