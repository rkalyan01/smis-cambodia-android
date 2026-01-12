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
import com.innovative.smis.data.api.ContainmentApiService
import com.innovative.smis.data.api.PostponeApiService
import com.innovative.smis.util.helper.PreferenceHelper
import com.innovative.smis.data.api.request.EmptyingServiceRequest
import com.innovative.smis.data.model.request.PostponeRequest
import com.innovative.smis.data.model.response.SanitationCustomerResponse
import com.innovative.smis.data.model.response.ContainmentIssuesResponse
import com.innovative.smis.data.model.response.ContainmentData
import com.innovative.smis.data.model.response.SimpleDropdownResponse
import com.innovative.smis.data.model.response.DesludgingVehicleListResponse
import com.innovative.smis.data.model.response.EmptyingReadonlyDataResponse
import com.innovative.smis.data.model.response.DesludgingVehicleResponse
import com.innovative.smis.data.local.dao.EmptyingServiceFormDao
import com.innovative.smis.data.local.entity.EmptyingServiceFormEntity
import com.innovative.smis.data.local.entity.toApiRequest
import com.innovative.smis.ui.features.emptyingservice.EmptyingServiceFormUiState
import com.innovative.smis.util.common.Resource
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class EmptyingServiceRepository(
    private val apiService: LaravelApiService,
    private val containmentApiService: ContainmentApiService,
    private val postponeApiService: PostponeApiService,
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
        try {
            // Get eto_id from logged-in user preferences
            val etoId = preferenceHelper.getEtoId()?.toString() ?: ""
            
            android.util.Log.d("EmptyingRepo", "=== SUBMITTING TO API ===")
            android.util.Log.d("EmptyingRepo", "Original request desludging_vehicle_id: '${request.desludging_vehicle_id}'")
            android.util.Log.d("EmptyingRepo", "Original request additional_repairing_id: '${request.additional_repairing_id}'")
            android.util.Log.d("EmptyingRepo", "Original request sludge_type_a: '${request.sludge_type_a}'")
            android.util.Log.d("EmptyingRepo", "Original request sludge_type_b: '${request.sludge_type_b}'")
            android.util.Log.d("EmptyingRepo", "Original request service_receiver_name: '${request.service_receiver_name}'")
            android.util.Log.d("EmptyingRepo", "Original request service_receiver_contact: '${request.service_receiver_contact}'")
            
            // Note: additional_repairing_id is now a List<Int>, no conversion needed
            // Keep the list as-is for PostgreSQL array column
            
            // ✅ CRITICAL FIX: Map presence_of_pumping_point to database enum values
            // Database expects: "Yes (Cover, Tube, Pierce)" or "No (need to pierce the tank)"
            val mappedPumpingPointPresence = when (request.presence_of_pumping_point) {
                "Yes" -> "Yes (Cover, Tube, Pierce)"
                "No" -> "No (need to pierce the tank)"
                else -> request.presence_of_pumping_point
            }
            
            android.util.Log.d("EmptyingRepo", "Mapped presence_of_pumping_point from '${request.presence_of_pumping_point}' to '$mappedPumpingPointPresence'")
            
            // Process request with proper defaults and Base64 image conversion
            val processedRequest = request.copy(
                volume_of_sludge = request.volume_of_sludge ?: "0",
                extra_payment = request.extra_payment?.takeIf { it.isNotBlank() } ?: "0",
                eto_id = request.eto_id?.takeIf { it.isNotBlank() } ?: etoId,
                desludging_vehicle_id = request.desludging_vehicle_id?.takeIf { it.isNotBlank() } ?: "1",
                presence_of_pumping_point = mappedPumpingPointPresence, // Use mapped database enum value
                receipt_image_base64 = request.receipt_image_base64?.let { convertUriToBase64(it) },
                picture_of_emptying_base64 = request.picture_of_emptying_base64?.let { convertUriToBase64(it) }
            )
            
            android.util.Log.d("EmptyingRepo", "Processed request desludging_vehicle_id: '${processedRequest.desludging_vehicle_id}'")
            android.util.Log.d("EmptyingRepo", "Processed request sludge_type_a: '${processedRequest.sludge_type_a}'")
            android.util.Log.d("EmptyingRepo", "Processed request sludge_type_b: '${processedRequest.sludge_type_b}'")

            // ✅ STEP 1: POST /api/emptyings/create/{application_id}
            // Send all fields EXCEPT extra_payment, receipt_number, comments, receipt_image_base64
            val createRequest = processedRequest.copy(
                extra_payment = null,
                receipt_number = null,
                comments = null,
                receipt_image_base64 = null
            )
            
            android.util.Log.d("EmptyingRepo", "STEP 1: Creating emptying record (without payment details)")
            android.util.Log.d("EmptyingRepo", "STEP 1 - service_receiver_name: '${createRequest.service_receiver_name}'")
            android.util.Log.d("EmptyingRepo", "STEP 1 - service_receiver_contact: '${createRequest.service_receiver_contact}'")
            val createResponse = apiService.createEmptyingService(applicationId, createRequest)

            if (!createResponse.isSuccessful) {
                android.util.Log.e("EmptyingRepo", "STEP 1 failed: HTTP ${createResponse.code()} - ${createResponse.message()}")
                return Resource.Error("Failed to create emptying service: ${createResponse.message()}")
            }

            // Get the emptying_id from the response
            val responseBody = createResponse.body()
            android.util.Log.d("EmptyingRepo", "STEP 1 response - isSuccess: ${responseBody?.isSuccess}, message: ${responseBody?.message}")
            
            val emptyingIdString = responseBody?.data?.id
            if (emptyingIdString.isNullOrBlank()) {
                android.util.Log.e("EmptyingRepo", "No emptying_id in response. Response body: $responseBody")
                return Resource.Error("Failed to get emptying ID from response")
            }
            
            val emptyingId = emptyingIdString.toIntOrNull()
            if (emptyingId == null) {
                android.util.Log.e("EmptyingRepo", "Invalid emptying_id format: $emptyingIdString")
                return Resource.Error("Invalid emptying ID format")
            }
            
            android.util.Log.d("EmptyingRepo", "STEP 1 successful. Emptying ID: $emptyingId")

            // ✅ STEP 2: PUT /api/emptyings/{emptying_id}
            // Send ONLY payment details with base64-encoded receipt image
            // Note: picture_of_emptying was already sent in STEP 1
            android.util.Log.d("EmptyingRepo", "STEP 2: Updating payment with JSON + base64 image")
            android.util.Log.d("EmptyingRepo", "STEP 2 - extra_payment: '${processedRequest.extra_payment}'")
            android.util.Log.d("EmptyingRepo", "STEP 2 - receipt_number: '${processedRequest.receipt_number}'")
            android.util.Log.d("EmptyingRepo", "STEP 2 - has receipt_image: ${processedRequest.receipt_image_base64 != null}")
            
            val paymentUpdateRequest = com.innovative.smis.data.api.request.EmptyingPaymentUpdateRequest(
                extra_payment = processedRequest.extra_payment,
                receipt_number = processedRequest.receipt_number,
                comments = processedRequest.comments,
                receipt_image_base64 = processedRequest.receipt_image_base64,
                picture_of_emptying_base64 = null // Already sent in STEP 1
            )
            
            val updateResponse = apiService.updateEmptyingPaymentDetails(
                emptyingId = emptyingId,
                request = paymentUpdateRequest
            )

            if (updateResponse.isSuccessful) {
                android.util.Log.d("EmptyingRepo", "STEP 2 successful. Both API calls completed!")
                return Resource.Success(Unit)
            } else {
                android.util.Log.e("EmptyingRepo", "STEP 2 failed: ${updateResponse.message()}")
                return Resource.Error("Emptying created but payment update failed: ${updateResponse.message()}")
            }
        } catch (e: IOException) {
            android.util.Log.e("EmptyingRepo", "Network error: ${e.message}", e)
            return Resource.Error("Network error submitting emptying service")
        } catch (e: Exception) {
            android.util.Log.e("EmptyingRepo", "Unknown error: ${e.message}", e)
            return Resource.Error(e.message ?: "Unknown error submitting emptying service")
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
            val entity = EmptyingServiceFormEntity(
                applicationId = applicationId,
                emptiedDate = System.currentTimeMillis(),
                startTime = formData.startTime,
                endTime = formData.endTime,
                additionalTripRequired = formData.additionalTripRequired,
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
                customerType = formData.customerType,
                otherCustomerType = formData.otherCustomerType,
                additionalRepairingInEmptying = formData.additionalRepairingKeys.joinToString(","),
                otherAdditionalRepairing = formData.otherAdditionalRepairing,
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
            Resource.Success(applicationId.toString())
        } catch (e: Exception) {
            Resource.Error("Failed to save draft: ${e.message}")
        }
    }

    suspend fun submitFormOffline(applicationId: Int, formData: EmptyingServiceFormUiState): Resource<String> {
        return try {
            android.util.Log.d("EmptyingRepo", "=== SAVING OFFLINE ===")
            android.util.Log.d("EmptyingRepo", "desludgingVehicleId: '${formData.desludgingVehicleId}'")
            android.util.Log.d("EmptyingRepo", "sludgeType: '${formData.sludgeType}'")
            android.util.Log.d("EmptyingRepo", "typeOfSludge: '${formData.typeOfSludge}'")
            
            val entity = EmptyingServiceFormEntity(
                applicationId = applicationId,
                emptiedDate = System.currentTimeMillis(),
                startTime = formData.startTime,
                endTime = formData.endTime,
                additionalTripRequired = formData.additionalTripRequired,
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
                customerType = formData.customerType,
                otherCustomerType = formData.otherCustomerType,
                additionalRepairingInEmptying = formData.additionalRepairingKeys.joinToString(","),
                otherAdditionalRepairing = formData.otherAdditionalRepairing,
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
            
            android.util.Log.d("EmptyingRepo", "Entity desludgingVehicleId: '${entity.desludgingVehicleId}'")
            android.util.Log.d("EmptyingRepo", "Entity sludgeType: '${entity.sludgeType}'")

            // ✅ CRITICAL FIX: Move database operations to IO thread to prevent main thread blocking
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                formDao.upsert(entity)
            }
            
            android.util.Log.d("EmptyingRepo", "Entity saved to database successfully")
            Resource.Success(applicationId.toString())
        } catch (e: Exception) {
            android.util.Log.e("EmptyingRepo", "Failed to save offline: ${e.message}", e)
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
                            formDao.markAsSynced(form.applicationId)
                        }
                        successCount++
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            formDao.updateSyncStatus(form.applicationId, "FAILED")
                        }
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        formDao.updateSyncStatus(form.applicationId, "FAILED")
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

    suspend fun getCustomerTypes(): Resource<SimpleDropdownResponse> {
        return try {
            val response = apiService.getCustomerTypes()
            if (response.isSuccessful) {
                response.body()?.let { customerTypeData ->
                    Resource.Success(customerTypeData)
                } ?: Resource.Error("No customer type data received")
            } else {
                Resource.Error("Failed to load customer types: ${response.message()}")
            }
        } catch (e: IOException) {
            Resource.Error("Network error loading customer types")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error loading customer types")
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

    fun getContainmentStatus(sanitationCustomerId: String): Flow<Resource<ContainmentData>> = flow {
        emit(Resource.Loading())
        try {
            val response = containmentApiService.getContainmentStatus(sanitationCustomerId)
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

    suspend fun postponeApplication(
        applicationId: Int,
        postponeType: String,
        postponeFrom: String,
        postponeTo: String,
        reason: String,
        remark: String
    ): Resource<Unit> {
        return try {
            val request = if (postponeType.equals("Prepone", ignoreCase = true)) {
                PostponeRequest(
                    type = postponeType,
                    preponeFrom = postponeFrom,
                    preponeTo = postponeTo,
                    postponeFrom = null,
                    postponeTo = null,
                    reason = reason,
                    remark = remark
                )
            } else {
                PostponeRequest(
                    type = postponeType,
                    postponeFrom = postponeFrom,
                    postponeTo = postponeTo,
                    preponeFrom = null,
                    preponeTo = null,
                    reason = reason,
                    remark = remark
                )
            }
            val response = postponeApiService.postponeApplication(
                applicationId = applicationId,
                postponeAt = "Emptying-Service",
                request = request
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(Unit)
            } else {
                Resource.Error(response.body()?.message ?: "Failed to postpone application")
            }
        } catch (e: IOException) {
            Resource.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
}