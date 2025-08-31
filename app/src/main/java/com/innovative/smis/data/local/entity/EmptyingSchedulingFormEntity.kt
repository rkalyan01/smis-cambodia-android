package com.innovative.smis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.innovative.smis.data.model.response.SanitationCustomerData
@Entity(tableName = "emptying_scheduling_forms")
data class EmptyingSchedulingFormEntity(
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
    val pbcCustomerType: String?,
    val freeServiceUnderPbc: Boolean?,

    // Applicant Details (can be same as customer)
    val applicantName: String?,
    val applicantContact: String?,
    val isApplicantSameAsCustomer: Boolean = false,

    // Emptying History
    val lastEmptiedYear: String?,
    val everEmptied: Boolean?,
    val emptiedNodateReason: String?,
    val notEmptiedBeforeReason: String?,

    // Current Request Details
    val purposeOfEmptying: String?,
    val purposeOfEmptyingOther: String?, // For "Others" specification
    val proposedEmptyingDate: Long?,
    val lastEmptiedDate: Long?,

    // Containment Details
    val sizeOfContainment: String?,
    val yearOfInstallation: String?,
    val containmentAccessibility: String?,
    val locationOfContainment: String?,
    val pumpingPointPresence: Boolean?,
    val containmentIssues: String?,
    val containmentIssuesOther: String?, // For "Others" specification

    // Payment & Visit
    val extraPaymentRequired: Boolean?,
    val extraPaymentAmount: String?,
    val siteVisitRequired: Boolean?,

    // Additional fields for comprehensive form data
    val remarks: String?,
    val estimatedVolume: String?,
    val urgencyLevel: String? = "NORMAL" // URGENT, HIGH, NORMAL, LOW
)

// Removed old extension function for RepositorySanitationCustomerDetails as it's no longer needed

// Extension function for SanitationCustomerData to create EmptyingSchedulingFormEntity
fun SanitationCustomerData.toEntity(applicationId: Int): EmptyingSchedulingFormEntity {
    return EmptyingSchedulingFormEntity(
        id = java.util.UUID.randomUUID().toString(),
        applicationId = applicationId,
        createdBy = null,
        sanitationCustomerName = this.sanitationCustomerName,
        sanitationCustomerContact = this.sanitationCustomerContact,
        sanitationCustomerAddress = null, // Not included in new API response
        pbcCustomerType = this.pbcCustomerType,
        freeServiceUnderPbc = this.freeServiceUnderPbc,
        applicantName = null,
        applicantContact = null,
        lastEmptiedYear = this.lastEmptiedYear?.toString(),
        everEmptied = this.everEmptied,
        emptiedNodateReason = this.emptiedNodateReason,
        notEmptiedBeforeReason = this.notEmptiedBeforeReason,
        purposeOfEmptying = null,
        purposeOfEmptyingOther = null,
        proposedEmptyingDate = null,
        lastEmptiedDate = null,
        sizeOfContainment = this.sizeOfStorageTankM3,
        yearOfInstallation = this.constructionYear?.toString(),
        containmentAccessibility = if (this.accessibility == true) "Yes" else if (this.accessibility == false) "No" else null,
        locationOfContainment = null,
        pumpingPointPresence = null,
        containmentIssues = null,
        containmentIssuesOther = null,
        extraPaymentRequired = null,
        extraPaymentAmount = null,
        siteVisitRequired = null,
        remarks = null,
        estimatedVolume = null,
        syncStatus = "SYNCED"
    )
}

fun EmptyingSchedulingFormEntity.toDomainModel(): SanitationCustomerData {
    return SanitationCustomerData(
        sanitationCustomerName = this.sanitationCustomerName,
        sanitationCustomerContact = this.sanitationCustomerContact,
        pbcCustomerType = this.pbcCustomerType,
        freeServiceUnderPbc = this.freeServiceUnderPbc,
        lastEmptiedYear = this.lastEmptiedYear?.toIntOrNull(),
        everEmptied = this.everEmptied,
        emptiedNodateReason = this.emptiedNodateReason,
        notEmptiedBeforeReason = this.notEmptiedBeforeReason,
        sizeOfStorageTankM3 = this.sizeOfContainment,
        constructionYear = this.yearOfInstallation?.toIntOrNull(),
        accessibility = when(this.containmentAccessibility) {
            "Yes" -> true
            "No" -> false
            else -> null
        }
    )
}

fun EmptyingSchedulingFormEntity.toApiRequest(): com.innovative.smis.data.model.request.EmptyingSchedulingFormRequest {
    return com.innovative.smis.data.model.request.EmptyingSchedulingFormRequest(
        customerName = this.sanitationCustomerName,
        customerPhone = this.sanitationCustomerContact,
        customerAddress = this.sanitationCustomerAddress,
        applicantName = this.applicantName,
        applicantContact = this.applicantContact,
        emptyingPurpose = this.purposeOfEmptying,
        proposedEmptyingDate = this.proposedEmptyingDate?.let {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it))
        },
        lastEmptiedDate = this.lastEmptiedDate?.let {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it))
        },
        emptiedNodateReason = this.emptiedNodateReason,
        notEmptiedBeforeReason = this.notEmptiedBeforeReason,
        additionalRepairing = this.containmentIssues,
        freeServiceUnderPbc = if (this.freeServiceUnderPbc == true) "yes" else "no",
        extraPaymentRequired = if (this.extraPaymentRequired == true) "yes" else "no",
        amountOfExtraPayment = this.extraPaymentAmount,
        siteVisitRequired = if (this.siteVisitRequired == true) "yes" else "no",
        sizeOfStorageTankM3 = this.sizeOfContainment,
        constructionYear = this.yearOfInstallation,
        accessibility = if (this.containmentAccessibility?.isNotEmpty() == true) "yes" else "no",
        everEmptied = if (this.everEmptied == true) "yes" else "no",
        lastEmptiedYear = this.lastEmptiedYear
    )
}
