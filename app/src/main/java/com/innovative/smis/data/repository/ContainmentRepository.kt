package com.innovative.smis.data.repository

import com.innovative.smis.data.api.ContainmentApiService
import com.innovative.smis.data.local.dao.ContainmentFormDao
import com.innovative.smis.data.local.dao.SyncQueueDao
import com.innovative.smis.data.local.entity.ContainmentFormEntity
import com.innovative.smis.data.local.entity.SyncQueueEntity
import com.innovative.smis.util.helper.PreferenceHelper
import com.innovative.smis.data.model.response.ContainmentStatusResponse
import com.innovative.smis.data.model.response.ContainmentData
import com.innovative.smis.ui.features.containment.ContainmentFormUiState
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import java.util.*

class ContainmentRepository(
    private val apiService: ContainmentApiService,
    private val preferenceHelper: PreferenceHelper,
    private val formDao: ContainmentFormDao,
    private val syncQueueDao: SyncQueueDao
) {

    fun getStorageTypes(): Flow<Resource<Map<String, String>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getStorageTypes()
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.let { types ->
                    emit(Resource.Success(types))
                } ?: emit(Resource.Error("No storage types data received"))
            } else {
                emit(Resource.Error("Failed to load storage types: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error loading storage types"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error loading storage types"))
        }
    }

    fun getStorageConnections(): Flow<Resource<Map<String, String>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getStorageConnections()
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.let { connections ->
                    emit(Resource.Success(connections))
                } ?: emit(Resource.Error("No storage connections data received"))
            } else {
                emit(Resource.Error("Failed to load storage connections: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error loading storage connections"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error loading storage connections"))
        }
    }

    fun getContainmentStatus(sanitationCustomerId: String): Flow<Resource<ContainmentData>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getContainmentStatus(sanitationCustomerId)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.let { containment ->
                    emit(Resource.Success(containment))
                } ?: emit(Resource.Error("Containment not found"))
            } else {
                emit(Resource.Error("Containment not found"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error loading containment status"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error loading containment status"))
        }
    }

    suspend fun saveContainment(
        sanitationCustomerId: String,
        applicationId: Int,
        formData: ContainmentFormUiState
    ): Resource<Unit> {
        return try {
            // Create entity from form data
            val entity = ContainmentFormEntity(
                sanitationCustomerId = sanitationCustomerId,
                applicationId = applicationId,
                typeOfStorageTank = formData.selectedStorageType,
                otherTypeOfStorageTank = if (formData.selectedStorageType == "Other") formData.otherTypeOfStorageTank else null,
                storageTankConnection = formData.selectedStorageConnection,
                otherStorageTankConnection = if (formData.selectedStorageConnection == "Other") formData.otherStorageTankConnection else null,
                sizeOfStorageTankM3 = formData.sizeOfStorageTankM3.takeIf { it.isNotEmpty() },
                constructionYear = formData.constructionYear.takeIf { it.isNotEmpty() },
                accessibility = formData.accessibility.takeIf { it.isNotEmpty() },
                everEmptied = formData.everEmptied.takeIf { it.isNotEmpty() },
                lastEmptiedYear = if (formData.everEmptiedKey == "yes") formData.lastEmptiedYear.takeIf { it.isNotEmpty() } else null,
                syncStatus = "PENDING",
                updatedAt = System.currentTimeMillis()
            )

            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                formDao.upsert(entity)
            }

            // Add to sync queue for background synchronization
            queueForSync(entity)

            // Try to submit to API immediately if network is available
            return trySubmitToApi(entity)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save containment form")
        }
    }

    private suspend fun queueForSync(entity: ContainmentFormEntity) {
        try {
            val syncEntity = SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                entityType = "CONTAINMENT_FORM",
                entityId = entity.id,
                operation = "CREATE", // Containment forms are typically created, not updated
                data = entityToJsonData(entity),
                retryCount = 0,
                maxRetries = 3,
                lastAttempt = null,
                createdAt = System.currentTimeMillis(),
                errorMessage = null,
                priority = 1
            )
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                syncQueueDao.insert(syncEntity)
            }
            println("DEBUG: Added containment form ${entity.id} to sync queue")
        } catch (e: Exception) {
            println("DEBUG: Failed to add containment form to sync queue: ${e.message}")
        }
    }

    private fun entityToJsonData(entity: ContainmentFormEntity): String {
        // Convert entity to JSON string for sync queue
        return """{
            "sanitation_customer_id": "${entity.sanitationCustomerId}",
            "type_of_storage_tank": "${entity.typeOfStorageTank ?: ""}",
            "other_type_of_storage_tank": "${entity.otherTypeOfStorageTank ?: ""}",
            "storage_tank_connection": "${entity.storageTankConnection ?: ""}",
            "other_storage_tank_connection": "${entity.otherStorageTankConnection ?: ""}",
            "size_of_storage_tank_m3": "${entity.sizeOfStorageTankM3 ?: ""}",
            "construction_year": "${entity.constructionYear ?: ""}",
            "accessibility": "${entity.accessibility ?: ""}",
            "ever_emptied": "${entity.everEmptied ?: ""}",
            "last_emptied_year": "${entity.lastEmptiedYear ?: ""}"
        }"""
    }

    private suspend fun trySubmitToApi(entity: ContainmentFormEntity): Resource<Unit> {
        return try {
            val requestBody = entity.toApiRequest()
            val response = apiService.createContainment(requestBody)

            if (response.isSuccessful) {
                // Successfully submitted to API
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    formDao.updateSyncStatus(
                        formId = entity.id,
                        status = "SYNCED",
                        attempts = 0,
                        lastAttempt = System.currentTimeMillis(),
                        error = null,
                        updatedAt = System.currentTimeMillis()
                    )

                    // Remove from sync queue since successfully synced
                    syncQueueDao.deleteSyncsByEntity("CONTAINMENT_FORM", entity.id)
                }
                println("DEBUG: Successfully synced Containment form ${entity.id} to API and removed from sync queue")

                Resource.Success(Unit)
            } else {
                // Get detailed error message from response body
                val errorBody = response.errorBody()?.string()
                val detailedError = if (errorBody != null) {
                    "API Error (${response.code()}): $errorBody"
                } else {
                    "API Error (${response.code()}): ${response.message()}"
                }

                println("DEBUG: API sync failed for Containment form ${entity.id} - $detailedError")

                if (entity.syncAttempts < 3) {
                    // Keep as pending for retry
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        formDao.updateSyncStatus(
                            formId = entity.id,
                            status = "PENDING",
                            attempts = entity.syncAttempts + 1,
                            lastAttempt = System.currentTimeMillis(),
                            error = detailedError,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    Resource.Error("Saved locally. Will retry sync later.")
                } else {
                    // Mark as failed after max retries
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        formDao.updateSyncStatus(
                            formId = entity.id,
                            status = "FAILED",
                            attempts = entity.syncAttempts + 1,
                            lastAttempt = System.currentTimeMillis(),
                            error = detailedError,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    Resource.Error("API sync failed after multiple attempts. Saved locally.")
                }
            }
        } catch (e: IOException) {
            println("DEBUG: Network error for Containment form ${entity.id}: ${e.message}")
            // Network error - keep as pending for retry
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                formDao.updateSyncStatus(
                    formId = entity.id,
                    status = "PENDING",
                    attempts = entity.syncAttempts + 1,
                    lastAttempt = System.currentTimeMillis(),
                    error = "Network error: ${e.message}",
                    updatedAt = System.currentTimeMillis()
                )
            }
            Resource.Error("Saved locally. Sync will be retried later.")
        } catch (e: Exception) {
            println("DEBUG: Unknown error for Containment form ${entity.id}: ${e.message}")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                formDao.updateSyncStatus(
                    formId = entity.id,
                    status = "PENDING",
                    attempts = entity.syncAttempts + 1,
                    lastAttempt = System.currentTimeMillis(),
                    error = "Unknown error: ${e.message}",
                    updatedAt = System.currentTimeMillis()
                )
            }
            Resource.Error("Saved locally. Sync will be retried later.")
        }
    }

    fun getContainmentFormByCustomerId(sanitationCustomerId: String): Flow<Resource<ContainmentFormEntity>> = flow {
        emit(Resource.Loading())
        try {
            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            val localData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                formDao.getFormBySanitationCustomerId(sanitationCustomerId)
            }
            localData?.let { emit(Resource.Success(it)) }

            // Then try to fetch from API and update local data
            val response = apiService.getContainmentStatus(sanitationCustomerId)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.let { networkData ->
                    // Convert API response to entity and save locally
                    val entity = ContainmentFormEntity(
                        sanitationCustomerId = sanitationCustomerId,
                        applicationId = 0, // Will need to be provided or fetched
                        typeOfStorageTank = networkData.type_of_storage_tank,
                        otherTypeOfStorageTank = networkData.other_type_of_storage_tank,
                        storageTankConnection = networkData.storage_tank_connection,
                        otherStorageTankConnection = networkData.other_storage_tank_connection,
                        sizeOfStorageTankM3 = networkData.size_of_storage_tank_m3,
                        constructionYear = networkData.construction_year,
                        accessibility = networkData.accessibility,
                        everEmptied = networkData.ever_emptied,
                        lastEmptiedYear = networkData.last_emptied_year,
                        syncStatus = "SYNCED"
                    )
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        formDao.upsert(entity)
                    }
                    emit(Resource.Success(entity))
                } ?: run {
                    // No network data but check for local data
                    val currentLocalData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        formDao.getFormBySanitationCustomerId(sanitationCustomerId)
                    }
                    if (currentLocalData != null) {
                        emit(Resource.Error("No containment data received from server.", currentLocalData))
                    } else {
                        emit(Resource.Error("No containment data found."))
                    }
                }
            } else {
                val currentLocalData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    formDao.getFormBySanitationCustomerId(sanitationCustomerId)
                }
                if (currentLocalData != null) {
                    emit(Resource.Error("API Error: ${response.message()}", currentLocalData))
                } else {
                    emit(Resource.Error("API Error: ${response.message()}"))
                }
            }
        } catch (e: IOException) {
            val cachedData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                formDao.getFormBySanitationCustomerId(sanitationCustomerId)
            }
            if (cachedData != null) {
                emit(Resource.Error("Network error. Displaying offline data.", cachedData))
            } else {
                emit(Resource.Error("Network error. No offline data available."))
            }
        } catch (e: Exception) {
            val cachedData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                formDao.getFormBySanitationCustomerId(sanitationCustomerId)
            }
            if (cachedData != null) {
                emit(Resource.Error(e.message ?: "An unknown error occurred.", cachedData))
            } else {
                emit(Resource.Error(e.message ?: "An unknown error occurred."))
            }
        }
    }

    @Deprecated("Use saveContainment instead for offline-first approach")
    suspend fun createContainment(
        sanitationCustomerId: String,
        formData: ContainmentFormUiState
    ): Resource<Unit> {
        return saveContainment(sanitationCustomerId, 0, formData)
    }

    @Deprecated("Use saveContainment instead for offline-first approach")
    suspend fun updateContainment(
        sanitationCustomerId: String,
        formData: ContainmentFormUiState
    ): Resource<Unit> {
        return saveContainment(sanitationCustomerId, 0, formData)
    }

    suspend fun getPendingSyncFormsCount(): Int {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            formDao.getPendingSyncCount()
        }
    }

    suspend fun getPendingSyncForms(): List<ContainmentFormEntity> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            formDao.getPendingSyncForms()
        }
    }
}