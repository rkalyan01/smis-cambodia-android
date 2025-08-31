package com.innovative.smis.data.model

import com.innovative.smis.util.common.Resource


data class EmptyingFormState(
    // Application Information
    val applicationId: Int = 0,
    val applicationDate: String = "",
    
    // Loading State
    val loadingState: Resource<String> = Resource.Idle(),
    val sanitationCustomerName: String = "",
    val sanitationCustomerContact: String = "",
    val sanitationCustomerAddress: String = "",
    
    // Applicant Information
    val isSamePersonAsCustomer: Boolean = false,
    val applicantName: String = "",
    val applicantContact: String = "",
    val applicantNameError: String? = null,
    val applicantContactError: String? = null,
    
    // Purpose and Request Details
    val purposeOfEmptyingRequest: String = "",
    val otherEmptyingPurpose: String = "",
    val proposeEmptyingDate: String = "",
    val proposeEmptyingDateError: String? = null,
    
    // Previous Emptying History
    val everEmptied: Boolean? = null,
    val lastEmptiedDate: String = "",
    val notEmptiedBeforeReason: String = "",
    val reasonForNoEmptiedDate: String = "",
    
    // Service Information
    val freeServiceUnderPbc: Boolean? = null,
    val sizeOfContainmentM3: String = "",
    val yearOfInstallation: String = "",
    
    // Containment Details
    val locationOfContainment: LocationOfContainment? = null,
    val pumpingPointPresence: PumpingPointPresence? = null,
    val presenceOfPumpingPoint: PumpingPointPresence? = null,
    val containmentAccessibility: Boolean? = null,
    val estimatedVolume: String = "",
    val estimatedVolumeError: String? = null,
    val remarks: String = "",
    
    // Experience and Payment
    val experienceIssuesWithContainment: String = "",
    val extraPaymentRequired: Boolean? = null,
    val amountOfExtraPayment: String = "",
    val siteVisitRequired: Boolean? = null,
    
    // UI State
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val isSaving: Boolean = false,
    val syncStatus: String = "DRAFT", // DRAFT, PENDING, SYNCED, FAILED
    
    // Dropdown Data
    val purposeOptions: List<PurposeOptionData> = emptyList(),
    val experienceIssuesOptions: List<PurposeOptionData> = emptyList()
)


fun EmptyingFormState.hasValidationErrors(): Boolean {
    return applicantNameError != null ||
            applicantContactError != null ||
            proposeEmptyingDateError != null ||
            estimatedVolumeError != null
}

fun EmptyingFormState.isFormValid(): Boolean {
    return (!isSamePersonAsCustomer || (applicantName.isNotBlank() && applicantContact.isNotBlank())) &&
            purposeOfEmptyingRequest.isNotBlank() &&
            proposeEmptyingDate.isNotBlank() &&
            !hasValidationErrors()
}

enum class LocationOfContainment(val displayName: String) {
    INSIDE_COMPOUND("Inside Compound"),
    OUTSIDE_COMPOUND("Outside Compound"),
    ROADSIDE("Roadside"),
    OTHER("Other")
}


enum class PumpingPointPresence(val displayName: String) {
    YES("Yes"),
    NO("No"),
    NEEDS_INSTALLATION("Needs Installation")
}

