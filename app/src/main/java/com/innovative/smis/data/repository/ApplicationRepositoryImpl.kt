package com.innovative.smis.data.repository

import com.innovative.smis.data.api.ApiService
import com.innovative.smis.data.model.response.Application as ApplicationModel
import com.innovative.smis.data.model.response.EmptyingDashboardData
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ApplicationRepositoryImpl(
    private val apiService: ApiService
) : ApplicationRepository {

    override suspend fun getFilteredApplications(
        status: String?,
        etoId: String?
    ): Flow<Resource<List<ApplicationModel>>> = flow {
        emit(Resource.Loading())

        try {
            val response = apiService.getFilteredApplications(status, etoId)
            if (response.isSuccessful) {
                response.body()?.let { applicationResponse ->
                    if (applicationResponse.success) {
                        emit(Resource.Success(applicationResponse.data ?: emptyList()))
                    } else {
                        emit(Resource.Error(applicationResponse.message ?: "Failed to load applications"))
                    }
                } ?: emit(Resource.Error("Empty response from server"))
            } else {
                emit(Resource.Error("Server error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: java.net.UnknownHostException) {
            emit(Resource.Error("No internet connection. Please check your network."))
        } catch (e: java.net.SocketTimeoutException) {
            emit(Resource.Error("Request timeout. Please try again."))
        } catch (e: Exception) {
            //TODO
        }
    }

    override suspend fun getEmptyingStats(): Flow<Resource<EmptyingDashboardData>> = flow {
        emit(Resource.Loading())

        try {
            val response = apiService.getEmptyingReadonlyData()
            if (response.isSuccessful) {
                response.body()?.let { statsResponse ->
                    if (statsResponse.success) {
                        emit(Resource.Success(statsResponse.data ?: EmptyingDashboardData(0, 0, 0)))
                    } else {
                        emit(Resource.Error("Failed to load statistics"))
                    }
                }
            } else {
                emit(Resource.Error("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load statistics"))
        }
    }

    override suspend fun syncOfflineData(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())

        try {
            kotlinx.coroutines.delay(2000)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error("Sync failed: ${e.message}"))
        }
    }

    override suspend fun updateApplicationStatus(
        applicationId: String,
        status: String
    ): Flow<Resource<ApplicationModel>> = flow {
        emit(Resource.Loading())

        try {
            // Create Application instance with all required parameters
            val application = ApplicationModel(
                id = applicationId,
                reference_number = "REF-$applicationId",
                status = status,
                applicant_name = "Updated Application",
                address = "Updated Address"
            )
            emit(Resource.Success(application))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update application"))
        }
    }
}