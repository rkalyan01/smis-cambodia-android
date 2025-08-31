package com.innovative.smis.data.api

import com.innovative.smis.data.model.response.TodoListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TodoListApiService {

    /**
     * Get all applications
     * Endpoint: /api/application/show
     */
    @GET("application/show")
    suspend fun getAllApplications(): Response<TodoListResponse>

    /**
     * Get applications filtered by date
     * Endpoint: /api/application/show?proposed_emptying_date={date}
     */
    @GET("application/show")
    suspend fun getApplicationsByDate(
        @Query("proposed_emptying_date") date: String
    ): Response<TodoListResponse>

    /**
     * Get applications filtered by status
     * Endpoint: /api/application/show?status={status}
     * Status values: 'Initiated', 'Scheduled', 'Rescheduled', 'Site-Preparation', 'Emptied', 'Completed', 'Pending', 'Cancelled', 'Reassigned'
     */
    @GET("application/show")
    suspend fun getApplicationsByStatus(
        @Query("status") status: String
    ): Response<TodoListResponse>

    /**
     * Get applications filtered by date range
     * Endpoint: /api/application/show?proposed_emptying_date_from={fromDate}&proposed_emptying_date_to={toDate}
     */
    @GET("application/show")
    suspend fun getApplicationsByDateRange(
        @Query("proposed_emptying_date_from") fromDate: String,
        @Query("proposed_emptying_date_to") toDate: String
    ): Response<TodoListResponse>

    /**
     * Get applications filtered by multiple criteria
     * Endpoint: /api/application/show with multiple query parameters
     */
    @GET("applications/filter")
    suspend fun getFilteredApplications(
        @Query("application_status") status: String? = null,
        @Query("eto_id") etoId: String? = null
    ): Response<TodoListResponse>
}