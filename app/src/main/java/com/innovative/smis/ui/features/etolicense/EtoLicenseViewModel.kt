package com.innovative.smis.ui.features.etolicense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.model.response.EtoLicenseData
import com.innovative.smis.data.repository.EtoLicenseRepository
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PreferenceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EtoLicenseUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val etoList: List<EtoLicenseData> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

class EtoLicenseViewModel(
    private val repository: EtoLicenseRepository,
    private val preferenceHelper: PreferenceHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(EtoLicenseUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val etoId = preferenceHelper.getEtoId()?.toString()

        if (etoId == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                errorMessage = "ETO ID not found. Please login again."
            )
            return
        }

        viewModelScope.launch {
            repository.getEtoLicenseStatus(etoId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                etoList = result.data?.data ?: emptyList(),
                                message = result.data?.message
                            ) 
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }
}