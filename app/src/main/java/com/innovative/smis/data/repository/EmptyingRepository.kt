package com.innovative.smis.data.repository

import com.innovative.smis.data.api.EmptyingApiService
import com.innovative.smis.data.api.request.EmptyingServiceRequest
import com.innovative.smis.data.model.response.*
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException

interface EmptyingRepository {
    suspend fun getSanitationCustomerDetails(applicationId: String): Flow<Resource<SanitationCustomerResponse>>
    suspend fun getEmptyingPurposes(): Flow<Resource<EmptyingPurposeResponse>>
    suspend fun getExperienceIssues(): Flow<Resource<ExperienceIssuesResponse>>
    suspend fun submitEmptyingForm(request: EmptyingFormRequest): Flow<Resource<EmptyingFormResponse>>
    suspend fun updateEmptyingForm(applicationId: String, request: EmptyingFormRequest): Flow<Resource<EmptyingFormResponse>>
    suspend fun getFilteredApplications(status: String? = null, etoId: String? = null): Flow<Resource<ApplicationListResponse>>
    suspend fun submitSitePreparation(request: EmptyingFormRequest): Flow<Resource<EmptyingFormResponse>>
    suspend fun submitEmptyingService(request: EmptyingServiceRequest): Flow<Resource<EmptyingFormResponse>>
    suspend fun getEmptyingReadonlyData(): Flow<Resource<EmptyingDashboardDataResponse>>
    suspend fun getEtoNames(): Flow<Resource<EtoNamesResponse>>
    suspend fun getTruckNumbers(): Flow<Resource<TruckNumbersResponse>>
}

class EmptyingRepositoryImpl(
    private val apiService: EmptyingApiService
) : EmptyingRepository {

    override suspend fun getSanitationCustomerDetails(applicationId: String): Flow<Resource<SanitationCustomerResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getSanitationCustomerDetails(applicationId)
            if (response.isSuccessful) {
                val customerResponse = response.body()
                if (customerResponse != null && customerResponse.success) {
                    emit(Resource.Success(customerResponse))
                } else {
                    emit(Resource.Error("Failed to get customer details"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getEmptyingPurposes(): Flow<Resource<EmptyingPurposeResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getEmptyingPurposes()
            if (response.isSuccessful) {
                val purposeResponse = response.body()
                if (purposeResponse != null && purposeResponse.success) {
                    emit(Resource.Success(purposeResponse))
                } else {
                    emit(Resource.Error("Failed to get emptying purposes"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getExperienceIssues(): Flow<Resource<ExperienceIssuesResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getExperienceIssues()
            if (response.isSuccessful) {
                val issuesResponse = response.body()
                if (issuesResponse != null && issuesResponse.success) {
                    emit(Resource.Success(issuesResponse))
                } else {
                    emit(Resource.Error("Failed to get experience issues"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun submitEmptyingForm(request: EmptyingFormRequest): Flow<Resource<EmptyingFormResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.submitEmptyingForm(request)
            if (response.isSuccessful) {
                val formResponse = response.body()
                if (formResponse != null && formResponse.success) {
                    emit(Resource.Success(formResponse))
                } else {
                    emit(Resource.Error(formResponse?.message ?: "Failed to submit form"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun updateEmptyingForm(applicationId: String, request: EmptyingFormRequest): Flow<Resource<EmptyingFormResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.updateEmptyingForm(applicationId, request)
            if (response.isSuccessful) {
                val formResponse = response.body()
                if (formResponse != null && formResponse.success) {
                    emit(Resource.Success(formResponse))
                } else {
                    emit(Resource.Error(formResponse?.message ?: "Failed to update form"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getFilteredApplications(status: String?, etoId: String?): Flow<Resource<com.innovative.smis.data.model.response.ApplicationListResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getEmptyingServiceApplications(etoId)
            if (response.isSuccessful) {
                val applicationResponse = response.body()
                if (applicationResponse != null && applicationResponse.success) {
                    emit(Resource.Success(applicationResponse))
                } else {
                    emit(Resource.Error("Failed to get applications"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun submitSitePreparation(request: EmptyingFormRequest): Flow<Resource<EmptyingFormResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.submitSitePreparation(request)
            if (response.isSuccessful) {
                val formResponse = response.body()
                if (formResponse != null && formResponse.success) {
                    emit(Resource.Success(formResponse))
                } else {
                    emit(Resource.Error(formResponse?.message ?: "Failed to submit site preparation"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun submitEmptyingService(request: EmptyingServiceRequest): Flow<Resource<EmptyingFormResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.submitEmptyingService(request)
            if (response.isSuccessful) {
                val formResponse = response.body()
                if (formResponse != null && formResponse.success) {
                    emit(Resource.Success(formResponse))
                } else {
                    emit(Resource.Error(formResponse?.message ?: "Failed to submit emptying service"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getEmptyingReadonlyData(): Flow<Resource<com.innovative.smis.data.model.response.EmptyingDashboardDataResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getEmptyingReadonlyData()
            if (response.isSuccessful) {
                val readonlyResponse = response.body()
                if (readonlyResponse != null && readonlyResponse.success) {
                    emit(Resource.Success(readonlyResponse))
                } else {
                    emit(Resource.Error("Failed to get readonly data"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getEtoNames(): Flow<Resource<com.innovative.smis.data.model.response.EtoNamesResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getEtoNames()
            if (response.isSuccessful) {
                val etoResponse = response.body()
                if (etoResponse != null && etoResponse.success) {
                    emit(Resource.Success(etoResponse))
                } else {
                    emit(Resource.Error("Failed to get ETO names"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getTruckNumbers(): Flow<Resource<com.innovative.smis.data.model.response.TruckNumbersResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getTruckNumbers()
            if (response.isSuccessful) {
                val truckResponse = response.body()
                if (truckResponse != null && truckResponse.success) {
                    emit(Resource.Success(truckResponse))
                } else {
                    emit(Resource.Error("Failed to get truck numbers"))
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("HTTP error: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Network error: Please check your connection"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}