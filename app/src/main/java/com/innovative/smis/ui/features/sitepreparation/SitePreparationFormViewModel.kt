package com.innovative.smis.ui.features.sitepreparation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.local.entity.SitePreparationFormEntity
import com.innovative.smis.data.repository.SitePreparationRepository
import com.innovative.smis.data.model.response.ContainmentIssuesResponse
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class SaveResult {
    data class Success(val message: String, val shouldRefreshList: Boolean = false) : SaveResult()
    data class Error(val message: String) : SaveResult()
}

data class SitePreparationFormState(
    val loadingState: Resource<SitePreparationFormEntity> = Resource.Idle(),
    val applicationId: String = "",
    val applicantName: String = "",
    val applicantContact: String = "",
    val serviceReceiverName: String = "",
    val serviceReceiverContact: String = "",
    val isReceiverSameAsApplicant: Boolean = false,
    val purposeOfEmptying: String = "",
    val otherEmptyingPurpose: String = "",
    val everEmptied: Boolean? = null,
    val lastEmptiedYear: String = "",
    val notEmptiedBeforeReason: String = "",
    val reasonForNoEmptiedDate: String = "",
    val freeServiceUnderPbc: Boolean = false,
    val additionalRepairing: String = "",
    val otherAdditionalRepairing: String = "",
    val extraPaymentRequired: Boolean? = null,
    val amountOfExtraPayment: String = "",
    val proposedEmptyingDate: String = "",
    val needReschedule: Boolean? = null,
    val newProposedEmptyingDate: String = "",
    val isLoadingDropdowns: Boolean = false,
    val emptyingReasonsList: Map<String, String> = emptyMap(),
    val containmentIssuesList: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false
)

