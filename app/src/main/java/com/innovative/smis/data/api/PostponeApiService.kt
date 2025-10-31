package com.innovative.smis.data.api

import com.innovative.smis.data.model.request.PostponeRequest
import com.innovative.smis.data.model.response.SimpleApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

interface PostponeApiService {
    
    /**
     * Postpone an application
     * @param applicationId The application ID
     * @param postponeAt The workflow type (e.g., "Emptying-Scheduling", "Site-Preparation", "Emptying-Service")
     * @param request The postpone request data
     */
    @PUT("postpone/{application_id}/{postpone_at}")
    suspend fun postponeApplication(
        @Path("application_id") applicationId: Int,
        @Path("postpone_at") postponeAt: String,
        @Body request: PostponeRequest
    ): Response<SimpleApiResponse>
}
