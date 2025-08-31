package com.innovative.smis.data.api

import com.innovative.smis.data.model.response.TodoListResponse
import com.innovative.smis.data.model.response.SitePreparationCustomerDetailsResponse
import com.innovative.smis.data.model.request.SitePreparationFormRequest
import com.innovative.smis.data.model.response.SimpleDropdownResponse
import com.innovative.smis.util.constants.AppConstants
import retrofit2.Response
import retrofit2.http.*

interface SitePreparationApiService {

    /**
     * Get filtered site preparation applications
     * Default to Site-Preparation status and eto_id=2 as per requirements
     */
    @GET("site-preparation/filter")
    suspend fun getSitePreparationApplications(
        @Query("application_status") status: String = "Scheduled",
        @Query("site_visit_required") siteVisitRequired: String = "yes",
        @Query("eto_id") etoId: Int
    ): Response<TodoListResponse>

    /**
     * Get applications filtered by status and eto_id
     */
    @GET("applications/filter")
    suspend fun getApplicationsByStatus(
        @Query("application_status") status: String,
        @Query("eto_id") etoId: Int = 2
    ): Response<TodoListResponse>

    /**
     * Get site preparation applications specifically
     */
    @GET("applications/filter")
    suspend fun getSitePreparationApplicationsSpecific(
        @Query("application_status") status: String = "Site-Preparation",
        @Query("eto_id") etoId: Int = 2
    ): Response<TodoListResponse>

    /**
     * Get sanitation customer details for site preparation
     */
    @GET("site-preparation/sanitation-customer-details/{id}")
    suspend fun getSanitationCustomerDetails(@Path("id") applicationId: Int): Response<SitePreparationCustomerDetailsResponse>

    /**
     * Get emptying reasons for Purpose of Emptying Request dropdown
     */
    @GET("site-preparation/show-emptying-reason")
    suspend fun getEmptyingReasons(): Response<SimpleDropdownResponse>

    /**
     * Get containment issues for dropdown
     */
    @GET("site-preparation/show-issue-with-containment")
    suspend fun getContainmentIssues(): Response<SimpleDropdownResponse>

    /**
     * Submit site preparation form data (uses POST for creation as per Laravel API)
     */
    @POST("site-preparation")
    suspend fun createSitePreparation(
        @Body request: SitePreparationFormRequest
    ): Response<SimpleDropdownResponse>

    /**
     * Update existing site preparation form data
     */
    @PUT("site-preparation/{id}")
    suspend fun updateSitePreparation(
        @Path("id") applicationId: Int,
        @Body request: SitePreparationFormRequest
    ): Response<SimpleDropdownResponse>

    /**
     * Update site preparation form with all required fields (FIXED ENDPOINT)
     */
    @PATCH("site-preparation/{application_id}")
    suspend fun updateSitePreparationForm(
        @Path("application_id") applicationId: Int,
        @Body request: SitePreparationFormRequest
    ): Response<SimpleDropdownResponse>
}