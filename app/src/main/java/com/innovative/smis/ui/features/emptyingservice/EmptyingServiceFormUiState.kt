package com.innovative.smis.ui.features.emptyingservice

import com.innovative.smis.util.common.Resource
import com.innovative.smis.data.local.entity.EmptyingServiceFormEntity
import com.innovative.smis.data.model.PurposeOptionData

data class EmptyingServiceFormUiState(
    // Application Information
    val applicationId: Int = 0,
    val sanitationCustomerId: String? = null,
    
    // Loading State
    val loadingState: Resource<EmptyingServiceFormEntity> = Resource.Idle(),
    val readonlyDataLoadingState: Resource<com.innovative.smis.data.model.response.EmptyingReadonlyDataResponse> = Resource.Idle(),
    val additionalRepairingOptionsLoadingState: Resource<com.innovative.smis.data.model.response.SimpleDropdownResponse> = Resource.Idle(),
    
    // Service Details
    val emptiedDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val additionalTripRequired: String = "no", // "yes" or "no"
    val emptiedDateError: String? = null,
    val startTimeError: String? = null,
    val endTimeError: String? = null,
    val additionalTripRequiredError: String? = null,
    
    // Personnel Information
    val applicantName: String = "",
    val applicantContact: String = "",
    val serviceReceiverName: String = "",
    val serviceReceiverContact: String = "",
    val isServiceReceiverSameAsApplicant: Boolean = false,
    val applicantNameError: String? = null,
    val applicantContactError: String? = null,
    val serviceReceiverNameError: String? = null,
    val serviceReceiverContactError: String? = null,
    
    // Vehicle and Sludge Information
    val desludgingVehicleId: String = "", // Stores the actual vehicle ID for API submission
    val selectedVehicleLicensePlate: String = "", // Stores the displayed license plate
    val sludgeType: String = "", // "Mixed" or "Not Mixed"
    val typeOfSludge: String = "", // Only when sludgeType is "Mixed": "Processing food", "Oil and fat (restaurant)", "Content of fuel"
    val pumpingPointPresence: String = "", // "Yes" or "No"
    val pumpingPointType: String = "", // Mandatory: "Cover", "Tube", "Pierce"
    val desludgingVehicleIdError: String? = null,
    val sludgeTypeError: String? = null,
    val pumpingPointTypeError: String? = null,
    
    // Service Information
    val freeUnderPBC: Boolean = false,
    val customerType: String = "", // Selected customer type ID/key
    val otherCustomerType: String = "", // Text input when "Other" is selected
    val customerTypeOptions: Map<String, String> = emptyMap(), // Customer type dropdown options
    val additionalRepairingKeys: List<String> = emptyList(),
    val pendingAdditionalRepairingKeys: List<String> = emptyList(), // Stores unvalidated keys from API until options load
    val otherAdditionalRepairing: String = "",
    val additionalRepairingOptions: Map<String, String> = emptyMap(),
    val additionalRepairingError: String? = null,
    val regularCost: String = "",
    val extraCost: String = "0",
    val regularCostError: String? = null,
    val extraCostError: String? = null,
    
    // Documentation
    val receiptNumber: String = "",
    val receiptImage: String = "",
    val pictureOfEmptying: String = "",
    val comments: String = "",
    val receiptNumberError: String? = null,
    val receiptImageError: String? = null,
    
    // Location Information
    val longitude: Double? = null,
    val latitude: Double? = null,
    val locationError: String? = null,
    val isLocationLoading: Boolean = false,
    val buildingPointGeomExist: Boolean = false,
    
    // UI State
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val firstErrorField: String? = null, // Tracks which field has the first validation error for auto-scroll
    
    // Readonly Fields (from API)
    val isApplicantNameReadonly: Boolean = false,
    val isApplicantContactReadonly: Boolean = false,
    val isFreeUnderPBCReadonly: Boolean = false,
    val isAdditionalRepairingReadonly: Boolean = false,
    val isRegularCostReadonly: Boolean = false,
    val isExtraCostReadonly: Boolean = false,
    val isSaving: Boolean = false,
    val syncStatus: String = "DRAFT", // DRAFT, PENDING, SYNCED, FAILED
    
    // Image Upload State
    val isUploadingReceiptImage: Boolean = false,
    val isUploadingEmptyingImage: Boolean = false,
    val receiptImageUploadError: String? = null,
    val emptyingImageUploadError: String? = null,
    
    // Dropdown Data
    val vehicleOptions: List<PurposeOptionData> = emptyList()
)

fun EmptyingServiceFormUiState.hasValidationErrors(): Boolean {
    return emptiedDateError != null ||
            startTimeError != null ||
            endTimeError != null ||
            additionalTripRequiredError != null ||
            applicantNameError != null ||
            applicantContactError != null ||
            serviceReceiverNameError != null ||
            serviceReceiverContactError != null ||
            desludgingVehicleIdError != null ||
            sludgeTypeError != null ||
            additionalRepairingError != null ||
            pumpingPointTypeError != null ||
            regularCostError != null ||
            extraCostError != null ||
            receiptNumberError != null ||
            receiptImageError != null ||
            locationError != null ||
            receiptImageUploadError != null ||
            emptyingImageUploadError != null
}

fun EmptyingServiceFormUiState.isFormValid(): Boolean {
    // Location is only required if building point geometry doesn't exist
    val locationValid = if (buildingPointGeomExist) {
        true // Location not required when building geometry exists
    } else {
        longitude != null && latitude != null
    }
    
    val baseValid = emptiedDate.isNotEmpty() &&
            startTime.isNotEmpty() &&
            endTime.isNotEmpty() &&
            additionalTripRequired.isNotEmpty() &&
            applicantName.isNotEmpty() &&
            applicantContact.isNotEmpty() &&
            (!isServiceReceiverSameAsApplicant || (serviceReceiverName.isNotEmpty() && serviceReceiverContact.isNotEmpty())) &&
            desludgingVehicleId.isNotEmpty() &&
            sludgeType.isNotEmpty() &&
            pumpingPointType.isNotEmpty() &&
            locationValid &&
            !hasValidationErrors()
    
    // If additional trip is NOT required (no), both receipt number and receipt image are mandatory
    val receiptValid = if (additionalTripRequired == "no") {
        receiptNumber.isNotEmpty() && receiptImage.isNotEmpty()
    } else {
        true // Receipt not required when additional trip is yes
    }
    
    return baseValid && receiptValid
}

fun EmptyingServiceFormUiState.isLocationCaptured(): Boolean {
    return longitude != null && latitude != null
}

fun EmptyingServiceFormUiState.hasImagesUploaded(): Boolean {
    return receiptImage.isNotEmpty() && pictureOfEmptying.isNotEmpty()
}