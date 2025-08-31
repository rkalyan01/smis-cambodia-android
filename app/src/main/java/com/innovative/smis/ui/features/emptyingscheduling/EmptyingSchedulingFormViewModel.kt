package com.innovative.smis.ui.features.emptyingscheduling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.local.entity.EmptyingSchedulingFormEntity
import com.innovative.smis.data.repository.EmptyingSchedulingRepository
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

sealed class SaveResult {
    data class Success(val message: String, val shouldRefreshList: Boolean = false) : SaveResult()
    data class Error(val message: String) : SaveResult()
}

data class EmptyingSchedulingFormState(
    val loadingState: Resource<EmptyingSchedulingFormEntity> = Resource.Idle(),
    val applicationDate: String = "",
    val sanitationCustomerName: String? = null,
    val sanitationCustomerContact: String? = null,
    val applicantName: String = "",
    val applicantContact: String = "",
    val isApplicantSameAsCustomer: Boolean = false,
    val purposeOfEmptying: String = "",
    val purposeOfEmptyingOther: String = "",
    val containmentIssuesOther: String = "",
    val proposeEmptyingDate: Long? = null,
    val everEmptied: Boolean? = null,
    val lastEmptiedYear: Int? = null,
    val lastEmptiedDate: Long? = null,
    val notEmptiedBeforeReason: String = "",
    val reasonForNoEmptiedDate: String = "",
    val freeServiceUnderPBC: Boolean? = null,
    val sizeOfStorageTankM3: String? = null,
    val constructionYear: Int? = null,
    val accessibility: Boolean? = null,
    val locationOfContainment: String? = null,
    val pumpingPointPresence: Boolean? = null,
    val pumpingPointDetails: String = "",
    val containmentIssues: String = "",
    val extraPaymentRequired: Boolean? = null,
    val extraPaymentAmount: String = "",
    val siteVisitRequired: Boolean? = null,
    
    // Dropdown data
    val emptyingReasons: Map<String, String> = emptyMap(),
    val containmentIssuesList: Map<String, String> = emptyMap(),
    val isLoadingDropdowns: Boolean = false,
    val isSubmitting: Boolean = false
)

