package com.innovative.smis.ui.features.emptyingservice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.repository.EmptyingServiceRepository
import com.innovative.smis.data.api.request.EmptyingServiceRequest
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.innovative.smis.data.model.PurposeOptionData
import com.innovative.smis.data.model.response.EmptyingReadonlyDataResponse

class EmptyingServiceFormViewModel(
    private val repository: EmptyingServiceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmptyingServiceFormUiState())
    val uiState: StateFlow<EmptyingServiceFormUiState> = _uiState.asStateFlow()

    private val _saveResult = Channel<SaveResult>()
    val saveResult = _saveResult.receiveAsFlow()

    private var currentApplicationId: Int = 0

    fun loadApplicationDetails(applicationId: Int) {
        if (applicationId == 0) return
        currentApplicationId = applicationId

        val todayDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        _uiState.update { it.copy(emptiedDate = todayDate) }

        // First load any existing draft
        loadDraft()

        // Load existing data from API if available
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load dropdown options first
            loadDropdownOptions()

            // Load applicant details
            repository.loadCustomerDetails(applicationId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.data?.let { customerData ->
                            _uiState.update { currentState ->
                                // Only update if not loaded from draft
                                if (currentState.applicantName.isBlank()) {
                                    currentState.copy(
                                        applicantName = customerData.sanitationCustomerName ?: "",
                                        applicantContact = customerData.sanitationCustomerContact ?: "",
                                        freeUnderPBC = customerData.freeServiceUnderPbc ?: false,
                                        isLoading = false
                                    )
                                } else {
                                    currentState.copy(isLoading = false)
                                }
                            }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false
                            )
                        }
                    }
                    is Resource.Loading -> {
                        // Loading state already set
                    }
                    else -> {}
                }
            }
        }
    }

    fun onStartTimeChange(startTime: String) {
        _uiState.update { it.copy(startTime = startTime) }
    }

    fun onEndTimeChange(endTime: String) {
        _uiState.update { it.copy(endTime = endTime) }
    }



    fun onNoOfTripsChange(trips: String) {
        _uiState.update { it.copy(noOfTrips = trips) }
    }

    fun onServiceReceiverSameAsApplicantChange(same: Boolean) {
        _uiState.update {
            it.copy(
                isServiceReceiverSameAsApplicant = same,
                serviceReceiverName = if (same) it.applicantName else "",
                serviceReceiverContact = if (same) it.applicantContact else ""
            )
        }
    }

    fun onServiceReceiverNameChange(name: String) {
        if (!_uiState.value.isServiceReceiverSameAsApplicant) {
            _uiState.update { it.copy(serviceReceiverName = name) }
        }
    }

    fun onServiceReceiverContactChange(contact: String) {
        if (!_uiState.value.isServiceReceiverSameAsApplicant) {
            _uiState.update { it.copy(serviceReceiverContact = contact) }
        }
    }

    fun onDesludgingVehicleIdChange(licensePlate: String) {
        // Find the vehicle ID based on the selected license plate
        val vehicleId = _uiState.value.vehicleOptions.find { it.type == licensePlate }?.id ?: ""
        _uiState.update {
            it.copy(
                selectedVehicleLicensePlate = licensePlate,
                desludgingVehicleId = vehicleId
            )
        }
    }

    fun onSludgeTypeChange(sludgeType: String) {
        _uiState.update {
            it.copy(
                sludgeType = sludgeType,
                // Clear Type of Sludge when changing Sludge Type
                typeOfSludge = if (sludgeType != "Mixed") "" else it.typeOfSludge
            )
        }
    }

    fun onTypeOfSludgeChange(typeOfSludge: String) {
        _uiState.update { it.copy(typeOfSludge = typeOfSludge) }
    }

    fun onPumpingPointPresenceChange(presence: String) {
        _uiState.update {
            it.copy(
                pumpingPointPresence = presence,
                // Clear Pumping Point Type when changing presence
                pumpingPointType = if (presence != "Yes") "" else it.pumpingPointType
            )
        }
    }

    fun onFreeUnderPBCChange(free: Boolean) {
        _uiState.update { it.copy(freeUnderPBC = free) }
    }

    fun onPumpingPointTypeChange(type: String) {
        _uiState.update { it.copy(pumpingPointType = type) }
    }

    fun onAdditionalRepairingChange(additionalRepairing: String) {
        _uiState.update { it.copy(additionalRepairingInEmptying = additionalRepairing) }
    }

    fun onRegularCostChange(regularCost: String) {
        _uiState.update { it.copy(regularCost = regularCost) }
    }

    fun onExtraCostChange(extraCost: String) {
        _uiState.update { it.copy(extraCost = extraCost) }
    }

    fun onReceiptNumberChange(receiptNumber: String) {
        _uiState.update { it.copy(receiptNumber = receiptNumber) }
    }

    fun onReceiptImageSelected(imageUri: String?) {
        _uiState.update { it.copy(receiptImage = imageUri ?: "") }
    }

    fun onEmptyingPictureSelected(imageUri: String?) {
        _uiState.update { it.copy(pictureOfEmptying = imageUri ?: "") }
    }

    fun onCommentsChange(comments: String) {
        _uiState.update { it.copy(comments = comments) }
    }

    fun captureLocation() {
        // TODO: Implement location capture using GPS
        viewModelScope.launch {
            // Mock location for now - replace with actual GPS implementation
            _uiState.update {
                it.copy(
                    longitude = 104.916668, // Sample longitude for Phnom Penh
                    latitude = 11.550000    // Sample latitude for Phnom Penh
                )
            }
        }
    }

    fun onEmptyingImageSelected(imageUri: String?) {
        _uiState.update { it.copy(pictureOfEmptying = imageUri ?: "") }
    }

    fun submitForm() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }

            val currentState = _uiState.value

            // Try online submission first, fallback to offline storage
            try {
                val request = EmptyingServiceRequest(
                    start_time = currentState.startTime,
                    end_time = currentState.endTime,
                    volume_of_sludge = "3", // Default volume
                    no_of_trips = currentState.noOfTrips,
                    sludge_type_a = if (currentState.sludgeType == "Mixed") "Mixed" else if (currentState.sludgeType == "Not Mixed") "Not mixed" else "",
                    sludge_type_b = if (currentState.sludgeType == "Mixed" && currentState.typeOfSludge.isNotEmpty()) currentState.typeOfSludge else "",
                    location_of_containment = "Around the house", // Default location
                    presence_of_pumping_point = if (currentState.pumpingPointPresence == "Yes") "Yes (Cover, Tube, Pierce)" else "No (need to pierce the tank)",
                    other_additional_repairing = currentState.additionalRepairingInEmptying,
                    extra_payment = currentState.extraCost,
                    receipt_number = currentState.receiptNumber,
                    comments = currentState.comments,
                    receipt_image = currentState.receiptImage,
                    picture_of_emptying = currentState.pictureOfEmptying,
                    eto_id = "4", // Default ETO ID
                    desludging_vehicle_id = currentState.desludgingVehicleId,
                    longitude = currentState.longitude,
                    latitude = currentState.latitude
                )

                val result = repository.submitEmptyingService(currentApplicationId, request)

                when (result) {
                    is Resource.Success -> {
                        _saveResult.send(SaveResult.Success("Emptying service updated successfully", shouldRefreshList = true))
                    }
                    is Resource.Error -> {
                        // Network error - save offline and sync later
                        val offlineResult = repository.submitFormOffline(currentApplicationId, currentState)
                        when (offlineResult) {
                            is Resource.Success -> {
                                _saveResult.send(SaveResult.Success("Form saved offline. Will sync when connection is available."))
                            }
                            is Resource.Error -> {
                                _saveResult.send(SaveResult.Error(offlineResult.message ?: "Failed to submit form"))
                            }
                            else -> {}
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                // Save offline if any exception occurs
                val offlineResult = repository.submitFormOffline(currentApplicationId, currentState)
                when (offlineResult) {
                    is Resource.Success -> {
                        _saveResult.send(SaveResult.Success("Form saved offline. Will sync when connection is available."))
                    }
                    is Resource.Error -> {
                        _saveResult.send(SaveResult.Error(offlineResult.message ?: "Failed to submit form"))
                    }
                    else -> {}
                }
            }

            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun saveDraft() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val result = repository.saveDraft(currentApplicationId, currentState)

            when (result) {
                is Resource.Success -> {
                    _saveResult.send(SaveResult.Success("Draft saved successfully"))
                }
                is Resource.Error -> {
                    _saveResult.send(SaveResult.Error(result.message ?: "Failed to save draft"))
                }
                else -> {}
            }
        }
    }

    fun loadDraft() {
        viewModelScope.launch {
            repository.loadDraft(currentApplicationId)?.let { draft ->
                _uiState.update { currentState ->
                    currentState.copy(
                        startTime = draft.startTime,
                        endTime = draft.endTime,
                        noOfTrips = draft.noOfTrips,
                        applicantName = draft.applicantName,
                        applicantContact = draft.applicantContact,
                        serviceReceiverName = draft.serviceReceiverName,
                        serviceReceiverContact = draft.serviceReceiverContact,
                        isServiceReceiverSameAsApplicant = draft.isServiceReceiverSameAsApplicant,
                        desludgingVehicleId = draft.desludgingVehicleId,
                        sludgeType = draft.sludgeType,
                        typeOfSludge = draft.typeOfSludge,
                        pumpingPointPresence = draft.pumpingPointPresence,
                        pumpingPointType = draft.pumpingPointType,
                        freeUnderPBC = draft.freeUnderPBC,
                        additionalRepairingInEmptying = draft.additionalRepairingInEmptying,
                        regularCost = draft.regularCost,
                        extraCost = draft.extraCost,
                        receiptNumber = draft.receiptNumber,
                        receiptImage = draft.receiptImage,
                        pictureOfEmptying = draft.pictureOfEmptying,
                        comments = draft.comments,
                        longitude = draft.longitude,
                        latitude = draft.latitude
                    )
                }
            }
        }
    }

    private suspend fun loadDropdownOptions() {
        // Load desludging vehicles
        try {
            val vehicleResult = repository.getDesludgingVehicles(4) // TODO: Use actual ETO ID
            when (vehicleResult) {
                is Resource.Success -> {
                    val vehicles = vehicleResult.data?.vehicles
                    if (vehicles != null) {
                        val vehicleOptions = vehicles.map { vehicle ->
                            PurposeOptionData(
                                id = vehicle.id.toString(),        // Vehicle ID (e.g., "4", "6")
                                type = vehicle.licensePlateNo      // License plate (e.g., "3A-3314", "3B-0546")
                            )
                        }
                        _uiState.update { it.copy(vehicleOptions = vehicleOptions) }
                    } else {
                        // Empty data, update with empty list
                        _uiState.update { it.copy(vehicleOptions = emptyList()) }
                    }
                }
                is Resource.Error -> {
                    // API error, update with empty list
                    _uiState.update { it.copy(vehicleOptions = emptyList()) }
                }
                else -> {
                    // Loading or other state, keep current options
                }
            }
        } catch (e: Exception) {
            // Exception occurred, update with empty list
            _uiState.update { it.copy(vehicleOptions = emptyList()) }
        }

        // Load additional repairing options using new API
        try {
            repository.loadAdditionalRepairingOptions().collect { resource ->
                if (resource is Resource.Success) {
                    val additionalOptions = resource.data?.data ?: emptyMap()
                    _uiState.update { it.copy(additionalRepairingOptions = additionalOptions) }
                }
            }
        } catch (e: Exception) {
            // Log error but continue loading
        }
    }

    private fun convertDateToTimestamp(dateString: String): Long? {
        return try {
            if (dateString.isBlank()) return null
            val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            formatter.parse(dateString)?.time
        } catch (e: Exception) {
            System.currentTimeMillis() // Fallback to current time
        }
    }

    fun loadReadonlyData(applicationId: Int) {
        viewModelScope.launch {
            repository.loadReadonlyData(applicationId).collect { resource ->
                _uiState.update { it.copy(readonlyDataLoadingState = resource) }
                if (resource is Resource.Success) {
                    handleReadonlyDataSuccess(resource.data)
                }
            }
        }
    }

    private fun handleReadonlyDataSuccess(response: com.innovative.smis.data.model.response.EmptyingReadonlyDataResponse?) {
        response?.data?.let { data ->
            _uiState.update { currentState ->
                val vehicleId = data.desludging_vehicle_id?.toString() ?: ""
                // Find the corresponding license plate for the vehicle ID
                val licensePlate = currentState.vehicleOptions.find { it.id == vehicleId }?.type ?: ""

                currentState.copy(
                    applicantName = data.applicant_name,
                    applicantContact = data.applicant_contact,
                    freeUnderPBC = data.free_service_under_pbc,
                    additionalRepairingInEmptying = data.additional_repairing ?: "",
                    extraCost = data.amount_of_extra_payment ?: "",
                    desludgingVehicleId = vehicleId,
                    selectedVehicleLicensePlate = licensePlate,
                    // Set readonly flags
                    isApplicantNameReadonly = true,
                    isApplicantContactReadonly = true,
                    isFreeUnderPBCReadonly = true,
                    isAdditionalRepairingReadonly = true,
                    isExtraCostReadonly = data.extra_payment_required,
                    isLoading = false
                )
            }
        }
    }

    sealed class SaveResult {
        data class Success(val message: String, val shouldRefreshList: Boolean = false) : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
}