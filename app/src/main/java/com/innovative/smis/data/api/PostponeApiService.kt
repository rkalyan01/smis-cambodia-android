package com.innovative.smis.data.api

import com.innovative.smis.data.model.response.SimpleApiResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Path

interface PostponeApiService {
    
    /**
     * Postpone an application
     * @param applicationId The application ID
     * @param postponeAt The workflow type (e.g., "Emptying-Scheduling", "Site-Preparation", "Emptying-Service")
     * @param type Postpone/Prepone
     * @param reason Reason for rescheduling
     * @param remark Remarks
     * @param postponeFrom Date from
     * @param postponeTo Date to
     * @param preponeFrom Date from (if prepone)
     * @param preponeTo Date to (if prepone)
     */
    @FormUrlEncoded
    @POST("postpone/{application_id}/{postpone_at}")
    suspend fun postponeApplication(
        @Path("application_id") applicationId: Int,
        @Path("postpone_at") postponeAt: String,
        @Field("type") type: String,
        @Field("reason") reason: String,
        @Field("remark") remark: String,
        @Field("postpone_from") postponeFrom: String?,
        @Field("postpone_to") postponeTo: String?,
        @Field("prepone_from") preponeFrom: String?,
        @Field("prepone_to") preponeTo: String?
    ): Response<SimpleApiResponse>
}
