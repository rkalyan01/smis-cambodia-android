package com.innovative.smis.data.repository

import com.innovative.smis.data.api.EtoApiService
import com.innovative.smis.data.model.response.EtoLicenseData
import com.innovative.smis.data.model.response.EtoLicenseResponse
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

data class EtoLicenseResult(
    val data: List<EtoLicenseData>,
    val message: String? = null
)

class EtoLicenseRepository(
    private val apiService: EtoApiService
) {
    fun getEtoLicenseStatus(etoId: String): Flow<Resource<EtoLicenseResult>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getEtoLicenseStatus(etoId)
            if (response.isSuccessful) {
                val body = response.body()
                emit(Resource.Success(EtoLicenseResult(
                    data = body?.data ?: emptyList(),
                    message = body?.message
                )))
            } else {
                emit(Resource.Error("API Error: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unknown error: ${e.localizedMessage}"))
        }
    }
}