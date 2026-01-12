package com.innovative.smis.data.api

import com.innovative.smis.data.model.response.EtoLicenseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface EtoApiService {
    @GET("etos/license-status/{etoID}")
    suspend fun getEtoLicenseStatus(
        @Path("etoID") etoId: String
    ): Response<EtoLicenseResponse>
}