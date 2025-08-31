package com.innovative.smis.data.repository

import com.innovative.smis.data.api.SitePreparationApiService
import com.innovative.smis.data.local.dao.SitePreparationFormDao
import com.innovative.smis.data.local.dao.SyncQueueDao
import com.innovative.smis.data.local.dao.TodoItemDao
import com.innovative.smis.data.local.entity.SitePreparationFormEntity
import com.innovative.smis.data.local.entity.SyncQueueEntity
import com.innovative.smis.data.local.entity.toDomainModel
import com.innovative.smis.data.local.entity.toEntity
import com.innovative.smis.data.local.entity.toApiRequest
import com.innovative.smis.data.model.response.TodoItem
import com.innovative.smis.data.model.response.SitePreparationCustomerDetails
import com.innovative.smis.data.model.response.SitePreparationCustomerDetailsResponse
import com.innovative.smis.data.model.request.SitePreparationFormRequest

import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson
import com.innovative.smis.util.helper.PreferenceHelper

class SitePreparationRepository(
    private val apiService: SitePreparationApiService,
    private val formDao: SitePreparationFormDao,
    private val syncQueueDao: SyncQueueDao,
    private val todoItemDao: TodoItemDao,
    private val preferenceHelper: PreferenceHelper
) {

    fun getFormDetails(applicationId: Int): Flow<Resource<SitePreparationFormEntity>> = flow {
        emit(Resource.Loading())

        // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
        val localData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            formDao.getFormByApplicationId(applicationId)
        }
        if (localData != null) emit(Resource.Success(localData))

        try {
            val response = apiService.getSanitationCustomerDetails(applicationId)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.let { networkData ->
                    // Convert network data to entity with complete field mapping from API
                    val entity = SitePreparationFormEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        applicationId = applicationId,
                        createdBy = null,
                        sanitationCustomerName = networkData.applicantName,
                        sanitationCustomerContact = networkData.applicantContact,
                        sanitationCustomerAddress = null,
                        applicantName = networkData.applicantName,
                        applicantContact = networkData.applicantContact,
                        customerName = networkData.applicantName, // Use applicant as customer by default
                        customerContact = networkData.applicantContact, // Use applicant contact as customer contact
                        purposeOfEmptying = networkData.purposeOfEmptying,
                        otherEmptyingPurpose = networkData.otherEmptyingPurpose,
                        everEmptied = networkData.everEmptied,
                        lastEmptiedDate = networkData.lastEmptiedYear?.toLong(),
                        lastEmptiedYear = networkData.lastEmptiedYear?.toString(),
                        notEmptiedBeforeReason = networkData.notEmptiedBeforeReason,
                        reasonForNoEmptiedDate = networkData.emptiedNodateReason,
                        freeServiceUnderPbc = networkData.freeServiceUnderPbc,
                        additionalRepairing = networkData.additionalRepairing,
                        otherAdditionalRepairing = networkData.otherAdditionalRepairing,
                        extraPaymentRequired = networkData.extraPaymentRequired,
                        amountOfExtraPayment = networkData.amountOfExtraPayment,
                        proposedEmptyingDate = networkData.proposedEmptyingDate,
                        needReschedule = null,
                        newProposedEmptyingDate = null,
                        syncStatus = "SYNCED"
                    )
                    // ✅ CRITICAL FIX: Move database operations to IO thread
                    val updatedLocalData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        formDao.upsert(entity)
                        formDao.getFormByApplicationId(applicationId)!!
                    }
                    emit(Resource.Success(updatedLocalData))
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

    fun getSanitationCustomerDetails(applicationId: String): Flow<Resource<SitePreparationCustomerDetailsResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getSanitationCustomerDetails(applicationId.toInt())
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("API Error: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error: ${e.message}"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unknown error occurred"))
        }
    }

    suspend fun saveFormDetails(entity: SitePreparationFormEntity): Resource<Unit> {
        return try {
            // Save locally first (offline-first approach)
            val entityWithPendingStatus = entity.copy(
                syncStatus = "PENDING",
                updatedAt = System.currentTimeMillis()
            )
            // ✅ CRITICAL FIX: Move database save operation to IO thread
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                formDao.upsert(entityWithPendingStatus)
            }

            // Add to sync queue for background synchronization
            queueForSync(entityWithPendingStatus)

            // Try to submit to API immediately if network is available
            return trySubmitToApi(entityWithPendingStatus)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save form details")
        }
    }

    private suspend fun trySubmitToApi(entity: SitePreparationFormEntity): Resource<Unit> {
        return try {
            val requestBody = entity.toApiRequest(entity.applicationId)
            // Use new dedicated PUT endpoint for site preparation form
            val response = apiService.updateSitePreparationForm(entity.applicationId, requestBody)

            if (response.isSuccessful && response.body()?.success == true) {
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
                syncQueueDao.deleteSyncsByEntity("SITE_PREPARATION_FORM", entity.id)
                println("DEBUG: Successfully synced Site Preparation form ${entity.id} to API and removed from sync queue")

                Resource.Success(Unit)
            } else {
                // Get detailed error message from response body
                val errorBody = response.errorBody()?.string()
                val detailedError = if (errorBody != null) {
                    "API Error (${response.code()}): $errorBody"
                } else {
                    "API Error (${response.code()}): ${response.message()}"
                }

                println("DEBUG: API sync failed for Site Preparation form ${entity.id} - $detailedError")

                if (entity.syncAttempts < 3) {
                    // Keep as pending for retry
                    formDao.updateSyncStatus(
                        formId = entity.id,
                        status = "PENDING",
                        attempts = entity.syncAttempts + 1,
                        lastAttempt = System.currentTimeMillis(),
                        error = detailedError,
                        updatedAt = System.currentTimeMillis()
                    )
                    Resource.Error("Saved locally. Will retry sync later.")
                } else {
                    // Mark as failed after max retries
                    formDao.updateSyncStatus(
                        formId = entity.id,
                        status = "FAILED",
                        attempts = entity.syncAttempts + 1,
                        lastAttempt = System.currentTimeMillis(),
                        error = detailedError,
                        updatedAt = System.currentTimeMillis()
                    )
                    Resource.Error("API sync failed after multiple attempts. Saved locally.")
                }
            }
        } catch (e: IOException) {
            println("DEBUG: Network error for Site Preparation form ${entity.id}: ${e.message}")
            // Network error - keep as pending for retry
            formDao.updateSyncStatus(
                formId = entity.id,
                status = "PENDING",
                attempts = entity.syncAttempts + 1,
                lastAttempt = System.currentTimeMillis(),
                error = "Network error: ${e.message}",
                updatedAt = System.currentTimeMillis()
            )
            Resource.Error("Saved locally. Sync will be retried later.")
        } catch (e: Exception) {
            println("DEBUG: Unexpected error for Site Preparation form ${entity.id}: ${e.message}")
            formDao.updateSyncStatus(
                formId = entity.id,
                status = "PENDING",
                attempts = entity.syncAttempts + 1,
                lastAttempt = System.currentTimeMillis(),
                error = "Error: ${e.message}",
                updatedAt = System.currentTimeMillis()
            )
            Resource.Error("Saved locally. Sync will be retried later.")
        }
    }

    private suspend fun queueForSync(entity: SitePreparationFormEntity) {
        try {
            val syncEntity = SyncQueueEntity(
                id = java.util.UUID.randomUUID().toString(),
                entityType = "SITE_PREPARATION_FORM",
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
            syncQueueDao.insert(syncEntity)
            println("DEBUG: Added Site Preparation form ${entity.id} to sync queue")
        } catch (e: Exception) {
            println("DEBUG: Failed to add Site Preparation form to sync queue: ${e.message}")
        }
    }

    private fun entityToJsonData(entity: SitePreparationFormEntity): String {
        return try {
            Gson().toJson(entity)
        } catch (e: Exception) {
            "{}"
        }
    }

    suspend fun initializeForm(applicationId: Int): SitePreparationFormEntity {
        // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val existingForm = formDao.getFormByApplicationId(applicationId)
            if (existingForm != null) return@withContext existingForm

            val draftForm = SitePreparationFormEntity(
                id = java.util.UUID.randomUUID().toString(),
                applicationId = applicationId,
                createdBy = null,
                syncStatus = "DRAFT",
                sanitationCustomerName = null,
                sanitationCustomerContact = null,
                sanitationCustomerAddress = null,
                applicantName = null,
                applicantContact = null,
                customerName = null,
                customerContact = null,
                purposeOfEmptying = null,
                otherEmptyingPurpose = null,
                everEmptied = null,
                lastEmptiedDate = null,
                lastEmptiedYear = null,
                notEmptiedBeforeReason = null,
                reasonForNoEmptiedDate = null,
                freeServiceUnderPbc = false,
                additionalRepairing = null,
                otherAdditionalRepairing = null,
                extraPaymentRequired = null,
                amountOfExtraPayment = null,
                proposedEmptyingDate = null,
                needReschedule = false,
                newProposedEmptyingDate = null
            )
            formDao.upsert(draftForm)
            draftForm
        }
    }

    suspend fun submitForm(formId: String): Resource<Unit> {
        return try {
            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                formDao.markAsPending(formId)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to submit form: ${e.message}")
        }
    }

    suspend fun getUnsyncedCount(): Int {
        // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            formDao.getUnsyncedCount()
        }
    }

    // Get emptying reasons for dropdown
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

    fun getSitePreparationApplications(): Flow<Resource<List<TodoItem>>> {
        return fetchAndCacheApplicationList {
            val etoId = preferenceHelper.getEtoId() ?: 2 // Default eto_id if not found
            apiService.getSitePreparationApplications(etoId = etoId)
        }
    }

    fun getSitePreparationApplicationsSpecific(): Flow<Resource<List<TodoItem>>> {
        return fetchAndCacheApplicationList {
            val etoId = preferenceHelper.getEtoId() ?: 2 // Default eto_id if not found
            apiService.getSitePreparationApplicationsSpecific(etoId = etoId)
        }
    }

    fun getApplicationsByStatus(status: String): Flow<Resource<List<TodoItem>>> {
        return fetchAndCacheApplicationList {
            val etoId = preferenceHelper.getEtoId() ?: 2 // Default eto_id if not found
            apiService.getApplicationsByStatus(status, etoId)
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

    suspend fun saveDraft(entity: SitePreparationFormEntity): Resource<Unit> {
        return try {
            val draftWithTimestamp = entity.copy(
                syncStatus = "DRAFT",
                updatedAt = System.currentTimeMillis()
            )
            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                formDao.upsert(draftWithTimestamp)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save draft")
        }
    }

    // Method to get pending syncs for debugging
    suspend fun getPendingSyncs(): List<SyncQueueEntity> {
        // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            syncQueueDao.getRetryableSyncs()
        }
    }

    suspend fun getFailedForms(): List<SitePreparationFormEntity> {
        // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            formDao.getFormsByStatus("FAILED")
        }
    }

    // Method to manually trigger sync for pending forms
    suspend fun syncPendingForms(): Resource<String> {
        return try {
            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            val pendingSyncs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                syncQueueDao.getRetryableSyncs()
                    .filter { it.entityType == "SITE_PREPARATION_FORM" }
            }

            if (pendingSyncs.isEmpty()) {
                return Resource.Success("No pending forms to sync")
            }

            var successCount = 0
            var failureCount = 0

            for (sync in pendingSyncs) {
                try {
                    // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
                    val formEntity = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        formDao.getFormById(sync.entityId)
                    }
                    if (formEntity != null) {
                        val result = trySubmitToApi(formEntity)
                        if (result is Resource.Success) {
                            successCount++
                        } else {
                            failureCount++
                        }
                    } else {
                        // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            // Form not found locally, remove from sync queue
                            syncQueueDao.deleteSyncsByEntity("SITE_PREPARATION_FORM", sync.entityId)
                        }
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

fun SitePreparationCustomerDetails.toEntity(applicationId: Int): SitePreparationFormEntity {
    return SitePreparationFormEntity(
        id = java.util.UUID.randomUUID().toString(),
        applicationId = applicationId,
        createdBy = null,
        sanitationCustomerName = this.sanitationCustomerName,
        sanitationCustomerContact = this.sanitationCustomerContact,
        sanitationCustomerAddress = this.sanitationCustomerAddress,
        applicantName = this.applicantName,
        applicantContact = this.applicantContact,
        customerName = this.sanitationCustomerName, // Default to sanitation customer name
        customerContact = this.sanitationCustomerContact, // Default to sanitation customer contact
        purposeOfEmptying = this.purposeOfEmptying,
        otherEmptyingPurpose = this.otherEmptyingPurpose,
        everEmptied = this.everEmptied,
        lastEmptiedDate = null, // Will be set from lastEmptiedYear if needed
        lastEmptiedYear = this.lastEmptiedYear,
        notEmptiedBeforeReason = this.notEmptiedBeforeReason,
        reasonForNoEmptiedDate = this.emptiedNodateReason,
        freeServiceUnderPbc = this.freeServiceUnderPbc ?: false,
        additionalRepairing = this.additionalRepairing,
        otherAdditionalRepairing = this.otherAdditionalRepairing,
        extraPaymentRequired = this.extraPaymentRequired,
        amountOfExtraPayment = this.amountOfExtraPayment,
        proposedEmptyingDate = this.proposedEmptyingDate,
        needReschedule = false,
        newProposedEmptyingDate = null,
        syncStatus = "SYNCED"
    )
}