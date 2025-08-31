package com.innovative.smis.data.api

import com.innovative.smis.data.model.response.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    /**
     * Get filtered applications for dashboard
     * GET /api/applications/filter
     */
    @GET("applications/filter")
    suspend fun getFilteredApplications(
        @Query("application_status") status: String? = null,
        @Query("eto_id") etoId: String? = null
    ): Response<ApplicationListResponse>
    
    /**
     * Get readonly data for dashboard stats
     * GET /api/emptyings/readonly-data
     */
    @GET("emptyings/readonly-data")
    suspend fun getEmptyingReadonlyData(): Response<EmptyingDashboardDataResponse>
    
    /**
     * Get sludge collection readonly data
     * GET /api/sludge-collections/readonly-data
     */
    @GET("sludge-collections/readonly-data")
    suspend fun getSludgeReadonlyData(): Response<EmptyingReadonlyDataResponse>
    
    /**
     * Get ETO names for dropdowns
     * GET /api/sludge-collections/get-eto-names
     */
    @GET("sludge-collections/get-eto-names")
    suspend fun getEtoNames(): Response<EtoNamesResponse>
    
    /**
     * Get truck numbers for dropdowns
     * GET /api/sludge-collections/get-truck-numbers
     */
    @GET("sludge-collections/get-truck-numbers")
    suspend fun getTruckNumbers(): Response<TruckNumbersResponse>
}