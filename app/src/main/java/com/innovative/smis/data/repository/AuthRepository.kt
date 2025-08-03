package com.innovative.smis.data.repository

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.innovative.smis.data.api.ApiService
import com.innovative.smis.data.model.response.LoginResponse
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AuthRepository(private val apiService: ApiService) {

    suspend fun login(loginRequest: JsonObject): Flow<Resource<LoginResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.login(loginRequest)
            if (response.isSuccessful && response.body() != null) {
                // This handles successful API calls (2xx status codes)
                val loginResponse = response.body()!!
                if (loginResponse.status == true) {
                    emit(Resource.Success(loginResponse))
                } else {
                    // This handles cases where the API returns 200 OK but with a logical error
                    emit(Resource.Error(loginResponse.message ?: "An unknown error occurred"))
                }
            } else {
                // This handles network errors (like the 500 error you're seeing)
                val errorBody = response.errorBody()?.string()
                val message = if (errorBody != null) {
                    try {
                        // Try to parse the specific "message" field from the error JSON
                        val jsonObj = JsonParser().parse(errorBody).asJsonObject
                        jsonObj.get("message")?.asString ?: "Server error: Invalid response format"
                    } catch (e: Exception) {
                        response.message() ?: "An unknown server error occurred"
                    }
                } else {
                    response.message() ?: "An unknown server error occurred"
                }
                emit(Resource.Error(message))
            }
        } catch (e: Exception) {
            // This handles exceptions like no internet connection
            emit(Resource.Error(e.message ?: "An unexpected error occurred"))
        }
    }.flowOn(Dispatchers.IO)
}