class EmptyingSchedulingFormViewModel(
    private val repository: EmptyingSchedulingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmptyingSchedulingFormState())
    val uiState = _uiState.asStateFlow()

    private val _saveResult = Channel<SaveResult>()
    val saveResult = _saveResult.receiveAsFlow()

    private var currentApplicationId: Int = 0
    private var currentFormId: String? = null

    fun loadApplicationDetails(applicationId: Int) {
        if (applicationId == 0) return
        currentApplicationId = applicationId
        
        initializeForm(applicationId)
        loadDropdownData()
    }

    private fun loadDropdownData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDropdowns = true) }
            
            // Load emptying reasons
            repository.getEmptyingReasons().collect { reasonsResult ->
                when (reasonsResult) {
                    is Resource.Success -> {
                        _uiState.update { 
                            it.copy(emptyingReasons = reasonsResult.data ?: emptyMap()) 
                        }
                    }
                    else -> {
                        // Handle error silently, keep empty map
                    }
                }
            }
            
            // Load containment issues
            repository.getContainmentIssues().collect { issuesResult ->
                when (issuesResult) {
                    is Resource.Success -> {
                        _uiState.update { 
                            it.copy(containmentIssuesList = issuesResult.data ?: emptyMap()) 
                        }
                    }
                    else -> {
                        // Handle error silently, keep empty map
                    }
                }
            }
            
            _uiState.update { it.copy(isLoadingDropdowns = false) }
        }
    }

    private fun initializeForm(applicationId: Int) {        
        viewModelScope.launch {
            repository.getFormDetails(applicationId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(loadingState = result) }
                        result.data?.let { entity ->
                            currentFormId = entity.id
                            
                            // Debug logging to check what data is loaded
                            println("DEBUG: Loading form data for editing:")
                            println("  Customer Name: '${entity.sanitationCustomerName}'")
                            println("  Customer Contact: '${entity.sanitationCustomerContact}'")
                            println("  Customer Address: '${entity.sanitationCustomerAddress}'")
                            println("  Applicant Name: '${entity.applicantName}'")
                            println("  Applicant Contact: '${entity.applicantContact}'")
                            println("  Is Applicant Same As Customer: ${entity.isApplicantSameAsCustomer}")
                            
                            _uiState.update {
                                it.copy(
                                    applicationDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
                                    sanitationCustomerName = entity.sanitationCustomerName,
                                    sanitationCustomerContact = entity.sanitationCustomerContact,
                                    applicantName = entity.applicantName ?: "",
                                    applicantContact = entity.applicantContact ?: "",
                                    isApplicantSameAsCustomer = entity.isApplicantSameAsCustomer ?: false,
                                    freeServiceUnderPBC = entity.freeServiceUnderPbc,
                                    everEmptied = entity.everEmptied,
                                    lastEmptiedYear = entity.lastEmptiedYear?.toIntOrNull(),
                                    notEmptiedBeforeReason = entity.notEmptiedBeforeReason ?: "",
                                    reasonForNoEmptiedDate = entity.emptiedNodateReason ?: "",
                                    purposeOfEmptying = entity.purposeOfEmptying ?: "",
                                    purposeOfEmptyingOther = entity.purposeOfEmptyingOther ?: "",
                                    proposeEmptyingDate = entity.proposedEmptyingDate,
                                    sizeOfStorageTankM3 = entity.sizeOfContainment,
                                    constructionYear = entity.yearOfInstallation?.toIntOrNull(),
                                    accessibility = if (entity.containmentAccessibility?.isNotEmpty() == true) true else null,
                                    locationOfContainment = entity.locationOfContainment,
                                    pumpingPointPresence = entity.pumpingPointPresence,
                                    containmentIssues = entity.containmentIssues ?: "",
                                    containmentIssuesOther = entity.containmentIssuesOther ?: "",
                                    extraPaymentRequired = entity.extraPaymentRequired,
                                    extraPaymentAmount = entity.extraPaymentAmount ?: "",
                                    siteVisitRequired = entity.siteVisitRequired
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        // For 404 errors (customer not found), show empty form to create new record
                        Log.d("EmptyingSchedulingVM", "API error (${result.message}), initializing empty form for new customer entry")
                        _uiState.update { 
                            it.copy(
                                loadingState = Resource.Idle(),
                                applicationDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
                                sanitationCustomerName = "",
                                sanitationCustomerContact = "",

                                applicantName = "",
                                applicantContact = "",
                                isApplicantSameAsCustomer = false,
                                freeServiceUnderPBC = false,
                                everEmptied = null,
                                notEmptiedBeforeReason = "",
                                reasonForNoEmptiedDate = "",
                                purposeOfEmptying = "",
                                purposeOfEmptyingOther = "",
                                proposeEmptyingDate = null,
                                lastEmptiedYear = null,
                                sizeOfStorageTankM3 = "",
                                constructionYear = null,
                                accessibility = null,
                                locationOfContainment = null,
                                pumpingPointPresence = null,
                                containmentIssues = "",
                                containmentIssuesOther = "",
                                extraPaymentRequired = null,
                                extraPaymentAmount = "",
                                siteVisitRequired = null
                            ) 
                        }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(loadingState = result) }
                    }
                    else -> {
                        _uiState.update { it.copy(loadingState = result) }
                    }
                }
            }
        }
    }



    fun onCustomerNameChange(name: String) { _uiState.update { it.copy(sanitationCustomerName = name) } }
    fun onCustomerContactChange(contact: String) { 
        _uiState.update { it.copy(sanitationCustomerContact = contact) }
        autoSaveDraft()
    }

    fun onApplicantNameChange(name: String) { 
        _uiState.update { it.copy(applicantName = name) }
        autoSaveDraft()
    }
    fun onApplicantContactChange(contact: String) { 
        _uiState.update { it.copy(applicantContact = contact) }
        autoSaveDraft()
    }
    fun onPurposeOfEmptyingChange(purpose: String) { 
        _uiState.update { it.copy(purposeOfEmptying = purpose) }
        autoSaveDraft()
    }
    
    fun onPurposeOfEmptyingOtherChange(other: String) {
        _uiState.update { it.copy(purposeOfEmptyingOther = other) }
        autoSaveDraft()
    }
    
    fun onContainmentIssuesOtherChange(other: String) {
        _uiState.update { it.copy(containmentIssuesOther = other) }
        autoSaveDraft()
    }
    
    fun onNotEmptiedBeforeReasonChange(reason: String) {
        _uiState.update { it.copy(notEmptiedBeforeReason = reason) }
        autoSaveDraft()
    }
    
    fun onPumpingPointDetailsChange(details: String) {
        _uiState.update { it.copy(pumpingPointDetails = details) }
        autoSaveDraft()
    }
    fun onProposeEmptyingDateChange(dateMillis: Long?) { 
        _uiState.update { it.copy(proposeEmptyingDate = dateMillis) }
        autoSaveDraft()
    }
    fun onEverEmptiedChange(emptied: Boolean) { 
        _uiState.update { it.copy(everEmptied = emptied) }
        autoSaveDraft()
    }
    fun onLastEmptiedYearChange(year: Int?) { 
        _uiState.update { it.copy(lastEmptiedYear = year) }
        autoSaveDraft()
    }
    fun onReasonForNoEmptiedDateChange(reason: String) { 
        _uiState.update { it.copy(reasonForNoEmptiedDate = reason) }
        autoSaveDraft()
    }
    fun onSizeOfContainmentChange(size: String) { 
        _uiState.update { it.copy(sizeOfStorageTankM3 = size) }
        autoSaveDraft()
    }
    fun onConstructionYearChange(year: Int) { 
        _uiState.update { it.copy(constructionYear = year) }
        autoSaveDraft()
    }
    fun onAccessibilityChange(accessibility: Boolean) { 
        _uiState.update { it.copy(accessibility = accessibility) }
        autoSaveDraft()
    }
    fun onPumpingPointPresenceChange(isPresent: Boolean) { 
        _uiState.update { it.copy(pumpingPointPresence = isPresent) }
        autoSaveDraft()
    }
    fun onContainmentIssuesChange(issues: String) { 
        _uiState.update { it.copy(containmentIssues = issues) }
        autoSaveDraft()
    }
    fun onExtraPaymentRequiredChange(isRequired: Boolean) { 
        _uiState.update { it.copy(extraPaymentRequired = isRequired) }
        autoSaveDraft()
    }
    fun onExtraPaymentAmountChange(amount: String) { 
        _uiState.update { it.copy(extraPaymentAmount = amount) }
        autoSaveDraft()
    }
    fun onSiteVisitRequiredChange(isRequired: Boolean) { 
        _uiState.update { it.copy(siteVisitRequired = isRequired) }
        autoSaveDraft()
    }
    
    fun onLastEmptiedDateChange(date: Long?) {
        _uiState.update { it.copy(lastEmptiedDate = date) }
        autoSaveDraft()
    }

    fun onApplicantSameAsCustomerChange(isSame: Boolean) {
        _uiState.update { state ->
            state.copy(
                isApplicantSameAsCustomer = isSame,
                applicantName = if (isSame) state.sanitationCustomerName ?: "" else "",
                applicantContact = if (isSame) state.sanitationCustomerContact ?: "" else ""
            )
        }
        autoSaveDraft()
    }

    fun onLocationOfContainmentChange(location: String) {
        _uiState.update { it.copy(locationOfContainment = location) }
        autoSaveDraft()
    }

    fun autoSaveDraft() {
        viewModelScope.launch {
            currentFormId?.let { formId ->
                val currentState = _uiState.value
                val originalEntity = uiState.value.loadingState.data

                val draftEntity = EmptyingSchedulingFormEntity(
                    id = formId,
                    applicationId = currentApplicationId,
                    createdBy = null,
                    sanitationCustomerName = currentState.sanitationCustomerName,
                    sanitationCustomerContact = currentState.sanitationCustomerContact,
                    sanitationCustomerAddress = null, // Not available in current UI state
                    pbcCustomerType = originalEntity?.pbcCustomerType,
                    freeServiceUnderPbc = currentState.freeServiceUnderPBC,
                    applicantName = currentState.applicantName,
                    applicantContact = currentState.applicantContact,
                    isApplicantSameAsCustomer = currentState.isApplicantSameAsCustomer,
                    lastEmptiedYear = currentState.lastEmptiedYear?.toString(),
                    everEmptied = currentState.everEmptied,
                    emptiedNodateReason = if (currentState.everEmptied == true && currentState.lastEmptiedYear == null) currentState.reasonForNoEmptiedDate else null,
                    notEmptiedBeforeReason = if (currentState.everEmptied == false) currentState.reasonForNoEmptiedDate else null,
                    purposeOfEmptying = currentState.purposeOfEmptying,
                    purposeOfEmptyingOther = currentState.purposeOfEmptyingOther,
                    proposedEmptyingDate = currentState.proposeEmptyingDate,
                    lastEmptiedDate = currentState.lastEmptiedDate,
                    sizeOfContainment = currentState.sizeOfStorageTankM3,
                    yearOfInstallation = currentState.constructionYear?.toString(),
                    containmentAccessibility = if (currentState.accessibility == true) "Yes" else if (currentState.accessibility == false) "No" else null,
                    locationOfContainment = currentState.locationOfContainment,
                    pumpingPointPresence = currentState.pumpingPointPresence,
                    containmentIssues = currentState.containmentIssues,
                    containmentIssuesOther = currentState.containmentIssuesOther,
                    extraPaymentRequired = currentState.extraPaymentRequired,
                    extraPaymentAmount = currentState.extraPaymentAmount,
                    siteVisitRequired = currentState.siteVisitRequired,
                    remarks = "",
                    estimatedVolume = "",
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
            
            val formEntity = EmptyingSchedulingFormEntity(
                id = formId,
                applicationId = currentApplicationId,
                createdBy = null,
                sanitationCustomerName = currentState.sanitationCustomerName,
                sanitationCustomerContact = currentState.sanitationCustomerContact,
                sanitationCustomerAddress = null, // Not available in current UI state
                pbcCustomerType = originalEntity?.pbcCustomerType,
                freeServiceUnderPbc = currentState.freeServiceUnderPBC,
                applicantName = currentState.applicantName,
                applicantContact = currentState.applicantContact,
                isApplicantSameAsCustomer = currentState.isApplicantSameAsCustomer,
                lastEmptiedYear = currentState.lastEmptiedYear?.toString(),
                everEmptied = currentState.everEmptied,
                emptiedNodateReason = if (currentState.everEmptied == true && currentState.lastEmptiedYear == null) currentState.reasonForNoEmptiedDate else null,
                notEmptiedBeforeReason = if (currentState.everEmptied == false) currentState.reasonForNoEmptiedDate else null,
                purposeOfEmptying = currentState.purposeOfEmptying,
                purposeOfEmptyingOther = currentState.purposeOfEmptyingOther,
                proposedEmptyingDate = currentState.proposeEmptyingDate,
                lastEmptiedDate = currentState.lastEmptiedDate,
                sizeOfContainment = currentState.sizeOfStorageTankM3,
                yearOfInstallation = currentState.constructionYear?.toString(),
                containmentAccessibility = if (currentState.accessibility == true) "Yes" else if (currentState.accessibility == false) "No" else null,
                locationOfContainment = currentState.locationOfContainment,
                pumpingPointPresence = currentState.pumpingPointPresence,
                containmentIssues = currentState.containmentIssues,
                containmentIssuesOther = currentState.containmentIssuesOther,
                extraPaymentRequired = currentState.extraPaymentRequired,
                extraPaymentAmount = currentState.extraPaymentAmount,
                siteVisitRequired = currentState.siteVisitRequired,
                remarks = "",
                estimatedVolume = "",
                syncStatus = "PENDING"
            )

            println("DEBUG: Saving form with customer data:")
            println("  Customer Name: '${formEntity.sanitationCustomerName}'")
            println("  Customer Contact: '${formEntity.sanitationCustomerContact}'")
            println("  Customer Address: '${formEntity.sanitationCustomerAddress}'")
            println("  Applicant Name: '${formEntity.applicantName}'")
            println("  Applicant Contact: '${formEntity.applicantContact}'")

            val repoResult = repository.saveFormDetails(formEntity)

            when (repoResult) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    
                    // Debug: Check sync queue status after save
                    val pendingSyncs = repository.getPendingSyncs()
                    println("DEBUG: After save - sync queue has ${pendingSyncs.size} pending items")
                    
                    // Optimized refresh: immediately update local status and sync
                    println("DEBUG: Form submitted successfully, updating application #$currentApplicationId")
                    repository.refreshApplicationsAfterSubmission(currentApplicationId)
                    
                    // Send success result with list refresh flag
                    _saveResult.send(SaveResult.Success("Application submitted successfully", shouldRefreshList = true))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    val message = repoResult.message ?: "Data saved locally and will sync when online"
                    _saveResult.send(SaveResult.Success(message))
                }
                else -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                }
            }
        }
    }
    
    fun saveDraft() {
        viewModelScope.launch {
            // Set submitting state to true
            _uiState.update { it.copy(isSubmitting = true) }
            
            val currentState = _uiState.value
            val originalEntity = uiState.value.loadingState.data

            val formId = currentFormId ?: java.util.UUID.randomUUID().toString()
            
            val formEntity = EmptyingSchedulingFormEntity(
                id = formId,
                applicationId = currentApplicationId,
                createdBy = null,
                sanitationCustomerName = currentState.sanitationCustomerName,
                sanitationCustomerContact = currentState.sanitationCustomerContact,
                sanitationCustomerAddress = null, // Not available in current UI state
                pbcCustomerType = originalEntity?.pbcCustomerType,
                freeServiceUnderPbc = currentState.freeServiceUnderPBC,
                applicantName = currentState.applicantName,
                applicantContact = currentState.applicantContact,
                isApplicantSameAsCustomer = currentState.isApplicantSameAsCustomer,
                lastEmptiedYear = currentState.lastEmptiedYear?.toString(),
                everEmptied = currentState.everEmptied,
                emptiedNodateReason = if (currentState.everEmptied == true && currentState.lastEmptiedYear == null) currentState.reasonForNoEmptiedDate else null,
                notEmptiedBeforeReason = if (currentState.everEmptied == false) currentState.reasonForNoEmptiedDate else null,
                purposeOfEmptying = currentState.purposeOfEmptying,
                purposeOfEmptyingOther = currentState.purposeOfEmptyingOther,
                proposedEmptyingDate = currentState.proposeEmptyingDate,
                lastEmptiedDate = currentState.lastEmptiedDate,
                sizeOfContainment = currentState.sizeOfStorageTankM3,
                yearOfInstallation = currentState.constructionYear?.toString(),
                containmentAccessibility = if (currentState.accessibility == true) "Yes" else if (currentState.accessibility == false) "No" else null,
                locationOfContainment = currentState.locationOfContainment,
                pumpingPointPresence = currentState.pumpingPointPresence,
                containmentIssues = currentState.containmentIssues,
                containmentIssuesOther = currentState.containmentIssuesOther,
                extraPaymentRequired = currentState.extraPaymentRequired,
                extraPaymentAmount = currentState.extraPaymentAmount,
                siteVisitRequired = currentState.siteVisitRequired,
                remarks = "",
                estimatedVolume = "",
                syncStatus = "DRAFT"
            )
            
            val repoResult = repository.saveFormDetails(formEntity)
            
            when (repoResult) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _saveResult.send(SaveResult.Success("Draft saved successfully"))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    val message = repoResult.message ?: "Draft saved locally"
                    _saveResult.send(SaveResult.Success(message))
                }
                else -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                }
            }
        }
    }

    // Debug method to manually trigger sync
    fun triggerSync() {
        viewModelScope.launch {
            println("DEBUG: Manually triggering sync...")
            val result = repository.syncPendingForms()
            println("DEBUG: Sync result: $result")
        }
    }

    // Debug method to check sync queue
    fun checkSyncQueue() {
        viewModelScope.launch {
            val pendingSyncs = repository.getPendingSyncs()
            println("DEBUG: Sync queue status:")
            pendingSyncs.forEach { sync ->
                println("  - Entity: ${sync.entityType}, ID: ${sync.entityId}, Retries: ${sync.retryCount}")
            }
        }
    }

}
