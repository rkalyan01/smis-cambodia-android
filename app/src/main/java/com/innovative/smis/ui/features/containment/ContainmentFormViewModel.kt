package com.innovative.smis.ui.features.containment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.repository.ContainmentRepository
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ContainmentFormViewModel(
    private val repository: ContainmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContainmentFormUiState())
    val uiState: StateFlow<ContainmentFormUiState> = _uiState.asStateFlow()

    private val _saveResult = Channel<SaveResult>()
    val saveResult = _saveResult.receiveAsFlow()

    private var currentSanitationCustomerId: String = ""
    private var isUpdateMode: Boolean = false

    fun loadContainmentData(sanitationCustomerId: String) {
        currentSanitationCustomerId = sanitationCustomerId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isLoadingDropdowns = true) }

            // Load dropdown data first and wait for completion
            val storageTypes = repository.getStorageTypes().first { it !is Resource.Loading }
            val storageConnections = repository.getStorageConnections().first { it !is Resource.Loading }
            
            // Update UI with dropdown options
            if (storageTypes is Resource.Success) {
                _uiState.update { it.copy(storageTypeOptions = storageTypes.data ?: emptyMap()) }
            }
            if (storageConnections is Resource.Success) {
                _uiState.update { it.copy(storageConnectionOptions = storageConnections.data ?: emptyMap()) }
            }
            _uiState.update { it.copy(isLoadingDropdowns = false) }

            // Now load existing containment data and map keys to values
            repository.getContainmentStatus(sanitationCustomerId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { containment ->
                            isUpdateMode = true
                            _uiState.update { currentState ->
                                currentState.copy(
                                    toiletConnection = "Storage Tank", // Default value
                                    selectedStorageTypeKey = containment.type_of_storage_tank ?: "",
                                    selectedStorageType = currentState.storageTypeOptions[containment.type_of_storage_tank] ?: "",
                                    otherTypeOfStorageTank = containment.other_type_of_storage_tank ?: "",
                                    selectedStorageConnectionKey = containment.storage_tank_connection ?: "",
                                    selectedStorageConnection = currentState.storageConnectionOptions[containment.storage_tank_connection] ?: "",
                                    otherStorageTankConnection = containment.other_storage_tank_connection ?: "",
                                    sizeOfStorageTankM3 = containment.size_of_storage_tank_m3 ?: "",
                                    constructionYear = containment.construction_year?.toString() ?: "",
                                    accessibilityKey = containment.accessibility?.let { if (it) "yes" else "no" } ?: "",
                                    accessibility = containment.accessibility?.let { if (it) "Yes" else "No" } ?: "",
                                    everEmptiedKey = containment.ever_emptied?.let { if (it) "yes" else "no" } ?: "",
                                    everEmptied = containment.ever_emptied?.let { if (it) "Yes" else "No" } ?: "",
                                    lastEmptiedYear = containment.last_emptied_year?.toString() ?: "",
                                    hasExistingData = true,
                                    isLoading = false
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        // Containment not found - create mode
                        isUpdateMode = false
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is Resource.Loading -> {
                        // Loading state already set
                    }
                    is Resource.Idle -> {
                        // Idle state - do nothing
                    }
                }
            }
        }
    }

    // UI Event Handlers
    fun onToiletConnectionChange(value: String) {
        _uiState.update { it.copy(toiletConnection = value) }
    }

    fun onStorageTypeSelected(key: String, value: String) {
        _uiState.update {
            it.copy(
                selectedStorageTypeKey = key,
                selectedStorageType = value,
                // Clear other field when different option selected
                otherTypeOfStorageTank = if (value != "Other") "" else it.otherTypeOfStorageTank
            )
        }
    }

    fun onOtherStorageTypeChange(value: String) {
        _uiState.update { it.copy(otherTypeOfStorageTank = value) }
    }

    fun onStorageConnectionSelected(key: String, value: String) {
        _uiState.update {
            it.copy(
                selectedStorageConnectionKey = key,
                selectedStorageConnection = value,
                // Clear other field when different option selected
                otherStorageTankConnection = if (value != "Other") "" else it.otherStorageTankConnection
            )
        }
    }

    fun onOtherStorageConnectionChange(value: String) {
        _uiState.update { it.copy(otherStorageTankConnection = value) }
    }

    fun onSizeOfStorageTankM3Change(value: String) {
        _uiState.update { it.copy(sizeOfStorageTankM3 = value) }
    }

    fun onConstructionYearChange(value: String) {
        _uiState.update { it.copy(constructionYear = value) }
    }

    fun onAccessibilitySelected(key: String, value: String) {
        _uiState.update {
            it.copy(
                accessibilityKey = key,
                accessibility = value
            )
        }
    }

    fun onEverEmptiedSelected(key: String, value: String) {
        _uiState.update {
            it.copy(
                everEmptiedKey = key,
                everEmptied = value,
                // Clear last emptied year when "No" is selected
                lastEmptiedYear = if (key != "yes") "" else it.lastEmptiedYear
            )
        }
    }

    fun onLastEmptiedYearChange(value: String) {
        _uiState.update { it.copy(lastEmptiedYear = value) }
    }

    fun submitForm() {
        val currentState = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            // Repository will check if containment exists and either create or update accordingly
            val result = repository.saveContainment(
                sanitationCustomerId = currentSanitationCustomerId,
                applicationId = 0, // Application ID not used for containment
                formData = currentState
            )

            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    val message = result.message ?: "Containment saved successfully"
                    _saveResult.send(SaveResult.Success(message, shouldRefreshList = true))
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.message
                        )
                    }
                    _saveResult.send(SaveResult.Error(result.message ?: "Unknown error"))
                }
                else -> {}
            }
        }
    }
}

sealed class SaveResult {
    data class Success(val message: String, val shouldRefreshList: Boolean = false) : SaveResult()
    data class Error(val message: String) : SaveResult()
}