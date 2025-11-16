package com.innovative.smis.ui.features.additionalrepairing

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.local.entity.AdditionalRepairingFormEntity
import com.innovative.smis.data.model.request.TripCreateRequest
import com.innovative.smis.data.model.request.TripEntryUiState
import com.innovative.smis.data.model.response.EmptyingDetailsData
import com.innovative.smis.data.repository.AdditionalRepairingRepository
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class AdditionalRepairingFormViewModel(
    private val repository: AdditionalRepairingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdditionalRepairingFormUiState())
    val uiState: StateFlow<AdditionalRepairingFormUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val saveState: StateFlow<Resource<Boolean>> = _saveState.asStateFlow()
    
    private val _draftState = MutableStateFlow<Resource<Boolean>>(Resource.Idle())
    val draftState: StateFlow<Resource<Boolean>> = _draftState.asStateFlow()

    fun loadEmptyingDetails(emptyingId: Int) {
        android.util.Log.d("AdditionalRepairingVM", "loadEmptyingDetails called with emptyingId: $emptyingId")
        _uiState.update { it.copy(emptyingId = emptyingId) }
        loadSavedDraft(emptyingId)
        loadRegularPaymentAmount()
        loadEmptyingAndApplicationDetails(emptyingId)
    }
    
    private fun loadRegularPaymentAmount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApplicationDetails = true) }
            repository.getRegularPaymentAmount().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { amount ->
                            android.util.Log.d("AdditionalRepairingVM", "Regular payment amount loaded: $amount")
                            
                            _uiState.update { currentState ->
                                val trips = currentState.tripEntries.toMutableList()
                                if (trips.isEmpty()) {
                                    trips.add(TripEntryUiState(1, amountOfRegularPayment = amount))
                                } else {
                                    trips[0] = trips[0].copy(amountOfRegularPayment = amount)
                                }
                                
                                currentState.copy(
                                    amountOfRegularPayment = amount,
                                    tripEntries = trips
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        android.util.Log.e("AdditionalRepairingVM", "Failed to load regular payment amount: ${result.message}")
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoadingApplicationDetails = true) }
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun loadEmptyingAndApplicationDetails(emptyingId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApplicationDetails = true) }
            repository.getApplicationByEmptyingId(emptyingId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { emptying ->
                            android.util.Log.d("AdditionalRepairingVM", "Emptying details loaded: applicationId=${emptying.applicationId}, extraPayment=${emptying.extraPayment}")
                            
                            _uiState.update { currentState ->
                                currentState.copy(
                                    applicationId = emptying.applicationId,
                                    amountOfExtraPayment = emptying.extraPayment ?: "",
                                    receiptNumber = emptying.receiptNumber ?: "",
                                    comments = emptying.comments ?: "",
                                    isLoadingApplicationDetails = false
                                )
                            }
                            
                            // Load application details using application_id
                            emptying.applicationId?.let { appId ->
                                loadApplicationDetailsByAppId(appId)
                            }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { 
                            it.copy(
                                errorMessage = result.message,
                                isLoadingApplicationDetails = false
                            ) 
                        }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoadingApplicationDetails = true) }
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun loadApplicationDetailsByAppId(applicationId: Int) {
        viewModelScope.launch {
            // Find application from the trip filter list or make a separate API call
            // For now, we'll just show the application ID
            // You can extend this to fetch full application details if needed
            android.util.Log.d("AdditionalRepairingVM", "Application ID loaded: $applicationId")
        }
    }
    
    private fun loadSavedDraft(emptyingId: Int) {
        viewModelScope.launch {
            repository.getSavedDraft(emptyingId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { draft ->
                            if (draft.syncStatus == "DRAFT" || draft.syncStatus == "PENDING" || draft.syncStatus == "FAILED") {
                                loadDraftIntoUi(draft)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun loadDraftIntoUi(draft: AdditionalRepairingFormEntity) {
        try {
            val tripEntries = JSONArray(draft.tripEntriesJson)
            val trips = (0 until tripEntries.length()).map { i ->
                val tripObj = tripEntries.getJSONObject(i)
                TripEntryUiState(
                    tripNumber = i + 1,
                    startTime = tripObj.getString("startTime"),
                    endTime = tripObj.getString("endTime"),
                    amountOfRegularPayment = tripObj.getString("amountOfRegularPayment"),
                    additionalTripRequired = tripObj.getString("additionalTripRequired")
                )
            }
            
            val receiptImageUri = if (draft.receiptImage.isNotEmpty()) {
                Uri.parse(draft.receiptImage)
            } else {
                null
            }
            
            _uiState.update {
                it.copy(
                    tripEntries = trips,
                    amountOfExtraPayment = draft.amountOfExtraPayment,
                    receiptNumber = draft.receiptNumber,
                    receiptImageUri = receiptImageUri,
                    comments = draft.comments,
                    showPaymentSection = trips.lastOrNull()?.additionalTripRequired?.equals("no", ignoreCase = true) == true
                )
            }
        } catch (e: Exception) {
            // Ignore parsing errors
        }
    }


    fun onTripStartTimeChange(tripIndex: Int, startTime: String) {
        _uiState.update { currentState ->
            val trips = currentState.tripEntries.toMutableList()
            if (tripIndex < trips.size) {
                val trip = trips[tripIndex].copy(startTime = startTime)
                val error = validateTripTimes(trip.startTime, trip.endTime)
                trips[tripIndex] = trip.copy(timeError = error)
            }
            currentState.copy(tripEntries = trips)
        }
    }

    fun onTripEndTimeChange(tripIndex: Int, endTime: String) {
        _uiState.update { currentState ->
            val trips = currentState.tripEntries.toMutableList()
            if (tripIndex < trips.size) {
                val trip = trips[tripIndex].copy(endTime = endTime)
                val error = validateTripTimes(trip.startTime, trip.endTime)
                trips[tripIndex] = trip.copy(timeError = error)
            }
            currentState.copy(tripEntries = trips)
        }
    }
    
    private fun validateTripTimes(startTime: String, endTime: String): String? {
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

    fun onTripAdditionalRequiredChange(tripIndex: Int, required: String) {
        _uiState.update { currentState ->
            val trips = currentState.tripEntries.toMutableList()
            if (tripIndex < trips.size) {
                trips[tripIndex] = trips[tripIndex].copy(additionalTripRequired = required)
                
                if (required.equals("yes", ignoreCase = true)) {
                    // Don't add new trip, just hide payment section and reset to initial state
                    while (trips.size > tripIndex + 1) {
                        trips.removeAt(trips.size - 1)
                    }
                    return@update currentState.copy(
                        tripEntries = trips, 
                        showPaymentSection = false,
                        formStep = FormStep.TripDetailsReady
                    )
                } else {
                    // "No" selected - stay in TripDetailsReady state, user needs to click "Next"
                    while (trips.size > tripIndex + 1) {
                        trips.removeAt(trips.size - 1)
                    }
                    return@update currentState.copy(
                        tripEntries = trips, 
                        showPaymentSection = false, // Don't show payment yet
                        formStep = FormStep.TripDetailsReady // Ready for "Next" button
                    )
                }
            } else {
                currentState
            }
        }
    }
    
    // ✅ STEP 1: Submit trip only and fetch payment details
    fun submitTripAndProceedToPayment() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val emptyingId = currentState.emptyingId ?: run {
                _uiState.update { it.copy(errorMessage = "Emptying ID not found") }
                return@launch
            }
            
            // Validate trip fields
            if (!validateTrips()) {
                _uiState.update { it.copy(errorMessage = "Please fill all trip fields") }
                return@launch
            }
            
            // State: TripSubmitting
            _uiState.update { it.copy(formStep = FormStep.TripSubmitting) }
            _saveState.value = Resource.Loading()
            
            // ✅ Track submission failure to prevent advancing to payment fetch
            var hasError = false
            
            // Submit trip(s) to POST /api/emptyings-trip/create/{emptying_id}
            try {
                for (trip in currentState.tripEntries) {
                    if (hasError) break // ✅ Stop processing remaining trips if error occurred
                    
                    val tripRequest = TripCreateRequest(
                        startTime = trip.startTime,
                        endTime = trip.endTime,
                        amountOfRegularPaymentPerTrip = trip.amountOfRegularPayment,
                        additionalTripRequired = trip.additionalTripRequired
                    )
                    
                    repository.createTripEntry(emptyingId, tripRequest).collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                android.util.Log.d("AdditionalRepairingVM", "Trip submitted successfully")
                            }
                            is Resource.Error -> {
                                hasError = true // ✅ Mark as failed
                                _uiState.update { it.copy(
                                    formStep = FormStep.TripDetailsReady,
                                    errorMessage = result.message ?: "Failed to submit trip"
                                )}
                                _saveState.value = Resource.Error(result.message ?: "Failed to submit trip")
                            }
                            else -> {}
                        }
                    }
                }
                
                // ✅ Only proceed to payment fetch if no errors occurred
                if (hasError) {
                    android.util.Log.e("AdditionalRepairingVM", "Trip submission failed, aborting payment fetch")
                    return@launch
                }
                
                // State: TripSubmitted - Now fetch payment details
                _uiState.update { it.copy(formStep = FormStep.TripSubmitted) }
                android.util.Log.d("AdditionalRepairingVM", "Fetching payment details from /api/emptyings/$emptyingId")
                
                // Fetch payment details from GET /api/emptyings/{emptying_id}
                repository.getEmptyingDetails(emptyingId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            result.data?.let { details ->
                                android.util.Log.d("AdditionalRepairingVM", "Payment details fetched: regularPayment=${details.amountOfRegularPayment}, extraPayment=${details.amountOfExtraPayment}")
                                
                                // Update UI with fetched payment details
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        amountOfRegularPayment = details.amountOfRegularPayment ?: "0",
                                        amountOfExtraPayment = details.amountOfExtraPayment ?: "",
                                        receiptNumber = details.receiptNumber ?: "",
                                        comments = details.comments ?: "",
                                        showPaymentSection = true, // ✅ NOW show payment section
                                        formStep = FormStep.PaymentReady, // ✅ Ready for payment submission
                                        errorMessage = null
                                    )
                                }
                                _saveState.value = Resource.Idle()
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(
                                formStep = FormStep.TripDetailsReady,
                                errorMessage = "Trip submitted but failed to load payment details: ${result.message}"
                            )}
                            _saveState.value = Resource.Error(result.message ?: "Failed to load payment details")
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    formStep = FormStep.TripDetailsReady,
                    errorMessage = e.message ?: "Unknown error"
                )}
                _saveState.value = Resource.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    // ✅ STEP 2: Submit payment details only
    fun submitPaymentDetails() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val emptyingId = currentState.emptyingId ?: return@launch
            
            // Validate payment fields
            if (!validatePayment()) {
                _uiState.update { it.copy(errorMessage = "Please fill receipt number or upload receipt image") }
                return@launch
            }
            
            // State: SubmittingPayment
            _uiState.update { it.copy(formStep = FormStep.SubmittingPayment) }
            _saveState.value = Resource.Loading()
            
            // Prepare receipt image
            val receiptImagePart = currentState.receiptImageUri?.let { uri ->
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("receipt_image", file.name, requestFile)
                } else null
            }
            
            // Submit to PUT /api/emptyings/{emptying_id}
            repository.updatePaymentDetails(
                emptyingId = emptyingId,
                amountOfExtraPayment = currentState.amountOfExtraPayment.takeIf { it.isNotEmpty() },
                receiptNumber = currentState.receiptNumber.takeIf { it.isNotEmpty() },
                comments = currentState.comments.takeIf { it.isNotEmpty() },
                receiptImage = receiptImagePart
            ).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(formStep = FormStep.Completed) }
                        _saveState.value = Resource.Success(true)
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(
                            formStep = FormStep.PaymentReady,
                            errorMessage = result.message ?: "Failed to submit payment"
                        )}
                        _saveState.value = Resource.Error(result.message ?: "Failed to submit payment")
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun submitTripAndReturn(trip: TripEntryUiState) {
        viewModelScope.launch {
            val emptyingId = _uiState.value.emptyingId ?: run {
                android.util.Log.e("AdditionalRepairingVM", "EmptyingId is null, cannot submit")
                return@launch
            }
            
            android.util.Log.d("AdditionalRepairingVM", "Submitting trip with emptyingId: $emptyingId")
            
            if (trip.startTime.isEmpty() || trip.endTime.isEmpty()) {
                _saveState.value = Resource.Error("Please fill start time and end time")
                return@launch
            }
            
            _saveState.value = Resource.Loading()
            
            val request = TripCreateRequest(
                startTime = trip.startTime,
                endTime = trip.endTime,
                amountOfRegularPaymentPerTrip = trip.amountOfRegularPayment,
                additionalTripRequired = "yes"
            )
            
            android.util.Log.d("AdditionalRepairingVM", "Trip request: startTime=${request.startTime}, endTime=${request.endTime}, amount=${request.amountOfRegularPaymentPerTrip}")
            
            repository.createTripEntry(emptyingId, request).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _saveState.value = Resource.Success(true)
                    }
                    is Resource.Error -> {
                        _saveState.value = Resource.Error(result.message ?: "Failed to submit trip")
                    }
                    else -> {}
                }
            }
        }
    }

    fun onExtraPaymentChange(amount: String) {
        _uiState.update { it.copy(amountOfExtraPayment = amount) }
    }

    fun onReceiptNumberChange(number: String) {
        _uiState.update { it.copy(receiptNumber = number) }
    }

    fun onReceiptImageSelected(uri: Uri?) {
        _uiState.update { it.copy(receiptImageUri = uri) }
    }

    fun onCommentsChange(comments: String) {
        _uiState.update { it.copy(comments = comments) }
    }

    fun saveTrips() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val emptyingId = currentState.emptyingId ?: return@launch

            if (!validateTrips()) {
                _saveState.value = Resource.Error("Please fill all required fields")
                return@launch
            }

            _saveState.value = Resource.Loading()

            for (trip in currentState.tripEntries) {
                val request = TripCreateRequest(
                    startTime = trip.startTime,
                    endTime = trip.endTime,
                    amountOfRegularPaymentPerTrip = trip.amountOfRegularPayment,
                    additionalTripRequired = trip.additionalTripRequired
                )

                repository.createTripEntry(emptyingId, request).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                        }
                        is Resource.Error -> {
                            _saveState.value = Resource.Error(result.message ?: "Failed to save trip")
                            return@collect
                        }
                        else -> {}
                    }
                }
            }

            _saveState.value = Resource.Success(true)
        }
    }

    fun updatePaymentDetails() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val emptyingId = currentState.emptyingId ?: return@launch

            if (!validatePayment()) {
                _saveState.value = Resource.Error("Please fill all payment fields")
                return@launch
            }

            _saveState.value = Resource.Loading()

            val receiptImage = currentState.receiptImageUri?.let { uri ->
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("receipt_image", file.name, requestFile)
                } else null
            }

            repository.updatePaymentDetails(
                emptyingId = emptyingId,
                amountOfExtraPayment = currentState.amountOfExtraPayment.takeIf { it.isNotEmpty() },
                receiptNumber = currentState.receiptNumber.takeIf { it.isNotEmpty() },
                comments = currentState.comments.takeIf { it.isNotEmpty() },
                receiptImage = receiptImage
            ).collect { result ->
                _saveState.value = result
            }
        }
    }

    private fun validateTrips(): Boolean {
        val trips = _uiState.value.tripEntries
        return trips.all { trip ->
            trip.startTime.isNotEmpty() && 
            trip.endTime.isNotEmpty() && 
            trip.additionalTripRequired.isNotEmpty()
        }
    }

    private fun validatePayment(): Boolean {
        // ✅ Receipt Number and Receipt Image validation removed - no longer required
        return true
    }

    fun saveDraft() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val emptyingId = currentState.emptyingId ?: return@launch
            
            _draftState.value = Resource.Loading()
            
            val tripEntriesJson = JSONArray().apply {
                currentState.tripEntries.forEach { trip ->
                    put(JSONObject().apply {
                        put("startTime", trip.startTime)
                        put("endTime", trip.endTime)
                        put("amountOfRegularPayment", trip.amountOfRegularPayment)
                        put("additionalTripRequired", trip.additionalTripRequired)
                    })
                }
            }.toString()
            
            val formEntity = AdditionalRepairingFormEntity(
                emptyingId = emptyingId,
                tripEntriesJson = tripEntriesJson,
                amountOfRegularPayment = currentState.amountOfRegularPayment,
                amountOfExtraPayment = currentState.amountOfExtraPayment,
                receiptNumber = currentState.receiptNumber,
                receiptImage = currentState.receiptImageUri?.toString() ?: "",
                comments = currentState.comments,
                syncStatus = "DRAFT"
            )
            
            repository.saveDraft(formEntity).collect { result ->
                _draftState.value = result
            }
        }
    }
    
    fun submitForm() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val emptyingId = currentState.emptyingId ?: return@launch

            if (!validateTrips()) {
                _saveState.value = Resource.Error("Please fill all required trip fields")
                return@launch
            }
            
            val lastTrip = currentState.tripEntries.lastOrNull()
            val isAdditionalTripRequired = lastTrip?.additionalTripRequired?.equals("yes", ignoreCase = true) == true
            
            _saveState.value = Resource.Loading()
            
            // If "Yes" selected, only submit trip(s) to POST /api/emptyings-trip/create/{emptying_id}
            if (isAdditionalTripRequired) {
                for (trip in currentState.tripEntries) {
                    val tripRequest = TripCreateRequest(
                        startTime = trip.startTime,
                        endTime = trip.endTime,
                        amountOfRegularPaymentPerTrip = trip.amountOfRegularPayment,
                        additionalTripRequired = trip.additionalTripRequired
                    )
                    
                    repository.createTripEntry(emptyingId, tripRequest).collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                _saveState.value = Resource.Success(true)
                            }
                            is Resource.Error -> {
                                _saveState.value = Resource.Error(result.message ?: "Failed to submit trip")
                                return@collect
                            }
                            else -> {}
                        }
                    }
                }
                return@launch
            }
            
            // If "No" selected, validate payment and submit both trip(s) and payment
            if (!validatePayment()) {
                _saveState.value = Resource.Error("Please fill receipt number or upload receipt image")
                return@launch
            }
            
            // Step 1: Submit trip(s) to POST /api/emptyings-trip/create/{emptying_id}
            for (trip in currentState.tripEntries) {
                val tripRequest = TripCreateRequest(
                    startTime = trip.startTime,
                    endTime = trip.endTime,
                    amountOfRegularPaymentPerTrip = trip.amountOfRegularPayment,
                    additionalTripRequired = trip.additionalTripRequired
                )
                
                repository.createTripEntry(emptyingId, tripRequest).collect { result ->
                    when (result) {
                        is Resource.Error -> {
                            _saveState.value = Resource.Error(result.message ?: "Failed to submit trip")
                            return@collect
                        }
                        else -> {}
                    }
                }
            }
            
            // Step 2: Submit payment details to PUT /api/emptyings/{emptying_id}
            val receiptImagePart = currentState.receiptImageUri?.let { uri ->
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("receipt_image", file.name, requestFile)
                } else null
            }
            
            repository.updatePaymentDetails(
                emptyingId = emptyingId,
                amountOfExtraPayment = currentState.amountOfExtraPayment.takeIf { it.isNotEmpty() },
                receiptNumber = currentState.receiptNumber.takeIf { it.isNotEmpty() },
                comments = currentState.comments.takeIf { it.isNotEmpty() },
                receiptImage = receiptImagePart
            ).collect { result ->
                _saveState.value = result
            }
        }
    }

    fun clearSaveState() {
        _saveState.value = Resource.Idle()
    }
    
    fun clearDraftState() {
        _draftState.value = Resource.Idle()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

// ✅ State machine for two-step submission flow
sealed class FormStep {
    object TripDetailsReady : FormStep()  // Initial state - trip form ready
    object TripSubmitting : FormStep()    // Submitting trip to API
    object TripSubmitted : FormStep()     // Trip submitted, fetching payment details
    object PaymentReady : FormStep()      // Payment section visible with data
    object SubmittingPayment : FormStep() // Submitting payment to API
    object Completed : FormStep()         // All done
}

data class AdditionalRepairingFormUiState(
    val emptyingId: Int? = null,
    val tripEntries: List<TripEntryUiState> = listOf(TripEntryUiState(1)),
    val formStep: FormStep = FormStep.TripDetailsReady, // ✅ State machine
    val showPaymentSection: Boolean = false,
    
    // Application Details
    val applicationId: Int? = null,
    val isLoadingApplicationDetails: Boolean = false,
    
    val paymentDetails: EmptyingDetailsData? = null,
    val amountOfRegularPayment: String = "",
    val amountOfExtraPayment: String = "",
    val receiptNumber: String = "",
    val receiptImageUri: Uri? = null,
    val receiptImageUrl: String? = null,
    val comments: String = "",
    
    val errorMessage: String? = null
)
