package com.innovative.smis.data.repository

import com.innovative.smis.data.api.EmptyingSchedulingApiService
import com.innovative.smis.data.model.response.SimpleDropdownResponse
import com.innovative.smis.data.model.response.SanitationCustomerResponse
import com.innovative.smis.data.model.response.SanitationCustomerData
import com.innovative.smis.data.local.dao.EmptyingSchedulingFormDao
import com.innovative.smis.data.local.dao.SyncQueueDao
import com.innovative.smis.data.local.dao.TodoItemDao
import com.innovative.smis.data.local.entity.EmptyingSchedulingFormEntity
import com.innovative.smis.data.local.entity.SyncQueueEntity
import com.innovative.smis.data.local.entity.toEntity
import com.innovative.smis.data.local.entity.toApiRequest
import com.innovative.smis.data.local.entity.toDomainModel
import com.innovative.smis.data.model.response.TodoItem
import com.innovative.smis.util.common.Resource
import com.squareup.moshi.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException

// Using the updated SanitationCustomerResponse and SanitationCustomerData from AllApiResponses.kt

class EmptyingSchedulingRepository(
    private val apiService: EmptyingSchedulingApiService,
    private val formDao: EmptyingSchedulingFormDao, // DAO for the form details
    private val syncQueueDao: SyncQueueDao, // DAO for sync queue management
    private val todoItemDao: TodoItemDao, // DAO for the list of applications
    private val preferenceHelper: com.innovative.smis.util.helper.PreferenceHelper // For getting eto_id
) {

    fun getFormDetails(applicationId: Int): Flow<Resource<EmptyingSchedulingFormEntity>> = flow {
        emit(Resource.Loading())

        // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
        val localData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            formDao.getFormByApplicationId(applicationId)
        }

        if (localData != null) {
            // ✅ PERFORMANCE: Move debug logging to IO thread and use Log.d instead of println
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                android.util.Log.d("EmptyingScheduling", "Local data found for applicationId $applicationId:")
                android.util.Log.d("EmptyingScheduling", "  Customer Name: '${localData.sanitationCustomerName}'")
                android.util.Log.d("EmptyingScheduling", "  Customer Contact: '${localData.sanitationCustomerContact}'")
                android.util.Log.d("EmptyingScheduling", "  Customer Address: '${localData.sanitationCustomerAddress}'")
                android.util.Log.d("EmptyingScheduling", "  Applicant Name: '${localData.applicantName}'")
                android.util.Log.d("EmptyingScheduling", "  Applicant Contact: '${localData.applicantContact}'")
            }

            emit(Resource.Success(localData))
        }

        try {
            val response = apiService.getSanitationCustomerDetails(applicationId)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.let { networkData ->
                    // Only update if we have meaningful data from API, preserve local data otherwise
                    if (localData != null) {
                        // Merge network data with existing local data, preserving user-entered fields
                        val mergedEntity = localData.copy(
                            // Only update customer info if API provides it
                            sanitationCustomerName = networkData.sanitationCustomerName ?: localData.sanitationCustomerName,
                            sanitationCustomerContact = networkData.sanitationCustomerContact ?: localData.sanitationCustomerContact,
                            pbcCustomerType = networkData.pbcCustomerType ?: localData.pbcCustomerType,
                            freeServiceUnderPbc = networkData.freeServiceUnderPbc ?: localData.freeServiceUnderPbc,
                            lastEmptiedYear = networkData.lastEmptiedYear?.toString() ?: localData.lastEmptiedYear,
                            everEmptied = networkData.everEmptied ?: localData.everEmptied,
                            emptiedNodateReason = networkData.emptiedNodateReason ?: localData.emptiedNodateReason,
                            notEmptiedBeforeReason = networkData.notEmptiedBeforeReason ?: localData.notEmptiedBeforeReason,
                            sizeOfContainment = networkData.sizeOfStorageTankM3 ?: localData.sizeOfContainment,
                            yearOfInstallation = networkData.constructionYear?.toString() ?: localData.yearOfInstallation,
                            containmentAccessibility = if (networkData.accessibility == true) "Yes" else if (networkData.accessibility == false) "No" else localData.containmentAccessibility,
                            // Keep applicant data and other user-entered fields unchanged
                            updatedAt = System.currentTimeMillis()
                        )
                        // ✅ CRITICAL FIX: Move database upsert to IO thread
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            formDao.upsert(mergedEntity)
                        }
                        emit(Resource.Success(mergedEntity))
                    } else {
                        // ✅ CRITICAL FIX: Move database operations to IO thread
                        val updatedLocalData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val entity = networkData.toEntity(applicationId)
                            formDao.upsert(entity)
                            formDao.getFormByApplicationId(applicationId)!!
                        }
                        emit(Resource.Success(updatedLocalData))
                    }
                } ?: emit(Resource.Error("No data received from server.", localData))
            } else {
                emit(Resource.Error("API Error: ${response.message()}", localData))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error. Displaying offline data.", localData))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unknown error occurred.", localData))
        }
    }



    suspend fun saveFormDetails(entity: EmptyingSchedulingFormEntity): Resource<Unit> {
        return try {
            val entityWithPendingStatus = entity.copy(
                syncStatus = "PENDING",
                updatedAt = System.currentTimeMillis()
            )

            // ✅ CRITICAL FIX: Always save locally first (offline-first approach) - move to IO thread
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                formDao.upsert(entityWithPendingStatus)
            }

            // Add to sync queue for background synchronization
            queueForSync(entityWithPendingStatus)

            // Try to submit to API immediately if network is available
            return trySubmitToApi(entityWithPendingStatus)
        } catch (e: Exception) {
            Resource.Error("Failed to save form: ${e.message}")
        }
    }

    private suspend fun queueForSync(entity: EmptyingSchedulingFormEntity) {
        try {
            val syncEntity = SyncQueueEntity(
                id = java.util.UUID.randomUUID().toString(),
                entityType = "EMPTYING_SCHEDULING_FORM",
                entityId = entity.id,
                operation = "UPDATE",
                data = entityToJsonData(entity),
                retryCount = 0,
                maxRetries = 3,
                lastAttempt = null,
                createdAt = System.currentTimeMillis(),
                errorMessage = null,
                priority = 1
            )
            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                syncQueueDao.insert(syncEntity)
            }
            println("DEBUG: Added form ${entity.id} to sync queue")
        } catch (e: Exception) {
            println("DEBUG: Failed to add to sync queue: ${e.message}")
        }
    }

    private fun entityToJsonData(entity: EmptyingSchedulingFormEntity): String {
        // Convert entity to JSON string for sync queue
        return """{
            "application_id": ${entity.applicationId},
            "customer_name": "${entity.sanitationCustomerName ?: ""}",
            "customer_phone": "${entity.sanitationCustomerContact ?: ""}",
            "customer_address": "${entity.sanitationCustomerAddress ?: ""}",
            "applicant_name": "${entity.applicantName ?: ""}",
            "applicant_contact": "${entity.applicantContact ?: ""}",
            "pbc_customer_type": "${entity.pbcCustomerType ?: ""}",
            "free_service_under_pbc": ${entity.freeServiceUnderPbc ?: false},
            "last_emptied_year": "${entity.lastEmptiedYear ?: ""}",
            "ever_emptied": ${entity.everEmptied ?: false},
            "emptied_nodate_reason": "${entity.emptiedNodateReason ?: ""}",
            "not_emptied_before_reason": "${entity.notEmptiedBeforeReason ?: ""}"
        }"""
    }

    private suspend fun trySubmitToApi(entity: EmptyingSchedulingFormEntity): Resource<Unit> {
        return try {
            val requestBody = entity.toApiRequest()
            val response = apiService.updateEmptyingScheduling(entity.applicationId, requestBody)

            if (response.isSuccessful && response.body()?.success == true) {
                // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // Successfully submitted to API
                    formDao.updateSyncStatus(
                        formId = entity.id,
                        status = "SYNCED",
                        attempts = 0,
                        lastAttempt = System.currentTimeMillis(),
                        error = null,
                        updatedAt = System.currentTimeMillis()
                    )

                    // Remove from sync queue since successfully synced
                    syncQueueDao.deleteSyncsByEntity("EMPTYING_SCHEDULING_FORM", entity.id)
                }
                println("DEBUG: Successfully synced form ${entity.id} to API and removed from sync queue")

                Resource.Success(Unit)
            } else {
                // Get detailed error message from response body
                val errorBody = response.errorBody()?.string()
                val detailedError = if (errorBody != null) {
                    "API Error (${response.code()}): $errorBody"
                } else {
                    "API Error (${response.code()}): ${response.message()}"
                }

                println("DEBUG: API sync failed for form ${entity.id} - $detailedError")

                // Check if it's a database constraint error that shouldn't be retried
                val shouldRetry = when {
                    errorBody?.contains("duplicate key value violates unique constraint") == true -> false
                    errorBody?.contains("SQLSTATE[23505]") == true -> false // Unique violation
                    errorBody?.contains("SQLSTATE[23503]") == true -> false // Foreign key violation
                    response.code() == 404 -> false // Not found
                    response.code() == 422 -> false // Validation error
                    else -> true
                }

                // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (shouldRetry && entity.syncAttempts < 3) {
                        // Keep as pending for retry
                        formDao.updateSyncStatus(
                            formId = entity.id,
                            status = "PENDING",
                            attempts = entity.syncAttempts + 1,
                            lastAttempt = System.currentTimeMillis(),
                            error = detailedError,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        // Mark as failed - don't retry
                        formDao.updateSyncStatus(
                            formId = entity.id,
                            status = "FAILED",
                            attempts = entity.syncAttempts + 1,
                            lastAttempt = System.currentTimeMillis(),
                            error = detailedError,
                            updatedAt = System.currentTimeMillis()
                        )
                        // Remove from sync queue to prevent endless retries
                        syncQueueDao.deleteSyncsByEntity("EMPTYING_SCHEDULING_FORM", entity.id)
                        println("DEBUG: Marked form ${entity.id} as FAILED - will not retry")
                    }
                }

                Resource.Success(Unit) // Still success because data is saved locally
            }
        } catch (e: IOException) {
            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // Network error - keep as pending for later sync
                formDao.updateSyncStatus(
                    formId = entity.id,
                    status = "PENDING",
                    attempts = entity.syncAttempts + 1,
                    lastAttempt = System.currentTimeMillis(),
                    error = "Network error: ${e.message}",
                    updatedAt = System.currentTimeMillis()
                )
            }
            Resource.Success(Unit) // Success because saved locally, will sync later
        } catch (e: Exception) {
            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // Other error - mark as failed
                formDao.updateSyncStatus(
                    formId = entity.id,
                    status = "FAILED",
                    attempts = entity.syncAttempts + 1,
                    lastAttempt = System.currentTimeMillis(),
                    error = "Error: ${e.message}",
                    updatedAt = System.currentTimeMillis()
                )
            }
            Resource.Error("Saved locally. Sync will be retried later.")
        }
    }

    // Background sync for pending forms when network becomes available
    suspend fun syncPendingFormsLegacy(): Int {
        // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
        val pendingForms = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            formDao.getUnsyncedForms()
        }
        var syncedCount = 0

        for (form in pendingForms) {
            try {
                val result = trySubmitToApi(form)
                if (result is Resource.Success) {
                    syncedCount++
                }
            } catch (e: Exception) {
                // Continue with next form
                continue
            }
        }

        return syncedCount
    }

    suspend fun createDraftForm(applicationId: Int, createdBy: String?): EmptyingSchedulingFormEntity {
        val draftForm = EmptyingSchedulingFormEntity(
            id = java.util.UUID.randomUUID().toString(),
            applicationId = applicationId,
            createdBy = createdBy,
            syncStatus = "DRAFT",
            sanitationCustomerName = null,
            sanitationCustomerContact = null,
            sanitationCustomerAddress = null,
            pbcCustomerType = null,
            freeServiceUnderPbc = null,
            applicantName = null,
            applicantContact = null,
            lastEmptiedYear = null,
            everEmptied = null,
            emptiedNodateReason = null,
            notEmptiedBeforeReason = null,
            purposeOfEmptying = null,
            purposeOfEmptyingOther = null,
            proposedEmptyingDate = null,
            lastEmptiedDate = null,
            sizeOfContainment = null,
            yearOfInstallation = null,
            containmentAccessibility = null,
            locationOfContainment = null,
            pumpingPointPresence = null,
            containmentIssues = null,
            containmentIssuesOther = null,
            extraPaymentRequired = null,
            extraPaymentAmount = null,
            siteVisitRequired = null,
            remarks = null,
            estimatedVolume = null
        )
        formDao.upsert(draftForm)
        return draftForm
    }

    suspend fun saveDraft(entity: EmptyingSchedulingFormEntity): Resource<Unit> {
        return try {
            val draftEntity = entity.copy(
                syncStatus = "DRAFT",
                updatedAt = System.currentTimeMillis()
            )
            formDao.upsert(draftEntity)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to save draft: ${e.message}")
        }
    }

    suspend fun submitForm(formId: String): Resource<Unit> {
        return try {
            formDao.markAsPending(formId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to submit form: ${e.message}")
        }
    }

    suspend fun refreshApplicationsAfterSubmission(applicationId: Int) {
        try {
            println("DEBUG: Optimized refresh after form submission for application #$applicationId")

            // 1. Immediately update the local status to "Scheduled" so it disappears from current list
            todoItemDao.updateApplicationStatus(applicationId, "Scheduled")
            println("DEBUG: Updated local status to 'Scheduled' for application #$applicationId")

            // 2. Optionally fetch fresh data to ensure sync (lightweight operation)
            val etoId = preferenceHelper.getEtoId() ?: 2
            val response = apiService.getEmptyingSchedulingApplications("Initiated", etoId)

            if (response.isSuccessful && response.body()?.success == true) {
                val applications = response.body()?.data ?: emptyList()
                todoItemDao.upsertAll(applications.map { it.toEntity() })
                println("DEBUG: Synced with server - ${applications.size} Initiated applications remain")
            }
        } catch (e: Exception) {
            println("DEBUG: Error in optimized refresh: ${e.message}")
            // Fallback: Even if API fails, the local status update will still work
        }
    }

    suspend fun getUnsyncedCount(): Int = formDao.getUnsyncedCount()

    fun getEmptyingSchedulingApplications(): Flow<Resource<List<TodoItem>>> {
        return fetchAndCacheApplicationList {
            val etoId = preferenceHelper.getEtoId() ?: 2 // Default to 2 if not set
            apiService.getEmptyingSchedulingApplications("Initiated", etoId)
        }
    }

    fun getInitiatedApplications(): Flow<Resource<List<TodoItem>>> {
        return fetchAndCacheApplicationList {
            val etoId = preferenceHelper.getEtoId() ?: 2 // Default to 2 if not set
            apiService.getInitiatedApplications("Initiated", etoId)
        }
    }

    fun getApplicationsByStatus(status: String): Flow<Resource<List<TodoItem>>> {
        return fetchAndCacheApplicationList {
            val etoId = preferenceHelper.getEtoId() ?: 2 // Default to 2 if not set
            apiService.getApplicationsByStatus(status, etoId)
        }
    }

    fun getShowContainment(): Flow<Resource<List<TodoItem>>> {
        return fetchAndCacheApplicationList {
            apiService.getShowContainment()
        }
    }

    private fun fetchAndCacheApplicationList(
        networkCall: suspend () -> retrofit2.Response<com.innovative.smis.data.model.response.TodoListResponse>
    ): Flow<Resource<List<TodoItem>>> = flow {
        emit(Resource.Loading())

        val localDataFlow = todoItemDao.getAllApplications().map { entities ->
            entities.map { it.toDomainModel() }
        }
        emit(Resource.Success(localDataFlow.first()))

        try {
            val response = networkCall()
            if (response.isSuccessful && response.body()?.success == true) {
                val networkItems = response.body()?.data ?: emptyList()
                todoItemDao.clearExpiredCache()
                todoItemDao.upsertAll(networkItems.map { it.toEntity() })
            } else {
                emit(Resource.Error("API Error: ${response.message()}", localDataFlow.first()))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error. Displaying cached data.", localDataFlow.first()))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unknown error occurred.", localDataFlow.first()))
        }
    }

    fun getEmptyingReasons(): Flow<Resource<Map<String, String>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getEmptyingReasons()
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.let { reasons ->
                    emit(Resource.Success(reasons))
                } ?: emit(Resource.Error("No emptying reasons data received"))
            } else {
                emit(Resource.Error("Failed to load emptying reasons: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error loading emptying reasons"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error loading emptying reasons"))
        }
    }

    fun getContainmentIssues(): Flow<Resource<Map<String, String>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getContainmentIssues()
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.let { issues ->
                    emit(Resource.Success(issues))
                } ?: emit(Resource.Error("No containment issues data received"))
            } else {
                emit(Resource.Error("Failed to load containment issues: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error loading containment issues"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error loading containment issues"))
        }
    }

    // Method to get pending syncs for debugging
    suspend fun getPendingSyncs(): List<SyncQueueEntity> {
        return syncQueueDao.getRetryableSyncs()
    }

    suspend fun getFailedForms(): List<EmptyingSchedulingFormEntity> {
        return formDao.getFormsByStatus("FAILED")
    }

    // Method to manually trigger sync for pending forms
    suspend fun syncPendingForms(): Resource<String> {
        return try {
            val pendingSyncs = syncQueueDao.getRetryableSyncs()
                .filter { it.entityType == "EMPTYING_SCHEDULING_FORM" }

            if (pendingSyncs.isEmpty()) {
                return Resource.Success("No pending forms to sync")
            }

            var successCount = 0
            var failureCount = 0

            for (sync in pendingSyncs) {
                try {
                    // Get the form data from local database
                    val formEntity = formDao.getFormById(sync.entityId)
                    if (formEntity != null) {
                        val result = trySubmitToApi(formEntity)
                        if (result is Resource.Success) {
                            successCount++
                        } else {
                            failureCount++
                        }
                    } else {
                        // Form not found locally, remove from sync queue
                        syncQueueDao.deleteSyncsByEntity("EMPTYING_SCHEDULING_FORM", sync.entityId)
                    }
                } catch (e: Exception) {
                    failureCount++
                    println("DEBUG: Failed to sync form ${sync.entityId}: ${e.message}")
                }
            }

            Resource.Success("Sync completed: $successCount successful, $failureCount failed")
        } catch (e: Exception) {
            Resource.Error("Sync failed: ${e.message}")
        }
    }
}
