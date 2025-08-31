package com.innovative.smis.data.model

import com.innovative.smis.util.common.Resource
import com.innovative.smis.data.local.entity.SitePreparationFormEntity

data class SitePreparationFormState(
    // Application Information
    val applicationId: Int = 0,
    val applicationDate: String = "",
    
    // Loading State
    val loadingState: Resource<SitePreparationFormEntity> = Resource.Idle(),
    
    // Applicant Information
    val applicantName: String = "",
    val applicantContact: String = "",
    val applicantNameError: String? = null,
    val applicantContactError: String? = null,
    
    // Customer Information
    val customerName: String = "",
    val customerContact: String = "",
    val isCustomerSameAsApplicant: Boolean = false,
    val customerNameError: String? = null,
    val customerContactError: String? = null,
    
    // Emptying Request Details
    val purposeOfEmptyingRequest: String = "",
    val otherEmptyingPurpose: String = "",
    val purposeOfEmptyingRequestError: String? = null,
    
    // Previous Emptying History
    val everEmptied: Boolean? = null,
    val lastEmptiedDate: String = "",
    val notEmptiedBeforeReason: String = "",
    val reasonForNoEmptiedDate: String = "",
    val lastEmptiedDateError: String? = null,
    
    // Service Information
    val freeServiceUnderPBC: Boolean = false,
    val additionalRepairing: String = "",
    val otherAdditionalRepairing: String = "",
    val extraPaymentRequired: Boolean = false,
    val amountOfExtraPayment: String = "",
    val amountOfExtraPaymentError: String? = null,
    
    // Scheduling Information
    val proposeEmptyingDate: String = "",
    val needReschedule: Boolean = false,
    val newProposeEmptyingDate: String = "",
    val proposedEmptyingDate: String = "",
    val proposeEmptyingDateError: String? = null,
    val newProposeEmptyingDateError: String? = null,
    
    // UI State
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val isSaving: Boolean = false,
    val syncStatus: String = "DRAFT", // DRAFT, PENDING, SYNCED, FAILED
    
    // Dropdown Data
    val purposeOptions: List<PurposeOptionData> = emptyList(),
    val additionalRepairingOptions: List<PurposeOptionData> = emptyList(),
    val reasonOptions: List<PurposeOptionData> = emptyList()
)


fun SitePreparationFormState.hasValidationErrors(): Boolean {
    return applicantNameError != null ||
            applicantContactError != null ||
            customerNameError != null ||
            customerContactError != null ||
            purposeOfEmptyingRequestError != null ||
            lastEmptiedDateError != null ||
            amountOfExtraPaymentError != null ||
            proposeEmptyingDateError != null ||
            newProposeEmptyingDateError != null
}

fun SitePreparationFormState.isFormValid(): Boolean {
    return applicantName.isNotBlank() &&
            applicantContact.isNotBlank() &&
            (!isCustomerSameAsApplicant || (customerName.isNotBlank() && customerContact.isNotBlank())) &&
            purposeOfEmptyingRequest.isNotBlank() &&
            proposeEmptyingDate.isNotBlank() &&
            !hasValidationErrors()
}