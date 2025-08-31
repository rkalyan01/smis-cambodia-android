package com.innovative.smis.data.api

import com.innovative.smis.data.model.response.SitePreparationCustomerDetailsResponse
import com.innovative.smis.data.api.request.EmptyingServiceRequest
import retrofit2.Response
import retrofit2.http.*

interface EmptyingServiceApiService {

    /**
     * Get application details for emptying service form
     */
    @GET("site-preparation/sanitation-customer-details/{id}")
    suspend fun getApplicationDetails(
        @Path("id") applicationId: Int
    ): Response<SitePreparationCustomerDetailsResponse>

    /**
     * Submit emptying service form
     * Corresponds to: PATCH /api/emptyings/{id}
     */
    @PATCH("emptyings/{id}")
    suspend fun submitEmptyingService(
        @Path("id") applicationId: Int,
        @Body request: EmptyingServiceRequest
    ): Response<Unit>
}