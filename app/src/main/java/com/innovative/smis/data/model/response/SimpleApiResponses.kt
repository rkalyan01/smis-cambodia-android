package com.innovative.smis.data.model.response

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
data class EmptyingReadonlyData(
    val application_id: Int,
    val eto_id: Int,
    val desludging_vehicle_id: Int?,
    val application_datetime: String,
    val applicant_name: String,
    val applicant_contact: String,
    val free_service_under_pbc: Boolean,
    val additional_repairing: String?,
    val other_additional_repairing: String?,
    val extra_payment_required: Boolean,
    val amount_of_extra_payment: String?
)

data class EmptyingReadonlyDataResponse(
    val success: Boolean,
    val message: String? = null,
    val data: EmptyingReadonlyData? = null
)