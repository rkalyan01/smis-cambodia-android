package com.innovative.smis.data.model


import com.innovative.smis.util.common.Resource
import com.innovative.smis.data.local.entity.EmptyingServiceFormEntity

data class EmptyingServiceFormState(
    // Application Information
    val applicationId: Int = 0,
    
    // Loading State
    val loadingState: Resource<EmptyingServiceFormEntity> = Resource.Idle(),
    
    // Service Details
    val emptiedDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val noOfTrips: String = "",
    val emptiedDateError: String? = null,
    val startTimeError: String? = null,
    val endTimeError: String? = null,
    val noOfTripsError: String? = null,
    
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
    val desludgingVehicleId: String = "",
    val sludgeType: String = "", // "Mixed" or "Not Mixed"
    val typeOfSludge: String = "", // Only when sludgeType is "Mixed": "Processing food", "Oil and fat (restaurant)", "Content of fuel"
    val pumpingPointPresence: String = "", // "Yes" or "No"
    val pumpingPointType: String = "", // When "Yes": "Cover", "Tube", "Pierce"
    val desludgingVehicleIdError: String? = null,
    val sludgeTypeError: String? = null,
    
    // Service Information
    val freeUnderPBC: Boolean = false,
    val additionalRepairingInEmptying: String = "",
    val regularCost: String = "",
    val extraCost: String = "",
    val regularCostError: String? = null,
    val extraCostError: String? = null,
    
    // Documentation
    val receiptNumber: String = "",
    val receiptImage: String = "",
    val pictureOfEmptying: String = "",
    val comments: String = "",
    val receiptNumberError: String? = null,
    
    // Location Information
    val longitude: Double? = null,
    val latitude: Double? = null,
    val locationError: String? = null,
    val isLocationLoading: Boolean = false,
    
    // UI State
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val isSaving: Boolean = false,
    val syncStatus: String = "DRAFT", // DRAFT, PENDING, SYNCED, FAILED
    
    // Image Upload State
    val isUploadingReceiptImage: Boolean = false,
    val isUploadingEmptyingImage: Boolean = false,
    val receiptImageUploadError: String? = null,
    val emptyingImageUploadError: String? = null,
    
    // Dropdown Data
    val vehicleOptions: List<PurposeOptionData> = emptyList(),
    val additionalRepairingOptions: List<PurposeOptionData> = emptyList()
)


fun EmptyingServiceFormState.hasValidationErrors(): Boolean {
    return emptiedDateError != null ||
            startTimeError != null ||
            endTimeError != null ||
            noOfTripsError != null ||
            applicantNameError != null ||
            applicantContactError != null ||
            serviceReceiverNameError != null ||
            serviceReceiverContactError != null ||
            desludgingVehicleIdError != null ||
            sludgeTypeError != null ||
            regularCostError != null ||
            extraCostError != null ||
            receiptNumberError != null ||
            locationError != null ||
            receiptImageUploadError != null ||
            emptyingImageUploadError != null
}

fun EmptyingServiceFormState.isFormValid(): Boolean {
    return emptiedDate.isNotBlank() &&
            startTime.isNotBlank() &&
            endTime.isNotBlank() &&
            noOfTrips.isNotBlank() &&
            applicantName.isNotBlank() &&
            applicantContact.isNotBlank() &&
            (!isServiceReceiverSameAsApplicant || (serviceReceiverName.isNotBlank() && serviceReceiverContact.isNotBlank())) &&
            desludgingVehicleId.isNotBlank() &&
            sludgeType.isNotBlank() &&
            receiptNumber.isNotBlank() &&
            longitude != null &&
            latitude != null &&
            !hasValidationErrors()
}

fun EmptyingServiceFormState.isLocationCaptured(): Boolean {
    return longitude != null && latitude != null
}

fun EmptyingServiceFormState.hasImagesUploaded(): Boolean {
    return receiptImage.isNotBlank() && pictureOfEmptying.isNotBlank()
}