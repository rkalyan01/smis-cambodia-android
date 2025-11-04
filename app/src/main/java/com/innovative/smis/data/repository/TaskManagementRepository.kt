package com.innovative.smis.data.repository

import com.innovative.smis.data.api.TodoListApiService
import com.innovative.smis.util.helper.PreferenceHelper
import com.innovative.smis.data.model.response.TodoItem
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

class TaskManagementRepository(
    private val todoListApiService: TodoListApiService,
    private val preferenceHelper: PreferenceHelper
) {

    fun getTaskManagementApplications(
        status: String = "",
        urgency: String? = null,
        applicationType: String? = null
    ): Flow<Resource<List<TodoItem>>> = flow {
        emit(Resource.Loading())
        try {
            val etoId = preferenceHelper.getEtoId()
            if (etoId == null) {
                emit(Resource.Error("ETO ID not found"))
                return@flow
            }
            
            // Call API with specific status parameter using TodoListApiService
            // For "Urgent" filter: urgency=yes, applicationType=On-Demand
            val response = todoListApiService.getFilteredApplications(
                status = if (status.isEmpty()) null else status,
                etoId = etoId.toString(),
                urgency = urgency,
                applicationType = applicationType
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val applications = response.body()?.data ?: emptyList()
                emit(Resource.Success(applications))
            } else {
                emit(Resource.Error("Failed to load applications - Code: ${response.code()}"))
            }
            
        } catch (e: IOException) {
            emit(Resource.Error("Network error loading task management applications"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error loading task management applications"))
        }
    }
}