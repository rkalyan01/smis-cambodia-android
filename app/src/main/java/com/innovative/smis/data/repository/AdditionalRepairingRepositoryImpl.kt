package com.innovative.smis.data.repository

import android.util.Log
import com.google.gson.Gson
import com.innovative.smis.data.api.LaravelApiService
import com.innovative.smis.data.local.dao.AdditionalRepairingFormDao
import com.innovative.smis.data.local.entity.AdditionalRepairingFormEntity
import com.innovative.smis.data.model.request.TripCreateRequest
import com.innovative.smis.data.model.response.EmptyingDetailsData
import com.innovative.smis.data.model.response.EmptyingDetailsResponse
import com.innovative.smis.data.model.response.TripFilterApplication
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray

class AdditionalRepairingRepositoryImpl(
    private val api: LaravelApiService,
    private val dao: AdditionalRepairingFormDao
) : AdditionalRepairingRepository {

    override suspend fun getTripFilterApplications(
        applicationStatus: String,
        etoId: String,
        additionalTripRequired: String
    ): Flow<Resource<List<TripFilterApplication>>> = flow {
        try {
            emit(Resource.Loading())
            val response = api.getTripFilterApplications(
                applicationStatus = applicationStatus,
                etoId = etoId,
                additionalTripRequired = additionalTripRequired
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    body.data.forEach { app ->
                        Log.d("AdditionalRepairing", "TripFilter - Application ID: ${app.id}, Emptying ID: ${app.emptyingId}, Status: ${app.applicationStatus}")
                    }
                    emit(Resource.Success(body.data))
                } else {
                    emit(Resource.Error(body?.message ?: "Failed to fetch applications"))
                }
            } else {
                emit(Resource.Error("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AdditionalRepairing", "Error fetching applications", e)
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun getRegularPaymentAmount(): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val response = api.getRegularPaymentAmount()
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("AdditionalRepairing", "Regular payment response: success=${body?.success}, amount=${body?.amount}")
                if (body?.success == true) {
                    val amount = body.amount ?: "0"
                    Log.d("AdditionalRepairing", "Emitting amount: $amount")
                    emit(Resource.Success(amount))
                } else {
                    emit(Resource.Error(body?.message ?: "Failed to fetch payment amount"))
                }
            } else {
                emit(Resource.Error("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AdditionalRepairing", "Error fetching payment amount", e)
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun getApplicationByEmptyingId(
        emptyingId: Int
    ): Flow<Resource<EmptyingDetailsResponse>> = flow {
        try {
            emit(Resource.Loading())
            val response = api.getEmptyingDetails(emptyingId)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d("AdditionalRepairing", "Emptying details loaded: ID=${body.id}, ApplicationID=${body.applicationId}, RegularPayment=${body.totalAmountOfRegularPayment}")
                    emit(Resource.Success(body))
                } else {
                    emit(Resource.Error("Failed to fetch emptying details"))
                }
            } else {
                emit(Resource.Error("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AdditionalRepairing", "Error fetching emptying details", e)
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun createTripEntry(
        emptyingId: Int,
        request: TripCreateRequest
    ): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = api.createTripEntry(emptyingId, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    emit(Resource.Success(true))
                } else {
                    emit(Resource.Error(body?.message ?: "Failed to create trip"))
                }
            } else {
                emit(Resource.Error("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AdditionalRepairing", "Error creating trip", e)
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun getEmptyingDetails(
        emptyingId: Int
    ): Flow<Resource<EmptyingDetailsData>> = flow {
        try {
            emit(Resource.Loading())
            val response = api.getEmptyingDetails(emptyingId)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Convert EmptyingDetailsResponse to EmptyingDetailsData
                    val data = EmptyingDetailsData(
                        id = body.id,
                        amountOfRegularPayment = body.totalAmountOfRegularPayment,
                        amountOfExtraPayment = body.extraPayment,
                        receiptNumber = body.receiptNumber,
                        receiptImage = body.receiptImage,
                        comments = body.comments
                    )
                    emit(Resource.Success(data))
                } else {
                    emit(Resource.Error("Failed to fetch details"))
                }
            } else {
                emit(Resource.Error("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AdditionalRepairing", "Error fetching details", e)
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun updatePaymentDetails(
        emptyingId: Int,
        amountOfExtraPayment: String?,
        receiptNumber: String?,
        comments: String?,
        receiptImage: MultipartBody.Part?
    ): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            
            val extraPaymentBody = amountOfExtraPayment?.toRequestBody()
            val receiptNumberBody = receiptNumber?.toRequestBody()
            val commentsBody = comments?.toRequestBody()
            
            val response = api.updatePaymentDetails(
                emptyingId = emptyingId,
                amountOfExtraPayment = extraPaymentBody,
                receiptNumber = receiptNumberBody,
                comments = commentsBody,
                receiptImage = receiptImage
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    emit(Resource.Success(true))
                } else {
                    emit(Resource.Error(body?.message ?: "Failed to update payment"))
                }
            } else {
                emit(Resource.Error("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AdditionalRepairing", "Error updating payment", e)
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun saveDraft(form: AdditionalRepairingFormEntity): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val draftForm = form.copy(
                syncStatus = "DRAFT",
                updatedAt = System.currentTimeMillis()
            )
            dao.upsert(draftForm)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            Log.e("AdditionalRepairing", "Error saving draft", e)
            emit(Resource.Error(e.message ?: "Failed to save draft"))
        }
    }

    override suspend fun submitForm(
        form: AdditionalRepairingFormEntity,
        receiptImagePart: MultipartBody.Part?
    ): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            
            // Parse trip entries from JSON
            val tripEntries = try {
                val jsonArray = JSONArray(form.tripEntriesJson)
                (0 until jsonArray.length()).map { i ->
                    val tripObj = jsonArray.getJSONObject(i)
                    TripCreateRequest(
                        startTime = tripObj.getString("startTime"),
                        endTime = tripObj.getString("endTime"),
                        amountOfRegularPaymentPerTrip = tripObj.getString("amountOfRegularPayment"),
                        additionalTripRequired = tripObj.getString("additionalTripRequired")
                    )
                }
            } catch (e: Exception) {
                Log.e("AdditionalRepairing", "Error parsing trip entries", e)
                emptyList()
            }
            
            // Submit trips
            for (trip in tripEntries) {
                val tripResponse = api.createTripEntry(form.emptyingId, trip)
                if (!tripResponse.isSuccessful || tripResponse.body()?.success != true) {
                    // Save as PENDING for later sync
                    dao.upsert(form.copy(syncStatus = "PENDING", updatedAt = System.currentTimeMillis()))
                    emit(Resource.Error("Failed to submit trip. Saved for later sync."))
                    return@flow
                }
            }
            
            // Update payment details
            val extraPaymentBody = form.amountOfExtraPayment.takeIf { it.isNotEmpty() }?.toRequestBody()
            val receiptNumberBody = form.receiptNumber.takeIf { it.isNotEmpty() }?.toRequestBody()
            val commentsBody = form.comments.takeIf { it.isNotEmpty() }?.toRequestBody()
            
            val paymentResponse = api.updatePaymentDetails(
                emptyingId = form.emptyingId,
                amountOfExtraPayment = extraPaymentBody,
                receiptNumber = receiptNumberBody,
                comments = commentsBody,
                receiptImage = receiptImagePart
            )
            
            if (paymentResponse.isSuccessful && paymentResponse.body()?.success == true) {
                // Mark as synced
                dao.upsert(form.copy(syncStatus = "SYNCED", updatedAt = System.currentTimeMillis()))
                emit(Resource.Success(true))
            } else {
                // Save as PENDING
                dao.upsert(form.copy(syncStatus = "PENDING", updatedAt = System.currentTimeMillis()))
                emit(Resource.Error("Failed to update payment. Saved for later sync."))
            }
        } catch (e: Exception) {
            Log.e("AdditionalRepairing", "Error submitting form", e)
            // Save as PENDING on network error
            dao.upsert(form.copy(syncStatus = "PENDING", updatedAt = System.currentTimeMillis()))
            emit(Resource.Error("Network error. Form saved for later sync."))
        }
    }

    override suspend fun getSavedDraft(emptyingId: Int): Flow<Resource<AdditionalRepairingFormEntity?>> = flow {
        try {
            emit(Resource.Loading())
            val draft = dao.getFormByEmptyingId(emptyingId)
            emit(Resource.Success(draft))
        } catch (e: Exception) {
            Log.e("AdditionalRepairing", "Error getting draft", e)
            emit(Resource.Error(e.message ?: "Failed to load draft"))
        }
    }

    override suspend fun getUnsyncedForms(): List<AdditionalRepairingFormEntity> {
        return try {
            dao.getUnsyncedForms()
        } catch (e: Exception) {
            Log.e("AdditionalRepairing", "Error getting unsynced forms", e)
            emptyList()
        }
    }

    override suspend fun syncForm(form: AdditionalRepairingFormEntity): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            
            // Parse and submit trips
            val tripEntries = try {
                val jsonArray = JSONArray(form.tripEntriesJson)
                (0 until jsonArray.length()).map { i ->
                    val tripObj = jsonArray.getJSONObject(i)
                    TripCreateRequest(
                        startTime = tripObj.getString("startTime"),
                        endTime = tripObj.getString("endTime"),
                        amountOfRegularPaymentPerTrip = tripObj.getString("amountOfRegularPayment"),
                        additionalTripRequired = tripObj.getString("additionalTripRequired")
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
            
            for (trip in tripEntries) {
                val tripResponse = api.createTripEntry(form.emptyingId, trip)
                if (!tripResponse.isSuccessful || tripResponse.body()?.success != true) {
                    dao.updateSyncStatus(form.emptyingId, "FAILED")
                    emit(Resource.Error("Sync failed for trip"))
                    return@flow
                }
            }
            
            // Update payment
            val extraPaymentBody = form.amountOfExtraPayment.takeIf { it.isNotEmpty() }?.toRequestBody()
            val receiptNumberBody = form.receiptNumber.takeIf { it.isNotEmpty() }?.toRequestBody()
            val commentsBody = form.comments.takeIf { it.isNotEmpty() }?.toRequestBody()
            
            val paymentResponse = api.updatePaymentDetails(
                emptyingId = form.emptyingId,
                amountOfExtraPayment = extraPaymentBody,
                receiptNumber = receiptNumberBody,
                comments = commentsBody,
                receiptImage = null // Can't sync images from cache
            )
            
            if (paymentResponse.isSuccessful && paymentResponse.body()?.success == true) {
                dao.markAsSynced(form.emptyingId)
                emit(Resource.Success(true))
            } else {
                dao.updateSyncStatus(form.emptyingId, "FAILED")
                emit(Resource.Error("Sync failed for payment"))
            }
        } catch (e: Exception) {
            dao.updateSyncStatus(form.emptyingId, "FAILED")
            emit(Resource.Error(e.message ?: "Sync failed"))
        }
    }
}
