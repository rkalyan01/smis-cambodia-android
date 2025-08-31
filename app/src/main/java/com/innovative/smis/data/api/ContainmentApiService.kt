package com.innovative.smis.data.api

import com.innovative.smis.data.model.response.StorageTypeResponse
import com.innovative.smis.data.model.response.StorageConnectionResponse
import com.innovative.smis.data.model.response.ContainmentStatusResponse
import com.innovative.smis.data.model.request.ContainmentRequest
import retrofit2.Response
import retrofit2.http.*

interface ContainmentApiService {

    /**
     * Get storage tank types
     * Corresponds to: GET /api/containment/storage-type
     */
    @GET("containment/storage-type")
    suspend fun getStorageTypes(): Response<StorageTypeResponse>

    /**
     * Get storage tank connections
     * Corresponds to: GET /api/containment/storage-connection
     */
    @GET("containment/storage-connection")
    suspend fun getStorageConnections(): Response<StorageConnectionResponse>

    /**
     * Get containment status by sanitation customer ID
     * Corresponds to: GET /api/containment/show-containment/{id}
     */
    @GET("containment/show-containment/{id}")
    suspend fun getContainmentStatus(@Path("id") sanitationCustomerId: String): Response<ContainmentStatusResponse>

    /**
     * Create new containment
     * Corresponds to: POST /api/containment
     */
    @POST("containment")
    suspend fun createContainment(@Body request: ContainmentRequest): Response<Unit>

    /**
     * Update existing containment
     * Corresponds to: PUT /api/containment/{id}
     */
    @PUT("containment/{id}")
    suspend fun updateContainment(
        @Path("id") containmentId: String,
        @Body request: ContainmentRequest
    ): Response<Unit>
}