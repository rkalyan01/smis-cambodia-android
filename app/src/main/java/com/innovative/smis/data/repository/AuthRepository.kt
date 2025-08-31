package com.innovative.smis.data.repository

import com.innovative.smis.data.api.AuthApiService
import com.innovative.smis.data.model.request.LoginRequest
import com.innovative.smis.data.model.response.ApiErrorResponse
import com.innovative.smis.data.model.response.LoginResponse
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException

class AuthRepository(
    private val apiService: AuthApiService,
    private val preferenceHelper: PreferenceHelper
) {
    suspend fun login(loginRequest: LoginRequest): Flow<Resource<LoginResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.login(loginRequest)
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                if (loginResponse.status == true) {
                    loginResponse.token?.let { token ->
                        preferenceHelper.saveAuthToken(token)
                    }
                    loginResponse.data?.let { userData ->
                        preferenceHelper.saveUserData(userData.name, userData.email)
                        // Extract and save eto_id from data object directly
                        preferenceHelper.saveEtoId(userData.etoId)
                    }
                    emit(Resource.Success(loginResponse))
                } else {
                    emit(Resource.Error(loginResponse.message ?: "An unknown error occurred"))
                }
            } else {
                // Try to parse the error message from the response body
                val errorBody = response.errorBody()?.string()
                val actualErrorMessage = try {
                    if (errorBody != null) {
                        // Parse JSON response to get the actual error message
                        val jsonObject = com.squareup.moshi.Moshi.Builder()
                            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                            .build()
                            .adapter(Map::class.java)
                            .fromJson(errorBody) as? Map<String, Any>
                        jsonObject?.get("message") as? String
                    } else null
                } catch (e: Exception) {
                    null
                }
                
                val errorMessage = actualErrorMessage ?: when (response.code()) {
                    401 -> "Invalid email or password"
                    404 -> "Login service not available"
                    500 -> "Server error. Please try again later"
                    else -> "Authentication failed (${response.code()})"
                }
                emit(Resource.Error(errorMessage))
            }
        } catch (e: HttpException) {
            // Try to parse the actual error message from HttpException
            val actualErrorMessage = try {
                e.response()?.errorBody()?.string()?.let { errorBody ->
                    val jsonObject = com.squareup.moshi.Moshi.Builder()
                        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                        .build()
                        .adapter(Map::class.java)
                        .fromJson(errorBody) as? Map<String, Any>
                    jsonObject?.get("message") as? String
                }
            } catch (parseException: Exception) {
                null
            }
            
            val errorMessage = actualErrorMessage ?: when (e.code()) {
                401 -> "Invalid email or password"
                404 -> "Service not found"
                500 -> "Server error. Please try again"
                else -> "Network error: ${e.message()}"
            }
            emit(Resource.Error(errorMessage))
        } catch (e: IOException) {
            emit(Resource.Error("Connection failed. Check your internet connection."))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getCurrentUser(): Flow<Resource<LoginResponse?>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getUserProfile()
            if (response.isSuccessful) {
                val userResponse = response.body()
                emit(Resource.Success(userResponse))
            } else {
                emit(Resource.Error("Failed to get user info"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Failed to get user info: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun logout(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.logout()
            if (response.isSuccessful) {
                preferenceHelper.clearAll()
                emit(Resource.Success(true))
            } else {
                preferenceHelper.clearAll()
                emit(Resource.Error("Logout failed on server, but cleared local data"))
            }
        } catch (e: Exception) {
            preferenceHelper.clearAll()
            emit(Resource.Error("Network error during logout, but cleared local data"))
        }
    }.flowOn(Dispatchers.IO)
}
