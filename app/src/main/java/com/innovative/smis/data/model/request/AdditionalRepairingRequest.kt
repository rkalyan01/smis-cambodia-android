package com.innovative.smis.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TripCreateRequest(
    @Json(name = "start_time") val startTime: String,
    @Json(name = "end_time") val endTime: String,
    @Json(name = "amount_of_regular_payment_per_trip") val amountOfRegularPaymentPerTrip: String,
    @Json(name = "additional_trip_required") val additionalTripRequired: String
)

@JsonClass(generateAdapter = true)
data class PaymentUpdateRequest(
    @Json(name = "amount_of_extra_payment") val amountOfExtraPayment: String?,
    @Json(name = "receipt_number") val receiptNumber: String?,
    @Json(name = "receipt_image") val receiptImage: String?,
    @Json(name = "comments") val comments: String?
)

data class TripEntryUiState(
    val tripNumber: Int,
    val startTime: String = "",
    val endTime: String = "",
    val amountOfRegularPayment: String = "",
    val additionalTripRequired: String = "",
    val timeError: String? = null
)
