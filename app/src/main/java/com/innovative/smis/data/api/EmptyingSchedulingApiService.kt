package com.innovative.smis.data.api

import com.innovative.smis.data.model.response.TodoListResponse
import com.innovative.smis.data.model.response.SimpleDropdownResponse
import com.innovative.smis.data.model.response.SimpleApiResponse
import com.innovative.smis.data.model.request.EmptyingSchedulingFormRequest
import com.innovative.smis.data.model.response.SanitationCustomerResponse
import com.innovative.smis.util.constants.AppConstants
import retrofit2.Response
import retrofit2.http.*

interface EmptyingSchedulingApiService {

    /**
     * Get filtered emptying scheduling applications
     * Default to Initiated status and eto_id=2 as per requirements
     */
    @GET("emptying-scheduling/filter")
    suspend fun getEmptyingSchedulingApplications(
        @Query("application_status") status: String = "Initiated",
        @Query("eto_id") etoId: Int
    ): Response<TodoListResponse>

    /**
     * Get containment information for emptying scheduling
     */
    @GET("emptying-scheduling/show-containment")
    suspend fun getShowContainment(): Response<TodoListResponse>

    /**
     * Get applications filtered by status and eto_id
     */
    @GET("emptying-scheduling/filter")
    suspend fun getApplicationsByStatus(
        @Query("application_status") status: String,
        @Query("eto_id") etoId: Int = 2
    ): Response<TodoListResponse>

    /**
     * Get initiated applications specifically
     */
    @GET("emptying-scheduling/filter")
    suspend fun getInitiatedApplications(
        @Query("application_status") status: String = "Initiated",
        @Query("eto_id") etoId: Int = 2
    ): Response<TodoListResponse>

    @GET("emptying-scheduling/sanitation-customer-details/{id}")
    suspend fun getSanitationCustomerDetails(@Path("id") applicationId: Int): Response<SanitationCustomerResponse>

    /**
     * Get emptying reasons for Purpose of Emptying Request dropdown
     */
    @GET("emptying-scheduling/show-emptying-reason")
    suspend fun getEmptyingReasons(): Response<SimpleDropdownResponse>

    /**
     * Get containment issues for Experience issues dropdown
     */
    @GET("emptying-scheduling/show-issue-with-containment")
    suspend fun getContainmentIssues(): Response<SimpleDropdownResponse>

    /**
     * Updates the details for a specific emptying scheduling application.
     * Corresponds to: PUT /api/emptying-scheduling/{id}
     */
    @PUT("emptying-scheduling/{id}")
    suspend fun updateEmptyingScheduling(
        @Path("id") applicationId: Int,
        @Body details: EmptyingSchedulingFormRequest
    ): Response<SimpleApiResponse>
}