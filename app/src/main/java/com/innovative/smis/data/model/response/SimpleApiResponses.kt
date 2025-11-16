package com.innovative.smis.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Simple response classes that don't require KSP code generation
// These are used for dropdown data endpoints that were causing compilation issues

data class SimpleDropdownResponse(
    val success: Boolean,
    val message: String? = null,
    val data: Map<String, String>? = null
)

data class SimpleApiResponse(
    val success: Boolean,
    val message: String? = null,
    val data: Any? = null
)

// Type aliases for backward compatibility
typealias EmptyingReasonsResponse = SimpleDropdownResponse
typealias ContainmentIssuesResponse = SimpleDropdownResponse
typealias EmptyingReasonResponse = SimpleDropdownResponse

// Emptying Service readonly data response
@JsonClass(generateAdapter = true)
data class EmptyingReadonlyData(
    @Json(name = "application_id") val applicationId: Int,
    @Json(name = "eto_id") val etoId: Int,
    @Json(name = "desludging_vehicle_id") val desludgingVehicleId: Int?,
    @Json(name = "sanitation_customer_id") val sanitationCustomerId: String?,
    @Json(name = "application_datetime") val applicationDatetime: String,
    @Json(name = "applicant_name") val applicantName: String?, // Nullable because API can return null
    @Json(name = "applicant_contact") val applicantContact: String?, // Nullable because API can return null
    @Json(name = "issues_with_containment") val issuesWithContainment: String?,
    @Json(name = "free_service_under_pbc") val freeServiceUnderPbc: Boolean,
    @Json(name = "amount_of_regular_payment") val amountOfRegularPayment: String?,
    @Json(name = "additional_repairing") val additionalRepairing: String?,
    @Json(name = "other_additional_repairing") val otherAdditionalRepairing: String?,
    @Json(name = "extra_payment_required") val extraPaymentRequired: Boolean,
    @Json(name = "amount_of_extra_payment") val amountOfExtraPayment: String?,
    @Json(name = "building_point_geom_exist") val buildingPointGeomExist: Boolean?,
    @Json(name = "latitude") val latitude: String?,
    @Json(name = "longitude") val longitude: String?
)

@JsonClass(generateAdapter = true)
data class EmptyingReadonlyDataResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: EmptyingReadonlyData? = null
)