package com.innovative.smis.data.repository

import com.innovative.smis.data.api.ApiService
import com.innovative.smis.data.api.EmptyingApiService
import com.innovative.smis.data.model.response.ApplicationListResponse
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PreferenceHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

/**
 * Repository for workflow-specific data fetching
 * Uses the correct endpoints with eto_id from login response
 */
class WorkflowRepository(
    private val apiService: ApiService,
    private val emptyingApiService: EmptyingApiService,
    private val preferenceHelper: PreferenceHelper
) {
    
    /**
     * Get applications for Emptying Scheduling workflow
     * Status: "Initiated"
     */
    fun getEmptyingSchedulingApplications(): Flow<Resource<ApplicationListResponse>> = flow {
        emit(Resource.Loading())
        try {
            val etoId = preferenceHelper.getEtoId()?.toString()
            val response = apiService.getFilteredApplications(
                status = "Initiated",
                etoId = etoId
            )
            
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to fetch emptying scheduling applications: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error. Please check your connection."))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred"))
        }
    }
    
    /**
     * Get applications for Site Preparation workflow  
     * Status: "Scheduled" and site_visit_required = "yes"
     */
    fun getSitePreparationApplications(): Flow<Resource<ApplicationListResponse>> = flow {
        emit(Resource.Loading())
        try {
            val etoId = preferenceHelper.getEtoId()?.toString()
            val response = apiService.getFilteredApplications(
                status = "Scheduled",
                etoId = etoId,
                siteVisitRequired = "yes"
            )
            
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to fetch site preparation applications: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error. Please check your connection."))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred"))
        }
    }
    
    /**
     * Get applications for Emptying Service workflow
     * Uses special endpoint that handles complex OR logic:
     * Status: "Scheduled" (site_visit_required=no) OR "Site-Preparation" (site_visit_required=yes)
     */
    fun getEmptyingServiceApplications(): Flow<Resource<ApplicationListResponse>> = flow {
        emit(Resource.Loading())
        try {
            val etoId = preferenceHelper.getEtoId()?.toString()
            // Use the special emptying-service-filter endpoint that handles complex OR logic on backend
            val response = emptyingApiService.getEmptyingServiceApplications(etoId)
            
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to fetch emptying service applications: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error. Please check your connection."))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred"))
        }
    }
    
    /**
     * Get applications with custom status and eto_id
     */
    fun getApplicationsByStatus(status: String): Flow<Resource<ApplicationListResponse>> = flow {
        emit(Resource.Loading())
        try {
            val etoId = preferenceHelper.getEtoId()?.toString()
            val response = apiService.getFilteredApplications(
                status = status,
                etoId = etoId
            )
            
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to fetch applications for status $status: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error. Please check your connection."))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred"))
        }
    }
}