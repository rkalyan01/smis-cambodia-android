package com.innovative.smis.ui.features.emptyingservice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.repository.EmptyingServiceRepository
import com.innovative.smis.data.api.request.EmptyingServiceRequest
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PreferenceHelper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.innovative.smis.data.model.PurposeOptionData
import com.innovative.smis.data.model.response.EmptyingReadonlyDataResponse

class EmptyingServiceFormViewModel(
    private val repository: EmptyingServiceRepository,
    private val preferenceHelper: PreferenceHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmptyingServiceFormUiState())
    val uiState: StateFlow<EmptyingServiceFormUiState> = _uiState.asStateFlow()

    private val _saveResult = Channel<SaveResult>()
    val saveResult = _saveResult.receiveAsFlow()

    private var currentApplicationId: Int = 0

    fun loadApplicationDetails(applicationId: Int) {
        if (applicationId == 0) return
        currentApplicationId = applicationId

        // Store date in API format (YYYY-MM-DD) for consistent backend communication
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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
                                // ✅ READONLY FIELDS: Always set from API, never from draft
                                // freeUnderPBC, regularCost, and building geometry always come from API
                                
                                // Only update applicant fields if not loaded from draft
                                if (currentState.applicantName.isBlank()) {
                                    currentState.copy(
                                        sanitationCustomerId = customerData.sanitationCustomerId,
                                        applicantName = customerData.applicantName ?: "",
                                        applicantContact = customerData.applicantContact ?: "",
                                        freeUnderPBC = customerData.freeServiceUnderPbc ?: false,
                                        regularCost = customerData.amountOfRegularPay ?: "",
                                        isRegularCostReadonly = true,
                                        // ✅ buildingPointGeomExist - ONLY loaded from loadReadonlyData(), not here
                                        isLoading = false
                                    )
                                } else {
                                    // Draft exists - keep applicant fields from draft, but ALWAYS update readonly fields from API
                                    currentState.copy(
                                        sanitationCustomerId = customerData.sanitationCustomerId,
                                        freeUnderPBC = customerData.freeServiceUnderPbc ?: false, // ✅ ALWAYS from API
                                        regularCost = customerData.amountOfRegularPay ?: "", // ✅ ALWAYS from API
                                        isRegularCostReadonly = true,
                                        // ✅ buildingPointGeomExist - ONLY loaded from loadReadonlyData(), not here
                                        isLoading = false
                                    )
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
        _uiState.update { 
            val error = validateTimes(startTime, it.endTime)
            it.copy(
                startTime = startTime,
                startTimeError = error,
                endTimeError = if (error != null) null else it.endTimeError
            )
        }
    }

    fun onEndTimeChange(endTime: String) {
        _uiState.update { 
            val error = validateTimes(it.startTime, endTime)
            it.copy(
                endTime = endTime,
                endTimeError = error,
                startTimeError = if (error != null) null else it.startTimeError
            )
        }
    }
    
    private fun validateTimes(startTime: String, endTime: String): String? {
        if (startTime.isEmpty() || endTime.isEmpty()) {
            return null
        }
        
        return try {
            val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val startDate = timeFormat.parse(startTime)
            val endDate = timeFormat.parse(endTime)
            
            if (startDate != null && endDate != null && startDate >= endDate) {
                "Start time must be less than end time"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }



    fun onAdditionalTripRequiredChange(required: String) {
        _uiState.update { it.copy(additionalTripRequired = required) }
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
        android.util.Log.d("EmptyingService", "Vehicle selected - License: $licensePlate, ID: $vehicleId")
        _uiState.update {
            it.copy(
                selectedVehicleLicensePlate = licensePlate,
                desludgingVehicleId = vehicleId,
                // Clear error when user selects a value
                desludgingVehicleIdError = null
            )
        }
        android.util.Log.d("EmptyingService", "State updated - desludgingVehicleId: ${_uiState.value.desludgingVehicleId}")
    }

    fun onSludgeTypeChange(sludgeType: String) {
        android.util.Log.d("EmptyingService", "Sludge type changed to: $sludgeType")
        _uiState.update {
            it.copy(
                sludgeType = sludgeType,
                // Clear Type of Sludge when changing Sludge Type
                typeOfSludge = if (sludgeType != "Mixed") "" else it.typeOfSludge
            )
        }
        android.util.Log.d("EmptyingService", "State updated - sludgeType: ${_uiState.value.sludgeType}")
    }

    fun onTypeOfSludgeChange(typeOfSludge: String) {
        _uiState.update { it.copy(typeOfSludge = typeOfSludge) }
    }

    fun onPumpingPointTypeChange(type: String) {
        _uiState.update { 
            it.copy(
                pumpingPointType = type,
                // Automatically set presence to "Yes" when a type is selected
                pumpingPointPresence = if (type.isNotEmpty()) "Yes" else "",
                // Clear error when user selects a value
                pumpingPointTypeError = null
            ) 
        }
    }

    fun onFreeUnderPBCChange(free: Boolean) {
        _uiState.update { it.copy(freeUnderPBC = free) }
    }

    fun onAdditionalRepairingChange(selectedKeys: List<String>) {
        // Check if "Others" is in the selection
        val hasOthers = selectedKeys.any { key ->
            val value = _uiState.value.additionalRepairingOptions[key] ?: key
            value.contains("Others", ignoreCase = true)
        }
        
        _uiState.update { 
            it.copy(
                additionalRepairingKeys = selectedKeys,
                // Clear "other" field when "Others" is not selected
                otherAdditionalRepairing = if (!hasOthers) "" else it.otherAdditionalRepairing
            ) 
        }
    }

    fun onOtherAdditionalRepairingChange(other: String) {
        _uiState.update { it.copy(otherAdditionalRepairing = other) }
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
    
    fun updateLocation(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    fun onEmptyingImageSelected(imageUri: String?) {
        _uiState.update { it.copy(pictureOfEmptying = imageUri ?: "") }
    }

    fun submitForm() {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            // Validate required fields
            var hasError = false
            var errorState = currentState
            
            // Validate Desludging Vehicle ID (mandatory field)
            if (currentState.desludgingVehicleId.isEmpty()) {
                errorState = errorState.copy(
                    desludgingVehicleIdError = "Desludging Vehicle is required"
                )
                hasError = true
            } else {
                errorState = errorState.copy(desludgingVehicleIdError = null)
            }
            
            // Validate Pumping Point Type (mandatory field)
            if (currentState.pumpingPointType.isEmpty()) {
                errorState = errorState.copy(
                    pumpingPointTypeError = "Pumping Point Type is required"
                )
                hasError = true
            } else {
                errorState = errorState.copy(pumpingPointTypeError = null)
            }
            
            // If there are errors, update state and return
            if (hasError) {
                _uiState.update { 
                    errorState.copy(isSubmitting = false) 
                }
                return@launch
            }
            
            // Clear any previous errors and set submitting state
            _uiState.update { 
                it.copy(
                    desludgingVehicleIdError = null,
                    pumpingPointTypeError = null,
                    isSubmitting = true
                ) 
            }

            // Try online submission first, fallback to offline storage
            try {
                // Get eto_id from logged-in user
                val etoId = preferenceHelper.getEtoId()?.toString() ?: ""
                
                // Separate additional_repairing_id (keys only) from other_additional_repairing (Others text)
                // Filter out "No" option and "Others" option
                val selectedKeys = currentState.additionalRepairingKeys
                    .filter { key ->
                        val value = currentState.additionalRepairingOptions[key] ?: key
                        // Exclude "No" and "Others" from the integer ID list
                        !value.contains("No", ignoreCase = true) && 
                        !value.contains("Others", ignoreCase = true)
                    }
                    .joinToString(",")
                
                val hasOthers = currentState.additionalRepairingKeys.any { key ->
                    val value = currentState.additionalRepairingOptions[key] ?: key
                    value.contains("Others", ignoreCase = true)
                }
                val othersText = if (hasOthers && currentState.otherAdditionalRepairing.isNotEmpty()) {
                    currentState.otherAdditionalRepairing
                } else {
                    null
                }
                
                android.util.Log.d("EmptyingService", "=== SUBMIT REQUEST DEBUG ===")
                android.util.Log.d("EmptyingService", "desludgingVehicleId from state: '${currentState.desludgingVehicleId}'")
                android.util.Log.d("EmptyingService", "sludgeType from state: '${currentState.sludgeType}'")
                android.util.Log.d("EmptyingService", "typeOfSludge from state: '${currentState.typeOfSludge}'")
                android.util.Log.d("EmptyingService", "additionalRepairingKeys: ${currentState.additionalRepairingKeys}")
                android.util.Log.d("EmptyingService", "selectedKeys (filtered): '$selectedKeys'")
                
                val request = EmptyingServiceRequest(
                    sanitation_customer_id = currentState.sanitationCustomerId,
                    start_time = currentState.startTime,
                    end_time = currentState.endTime,
                    volume_of_sludge = "3", // Default volume
                    amount_of_regular_payment_per_trip = currentState.regularCost,
                    additional_trip_required = currentState.additionalTripRequired,
                    sludge_type_a = if (currentState.sludgeType == "Mixed") "Mixed" else if (currentState.sludgeType == "Not Mixed") "Not mixed" else "",
                    sludge_type_b = if (currentState.sludgeType == "Mixed" && currentState.typeOfSludge.isNotEmpty()) currentState.typeOfSludge else "",
                    location_of_containment = "Around the house", // Default location
                    presence_of_pumping_point = currentState.pumpingPointPresence.ifEmpty { null },
                    pumping_point_type = if (currentState.pumpingPointPresence == "Yes" && currentState.pumpingPointType.isNotEmpty()) currentState.pumpingPointType else null,
                    additional_repairing_id = selectedKeys.takeIf { it.isNotEmpty() },
                    other_additional_repairing = othersText,
                    extra_payment = currentState.extraCost,
                    receipt_number = currentState.receiptNumber,
                    comments = currentState.comments,
                    receipt_image = currentState.receiptImage,
                    picture_of_emptying = currentState.pictureOfEmptying,
                    eto_id = etoId,
                    desludging_vehicle_id = currentState.desludgingVehicleId,
                    lng = currentState.longitude,
                    lat = currentState.latitude,
                    service_receiver_name = currentState.serviceReceiverName,
                    service_receiver_contact = currentState.serviceReceiverContact
                )
                
                android.util.Log.d("EmptyingService", "Request desludging_vehicle_id: '${request.desludging_vehicle_id}'")
                android.util.Log.d("EmptyingService", "Request sludge_type_a: '${request.sludge_type_a}'")
                android.util.Log.d("EmptyingService", "Request sludge_type_b: '${request.sludge_type_b}'")

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
                    // Find the license plate for the saved vehicle ID
                    val licensePlate = currentState.vehicleOptions.find { 
                        it.id == draft.desludgingVehicleId 
                    }?.type ?: ""
                    
                    android.util.Log.d("EmptyingService", "=== LOADING DRAFT ===")
                    android.util.Log.d("EmptyingService", "Draft freeUnderPBC: ${draft.freeUnderPBC} (IGNORED - readonly field)")
                    android.util.Log.d("EmptyingService", "Loading draft - vehicleId: ${draft.desludgingVehicleId}, licensePlate: $licensePlate")
                    
                    currentState.copy(
                        startTime = draft.startTime,
                        endTime = draft.endTime,
                        additionalTripRequired = draft.additionalTripRequired,
                        // ✅ READONLY FIELDS - Excluded from draft loading, always loaded from API
                        // applicantName - loaded from loadReadonlyData()
                        // applicantContact - loaded from loadReadonlyData()
                        serviceReceiverName = draft.serviceReceiverName,
                        serviceReceiverContact = draft.serviceReceiverContact,
                        isServiceReceiverSameAsApplicant = draft.isServiceReceiverSameAsApplicant,
                        desludgingVehicleId = draft.desludgingVehicleId,
                        selectedVehicleLicensePlate = licensePlate,
                        sludgeType = draft.sludgeType,
                        typeOfSludge = draft.typeOfSludge,
                        pumpingPointPresence = draft.pumpingPointPresence,
                        pumpingPointType = draft.pumpingPointType,
                        // freeUnderPBC - READONLY: Always loaded from API via loadReadonlyData()
                        additionalRepairingKeys = (draft.additionalRepairingInEmptying ?: "")
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() },
                        otherAdditionalRepairing = draft.otherAdditionalRepairing,
                        // regularCost - READONLY: Always loaded from API via loadReadonlyData()
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
            // Get eto_id from logged-in user
            val etoId = preferenceHelper.getEtoId() ?: 0
            val vehicleResult = repository.getDesludgingVehicles(etoId)
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
                    android.util.Log.d("EmptyingService", "=== ADDITIONAL REPAIRING OPTIONS LOADED ===")
                    android.util.Log.d("EmptyingService", "Options: $additionalOptions")
                    
                    _uiState.update { currentState ->
                        // Use pending keys (from readonly data) if available, otherwise use current keys
                        val keysToValidate = if (currentState.pendingAdditionalRepairingKeys.isNotEmpty()) {
                            currentState.pendingAdditionalRepairingKeys
                        } else {
                            currentState.additionalRepairingKeys
                        }
                        
                        // Validate keys against newly loaded options
                        val validKeys = keysToValidate.filter { key ->
                            additionalOptions.containsKey(key)
                        }
                        
                        android.util.Log.d("EmptyingService", "Validating keys: $keysToValidate")
                        android.util.Log.d("EmptyingService", "Valid keys: $validKeys")
                        
                        if (validKeys.size != keysToValidate.size) {
                            android.util.Log.w("EmptyingService", "Some keys invalid. Valid: $validKeys, Invalid: ${keysToValidate - validKeys.toSet()}")
                        }
                        
                        currentState.copy(
                            additionalRepairingOptions = additionalOptions,
                            additionalRepairingKeys = validKeys, // Apply validated keys
                            pendingAdditionalRepairingKeys = emptyList() // Clear pending keys after validation
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EmptyingService", "Error loading additional repairing options", e)
        }
    }

    private fun convertDateToTimestamp(dateString: String): Long? {
        return try {
            if (dateString.isBlank()) return null
            // Date is now stored in API format (yyyy-MM-dd)
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            formatter.parse(dateString)?.time
        } catch (e: Exception) {
            System.currentTimeMillis() // Fallback to current time
        }
    }

    fun loadReadonlyData(applicationId: Int) {
        android.util.Log.d("EmptyingService", "=== LOADING READONLY DATA for app $applicationId ===")
        viewModelScope.launch {
            repository.loadReadonlyData(applicationId).collect { resource ->
                android.util.Log.d("EmptyingService", "Readonly data resource: ${resource.javaClass.simpleName}")
                _uiState.update { it.copy(readonlyDataLoadingState = resource) }
                when (resource) {
                    is Resource.Success -> {
                        android.util.Log.d("EmptyingService", "Readonly data SUCCESS")
                        handleReadonlyDataSuccess(resource.data)
                    }
                    is Resource.Error -> {
                        android.util.Log.e("EmptyingService", "Readonly data ERROR: ${resource.message}")
                    }
                    is Resource.Loading -> {
                        android.util.Log.d("EmptyingService", "Readonly data LOADING...")
                    }
                    else -> {
                        android.util.Log.d("EmptyingService", "Readonly data IDLE or other state")
                    }
                }
            }
        }
    }

    private fun handleReadonlyDataSuccess(response: com.innovative.smis.data.model.response.EmptyingReadonlyDataResponse?) {
        response?.data?.let { data ->
            _uiState.update { currentState ->
                val vehicleId = data.desludgingVehicleId?.toString() ?: ""
                // Find the corresponding license plate for the vehicle ID
                val licensePlate = currentState.vehicleOptions.find { it.id == vehicleId }?.type ?: ""
                
                val additionalRepairingKeys = (data.additionalRepairing ?: "")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                
                android.util.Log.d("EmptyingService", "=== READONLY DATA LOADED ===")
                android.util.Log.d("EmptyingService", "freeServiceUnderPbc from API: ${data.freeServiceUnderPbc}")
                android.util.Log.d("EmptyingService", "buildingPointGeomExist from API: ${data.buildingPointGeomExist}")
                android.util.Log.d("EmptyingService", "additionalRepairing from API: '${data.additionalRepairing}'")
                android.util.Log.d("EmptyingService", "Parsed keys: $additionalRepairingKeys")
                android.util.Log.d("EmptyingService", "Available options: ${currentState.additionalRepairingOptions}")
                android.util.Log.d("EmptyingService", "otherAdditionalRepairing: '${data.otherAdditionalRepairing}'")
                
                // Verify that the keys actually exist in the options
                val validKeys = additionalRepairingKeys.filter { key ->
                    currentState.additionalRepairingOptions.containsKey(key)
                }
                
                if (validKeys.size != additionalRepairingKeys.size) {
                    android.util.Log.w("EmptyingService", "Options not loaded yet. Storing pending keys: $additionalRepairingKeys")
                }

                currentState.copy(
                    applicantName = data.applicantName ?: "",
                    applicantContact = data.applicantContact ?: "",
                    freeUnderPBC = data.freeServiceUnderPbc,
                    additionalRepairingKeys = validKeys,
                    pendingAdditionalRepairingKeys = additionalRepairingKeys, // Store original keys for later validation
                    otherAdditionalRepairing = data.otherAdditionalRepairing ?: "",
                    regularCost = data.amountOfRegularPayment ?: "",
                    extraCost = data.amountOfExtraPayment ?: "0",
                    desludgingVehicleId = vehicleId,
                    selectedVehicleLicensePlate = licensePlate,
                    buildingPointGeomExist = data.buildingPointGeomExist ?: false, // ✅ Set from readonly data
                    // Set readonly flags
                    isApplicantNameReadonly = true,
                    isApplicantContactReadonly = true,
                    isFreeUnderPBCReadonly = true,
                    isAdditionalRepairingReadonly = false,
                    isRegularCostReadonly = true,
                    isExtraCostReadonly = false,
                    isLoading = false
                )
            }
        }
    }

    fun postponeApplication(
        postponeFrom: String,
        postponeUntil: String,
        reason: String,
        remark: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            
            val result = repository.postponeApplication(
                applicationId = currentApplicationId,
                postponeFrom = postponeFrom,
                postponeUntil = postponeUntil,
                reason = reason,
                remark = remark
            )
            
            _uiState.update { it.copy(isSubmitting = false) }
            
            when (result) {
                is Resource.Success -> {
                    onSuccess()
                }
                is Resource.Error -> {
                    onError(result.message ?: "Failed to postpone application")
                }
                else -> {}
            }
        }
    }

    sealed class SaveResult {
        data class Success(val message: String, val shouldRefreshList: Boolean = false) : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
}