class SitePreparationFormViewModel(
    private val repository: SitePreparationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SitePreparationFormState())
    val uiState = _uiState.asStateFlow()

    private val _saveResult = Channel<SaveResult>()
    val saveResult = _saveResult.receiveAsFlow()

    private var currentApplicationId: Int = 0
    private var currentFormId: String? = null

    fun loadApplicationDetails(applicationId: Int) {
        if (applicationId == 0) return
        currentApplicationId = applicationId

        // Set Application ID in UI state and start loading
        _uiState.update { it.copy(
            applicationId = applicationId.toString(),
            loadingState = Resource.Loading<SitePreparationFormEntity>()
        ) }

        initializeForm(applicationId)
        loadDropdownData()

        // Load API data
        viewModelScope.launch {
            repository.getSanitationCustomerDetails(applicationId.toString()).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.data?.let { apiData ->
                            _uiState.update { currentState ->
                                currentState.copy(
                                    loadingState = Resource.Idle(), // Clear loading state
                                    applicantName = apiData.applicantName ?: "",
                                    applicantContact = apiData.applicantContact ?: "",
                                    purposeOfEmptying = getEmptyingPurposeText(apiData.purposeOfEmptying),
                                    otherEmptyingPurpose = apiData.otherEmptyingPurpose ?: "",
                                    everEmptied = apiData.everEmptied,
                                    lastEmptiedYear = apiData.lastEmptiedYear?.toString() ?: "",
                                    notEmptiedBeforeReason = apiData.notEmptiedBeforeReason ?: "",
                                    freeServiceUnderPbc = apiData.freeServiceUnderPbc ?: false,
                                    additionalRepairing = getAdditionalRepairingText(apiData.additionalRepairing),
                                    otherAdditionalRepairing = apiData.otherAdditionalRepairing ?: "",
                                    extraPaymentRequired = apiData.extraPaymentRequired,
                                    amountOfExtraPayment = apiData.amountOfExtraPayment ?: "",
                                    proposedEmptyingDate = apiData.proposedEmptyingDate ?: ""
                                )
                            }
                        } ?: run {
                            // If no data, still clear loading state
                            _uiState.update { it.copy(loadingState = Resource.Idle()) }
                        }
                    }
                    is Resource.Error -> {
                        // Handle error case
                        _uiState.update {
                            it.copy(
                                loadingState = Resource.Idle()
                            )
                        }
                    }
                    is Resource.Loading -> {
                        // Loading state is already set when loadApplicationDetails starts
                    }
                    else -> {}
                }
            }
        }

        // Also load any existing form data
        viewModelScope.launch {
            repository.getFormDetails(applicationId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { entity ->
                            currentFormId = entity.id
                            // Only update editable fields from saved form
                            _uiState.update { currentState ->
                                currentState.copy(
                                    loadingState = Resource.Idle(), // Ensure loading state is cleared
                                    serviceReceiverName = entity.customerName ?: currentState.serviceReceiverName,
                                    serviceReceiverContact = entity.customerContact ?: currentState.serviceReceiverContact,
                                    isReceiverSameAsApplicant = entity.customerName == currentState.applicantName,
                                    needReschedule = entity.needReschedule,
                                    newProposedEmptyingDate = entity.newProposedEmptyingDate?.toString() ?: ""
                                )
                            }
                        } ?: run {
                            // Even if no saved form data, clear loading state
                            _uiState.update { it.copy(loadingState = Resource.Idle()) }
                        }
                    }
                    is Resource.Error -> {
                        // Create new form if none exists and clear loading state
                        _uiState.update { it.copy(loadingState = Resource.Idle()) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun getEmptyingPurposeText(purposeId: String?): String {
        // Convert purpose ID to text based on dropdown mapping
        return uiState.value.emptyingReasonsList[purposeId] ?: purposeId ?: ""
    }

    private fun getAdditionalRepairingText(repairingId: String?): String {
        // Convert repairing ID to text based on dropdown mapping  
        return uiState.value.containmentIssuesList[repairingId] ?: repairingId ?: ""
    }

    private fun initializeForm(applicationId: Int) {
        viewModelScope.launch {
            repository.initializeForm(applicationId)
        }
    }

    fun onApplicantNameChange(name: String) {
        _uiState.update { it.copy(applicantName = name) }
        autoSaveDraft()
    }

    fun onApplicantContactChange(contact: String) {
        _uiState.update { it.copy(applicantContact = contact) }
        autoSaveDraft()
    }

    fun onServiceReceiverNameChange(name: String) {
        _uiState.update { it.copy(serviceReceiverName = name) }
        autoSaveDraft()
    }

    fun onServiceReceiverContactChange(contact: String) {
        _uiState.update { it.copy(serviceReceiverContact = contact) }
        autoSaveDraft()
    }

    fun onReceiverSameAsApplicantChange(isSame: Boolean) {
        _uiState.update { state ->
            state.copy(
                isReceiverSameAsApplicant = isSame,
                serviceReceiverName = if (isSame) state.applicantName else "",
                serviceReceiverContact = if (isSame) state.applicantContact else ""
            )
        }
        autoSaveDraft()
    }

    fun onPurposeOfEmptyingChange(purpose: String) {
        _uiState.update { it.copy(purposeOfEmptying = purpose) }
        autoSaveDraft()
    }

    fun onOtherEmptyingPurposeChange(other: String) {
        _uiState.update { it.copy(otherEmptyingPurpose = other) }
        autoSaveDraft()
    }

    fun onEverEmptiedChange(emptied: Boolean) {
        _uiState.update { it.copy(everEmptied = emptied) }
        autoSaveDraft()
    }

    fun onLastEmptiedYearChange(year: String) {
        _uiState.update { it.copy(lastEmptiedYear = year) }
        autoSaveDraft()
    }

    fun onNotEmptiedBeforeReasonChange(reason: String) {
        _uiState.update { it.copy(notEmptiedBeforeReason = reason) }
        autoSaveDraft()
    }

    fun onReasonForNoEmptiedDateChange(reason: String) {
        _uiState.update { it.copy(reasonForNoEmptiedDate = reason) }
        autoSaveDraft()
    }

    fun onAdditionalRepairingChange(repairing: String) {
        _uiState.update { it.copy(additionalRepairing = repairing) }
        autoSaveDraft()
    }

    fun onOtherAdditionalRepairingChange(other: String) {
        _uiState.update { it.copy(otherAdditionalRepairing = other) }
        autoSaveDraft()
    }

    fun onExtraPaymentRequiredChange(isRequired: Boolean) {
        _uiState.update { it.copy(extraPaymentRequired = isRequired) }
        autoSaveDraft()
    }

    fun onAmountOfExtraPaymentChange(amount: String) {
        _uiState.update { it.copy(amountOfExtraPayment = amount) }
        autoSaveDraft()
    }

    fun onNeedRescheduleChange(needReschedule: Boolean) {
        _uiState.update { it.copy(needReschedule = needReschedule) }
        autoSaveDraft()
    }

    fun onProposedEmptyingDateChange(date: String) {
        _uiState.update { it.copy(proposedEmptyingDate = date) }
        autoSaveDraft()
    }

    fun onNewProposedEmptyingDateChange(date: String) {
        _uiState.update { it.copy(newProposedEmptyingDate = date) }
        autoSaveDraft()
    }

    private fun autoSaveDraft() {
        viewModelScope.launch {
            currentFormId?.let { formId ->
                val currentState = _uiState.value
                val originalEntity = uiState.value.loadingState.data

                val draftEntity = SitePreparationFormEntity(
                    id = formId,
                    applicationId = currentApplicationId,
                    createdBy = null,
                    sanitationCustomerName = originalEntity?.sanitationCustomerName,
                    sanitationCustomerContact = originalEntity?.sanitationCustomerContact,
                    sanitationCustomerAddress = originalEntity?.sanitationCustomerAddress,
                    applicantName = currentState.applicantName,
                    applicantContact = currentState.applicantContact,
                    customerName = currentState.serviceReceiverName,
                    customerContact = currentState.serviceReceiverContact,
                    purposeOfEmptying = currentState.purposeOfEmptying,
                    otherEmptyingPurpose = currentState.otherEmptyingPurpose,
                    everEmptied = currentState.everEmptied,
                    lastEmptiedDate = if (currentState.lastEmptiedYear.isNotBlank()) currentState.lastEmptiedYear.toLongOrNull() else null,
                    lastEmptiedYear = currentState.lastEmptiedYear,
                    notEmptiedBeforeReason = currentState.notEmptiedBeforeReason,
                    reasonForNoEmptiedDate = currentState.reasonForNoEmptiedDate,
                    freeServiceUnderPbc = currentState.freeServiceUnderPbc,
                    additionalRepairing = currentState.additionalRepairing,
                    otherAdditionalRepairing = currentState.otherAdditionalRepairing,
                    extraPaymentRequired = currentState.extraPaymentRequired,
                    amountOfExtraPayment = currentState.amountOfExtraPayment,
                    proposedEmptyingDate = currentState.proposedEmptyingDate,
                    needReschedule = currentState.needReschedule,
                    newProposedEmptyingDate = null, // Will be handled by string conversion later
                    syncStatus = "DRAFT"
                )

                repository.saveDraft(draftEntity)
            }
        }
    }

    fun saveForm() {
        viewModelScope.launch {
            // Set submitting state to true
            _uiState.update { it.copy(isSubmitting = true) }

            val currentState = _uiState.value
            val originalEntity = uiState.value.loadingState.data

            val formId = currentFormId ?: java.util.UUID.randomUUID().toString()

            // Format dates for PostgreSQL (YYYY-MM-DD format)
            val pgDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val formEntity = SitePreparationFormEntity(
                id = formId,
                applicationId = currentApplicationId,
                createdBy = null,
                sanitationCustomerName = originalEntity?.sanitationCustomerName,
                sanitationCustomerContact = originalEntity?.sanitationCustomerContact,
                sanitationCustomerAddress = originalEntity?.sanitationCustomerAddress,
                applicantName = currentState.applicantName,
                applicantContact = currentState.applicantContact,
                customerName = currentState.serviceReceiverName,
                customerContact = currentState.serviceReceiverContact,
                purposeOfEmptying = currentState.purposeOfEmptying,
                otherEmptyingPurpose = currentState.otherEmptyingPurpose,
                everEmptied = currentState.everEmptied,
                lastEmptiedDate = if (currentState.lastEmptiedYear.isNotBlank()) currentState.lastEmptiedYear.toLongOrNull() else null,
                lastEmptiedYear = currentState.lastEmptiedYear,
                notEmptiedBeforeReason = currentState.notEmptiedBeforeReason,
                reasonForNoEmptiedDate = currentState.reasonForNoEmptiedDate,
                freeServiceUnderPbc = currentState.freeServiceUnderPbc,
                additionalRepairing = currentState.additionalRepairing,
                otherAdditionalRepairing = currentState.otherAdditionalRepairing,
                extraPaymentRequired = currentState.extraPaymentRequired,
                amountOfExtraPayment = currentState.amountOfExtraPayment,
                proposedEmptyingDate = currentState.proposedEmptyingDate,
                needReschedule = currentState.needReschedule,
                newProposedEmptyingDate = null, // Will be handled by string conversion later
                syncStatus = "PENDING"
            )

            println("DEBUG: Saving Site Preparation form with customer data:")
            println("Service Receiver Name: ${currentState.serviceReceiverName}")
            println("Applicant Contact: ${currentState.applicantContact}")
            println("Purpose: ${currentState.purposeOfEmptying}")

            when (val result = repository.saveFormDetails(formEntity)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _saveResult.send(SaveResult.Success("Site Preparation form submitted successfully", shouldRefreshList = true))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    val message = result.message ?: "Data saved locally and will sync when online"
                    _saveResult.send(SaveResult.Success(message, shouldRefreshList = true))
                }
                else -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                }
            }

            // Debug sync queue status
            println("DEBUG: Site Preparation form saved. Check sync queue status...")
        }
    }

    fun saveDraft() {
        viewModelScope.launch {
            // Set submitting state to true
            _uiState.update { it.copy(isSubmitting = true) }

            // Same logic as saveForm but for draft saving
            val currentState = _uiState.value
            val originalEntity = uiState.value.loadingState.data

            val formId = currentFormId ?: java.util.UUID.randomUUID().toString()

            val formEntity = SitePreparationFormEntity(
                id = formId,
                applicationId = currentApplicationId,
                createdBy = null,
                sanitationCustomerName = originalEntity?.sanitationCustomerName,
                sanitationCustomerContact = originalEntity?.sanitationCustomerContact,
                sanitationCustomerAddress = originalEntity?.sanitationCustomerAddress,
                applicantName = currentState.applicantName,
                applicantContact = currentState.applicantContact,
                customerName = currentState.serviceReceiverName,
                customerContact = currentState.serviceReceiverContact,
                purposeOfEmptying = currentState.purposeOfEmptying,
                otherEmptyingPurpose = currentState.otherEmptyingPurpose,
                everEmptied = currentState.everEmptied,
                lastEmptiedDate = if (currentState.lastEmptiedYear.isNotBlank()) currentState.lastEmptiedYear.toLongOrNull() else null,
                lastEmptiedYear = currentState.lastEmptiedYear,
                notEmptiedBeforeReason = currentState.notEmptiedBeforeReason,
                reasonForNoEmptiedDate = currentState.reasonForNoEmptiedDate,
                freeServiceUnderPbc = currentState.freeServiceUnderPbc,
                additionalRepairing = currentState.additionalRepairing,
                otherAdditionalRepairing = currentState.otherAdditionalRepairing,
                extraPaymentRequired = currentState.extraPaymentRequired,
                amountOfExtraPayment = currentState.amountOfExtraPayment,
                proposedEmptyingDate = currentState.proposedEmptyingDate,
                needReschedule = currentState.needReschedule,
                newProposedEmptyingDate = null, // Will be handled by string conversion later
                syncStatus = "DRAFT"
            )

            when (val result = repository.saveFormDetails(formEntity)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _saveResult.send(SaveResult.Success("Draft saved successfully"))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    val message = result.message ?: "Draft saved locally"
                    _saveResult.send(SaveResult.Success(message))
                }
                else -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                }
            }
        }
    }

    private fun loadDropdownData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDropdowns = true) }

            // Load emptying reasons
            repository.getEmptyingReasons().collect { emptyingResult ->
                when (emptyingResult) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(emptyingReasonsList = emptyingResult.data ?: emptyMap())
                        }
                    }
                    is Resource.Error -> {
                        // Continue loading other data even if this fails
                    }
                    else -> {}
                }
            }

            // Load containment issues
            repository.getContainmentIssues().collect { containmentResult ->
                when (containmentResult) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                containmentIssuesList = containmentResult.data ?: emptyMap(),
                                isLoadingDropdowns = false
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoadingDropdowns = false) }
                    }
                    is Resource.Loading -> {
                        // Already set loading state above
                    }
                    else -> {}
                }
            }
        }
    }
}