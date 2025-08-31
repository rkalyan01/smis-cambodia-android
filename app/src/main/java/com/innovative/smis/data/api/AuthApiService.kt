package com.innovative.smis.data.api

import com.innovative.smis.data.model.request.LoginRequest
import com.innovative.smis.data.model.response.LoginResponse
import com.innovative.smis.data.model.response.LogoutResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {

    @POST("login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @GET("user")
    suspend fun getUserProfile(): Response<LoginResponse>

    @POST("logout")
    suspend fun logout(): Response<LogoutResponse>
}