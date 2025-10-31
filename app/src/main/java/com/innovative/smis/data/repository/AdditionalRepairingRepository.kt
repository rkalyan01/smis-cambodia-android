package com.innovative.smis.data.repository

import com.innovative.smis.data.local.entity.AdditionalRepairingFormEntity
import com.innovative.smis.data.model.request.PaymentUpdateRequest
import com.innovative.smis.data.model.request.TripCreateRequest
import com.innovative.smis.data.model.response.EmptyingDetailsData
import com.innovative.smis.data.model.response.EmptyingDetailsResponse
import com.innovative.smis.data.model.response.TripFilterApplication
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody

interface AdditionalRepairingRepository {
    
    suspend fun getTripFilterApplications(
        applicationStatus: String,
        etoId: String,
        additionalTripRequired: String
    ): Flow<Resource<List<TripFilterApplication>>>
    
    suspend fun getRegularPaymentAmount(): Flow<Resource<String>>
    
    suspend fun getApplicationByEmptyingId(
        emptyingId: Int
    ): Flow<Resource<EmptyingDetailsResponse>>
    
    suspend fun createTripEntry(
        emptyingId: Int,
        request: TripCreateRequest
    ): Flow<Resource<Boolean>>
    
    suspend fun getEmptyingDetails(
        emptyingId: Int
    ): Flow<Resource<EmptyingDetailsData>>
    
    suspend fun updatePaymentDetails(
        emptyingId: Int,
        amountOfExtraPayment: String?,
        receiptNumber: String?,
        comments: String?,
        receiptImage: MultipartBody.Part?
    ): Flow<Resource<Boolean>>
    
    // Offline support
    suspend fun saveDraft(form: AdditionalRepairingFormEntity): Flow<Resource<Boolean>>
    
    suspend fun submitForm(
        form: AdditionalRepairingFormEntity,
        receiptImagePart: MultipartBody.Part?
    ): Flow<Resource<Boolean>>
    
    suspend fun getSavedDraft(emptyingId: Int): Flow<Resource<AdditionalRepairingFormEntity?>>
    
    suspend fun getUnsyncedForms(): List<AdditionalRepairingFormEntity>
    
    suspend fun syncForm(form: AdditionalRepairingFormEntity): Flow<Resource<Boolean>>
}
