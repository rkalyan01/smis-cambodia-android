package com.innovative.smis.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import java.util.*
import java.io.File
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

import com.innovative.smis.data.api.LaravelApiService
import com.innovative.smis.util.helper.PreferenceHelper
import com.innovative.smis.data.api.request.EmptyingServiceRequest
import com.innovative.smis.data.model.response.SanitationCustomerResponse
import com.innovative.smis.data.model.response.ContainmentIssuesResponse
import com.innovative.smis.data.model.response.SimpleDropdownResponse
import com.innovative.smis.data.model.response.DesludgingVehicleListResponse
import com.innovative.smis.data.model.response.EmptyingReadonlyDataResponse
import com.innovative.smis.data.model.response.DesludgingVehicleResponse
import com.innovative.smis.data.local.dao.EmptyingServiceFormDao
import com.innovative.smis.data.local.entity.EmptyingServiceFormEntity
import com.innovative.smis.data.local.entity.toApiRequest
import com.innovative.smis.ui.features.emptyingservice.EmptyingServiceFormUiState
import com.innovative.smis.util.common.Resource

class EmptyingServiceRepository(
    private val apiService: LaravelApiService,
    private val formDao: EmptyingServiceFormDao,
    private val preferenceHelper: PreferenceHelper,
    private val context: Context
) {

    fun loadCustomerDetails(applicationId: Int): Flow<Resource<SanitationCustomerResponse>> = flow {
        emit(Resource.Loading())

        try {
            val token = preferenceHelper.getAuthToken()
            if (token.isNullOrBlank()) {
                emit(Resource.Error("Authentication token not found"))
                return@flow
            }

            val response = apiService.getSanitationCustomerDetails(applicationId.toString())
            if (response.isSuccessful) {
                response.body()?.let { customerData ->
                    emit(Resource.Success(customerData))
                } ?: emit(Resource.Error("No customer data received"))
            } else {
                emit(Resource.Error("Failed to load customer details: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error loading customer details"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error loading customer details"))
        }
    }

    suspend fun submitEmptyingService(applicationId: Int, request: EmptyingServiceRequest): Resource<Unit> {
        return try {
            // Process request with proper defaults and Base64 image conversion
            val processedRequest = request.copy(
                no_of_trips = request.no_of_trips?.takeIf { it.isNotBlank() } ?: "1",
                volume_of_sludge = request.volume_of_sludge ?: "0",
                extra_payment = request.extra_payment?.takeIf { it.isNotBlank() } ?: "0",
                eto_id = request.eto_id?.takeIf { it.isNotBlank() } ?: "1",
                desludging_vehicle_id = request.desludging_vehicle_id?.takeIf { it.isNotBlank() } ?: "1",
                receipt_image = request.receipt_image?.let { convertUriToBase64(it) },
                picture_of_emptying = request.picture_of_emptying?.let { convertUriToBase64(it) }
            )

            val response = apiService.updateEmptyingService(applicationId.toString(), processedRequest)

            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to submit emptying service: ${response.message()}")
            }
        } catch (e: IOException) {
            Resource.Error("Network error submitting emptying service")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error submitting emptying service")
        }
    }

    private fun convertUriToBase64(imageUri: String): String? {
        return try {
            if (imageUri.isBlank() || !imageUri.startsWith("content://")) {
                return null
            }

            val uri = Uri.parse(imageUri)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                // Add data URI prefix for Laravel API compatibility
                "data:image/jpeg;base64,$base64String"
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveDraft(applicationId: Int, formData: EmptyingServiceFormUiState): Resource<String> {
        return try {
            val formId = UUID.randomUUID().toString()
            val entity = EmptyingServiceFormEntity(
                id = formId,
                applicationId = applicationId,
                emptiedDate = System.currentTimeMillis(),
                startTime = formData.startTime,
                endTime = formData.endTime,
                noOfTrips = formData.noOfTrips,
                applicantName = formData.applicantName,
                applicantContact = formData.applicantContact,
                serviceReceiverName = formData.serviceReceiverName,
                serviceReceiverContact = formData.serviceReceiverContact,
                isServiceReceiverSameAsApplicant = formData.isServiceReceiverSameAsApplicant,
                desludgingVehicleId = formData.desludgingVehicleId,
                sludgeType = formData.sludgeType,
                typeOfSludge = formData.typeOfSludge,
                pumpingPointPresence = formData.pumpingPointPresence,
                pumpingPointType = formData.pumpingPointType,
                freeUnderPBC = formData.freeUnderPBC,
                additionalRepairingInEmptying = formData.additionalRepairingInEmptying,
                regularCost = formData.regularCost,
                extraCost = formData.extraCost,
                receiptNumber = formData.receiptNumber,
                receiptImage = formData.receiptImage,
                pictureOfEmptying = formData.pictureOfEmptying,
                comments = formData.comments,
                longitude = formData.longitude,
                latitude = formData.latitude,
                syncStatus = "DRAFT"
            )

            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                formDao.upsert(entity)
            }
            Resource.Success(formId)
        } catch (e: Exception) {
            Resource.Error("Failed to save draft: ${e.message}")
        }
    }

    suspend fun submitFormOffline(applicationId: Int, formData: EmptyingServiceFormUiState): Resource<String> {
        return try {
            val formId = UUID.randomUUID().toString()
            val entity = EmptyingServiceFormEntity(
                id = formId,
                applicationId = applicationId,
                emptiedDate = System.currentTimeMillis(),
                startTime = formData.startTime,
                endTime = formData.endTime,
                noOfTrips = formData.noOfTrips,
                applicantName = formData.applicantName,
                applicantContact = formData.applicantContact,
                serviceReceiverName = formData.serviceReceiverName,
                serviceReceiverContact = formData.serviceReceiverContact,
                isServiceReceiverSameAsApplicant = formData.isServiceReceiverSameAsApplicant,
                desludgingVehicleId = formData.desludgingVehicleId,
                sludgeType = formData.sludgeType,
                typeOfSludge = formData.typeOfSludge,
                pumpingPointPresence = formData.pumpingPointPresence,
                pumpingPointType = formData.pumpingPointType,
                freeUnderPBC = formData.freeUnderPBC,
                additionalRepairingInEmptying = formData.additionalRepairingInEmptying,
                regularCost = formData.regularCost,
                extraCost = formData.extraCost,
                receiptNumber = formData.receiptNumber,
                receiptImage = formData.receiptImage,
                pictureOfEmptying = formData.pictureOfEmptying,
                comments = formData.comments,
                longitude = formData.longitude,
                latitude = formData.latitude,
                syncStatus = "PENDING"
            )

            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                formDao.upsert(entity)
            }
            Resource.Success(formId)
        } catch (e: Exception) {
            Resource.Error("Failed to save form offline: ${e.message}")
        }
    }

    suspend fun syncPendingForms(): Resource<Int> {
        return try {
            // ✅ CRITICAL FIX: Move all database operations to IO thread to prevent main thread blocking
            val pendingForms = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                formDao.getUnsyncedForms()
            }
            var successCount = 0

            pendingForms.forEach { form ->
                try {
                    val request = form.toApiRequest()
                    val result = submitEmptyingService(form.applicationId, request)
                    if (result is Resource.Success) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            formDao.markAsSynced(form.id)
                        }
                        successCount++
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            formDao.updateSyncStatus(form.id, "FAILED")
                        }
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        formDao.updateSyncStatus(form.id, "FAILED")
                    }
                }
            }

            Resource.Success(successCount)
        } catch (e: Exception) {
            Resource.Error("Sync failed: ${e.message}")
        }
    }

    suspend fun loadDraft(applicationId: Int): EmptyingServiceFormEntity? {
        // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            formDao.getFormByApplicationId(applicationId)
        }
    }

    suspend fun getDesludgingVehicles(etoId: Int): Resource<DesludgingVehicleListResponse> {
        return try {
            val response = apiService.getDesludgingVehicles(etoId)
            if (response.isSuccessful) {
                response.body()?.let { vehicleData ->
                    Resource.Success(vehicleData)
                } ?: Resource.Error("No vehicle data received")
            } else {
                Resource.Error("Failed to load vehicles: ${response.message()}")
            }
        } catch (e: IOException) {
            Resource.Error("Network error loading vehicles")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error loading vehicles")
        }
    }

    suspend fun getAdditionalRepairingOptions(): Resource<SimpleDropdownResponse> {
        return try {
            // Use the existing endpoint for repairing issues/containment
            val response = apiService.showContainmentIssue()
            if (response.isSuccessful) {
                response.body()?.let { containmentData ->
                    // ContainmentIssuesResponse is already a SimpleDropdownResponse
                    Resource.Success(containmentData)
                } ?: Resource.Error("No repairing options data received")
            } else {
                Resource.Error("Failed to load repairing options: ${response.message()}")
            }
        } catch (e: IOException) {
            Resource.Error("Network error loading repairing options")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error loading repairing options")
        }
    }

    fun loadReadonlyData(applicationId: Int): Flow<Resource<EmptyingReadonlyDataResponse>> = flow {
        emit(Resource.Loading())

        try {
            val token = preferenceHelper.getAuthToken()
            if (token.isNullOrBlank()) {
                emit(Resource.Error("Authentication token not found"))
                return@flow
            }

            val response = apiService.getEmptyingReadonlyData(applicationId)
            if (response.isSuccessful) {
                response.body()?.let { readonlyData ->
                    emit(Resource.Success(readonlyData))
                } ?: emit(Resource.Error("No readonly data received"))
            } else {
                emit(Resource.Error("Failed to load readonly data: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error loading readonly data"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error loading readonly data"))
        }
    }

    fun loadAdditionalRepairingOptions(): Flow<Resource<SimpleDropdownResponse>> = flow {
        emit(Resource.Loading())

        try {
            val token = preferenceHelper.getAuthToken()
            if (token.isNullOrBlank()) {
                emit(Resource.Error("Authentication token not found"))
                return@flow
            }

            val response = apiService.getAdditionalRepairingOptions()
            if (response.isSuccessful) {
                response.body()?.let { options ->
                    emit(Resource.Success(options))
                } ?: emit(Resource.Error("No additional repairing options received"))
            } else {
                emit(Resource.Error("Failed to load additional repairing options: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Network error loading additional repairing options"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error loading additional repairing options"))
        }
    }
}