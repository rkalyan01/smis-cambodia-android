package com.innovative.smis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "additional_repairing_forms")
data class AdditionalRepairingFormEntity(
    @PrimaryKey
    val emptyingId: Int,
    
    // Trip entries stored as JSON string
    val tripEntriesJson: String = "[]",
    
    // Payment Details
    val amountOfRegularPayment: String = "",
    val amountOfExtraPayment: String = "",
    val receiptNumber: String = "",
    val receiptImage: String = "",
    val comments: String = "",
    
    // Metadata
    val createdBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "DRAFT" // DRAFT, PENDING, FAILED, SYNCED
)
