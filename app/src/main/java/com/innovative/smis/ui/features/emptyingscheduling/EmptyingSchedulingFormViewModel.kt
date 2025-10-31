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
    val applicationType: String? = null,
    val sanitationCustomerId: String? = null,
    val sanitationCustomerName: String? = null,
    val sanitationCustomerContact: String? = null,
    val applicantName: String = "",
    val applicantContact: String = "",
    val isApplicantSameAsCustomer: Boolean = false,
    val purposeOfEmptying: String? = null, // ✅ Nullable - null means no selection
    val purposeOfEmptyingOther: String = "",
    val isPurposeOfEmptyingReadonly: Boolean = false, // ✅ Readonly flag for purpose field
    val containmentIssuesOther: String = "",
    val proposeEmptyingDate: Long? = null,
    val everEmptied: Boolean? = null,
    val lastEmptiedYear: Int? = null,
    val lastEmptiedDate: String = "",
    val notEmptiedBeforeReason: String = "",
    val reasonForNoEmptiedDate: String = "",
    val freeServiceUnderPBC: Boolean? = null,
    val sizeOfStorageTankM3: String? = null,
    val constructionYear: Int? = null,
    val accessibility: String? = null,
    val locationOfContainment: String? = null,
    val pumpingPointPresence: Boolean? = null,
    val pumpingPointDetails: String = "",
    val containmentIssues: String = "",
    val extraPaymentRequired: Boolean? = null,
    val extraPaymentAmount: String = "",
    val amountOfRegularPayment: String = "",
    val siteVisitRequired: Boolean? = null,
    
    // Dropdown data
    val emptyingReasons: Map<String, String> = emptyMap(),
    val containmentIssuesList: Map<String, String> = emptyMap(),
    val emptiedNoDateReasons: Map<String, String> = emptyMap(),
    val notEmptiedReasons: Map<String, String> = emptyMap(),
    val notEmptiedReasonOther: String = "",
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
    
    /**
     * Smart year expansion: Converts truncated years to 4-digit years
     * - 0-9 → 2000-2009
     * - 10-30 → 2010-2030
     * - 31-99 → 1931-1999
     * - 1000+ → unchanged
     */
    private fun expandYear(year: Int?): Int? {
        return year?.let {
            when {
                it >= 1000 -> it // Already 4 digits
                it in 0..9 -> 2000 + it // 0-9 → 2000-2009
                it in 10..30 -> 2000 + it // 10-30 → 2010-2030
                it in 31..99 -> 1900 + it // 31-99 → 1931-1999
                else -> it
            }
        }
    }
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
            
            // Load emptied no date reasons
            repository.getEmptiedNoDateReasons().collect { reasonsResult ->
                when (reasonsResult) {
                    is Resource.Success -> {
                        _uiState.update { 
                            it.copy(emptiedNoDateReasons = reasonsResult.data ?: emptyMap()) 
                        }
                    }
                    else -> {
                        // Handle error silently, keep empty map
                    }
                }
            }
            
            // Load not emptied reasons
            repository.getNotEmptiedReasons().collect { reasonsResult ->
                when (reasonsResult) {
                    is Resource.Success -> {
                        _uiState.update { 
                            it.copy(notEmptiedReasons = reasonsResult.data ?: emptyMap()) 
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

    /**
     * ✅ READONLY FIELD PATTERN: Load readonly fields from API
     * This ensures that readonly fields always show authoritative API data,
     * even if the local draft has different values saved.
     */
    private fun loadReadonlyDataFromApi(applicationId: Int) {
        viewModelScope.launch {
            repository.getSanitationCustomerDetails(applicationId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.data?.let { apiData ->
                            println("DEBUG: Reloading readonly fields from API:")
                            println("  Application Type (from API): '${apiData.applicationType}'")
                            println("  Purpose Of Emptying (from API): '${apiData.purposeOfEmptying}'")
                            println("  Applicant Name (from API): '${apiData.applicantName}'")
                            println("  Applicant Contact (from API): '${apiData.applicantContact}'")
                            
                            // ✅ Update ONLY readonly fields with fresh API data
                            _uiState.update { currentState ->
                                currentState.copy(
                                    // Readonly fields - ALWAYS from API
                                    applicationType = apiData.applicationType,
                                    purposeOfEmptying = apiData.purposeOfEmptying, // ✅ Preserve null
                                    applicantName = apiData.applicantName ?: "",
                                    applicantContact = apiData.applicantContact ?: "",
                                    isPurposeOfEmptyingReadonly = !apiData.purposeOfEmptying.isNullOrBlank()
                                )
                            }
                            
                            println("DEBUG: Readonly fields updated. applicationType = ${apiData.applicationType}, isPurposeOfEmptyingReadonly = ${!apiData.purposeOfEmptying.isNullOrBlank()}")
                        }
                    }
                    is Resource.Error -> {
                        Log.d("EmptyingSchedulingVM", "Failed to reload readonly data from API: ${result.message}")
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun initializeForm(applicationId: Int) {        
        viewModelScope.launch {
            repository.getFormDetails(applicationId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(loadingState = result) }
                        result.data?.let { entity ->
                            currentFormId = entity.applicationId.toString()
                            
                            // Debug logging to check what data is loaded
                            println("DEBUG: Loading form data from local database:")
                            println("  Purpose Of Emptying (from draft): '${entity.purposeOfEmptying}'")
                            println("  Customer Name: '${entity.sanitationCustomerName}'")
                            println("  Customer Contact: '${entity.sanitationCustomerContact}'")
                            println("  Customer Address: '${entity.sanitationCustomerAddress}'")
                            println("  Applicant Name: '${entity.applicantName}'")
                            println("  Applicant Contact: '${entity.applicantContact}'")
                            println("  Is Applicant Same As Customer: ${entity.isApplicantSameAsCustomer}")
                            
                            _uiState.update {
                                it.copy(
                                    applicationDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
                                    applicationType = entity.applicationType,
                                    sanitationCustomerId = entity.sanitationCustomerId,
                                    sanitationCustomerName = entity.sanitationCustomerName,
                                    sanitationCustomerContact = entity.sanitationCustomerContact,
                                    applicantName = entity.applicantName ?: "",
                                    applicantContact = entity.applicantContact ?: "",
                                    isApplicantSameAsCustomer = entity.isApplicantSameAsCustomer ?: false,
                                    freeServiceUnderPBC = entity.freeServiceUnderPbc,
                                    everEmptied = entity.everEmptied,
                                    lastEmptiedYear = expandYear(entity.lastEmptiedYear?.toIntOrNull()),
                                    lastEmptiedDate = entity.lastEmptiedDate?.let { 
                                        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(it)) 
                                    } ?: "",
                                    notEmptiedBeforeReason = entity.notEmptiedBeforeReason ?: "",
                                    notEmptiedReasonOther = entity.notEmptiedBeforeReasonOther ?: "",
                                    reasonForNoEmptiedDate = entity.emptiedNodateReason ?: "",
                                    purposeOfEmptying = entity.purposeOfEmptying, // ✅ Keep null as null - don't convert to ""
                                    purposeOfEmptyingOther = entity.purposeOfEmptyingOther ?: "",
                                    isPurposeOfEmptyingReadonly = false, // ✅ FIXED: Always false initially, will be set by API
                                    proposeEmptyingDate = entity.proposedEmptyingDate,
                                    sizeOfStorageTankM3 = entity.sizeOfContainment,
                                    constructionYear = expandYear(entity.yearOfInstallation?.toIntOrNull()),
                                    accessibility = when(entity.containmentAccessibility) {
                                        "Yes" -> "Accessible"
                                        "No" -> "Not Accessible"
                                        else -> null
                                    },
                                    locationOfContainment = entity.locationOfContainment,
                                    pumpingPointPresence = entity.pumpingPointPresence,
                                    containmentIssues = entity.containmentIssues ?: "",
                                    containmentIssuesOther = entity.containmentIssuesOther ?: "",
                                    extraPaymentRequired = entity.extraPaymentRequired,
                                    extraPaymentAmount = entity.extraPaymentAmount ?: "",
                                    amountOfRegularPayment = entity.amountOfRegularPayment ?: "",
                                    siteVisitRequired = entity.siteVisitRequired
                                )
                            }
                            
                            // ✅ READONLY FIELD PATTERN: Reload readonly fields from API to ensure authoritative data
                            loadReadonlyDataFromApi(applicationId)
                        }
                    }
                    is Resource.Error -> {
                        // For 404 errors (customer not found), show empty form to create new record
                        Log.d("EmptyingSchedulingVM", "API error (${result.message}), initializing empty form for new customer entry")
                        _uiState.update { 
                            it.copy(
                                loadingState = Resource.Idle(),
                                applicationDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
                                sanitationCustomerId = "",
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
    
    fun onNotEmptiedReasonOtherChange(other: String) {
        _uiState.update { it.copy(notEmptiedReasonOther = other) }
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
        // Store raw value, expansion happens on submission
        _uiState.update { it.copy(lastEmptiedYear = year) }
        autoSaveDraft()
    }
    fun onReasonForNoEmptiedDateChange(reason: String) { 
        _uiState.update { it.copy(reasonForNoEmptiedDate = reason, lastEmptiedDate = "") }
        autoSaveDraft()
    }
    fun onSizeOfContainmentChange(size: String) { 
        _uiState.update { it.copy(sizeOfStorageTankM3 = size) }
        autoSaveDraft()
    }
    fun onConstructionYearChange(year: Int?) { 
        // Store raw value, expansion happens on submission
        _uiState.update { it.copy(constructionYear = year) }
        autoSaveDraft()
    }
    fun onAccessibilityChange(accessibility: String) { 
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
    fun onAmountOfRegularPaymentChange(amount: String) { 
        _uiState.update { it.copy(amountOfRegularPayment = amount) }
        autoSaveDraft()
    }
    fun onSiteVisitRequiredChange(isRequired: Boolean) { 
        _uiState.update { it.copy(siteVisitRequired = isRequired) }
        autoSaveDraft()
    }
    
    fun onLastEmptiedDateChange(date: String) {
        _uiState.update { it.copy(lastEmptiedDate = date, reasonForNoEmptiedDate = "") }
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

    private fun convertDateStringToTimestamp(dateString: String): Long? {
        return if (dateString.isNotBlank()) {
            try {
                // Extract year from date string and expand if needed
                val parts = dateString.split("-")
                if (parts.size == 3) {
                    val day = parts[0]
                    val month = parts[1]
                    val yearStr = parts[2]
                    
                    // Expand 2-digit year to 4-digit year
                    val expandedYear = if (yearStr.length <= 2) {
                        val year = yearStr.toIntOrNull() ?: return null
                        expandYear(year) ?: year
                    } else {
                        yearStr.toIntOrNull() ?: return null
                    }
                    
                    // Create date string with expanded year
                    val expandedDateString = "$day-$month-$expandedYear"
                    SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(expandedDateString)?.time
                } else {
                    SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(dateString)?.time
                }
            } catch (e: Exception) {
                null
            }
        } else null
    }

    fun autoSaveDraft() {
        viewModelScope.launch {
            currentFormId?.let { formId ->
                val currentState = _uiState.value
                val originalEntity = uiState.value.loadingState.data

                val draftEntity = EmptyingSchedulingFormEntity(
                    applicationId = currentApplicationId,
                    createdBy = null,
                    sanitationCustomerId = currentState.sanitationCustomerId,
                    sanitationCustomerName = currentState.sanitationCustomerName,
                    sanitationCustomerContact = currentState.sanitationCustomerContact,
                    sanitationCustomerAddress = null, // Not available in current UI state
                    pbcCustomerType = originalEntity?.pbcCustomerType,
                    freeServiceUnderPbc = currentState.freeServiceUnderPBC,
                    applicantName = currentState.applicantName,
                    applicantContact = currentState.applicantContact,
                    isApplicantSameAsCustomer = currentState.isApplicantSameAsCustomer,
                    lastEmptiedYear = expandYear(currentState.lastEmptiedYear)?.toString(),
                    everEmptied = currentState.everEmptied,
                    emptiedNodateReason = if (currentState.everEmptied == true && currentState.lastEmptiedDate.isBlank()) currentState.reasonForNoEmptiedDate else null,
                    notEmptiedBeforeReason = if (currentState.everEmptied == false) currentState.reasonForNoEmptiedDate else null,
                    notEmptiedBeforeReasonOther = if (currentState.everEmptied == false) currentState.notEmptiedReasonOther else null,
                    purposeOfEmptying = currentState.purposeOfEmptying,
                    purposeOfEmptyingOther = currentState.purposeOfEmptyingOther,
                    proposedEmptyingDate = currentState.proposeEmptyingDate,
                    lastEmptiedDate = convertDateStringToTimestamp(currentState.lastEmptiedDate),
                    sizeOfContainment = currentState.sizeOfStorageTankM3,
                    yearOfInstallation = expandYear(currentState.constructionYear)?.toString(),
                    containmentAccessibility = when(currentState.accessibility) {
                    "Accessible" -> "Yes"
                    "Not Accessible" -> "No"
                    else -> null
                },
                    locationOfContainment = currentState.locationOfContainment,
                    pumpingPointPresence = currentState.pumpingPointPresence,
                    containmentIssues = currentState.containmentIssues,
                    containmentIssuesOther = currentState.containmentIssuesOther,
                    extraPaymentRequired = currentState.extraPaymentRequired,
                    extraPaymentAmount = currentState.extraPaymentAmount,
                    amountOfRegularPayment = currentState.amountOfRegularPayment,
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

            
            val formEntity = EmptyingSchedulingFormEntity(
                applicationId = currentApplicationId,
                createdBy = null,
                applicationType = currentState.applicationType,
                sanitationCustomerId = currentState.sanitationCustomerId,
                sanitationCustomerName = currentState.sanitationCustomerName,
                sanitationCustomerContact = currentState.sanitationCustomerContact,
                sanitationCustomerAddress = null, // Not available in current UI state
                pbcCustomerType = originalEntity?.pbcCustomerType,
                freeServiceUnderPbc = currentState.freeServiceUnderPBC,
                applicantName = currentState.applicantName,
                applicantContact = currentState.applicantContact,
                isApplicantSameAsCustomer = currentState.isApplicantSameAsCustomer,
                lastEmptiedYear = expandYear(currentState.lastEmptiedYear)?.toString(),
                everEmptied = currentState.everEmptied,
                emptiedNodateReason = if (currentState.everEmptied == true && currentState.lastEmptiedDate.isBlank()) currentState.reasonForNoEmptiedDate else null,
                notEmptiedBeforeReason = if (currentState.everEmptied == false) currentState.reasonForNoEmptiedDate else null,
                notEmptiedBeforeReasonOther = if (currentState.everEmptied == false) currentState.notEmptiedReasonOther else null,
                purposeOfEmptying = currentState.purposeOfEmptying,
                purposeOfEmptyingOther = currentState.purposeOfEmptyingOther,
                proposedEmptyingDate = currentState.proposeEmptyingDate,
                lastEmptiedDate = convertDateStringToTimestamp(currentState.lastEmptiedDate),
                sizeOfContainment = currentState.sizeOfStorageTankM3,
                yearOfInstallation = expandYear(currentState.constructionYear)?.toString(),
                containmentAccessibility = when(currentState.accessibility) {
                    "Accessible" -> "Yes"
                    "Not Accessible" -> "No"
                    else -> null
                },
                locationOfContainment = currentState.locationOfContainment,
                pumpingPointPresence = currentState.pumpingPointPresence,
                containmentIssues = currentState.containmentIssues,
                containmentIssuesOther = currentState.containmentIssuesOther,
                extraPaymentRequired = currentState.extraPaymentRequired,
                extraPaymentAmount = currentState.extraPaymentAmount,
                amountOfRegularPayment = currentState.amountOfRegularPayment,
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

            
            val formEntity = EmptyingSchedulingFormEntity(
                applicationId = currentApplicationId,
                createdBy = null,
                applicationType = currentState.applicationType,
                sanitationCustomerId = currentState.sanitationCustomerId,
                sanitationCustomerName = currentState.sanitationCustomerName,
                sanitationCustomerContact = currentState.sanitationCustomerContact,
                sanitationCustomerAddress = null, // Not available in current UI state
                pbcCustomerType = originalEntity?.pbcCustomerType,
                freeServiceUnderPbc = currentState.freeServiceUnderPBC,
                applicantName = currentState.applicantName,
                applicantContact = currentState.applicantContact,
                isApplicantSameAsCustomer = currentState.isApplicantSameAsCustomer,
                lastEmptiedYear = expandYear(currentState.lastEmptiedYear)?.toString(),
                everEmptied = currentState.everEmptied,
                emptiedNodateReason = if (currentState.everEmptied == true && currentState.lastEmptiedDate.isBlank()) currentState.reasonForNoEmptiedDate else null,
                notEmptiedBeforeReason = if (currentState.everEmptied == false) currentState.reasonForNoEmptiedDate else null,
                notEmptiedBeforeReasonOther = if (currentState.everEmptied == false) currentState.notEmptiedReasonOther else null,
                purposeOfEmptying = currentState.purposeOfEmptying,
                purposeOfEmptyingOther = currentState.purposeOfEmptyingOther,
                proposedEmptyingDate = currentState.proposeEmptyingDate,
                lastEmptiedDate = convertDateStringToTimestamp(currentState.lastEmptiedDate),
                sizeOfContainment = currentState.sizeOfStorageTankM3,
                yearOfInstallation = expandYear(currentState.constructionYear)?.toString(),
                containmentAccessibility = when(currentState.accessibility) {
                    "Accessible" -> "Yes"
                    "Not Accessible" -> "No"
                    else -> null
                },
                locationOfContainment = currentState.locationOfContainment,
                pumpingPointPresence = currentState.pumpingPointPresence,
                containmentIssues = currentState.containmentIssues,
                containmentIssuesOther = currentState.containmentIssuesOther,
                extraPaymentRequired = currentState.extraPaymentRequired,
                extraPaymentAmount = currentState.extraPaymentAmount,
                amountOfRegularPayment = currentState.amountOfRegularPayment,
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
