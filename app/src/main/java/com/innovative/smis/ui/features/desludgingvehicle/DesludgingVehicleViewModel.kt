package com.innovative.smis.ui.features.desludgingvehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.model.response.VehicleResponse
import com.innovative.smis.data.repository.DesludgingVehicleRepository
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.constants.AppConstants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DesludgingVehicleUiState(
    val vehicles: List<VehicleResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isUpdatingVehicle: Boolean = false,
    val updateSuccess: Boolean = false
)

class DesludgingVehicleViewModel(
    private val repository: DesludgingVehicleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DesludgingVehicleUiState())
    val uiState: StateFlow<DesludgingVehicleUiState> = _uiState.asStateFlow()

    init {
        loadVehicles()
    }

    fun loadVehicles() {
        viewModelScope.launch {
            repository.getDesludgingVehicles(AppConstants.ETO_ID.toString()).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = _uiState.value.vehicles.isEmpty(),
                            isRefreshing = _uiState.value.vehicles.isNotEmpty(),
                            errorMessage = null
                        )
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            vehicles = resource.data ?: emptyList(),
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = resource.message
                        )
                    }
                    is Resource.Idle -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    fun refreshVehicles() {
        loadVehicles()
    }

    fun updateVehicleStatus(vehicleId: Int, newStatus: String) {
        viewModelScope.launch {
            repository.updateVehicleStatus(vehicleId, newStatus).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isUpdatingVehicle = true,
                            errorMessage = null,
                            updateSuccess = false
                        )
                    }
                    is Resource.Success -> {
                        // Update the local vehicle list with the new status
                        val updatedVehicles = _uiState.value.vehicles.map { vehicle ->
                            if (vehicle.id == vehicleId) {
                                resource.data ?: vehicle
                            } else {
                                vehicle
                            }
                        }

                        _uiState.value = _uiState.value.copy(
                            vehicles = updatedVehicles,
                            isUpdatingVehicle = false,
                            updateSuccess = true,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isUpdatingVehicle = false,
                            errorMessage = resource.message,
                            updateSuccess = false
                        )
                    }
                    is Resource.Idle -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearUpdateSuccess() {
        _uiState.value = _uiState.value.copy(updateSuccess = false)
    }

    // Helper function to get vehicle statistics
    fun getVehicleStats(): Map<String, Int> {
        val vehicles = _uiState.value.vehicles
        return mapOf(
            "active" to vehicles.count { it.status == "active" },
            "under-maintenance" to vehicles.count { it.status == "under-maintenance" },
            "inactive" to vehicles.count { it.status == "inactive" }
        )
    }
}