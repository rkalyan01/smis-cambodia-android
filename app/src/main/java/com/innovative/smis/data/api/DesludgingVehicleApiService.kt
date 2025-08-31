package com.innovative.smis.data.api

import com.innovative.smis.data.model.request.VehicleStatusUpdateRequest
import com.innovative.smis.data.model.response.DesludgingVehicleListResponse
import com.innovative.smis.data.model.response.VehicleStatusUpdateResponse
import retrofit2.Response
import retrofit2.http.*

interface DesludgingVehicleApiService {

    /**
     * Get desludging vehicles for a specific ETO
     * GET /api/desludging-vehicle/{eto_id}
     */
    @GET("desludging-vehicle/{eto_id}")
    suspend fun getDesludgingVehicles(
        @Path("eto_id") etoId: String
    ): Response<DesludgingVehicleListResponse>

    /**
     * Update vehicle status
     * PATCH /api/desludging-vehicle/{vehicle_id}
     */
    @PATCH("desludging-vehicle/{vehicle_id}")
    suspend fun updateVehicleStatus(
        @Path("vehicle_id") vehicleId: Int,
        @Body request: VehicleStatusUpdateRequest
    ): Response<VehicleStatusUpdateResponse>
}