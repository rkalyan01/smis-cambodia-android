package com.innovative.smis.ui.features.additionalrepairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.model.response.TripFilterApplication
import com.innovative.smis.data.repository.AdditionalRepairingRepository
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PreferenceHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AdditionalRepairingListViewModel(
    private val repository: AdditionalRepairingRepository,
    private val preferenceHelper: PreferenceHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdditionalRepairingListUiState())
    val uiState: StateFlow<AdditionalRepairingListUiState> = _uiState.asStateFlow()

    init {
        loadApplications()
    }

    fun refreshList() {
        loadApplications(isRefresh = true)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadApplications(isRefresh: Boolean = false) {
        viewModelScope.launch {
            val etoId = preferenceHelper.getEtoId()?.toString() ?: ""
            
            if (etoId.isEmpty()) {
                _uiState.update {
                    it.copy(
                        listState = Resource.Error("ETO ID not found"),
                        errorMessage = "ETO ID not found. Please login again."
                    )
                }
                return@launch
            }

            repository.getTripFilterApplications(
                applicationStatus = "Emptied",
                etoId = etoId,
                additionalTripRequired = "yes"
            )
                .onStart {
                    if (isRefresh) {
                        _uiState.update { it.copy(isRefreshing = true) }
                    } else {
                        _uiState.update { it.copy(listState = Resource.Loading()) }
                    }
                }
                .onCompletion {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
                .collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            result.data?.let { cachedApplications ->
                                _uiState.update { it.copy(applications = cachedApplications) }
                            }
                        }
                        is Resource.Success -> {
                            val applications = result.data ?: emptyList()
                            _uiState.update {
                                it.copy(
                                    listState = Resource.Success(applications),
                                    applications = applications
                                )
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update {
                                it.copy(
                                    listState = Resource.Error(result.message, result.data),
                                    applications = result.data ?: emptyList(),
                                    errorMessage = result.message
                                )
                            }
                        }
                        is Resource.Idle -> { }
                    }
                }
        }
    }
}

data class AdditionalRepairingListUiState(
    val listState: Resource<List<TripFilterApplication>> = Resource.Idle(),
    val isRefreshing: Boolean = false,
    val applications: List<TripFilterApplication> = emptyList(),
    val errorMessage: String? = null
)
