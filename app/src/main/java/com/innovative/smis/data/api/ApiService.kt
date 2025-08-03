package com.innovative.smis.data.api

import com.google.gson.JsonObject
import com.innovative.smis.data.model.response.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    suspend fun login(@Body body: JsonObject): Response<LoginResponse>
}
