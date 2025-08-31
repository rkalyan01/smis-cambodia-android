package com.innovative.smis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.innovative.smis.data.model.request.SitePreparationFormRequest
import com.innovative.smis.data.model.response.SanitationCustomerData
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "site_preparation_forms")
data class SitePreparationFormEntity(
    @PrimaryKey
    val id: String,
    val applicationId: Int,
    // Audit fields
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String?,
    val syncStatus: String = "DRAFT",
    val syncAttempts: Int = 0,
    val lastSyncAttempt: Long? = null,
    val errorMessage: String? = null,

    // Customer Details
    val sanitationCustomerName: String?,
    val sanitationCustomerContact: String?,
    val sanitationCustomerAddress: String?,

    // Applicant Details
    val applicantName: String?,
    val applicantContact: String?,

    // Customer Details (can be same as applicant)
    val customerName: String?,
    val customerContact: String?,

    // Purpose and History
    val purposeOfEmptying: String?,
    val otherEmptyingPurpose: String?,
    val everEmptied: Boolean?,
    val lastEmptiedDate: Long?,
    val lastEmptiedYear: String?,
    val notEmptiedBeforeReason: String?,
    val reasonForNoEmptiedDate: String?,

    // Service Details
    val freeServiceUnderPbc: Boolean?,
    val additionalRepairing: String?,
    val otherAdditionalRepairing: String?,
    val extraPaymentRequired: Boolean?,
    val amountOfExtraPayment: String?,

    // Scheduling
    val proposedEmptyingDate: String?,
    val needReschedule: Boolean?,
    val newProposedEmptyingDate: Long?
)

// Extension function for SanitationCustomerData to create SitePreparationFormEntity
fun SanitationCustomerData.toSitePreparationEntity(applicationId: Int): SitePreparationFormEntity {
    return SitePreparationFormEntity(
        id = java.util.UUID.randomUUID().toString(),
        applicationId = applicationId,
        createdBy = null,
        sanitationCustomerName = this.sanitationCustomerName,
        sanitationCustomerContact = this.sanitationCustomerContact,
        sanitationCustomerAddress = null, // Not available in SanitationCustomerData
        applicantName = null,
        applicantContact = null,
        customerName = this.sanitationCustomerName,
        customerContact = this.sanitationCustomerContact,
        purposeOfEmptying = null,
        otherEmptyingPurpose = null,
        everEmptied = this.everEmptied,
        lastEmptiedDate = null, // Will need to be set separately
        lastEmptiedYear = null,
        notEmptiedBeforeReason = this.notEmptiedBeforeReason,
        reasonForNoEmptiedDate = this.emptiedNodateReason,
        freeServiceUnderPbc = this.freeServiceUnderPbc,
        additionalRepairing = null,
        otherAdditionalRepairing = null,
        extraPaymentRequired = null,
        amountOfExtraPayment = null,
        proposedEmptyingDate = null,
        needReschedule = null,
        newProposedEmptyingDate = null,
        syncStatus = "SYNCED"
    )
}

fun SitePreparationFormEntity.toApiRequest(applicationId: Int): SitePreparationFormRequest {
    val pgDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // PostgreSQL format

    // Get the emptying purpose - if it's a text value, map it to the corresponding API code
    val emptyingPurposeValue = when {
        this.purposeOfEmptying?.contains("Bought house", ignoreCase = true) == true -> "1"
        this.purposeOfEmptying?.contains("Renter", ignoreCase = true) == true -> "2"
        this.purposeOfEmptying?.contains("Others", ignoreCase = true) == true -> "3"
        this.purposeOfEmptying?.isNotBlank() == true -> "3" // Default to "Others" if populated
        else -> "1" // Default to "Bought house" if empty
    }

    // Handle last emptied date properly - if everEmptied is false, send empty string
    val lastEmptiedDateValue = when {
        this.everEmptied == false -> ""
        this.lastEmptiedDate != null && this.lastEmptiedDate != 0L -> {
            // If we have a valid timestamp, format it
            try {
                pgDateFormatter.format(Date(this.lastEmptiedDate))
            } catch (e: Exception) {
                // If timestamp is invalid, try to use the year string if available
                if (!this.lastEmptiedYear.isNullOrBlank()) {
                    "${this.lastEmptiedYear}-01-01"
                } else {
                    ""
                }
            }
        }
        !this.lastEmptiedYear.isNullOrBlank() -> "${this.lastEmptiedYear}-01-01"
        else -> ""
    }

    return SitePreparationFormRequest(
        applicationId = applicationId,
        sitePrepDate = "", // Site prep date is usually current date, can be set by form
        customerName = this.customerName ?: this.sanitationCustomerName ?: "",
        customerContact = this.customerContact ?: this.sanitationCustomerContact ?: "",
        additionalRepairing = this.additionalRepairing ?: this.otherAdditionalRepairing ?: "",
        extraPaymentRequired = if (this.extraPaymentRequired == true) "yes" else if (this.extraPaymentRequired == false) "no" else "yes",
        amountOfExtraPayment = this.amountOfExtraPayment ?: "",
        applicantName = this.applicantName ?: this.sanitationCustomerName ?: "",
        applicantContact = this.applicantContact ?: this.sanitationCustomerContact ?: "",
        emptyingPurpose = this.purposeOfEmptying ?: this.otherEmptyingPurpose ?: "",
        proposedEmptyingDate = this.proposedEmptyingDate ?: "",
        lastEmptiedDate = lastEmptiedDateValue,
        notEmptiedBeforeReason = this.notEmptiedBeforeReason ?: "",
        emptiedNodateReason = this.reasonForNoEmptiedDate ?: "",
        freeServiceUnderPbc = if (this.freeServiceUnderPbc == true) "yes" else if (this.freeServiceUnderPbc == false) "no" else "yes",
        typeOfStorageTank = "", // Will be filled from containment data if available
        storageTankConnection = "", // Will be filled from containment data if available
        sizeOfStorageTankM3 = "", // Will be filled from containment data if available
        constructionYear = "", // Will be filled from containment data if available
        accessibility = "", // Will be filled from containment data if available
        everEmptied = if (this.everEmptied == true) "yes" else if (this.everEmptied == false) "no" else "yes"
    )
}