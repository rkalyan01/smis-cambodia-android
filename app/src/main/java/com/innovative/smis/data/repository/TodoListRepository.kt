package com.innovative.smis.data.repository

import com.innovative.smis.data.api.TodoListApiService
import com.innovative.smis.data.local.dao.TodoItemDao
import com.innovative.smis.data.local.entity.toDomainModel
import com.innovative.smis.data.local.entity.toEntity
import com.innovative.smis.data.model.response.TodoItem
import com.innovative.smis.data.model.response.TodoFilter
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PreferenceHelper
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class TodoListRepository(
    private val apiService: TodoListApiService,
    private val dao: TodoItemDao,
    private val preferenceHelper: PreferenceHelper
) {
    fun getAllTodoItems(): Flow<Resource<List<TodoItem>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getAllApplications()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    emit(Resource.Success(body.data ?: emptyList()))
                } else {
                    emit(Resource.Error(body?.message ?: "Failed to fetch applications"))
                }
            } else {
                emit(Resource.Error("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("TodoListRepository", "❌ Error: ${e.javaClass.simpleName} - ${e.message}", e)
            // ✅ Simple, user-friendly message for all errors
            emit(Resource.Error("Could not connect to server"))
        }
    }

    fun getFilteredTodoItems(filter: TodoFilter): Flow<Resource<List<TodoItem>>> = flow {
        emit(Resource.Loading())

        // Check if filter is for "Urgent" or "On-Demand" applications
        val isUrgentFilter = filter.status?.equals("Urgent", ignoreCase = true) == true
        val isOnDemandFilter = filter.status?.equals("On-Demand", ignoreCase = true) == true

        // First check for existing cached data based on the filter
        // For special filters (On-Demand, Urgent), retrieve all items and filter in memory
        val localDataFlow = if (isUrgentFilter || isOnDemandFilter) {
            dao.getValidCachedApplications().map { entities ->
                entities.map { it.toDomainModel() }
            }
        } else if (filter.status == null) { // null means "All"
            dao.getValidCachedApplications().map { entities ->
                entities.map { it.toDomainModel() }
            }
        } else {
            dao.getValidCachedApplicationsByStatus(filter.status).map { entities ->
                entities.map { it.toDomainModel() }
            }
        }

        // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
        val initialData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            localDataFlow.first()
        }
        
        // Apply filtering based on filter type
        var filteredInitialData = initialData
        
        // Apply On-Demand filter (applicationType = "On-Demand")
        if (isOnDemandFilter) {
            filteredInitialData = filteredInitialData.filter { 
                it.applicationType?.equals("On-Demand", ignoreCase = true) == true 
            }
        }
        
        // Apply Urgent filter (urgency = "yes" AND applicationType = "On-Demand")
        if (isUrgentFilter) {
            filteredInitialData = filteredInitialData.filter { 
                it.urgency?.equals("yes", ignoreCase = true) == true &&
                it.applicationType?.equals("On-Demand", ignoreCase = true) == true
            }
        }
        
        // Apply date filtering to cached data if needed
        if (filter.isToday) {
            filteredInitialData = filteredInitialData.filter { matchesToday(it) }
        }
        
        if (filteredInitialData.isNotEmpty()) {
            emit(Resource.Success(filteredInitialData))
        }

        try {
            val apiStatus: String?
            val apiFromDate: String?
            val apiToDate: String?
            val apiUrgency: String?
            val apiApplicationType: String?

            if (isUrgentFilter) {
                // For Urgent filter: urgency=yes and application_type=On-Demand
                apiStatus = null
                apiFromDate = null
                apiToDate = null
                apiUrgency = "yes"
                apiApplicationType = "On-Demand"
            } else if (isOnDemandFilter) {
                // For On-Demand filter: application_type=On-Demand only
                apiStatus = null
                apiFromDate = null
                apiToDate = null
                apiUrgency = null
                apiApplicationType = "On-Demand"
            } else if (filter.isToday) {
                // For Today filter: no date range sent to API, we'll filter locally by status-aware date matching
                apiStatus = null
                apiFromDate = null
                apiToDate = null
                apiUrgency = null
                apiApplicationType = null
            } else {
                apiStatus = if (filter.status?.equals("All", true) == true) null else filter.status
                apiFromDate = filter.dateFrom
                apiToDate = filter.dateTo
                apiUrgency = null
                apiApplicationType = null
            }

            // Get eto_id from preferences
            val etoId = preferenceHelper.getEtoId()?.toString()

            val response = if (isUrgentFilter || isOnDemandFilter) {
                // Use the filtered endpoint with urgency and/or applicationType parameters
                apiService.getFilteredApplications(
                    status = null,
                    etoId = etoId,
                    urgency = apiUrgency,
                    applicationType = apiApplicationType
                )
            } else {
                // Use the existing getFilteredApplications endpoint for other filters
                apiService.getFilteredApplications(
                    status = apiStatus,
                    etoId = etoId,
                    dateFrom = apiFromDate,
                    dateTo = apiToDate
                )
            }

            if (response.isSuccessful && response.body()?.success == true) {
                val networkItems = response.body()?.data ?: emptyList()
                android.util.Log.d("TodoListRepository", "📦 API returned ${networkItems.size} items")

                // ✅ CRITICAL FIX: Move all database operations to IO thread to prevent main thread blocking
                val updatedData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // Clean expired cache before adding new data
                    dao.clearExpiredCache()
                    android.util.Log.d("TodoListRepository", "🗑️ Cleared expired cache")

                    // Use upsert instead of clearAll + upsertAll to preserve other cached data
                    val entities = networkItems.map { it.toEntity() }
                    android.util.Log.d("TodoListRepository", "💾 Converting ${networkItems.size} items to ${entities.size} entities")
                    dao.upsertAll(entities)
                    android.util.Log.d("TodoListRepository", "✅ Upserted ${entities.size} entities to database")

                    // Return filtered data from the updated cache
                    val currentTime = System.currentTimeMillis()
                    
                    // For special filters (On-Demand, Urgent), retrieve all items and filter in memory
                    // since these filters are based on applicationType/urgency, not workflow status
                    val cachedItems = if (isUrgentFilter || isOnDemandFilter) {
                        android.util.Log.d("TodoListRepository", "🔍 Retrieving ALL cached applications for special filter (currentTime: $currentTime)")
                        dao.getValidCachedApplications(currentTime).first()
                    } else if (filter.status == null) { // null means "All"
                        android.util.Log.d("TodoListRepository", "🔍 Retrieving ALL cached applications (currentTime: $currentTime)")
                        dao.getValidCachedApplications(currentTime).first()
                    } else {
                        android.util.Log.d("TodoListRepository", "🔍 Retrieving cached applications for status: ${filter.status} (currentTime: $currentTime)")
                        dao.getValidCachedApplicationsByStatus(filter.status, currentTime).first()
                    }
                    android.util.Log.d("TodoListRepository", "📋 Retrieved ${cachedItems.size} cached items from database")
                    if (cachedItems.isEmpty()) {
                        android.util.Log.w("TodoListRepository", "⚠️ No cached items found! Checking if items were saved with future expiry...")
                        // Debug: Check if any items exist in database regardless of expiry
                        val allItems = dao.getAllApplications().first()
                        android.util.Log.d("TodoListRepository", "🔍 Total items in database (ignoring expiry): ${allItems.size}")
                        if (allItems.isNotEmpty()) {
                            val firstItem = allItems.first()
                            android.util.Log.d("TodoListRepository", "📅 First item expiry: ${firstItem.cacheExpiry}, current time: $currentTime, diff: ${firstItem.cacheExpiry - currentTime}ms")
                        }
                    }

                    val domainItems = cachedItems.map { it.toDomainModel() }
                    android.util.Log.d("TodoListRepository", "🔄 Converted ${cachedItems.size} entities to ${domainItems.size} domain models")
                    
                    // Apply filtering based on filter type
                    var filteredDomainItems = domainItems
                    
                    // Apply On-Demand filter (applicationType = "On-Demand")
                    if (isOnDemandFilter) {
                        filteredDomainItems = filteredDomainItems.filter { 
                            it.applicationType?.equals("On-Demand", ignoreCase = true) == true 
                        }
                        android.util.Log.d("TodoListRepository", "🔍 On-Demand filter: ${filteredDomainItems.size} items")
                    }
                    
                    // Apply Urgent filter (urgency = "yes" AND applicationType = "On-Demand")
                    if (isUrgentFilter) {
                        filteredDomainItems = filteredDomainItems.filter { 
                            it.urgency?.equals("yes", ignoreCase = true) == true &&
                            it.applicationType?.equals("On-Demand", ignoreCase = true) == true
                        }
                        android.util.Log.d("TodoListRepository", "🔍 Urgent filter: ${filteredDomainItems.size} items")
                    }
                    
                    // Apply date filtering if needed
                    if (filter.isToday) {
                        filteredDomainItems = filteredDomainItems.filter { matchesToday(it) }
                        android.util.Log.d("TodoListRepository", "🔍 Today filter: ${filteredDomainItems.size} items")
                    }
                    
                    filteredDomainItems
                }

                android.util.Log.d("TodoListRepository", "🎯 Final result: ${updatedData.size} items to emit")
                emit(Resource.Success(updatedData))

            } else {
                val errorMessage = response.body()?.message ?: "API Error: ${response.code()}"
                emit(Resource.Error(errorMessage, filteredInitialData))
            }

        } catch (e: Exception) {
            android.util.Log.e("TodoListRepository", "❌ Error occurred: ${e.javaClass.simpleName} - ${e.message}", e)
            // ✅ Simple, user-friendly message for all errors
            emit(Resource.Error("Could not connect to server. Showing saved data.", filteredInitialData))
        }
    }

    fun getTodaysTodoItems(): Flow<Resource<List<TodoItem>>> = flow {
        emit(Resource.Loading())
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val response = apiService.getApplicationsByDate(today)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    emit(Resource.Success(body.data ?: emptyList()))
                } else {
                    emit(Resource.Error(body?.message ?: "Failed to fetch today's applications"))
                }
            } else {
                emit(Resource.Error("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error occurred"))
        }
    }

    fun isApplicationDueToday(proposedEmptyingDate: String): Boolean {
        return try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            proposedEmptyingDate == today
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Helper function to check if a TodoItem matches today's date.
     * For "Initiated" status: uses application_datetime
     * For other statuses: uses proposed_emptying_date
     */
    private fun matchesToday(item: TodoItem): Boolean {
        return try {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            // For "Initiated" status, use application_datetime
            if (item.status?.equals("Initiated", ignoreCase = true) == true) {
                // application_datetime is in format "yyyy-MM-dd HH:mm:ss", extract date part
                val applicationDate = item.applicationDatetime?.substring(0, 10)
                return applicationDate == todayStr
            }
            
            // For all other statuses, use proposed_emptying_date
            return item.proposedEmptyingDate == todayStr
        } catch (e: Exception) {
            android.util.Log.w("TodoListRepository", "Error matching date for item ${item.applicationId}: ${e.message}")
            false
        }
    }
}