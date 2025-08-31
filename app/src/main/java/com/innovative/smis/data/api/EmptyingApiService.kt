package com.innovative.smis.data.api

import com.innovative.smis.data.api.request.EmptyingServiceRequest
import com.innovative.smis.data.model.response.*
import retrofit2.Response
import retrofit2.http.*

interface EmptyingApiService {

    /**
     * Get sanitation customer details for an application
     * GET /api/emptying-scheduling/sanitation-customer-details/{application_id}
     */
    @GET("emptying-scheduling/sanitation-customer-details/{application_id}")
    suspend fun getSanitationCustomerDetails(
        @Path("application_id") applicationId: String
    ): Response<SanitationCustomerResponse>

    /**
     * Get purpose of emptying options dynamically from API
     * GET /api/emptying-scheduling/show-emptying-reason
     */
    @GET("emptying-scheduling/show-emptying-reason")
    suspend fun getEmptyingPurposes(): Response<EmptyingPurposeResponse>

    /**
     * Get experience issues with containment options
     * GET /api/emptying-scheduling/show-issue-with-containment
     */
    @GET("emptying-scheduling/show-issue-with-containment")
    suspend fun getExperienceIssues(): Response<ExperienceIssuesResponse>

    /**
     * Submit emptying form (Emptying Scheduling)
     * POST /api/emptying-scheduling
     */
    @POST("emptying-scheduling")
    suspend fun submitEmptyingForm(
        @Body request: EmptyingFormRequest
    ): Response<EmptyingFormResponse>

    /**
     * Update existing emptying form
     * PUT /api/emptying-scheduling/{id}
     */
    @PUT("emptying-scheduling/{id}")
    suspend fun updateEmptyingForm(
        @Path("id") applicationId: String,
        @Body request: EmptyingFormRequest
    ): Response<EmptyingFormResponse>

    /**
     * Get filtered applications for emptying
     * GET /api/emptying-scheduling/filter
     */
    @GET("emptying-scheduling/emptying-service-filter")
    suspend fun getEmptyingServiceApplications(
        @Query("eto_id") etoId: String? = null
    ): Response<com.innovative.smis.data.model.response.ApplicationListResponse>

    /**
     * Submit site preparation form
     * POST /api/site-preparation
     */
    @POST("site-preparation")
    suspend fun submitSitePreparation(
        @Body request: EmptyingFormRequest
    ): Response<EmptyingFormResponse>

    /**
     * Submit emptying service record
     * POST /api/emptyings
     */
    @POST("emptyings")
    suspend fun submitEmptyingService(
        @Body request: EmptyingServiceRequest
    ): Response<EmptyingFormResponse>

    /**
     * Get readonly data for emptying forms
     * GET /api/emptyings/readonly-data
     */
    @GET("emptyings/readonly-data")
    suspend fun getEmptyingReadonlyData(): Response<com.innovative.smis.data.model.response.EmptyingDashboardDataResponse>

    /**
     * Get ETO names for selection
     * GET /api/sludge-collections/get-eto-names
     */
    @GET("sludge-collections/get-eto-names")
    suspend fun getEtoNames(): Response<com.innovative.smis.data.model.response.EtoNamesResponse>

    /**
     * Get truck numbers for selection
     * GET /api/sludge-collections/get-truck-numbers
     */
    @GET("sludge-collections/get-truck-numbers")
    suspend fun getTruckNumbers(): Response<com.innovative.smis.data.model.response.TruckNumbersResponse>
}

