package com.innovative.smis.data.repository

import com.innovative.smis.data.api.DesludgingVehicleApiService
import com.innovative.smis.data.model.request.VehicleStatusUpdateRequest
import com.innovative.smis.data.model.response.VehicleResponse
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DesludgingVehicleRepository(
    private val apiService: DesludgingVehicleApiService
) {

    /**
     * Get desludging vehicles for a specific ETO
     */
    suspend fun getDesludgingVehicles(etoId: String): Flow<Resource<List<VehicleResponse>>> = flow {
        try {
            emit(Resource.Loading())
            
            val response = apiService.getDesludgingVehicles(etoId)
            
            if (response.isSuccessful) {
                val vehicleData = response.body()
                if (vehicleData?.vehicles != null) {
                    emit(Resource.Success(vehicleData.vehicles))
                } else {
                    emit(Resource.Error("Failed to load vehicles: No vehicle data received"))
                }
            } else {
                emit(Resource.Error("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred"))
        }
    }

    /**
     * Update vehicle status
     */
    suspend fun updateVehicleStatus(
        vehicleId: Int, 
        status: String
    ): Flow<Resource<VehicleResponse>> = flow {
        try {
            emit(Resource.Loading())
            
            val request = VehicleStatusUpdateRequest(status)
            val response = apiService.updateVehicleStatus(vehicleId, request)
            
            if (response.isSuccessful) {
                val updateResponse = response.body()
                if (updateResponse?.success == true && updateResponse.data != null) {
                    emit(Resource.Success(updateResponse.data))
                } else {
                    emit(Resource.Error(updateResponse?.message ?: "Failed to update vehicle status"))
                }
            } else {
                emit(Resource.Error("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred"))
        }
    }
}