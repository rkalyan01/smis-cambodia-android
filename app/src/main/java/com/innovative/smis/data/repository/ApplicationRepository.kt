package com.innovative.smis.data.repository

import com.innovative.smis.data.model.response.Application as ApplicationModel
import com.innovative.smis.data.model.response.EmptyingDashboardData
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.Flow

interface ApplicationRepository {
    suspend fun getFilteredApplications(
        status: String? = null,
        etoId: String? = null
    ): Flow<Resource<List<ApplicationModel>>>
    
    suspend fun getEmptyingStats(): Flow<Resource<EmptyingDashboardData>>
    
    suspend fun updateApplicationStatus(
        applicationId: String,
        status: String
    ): Flow<Resource<ApplicationModel>>
    
    suspend fun syncOfflineData(): Flow<Resource<Boolean>>
}