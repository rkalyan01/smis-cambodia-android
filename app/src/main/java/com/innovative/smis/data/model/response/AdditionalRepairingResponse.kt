package com.innovative.smis.data.model.response

import com.innovative.smis.util.adapter.PostgresArrayInt
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TripFilterResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: List<TripFilterApplication>?
)

@JsonClass(generateAdapter = true)
data class TripFilterApplicationResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: TripFilterApplication?
)

@JsonClass(generateAdapter = true)
data class TripFilterApplication(
    @Json(name = "id") val id: Int,
    @Json(name = "emptying_id") val emptyingId: Int?,
    @Json(name = "application_datetime") val applicationDatetime: String?,
    @Json(name = "applicant_name") val applicantName: String?,
    @Json(name = "applicant_contact") val applicantContact: String?,
    @Json(name = "phone_no") val phoneNo: String?,
    @Json(name = "proposed_emptying_date") val proposedEmptyingDate: String?,
    @Json(name = "application_status") val applicationStatus: String?,
    @Json(name = "additional_trip_required") val additionalTripRequired: Boolean?
)

@JsonClass(generateAdapter = true)
data class RegularPaymentAmountResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "amount") val amount: String?
)

@JsonClass(generateAdapter = true)
data class TripCreateResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?
)

@JsonClass(generateAdapter = true)
data class PaymentUpdateResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?
)

@JsonClass(generateAdapter = true)
data class EmptyingDetailsResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "application_id") val applicationId: Int?,
    @Json(name = "emptied_date") val emptiedDate: String?,
    @Json(name = "volume_of_sludge") val volumeOfSludge: String?,
    @Json(name = "no_of_trips") val noOfTrips: Int?,
    @Json(name = "sludge_type_a") val sludgeTypeA: String?,
    @Json(name = "sludge_type_b") val sludgeTypeB: String?,
    @Json(name = "location_of_containment") val locationOfContainment: String?,
    @Json(name = "presence_of_pumping_point") val presenceOfPumpingPoint: String?,
    @PostgresArrayInt @Json(name = "additional_repairing_id") val additionalRepairingId: Int?,
    @Json(name = "other_additional_repairing") val otherAdditionalRepairing: String?,
    @Json(name = "extra_payment") val extraPayment: String?,
    @Json(name = "issues_during_emptying_service") val issuesDuringEmptyingService: String?,
    @Json(name = "desludging_vehicle_id") val desludgingVehicleId: Int?,
    @Json(name = "eto_id") val etoId: Int?,
    @Json(name = "receipt_number") val receiptNumber: String?,
    @Json(name = "receipt_image") val receiptImage: String?,
    @Json(name = "picture_of_emptying") val pictureOfEmptying: String?,
    @Json(name = "comments") val comments: String?,
    @Json(name = "user_id") val userId: Int?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "deleted_at") val deletedAt: String?,
    @Json(name = "pumping_point_type") val pumpingPointType: String?,
    @Json(name = "service_receiver_name") val serviceReceiverName: String?,
    @Json(name = "service_receiver_contact") val serviceReceiverContact: String?,
    @Json(name = "total_no_of_trips") val totalNoOfTrips: Int?,
    @Json(name = "total_amount_of_regular_payment") val totalAmountOfRegularPayment: String?
)

@JsonClass(generateAdapter = true)
data class EmptyingDetailsData(
    @Json(name = "id") val id: Int,
    @Json(name = "amount_of_regular_payment") val amountOfRegularPayment: String?,
    @Json(name = "amount_of_extra_payment") val amountOfExtraPayment: String?,
    @Json(name = "receipt_number") val receiptNumber: String?,
    @Json(name = "receipt_image") val receiptImage: String?,
    @Json(name = "comments") val comments: String?
)
