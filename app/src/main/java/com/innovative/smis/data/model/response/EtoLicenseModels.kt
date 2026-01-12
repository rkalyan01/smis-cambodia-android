package com.innovative.smis.data.model.response

import com.squareup.moshi.Json

data class EtoLicenseResponse(
    @Json(name = "status") val status: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "count") val count: Int,
    @Json(name = "data") val data: List<EtoLicenseData>
)

data class EtoLicenseData(
    @Json(name = "id") val id: Int,
    @Json(name = "company_name") val companyName: String?,
    @Json(name = "license_number") val licenseNumber: String?,
    @Json(name = "registration_date") val registrationDate: String?,
    @Json(name = "pbc_status") val pbcStatus: String?,
    @Json(name = "pbc_contract_id") val pbcContractId: String?,
    @Json(name = "contract_start_date") val contractStartDate: String?,
    @Json(name = "contract_period") val contractPeriod: String?,
    @Json(name = "contract_expiration_date") val contractExpirationDate: String?,
    @Json(name = "renewal_history") val renewalHistory: List<RenewalHistoryItem> = emptyList(),
    @Json(name = "termination_history") val terminationHistory: List<TerminationHistoryItem> = emptyList()
)

data class RenewalHistoryItem(
    @Json(name = "prev_expiration_date") val prevExpirationDate: String?,
    @Json(name = "renew_date") val renewDate: String?,
    @Json(name = "new_expiration_date") val newExpirationDate: String?
)

data class TerminationHistoryItem(
    @Json(name = "termination_date") val terminationDate: String?, // Assuming JSON key based on screenshot
    @Json(name = "terminated_by") val terminatedBy: String?,       // Assuming JSON key
    @Json(name = "termination_cause") val terminationCause: String?, // Assuming JSON key
    @Json(name = "remark") val remark: String?
)