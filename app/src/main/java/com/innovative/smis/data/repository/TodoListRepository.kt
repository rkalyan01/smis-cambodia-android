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
            emit(Resource.Error(e.message ?: "Network error occurred"))
        }
    }

    fun getFilteredTodoItems(filter: TodoFilter): Flow<Resource<List<TodoItem>>> = flow {
        emit(Resource.Loading())

        // First check for existing cached data based on the filter
        val localDataFlow = if (filter.status == null) { // null means "All"
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
        if (initialData.isNotEmpty()) {
            emit(Resource.Success(initialData))
        }

        try {
            val apiStatus: String?
            val apiFromDate: String?
            val apiToDate: String?

            if (filter.isToday) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                apiStatus = null
                apiFromDate = todayStr
                apiToDate = todayStr
            } else {
                apiStatus = if (filter.status.equals("All", true)) null else filter.status
                apiFromDate = filter.dateFrom
                apiToDate = filter.dateTo
            }

            // Get eto_id from preferences
            val etoId = preferenceHelper.getEtoId()?.toString()

            val response = apiService.getFilteredApplications(
                status = apiStatus,
                etoId = etoId
            )

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
                    val cachedItems = if (filter.status == null) { // null means "All"
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
                    domainItems
                }

                android.util.Log.d("TodoListRepository", "🎯 Final result: ${updatedData.size} items to emit")
                emit(Resource.Success(updatedData))

            } else {
                val errorMessage = response.body()?.message ?: "API Error: ${response.code()}"
                emit(Resource.Error(errorMessage, initialData))
            }

        } catch (e: IOException) {
            emit(Resource.Error("Network error. Displaying cached data.", initialData))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unknown error occurred.", initialData))
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
}