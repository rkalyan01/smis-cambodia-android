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
            _uiState.update { it.copy(isLoading = true) }

            // Load dropdown data first
            loadDropdownData()

            // Try to load existing containment data
            repository.getContainmentStatus(sanitationCustomerId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { containment ->
                            isUpdateMode = true
                            _uiState.update { currentState ->
                                currentState.copy(
                                    toiletConnection = "Storage Tank", // Default value
                                    selectedStorageTypeKey = findKeyByValue(currentState.storageTypeOptions, containment.type_of_storage_tank),
                                    selectedStorageType = containment.type_of_storage_tank ?: "",
                                    otherTypeOfStorageTank = containment.other_type_of_storage_tank ?: "",
                                    selectedStorageConnectionKey = findKeyByValue(currentState.storageConnectionOptions, containment.storage_tank_connection),
                                    selectedStorageConnection = containment.storage_tank_connection ?: "",
                                    otherStorageTankConnection = containment.other_storage_tank_connection ?: "",
                                    sizeOfStorageTankM3 = containment.size_of_storage_tank_m3 ?: "",
                                    constructionYear = containment.construction_year ?: "",
                                    accessibilityKey = containment.accessibility?.lowercase() ?: "",
                                    accessibility = containment.accessibility ?: "",
                                    everEmptiedKey = containment.ever_emptied?.lowercase() ?: "",
                                    everEmptied = containment.ever_emptied ?: "",
                                    lastEmptiedYear = containment.last_emptied_year ?: "",
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

    private fun findKeyByValue(options: Map<String, String>, value: String?): String {
        return options.entries.find { it.value == value }?.key ?: ""
    }

    private fun loadDropdownData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDropdowns = true) }

            // Load storage types
            repository.getStorageTypes().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                storageTypeOptions = result.data ?: emptyMap(),
                                isLoadingDropdowns = false
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                errorMessage = result.message,
                                isLoadingDropdowns = false
                            )
                        }
                    }
                    else -> {}
                }
            }

            // Load storage connections
            repository.getStorageConnections().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                storageConnectionOptions = result.data ?: emptyMap()
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(errorMessage = result.message)
                        }
                    }
                    else -> {}
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

            val result = if (isUpdateMode) {
                repository.updateContainment(currentSanitationCustomerId, currentState)
            } else {
                repository.createContainment(currentSanitationCustomerId, currentState)
            }

            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    val message = result.message ?: "Containment updated successfully"
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