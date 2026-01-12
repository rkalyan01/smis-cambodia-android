package com.innovative.smis.ui.features.emptyingservice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.repository.EmptyingServiceRepository
import com.innovative.smis.data.api.request.EmptyingServiceRequest
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PreferenceHelper
import com.innovative.smis.util.helper.PhoneNumberFormatter
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

    // Helper functions to normalize translated values to English for API submission
    private fun normalizeSludgeType(value: String): String = when (value) {
        "Mixed", "លាយ" -> "Mixed"
        "Not Mixed", "មិនលាយ" -> "Not mixed"
        else -> value // Return as-is if already in English or unknown
    }

    private fun normalizeTypeOfSludge(value: String): String = when (value) {
        "Processing food", "ប្រែហ្គរប់អាហារ", "កែច្នៃអាហារ" -> "Processing food"
        "Oil and fat (restaurant)", "ប្រេង និងខ្លាញ់ (ភោជនីយដ្ឋាន)" -> "Oil and fat (restaurant)"
        "Content of fuel", "មាតិកាឥន្ធនៈ", "មានលាយប្រេងឥន្ទនៈ" -> "Content of fuel"
        else -> value // Return as-is if already in English or unknown
    }

    private fun normalizeYesNo(value: String): String = when (value) {
        "Yes", "បាទ", "ចាស/បាទ" -> "Yes"
        "No", "ទេ" -> "No"
        else -> value // Return as-is if already in English or unknown
    }

    private fun normalizePumpingPointType(value: String): String = when (value) {
        "Cover", "គម្រប", "មានគម្រប" -> "Cover"
        "Tube", "បំពង់", "មានបំពង់ទីប" -> "Tube"
        "Pierce", "ចាក់", "ត្រូវចោះឬគម្រប" -> "Pierce"
        else -> value // Return as-is if already in English or unknown
    }

    private fun isMixed(value: String): Boolean = value == "Mixed" || value == "លាយ"
    
    private fun isYes(value: String): Boolean = value == "Yes" || value == "បាទ" || value == "ចាស/បាទ"

    private fun parseCustomerContacts(jsonString: String?): List<String> {
        if (jsonString.isNullOrBlank()) return emptyList()
        val contacts = mutableListOf<String>()
        try {
            // Try parsing as JSON array
            val jsonArray = org.json.JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val contact = jsonArray.optString(i)
                if (!contact.isNullOrBlank() && contact != "null") {
                    contacts.add(PhoneNumberFormatter.formatCambodianNumber(contact))
                }
            }
        } catch (e: Exception) {
            // Fallback: treat as single string (e.g., CSV or plain text)
            // Just use the string as-is if it's not a JSON array
            if (jsonString.isNotBlank() && jsonString != "null") {
                contacts.add(PhoneNumberFormatter.formatCambodianNumber(jsonString))
            }
        }
        return contacts.distinct()
    }

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
                                    val contacts = parseCustomerContacts(customerData.sanitationCustomerContact)
                                    currentState.copy(
                                        sanitationCustomerId = customerData.sanitationCustomerId,
                                        applicantName = customerData.applicantName ?: "",
                                        applicantContact = customerData.applicantContact ?: "",
                                        customerContactList = contacts,
                                        freeUnderPBC = customerData.freeServiceUnderPbc ?: false,
                                        regularCost = customerData.amountOfRegularPay ?: "",
                                        isRegularCostReadonly = true,
                                        // ✅ buildingPointGeomExist - ONLY loaded from loadReadonlyData(), not here
                                        isLoading = false
                                    )
                                } else {
                                    // Draft exists - keep applicant fields from draft, but ALWAYS update readonly fields from API
                                    val contacts = parseCustomerContacts(customerData.sanitationCustomerContact)
                                    currentState.copy(
                                        sanitationCustomerId = customerData.sanitationCustomerId,
                                        // Refresh contact list even if draft exists
                                        customerContactList = contacts,
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
        _uiState.update { it.copy(
            additionalTripRequired = required,
            additionalTripRequiredError = null,
            firstErrorField = null
        ) }
    }

    fun onServiceReceiverSameAsApplicantChange(same: Boolean) {
        _uiState.update { state ->
            state.copy(
                isServiceReceiverSameAsApplicant = same,
                serviceReceiverName = if (same) state.applicantName else "",
                serviceReceiverContact = if (same) {
                    val contacts = state.customerContactList
                    // If only one contact, auto-fill it. If multiple, let user select (so default to empty or first? User request says Dropdown if > 1)
                    // If we set it empty here, the dropdown will just wait for selection. 
                    // If we set first, it's a good default.
                    if (contacts.isNotEmpty()) contacts.first() else PhoneNumberFormatter.formatCambodianNumber(state.applicantContact)
                } else ""
            )
        }
    }

    fun onServiceReceiverNameChange(name: String) {
        if (!_uiState.value.isServiceReceiverSameAsApplicant) {
            _uiState.update { it.copy(
                serviceReceiverName = name,
                serviceReceiverNameError = null,
                firstErrorField = null
            ) }
        }
    }

    fun onServiceReceiverContactChange(contact: String) {
        if (!_uiState.value.isServiceReceiverSameAsApplicant) {
            _uiState.update { it.copy(
                serviceReceiverContact = contact,
                serviceReceiverContactError = null,
                firstErrorField = null
            ) }
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
                desludgingVehicleIdError = null,
                firstErrorField = null
            )
        }
        android.util.Log.d("EmptyingService", "State updated - desludgingVehicleId: ${_uiState.value.desludgingVehicleId}")
    }

    fun onSludgeTypeChange(sludgeType: String) {
        android.util.Log.d("EmptyingService", "Sludge type changed to: $sludgeType")
        _uiState.update {
            it.copy(
                sludgeType = sludgeType,
                sludgeTypeError = null,
                firstErrorField = null,
                // Clear Type of Sludge when changing Sludge Type (works with both English and Khmer)
                typeOfSludge = if (!isMixed(sludgeType)) "" else it.typeOfSludge
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

    fun onCustomerTypeChange(customerType: String) {
        // Get the display value for the selected key
        val displayValue = _uiState.value.customerTypeOptions[customerType] ?: ""
        
        // Check if "Other" is selected - works for both English and Khmer
        // English: "Others, specify" contains "other"
        // Khmer: "ផ្សេងទៀត" or "ផ្សេងៗ" contains "ផ្សេង"
        val hasOther = displayValue.contains("other", ignoreCase = true) || 
                      displayValue.contains("ផ្សេង")
        
        _uiState.update { 
            it.copy(
                customerType = customerType,
                // Clear "other customer type" field when "Other" is not selected
                otherCustomerType = if (!hasOther) "" else it.otherCustomerType
            ) 
        }
    }

    fun onOtherCustomerTypeChange(otherType: String) {
        _uiState.update { it.copy(otherCustomerType = otherType) }
    }

    fun onAdditionalRepairingChange(selectedKeys: List<String>) {
        // Enforce exclusivity:
        // If "No" (ID: 1) is selected, uncheck others.
        // If others are selected, uncheck "No".
        
        val currentlyHasNo = _uiState.value.additionalRepairingKeys.contains("1")
        val newHasNo = selectedKeys.contains("1")
        
        val finalKeys = if (newHasNo && !currentlyHasNo) {
            // "No" was just selected -> clear others
            listOf("1")
        } else if (newHasNo && currentlyHasNo && selectedKeys.size > 1) {
            // "No" was already selected, but user selected something else -> remove "No"
            selectedKeys.filter { it != "1" }
        } else {
            selectedKeys
        }
        
        // Check if "Others" (ID: 7) or custom key is in the selection
        // Logic should match SitePreparation's robustness
        // Check for ID "7" OR label specific checks if possible, but map keys are IDs.
        val hasOthers = finalKeys.contains("7")
        
        _uiState.update { 
            it.copy(
                additionalRepairingError = null,
                firstErrorField = null,
                additionalRepairingKeys = finalKeys,
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
        _uiState.update { it.copy(
            extraCost = extraCost,
            extraCostError = null,
            firstErrorField = null
        ) }
    }

    fun onReceiptNumberChange(receiptNumber: String) {
        _uiState.update { it.copy(
            receiptNumber = receiptNumber,
            receiptNumberError = null,
            firstErrorField = null
        ) }
    }

    fun onReceiptImageSelected(imageUri: String?) {
        _uiState.update { it.copy(
            receiptImage = imageUri ?: "",
            receiptImageError = null,
            firstErrorField = null
        ) }
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
            var firstError: String? = null
            
            // Clear all errors first
            errorState = errorState.copy(
                serviceReceiverNameError = null,
                serviceReceiverContactError = null,
                emptiedDateError = null,
                startTimeError = null,
                endTimeError = null,
                additionalTripRequiredError = null,
                desludgingVehicleIdError = null,
                sludgeTypeError = null,
                additionalRepairingError = null,
                extraCostError = null,
                receiptNumberError = null,
                receiptImageError = null,
                firstErrorField = null
            )
            
            // 1. Validate Service Receiver Name
            if (currentState.serviceReceiverName.isBlank()) {
                errorState = errorState.copy(serviceReceiverNameError = "Please enter service receiver name")
                if (firstError == null) firstError = "serviceReceiverName"
                hasError = true
            }
            
            // 2. Validate Service Receiver Contact
            if (currentState.serviceReceiverContact.isBlank()) {
                errorState = errorState.copy(serviceReceiverContactError = "Please enter service receiver contact")
                if (firstError == null) firstError = "serviceReceiverContact"
                hasError = true
            }
            
            // 3. Validate Emptied Date
            if (currentState.emptiedDate.isBlank()) {
                errorState = errorState.copy(emptiedDateError = "Please select emptied date")
                if (firstError == null) firstError = "emptiedDate"
                hasError = true
            }
            
            // 4. Validate Start Time
            if (currentState.startTime.isBlank()) {
                errorState = errorState.copy(startTimeError = "Please select start time")
                if (firstError == null) firstError = "startTime"
                hasError = true
            }
            
            // 5. Validate End Time
            if (currentState.endTime.isBlank()) {
                errorState = errorState.copy(endTimeError = "Please select end time")
                if (firstError == null) firstError = "endTime"
                hasError = true
            }
            
            // 6. Validate Additional Trip Required
            if (currentState.additionalTripRequired.isBlank()) {
                errorState = errorState.copy(additionalTripRequiredError = "Please select if additional trip is required")
                if (firstError == null) firstError = "additionalTripRequired"
                hasError = true
            }
            
            // 7. Validate Desludging Vehicle
            if (currentState.desludgingVehicleId.isEmpty()) {
                errorState = errorState.copy(desludgingVehicleIdError = "Please select desludging vehicle")
                if (firstError == null) firstError = "desludgingVehicle"
                hasError = true
            }
            
            // 8. Validate Sludge Type
            if (currentState.sludgeType.isBlank()) {
                errorState = errorState.copy(sludgeTypeError = "Please select sludge type")
                if (firstError == null) firstError = "sludgeType"
                hasError = true
            }
            
            // 9. Validate Additional Repairing
            if (currentState.additionalRepairingKeys.isEmpty()) {
                errorState = errorState.copy(additionalRepairingError = "Please select at least one option")
                if (firstError == null) firstError = "additionalRepairing"
                hasError = true
            }
            
            // ✅ Extra Cost validation removed - no longer required
            // ✅ Receipt Number and Receipt Image validation removed - no longer required
            
            // If there are errors, update state with first error field for auto-scroll
            if (hasError) {
                _uiState.update { 
                    errorState.copy(
                        isSubmitting = false,
                        firstErrorField = firstError
                    ) 
                }
                return@launch
            }
            
            // Clear any previous errors and set submitting state
            _uiState.update { 
                it.copy(isSubmitting = true) 
            }

            // Try online submission first, fallback to offline storage
            try {
                // Get eto_id from logged-in user
                val etoId = preferenceHelper.getEtoId()?.toString() ?: ""
                
                // Database column is PostgreSQL integer[] array
                // Send PostgreSQL literal format: "{2,4,5}" as string (database-specific format)
                // Filter out special options: "No" (ID: 1) and "Others" (ID: 7)
                val selectedIds = currentState.additionalRepairingKeys
                    .filter { key ->
                        key != "1" &&  // No
                        key != "7"     // Others
                    }
                    .mapNotNull { it.toIntOrNull() } // Convert to integers
                
                // Format as PostgreSQL literal: {2,4,5}
                val postgresArrayLiteral = if (selectedIds.isNotEmpty()) {
                    "{${selectedIds.joinToString(",")}}"
                } else {
                    null
                }
                
                val hasOthers = currentState.additionalRepairingKeys.contains("7")
                // Single string value for "other_additional_repairing" (character varying)
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
                android.util.Log.d("EmptyingService", "postgresArrayLiteral: $postgresArrayLiteral")
                
                val request = EmptyingServiceRequest(
                    sanitation_customer_id = currentState.sanitationCustomerId,
                    start_time = currentState.startTime,
                    end_time = currentState.endTime,
                    volume_of_sludge = "3", // Default volume
                    amount_of_regular_payment_per_trip = currentState.regularCost,
                    additional_trip_required = normalizeYesNo(currentState.additionalTripRequired),
                    sludge_type_a = normalizeSludgeType(currentState.sludgeType),
                    sludge_type_b = if (isMixed(currentState.sludgeType) && currentState.typeOfSludge.isNotEmpty()) normalizeTypeOfSludge(currentState.typeOfSludge) else "",
                    location_of_containment = null, // Not in form UI - send null
                    presence_of_pumping_point = if (currentState.pumpingPointPresence.isNotEmpty()) normalizeYesNo(currentState.pumpingPointPresence) else null,
                    pumping_point_type = if (isYes(currentState.pumpingPointPresence) && currentState.pumpingPointType.isNotEmpty()) normalizePumpingPointType(currentState.pumpingPointType) else null,
                    additional_repairing_id = postgresArrayLiteral,
                    other_additional_repairing = othersText,
                    customer_type = currentState.customerType.ifEmpty { null },
                    other_customer_type = currentState.otherCustomerType.ifEmpty { null },
                    extra_payment = currentState.extraCost,
                    receipt_number = currentState.receiptNumber,
                    comments = currentState.comments,
                    receipt_image_base64 = currentState.receiptImage,
                    picture_of_emptying_base64 = currentState.pictureOfEmptying,
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
                        customerType = draft.customerType,
                        otherCustomerType = draft.otherCustomerType,
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

        // Load customer types
        try {
            val customerTypesResult = repository.getCustomerTypes()
            when (customerTypesResult) {
                is Resource.Success -> {
                    val customerTypeOptions = customerTypesResult.data?.data ?: emptyMap()
                    android.util.Log.d("EmptyingService", "=== CUSTOMER TYPES LOADED ===")
                    android.util.Log.d("EmptyingService", "Options: $customerTypeOptions")
                    _uiState.update { it.copy(customerTypeOptions = customerTypeOptions) }
                }
                is Resource.Error -> {
                    android.util.Log.e("EmptyingService", "Error loading customer types: ${customerTypesResult.message}")
                    _uiState.update { it.copy(customerTypeOptions = emptyMap()) }
                }
                else -> {}
            }
        } catch (e: Exception) {
            android.util.Log.e("EmptyingService", "Exception loading customer types", e)
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
                android.util.Log.d("EmptyingService", "latitude from API: ${data.latitude}")
                android.util.Log.d("EmptyingService", "longitude from API: ${data.longitude}")
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
                    latitude = data.latitude?.toDoubleOrNull(), // ✅ Load GPS coordinates from API (convert string to double)
                    longitude = data.longitude?.toDoubleOrNull(), // ✅ Load GPS coordinates from API (convert string to double)
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
        postponeType: String,
        postponeFrom: String,
        postponeTo: String,
        reason: String,
        remark: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            
            val result = repository.postponeApplication(
                applicationId = currentApplicationId,
                postponeType = postponeType,
                postponeFrom = postponeFrom,
                postponeTo = postponeTo,
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

    fun clearFirstErrorField() {
        _uiState.update { it.copy(firstErrorField = null) }
    }

    sealed class SaveResult {
        data class Success(val message: String, val shouldRefreshList: Boolean = false) : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
}