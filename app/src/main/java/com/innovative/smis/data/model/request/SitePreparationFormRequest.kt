package com.innovative.smis.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SitePreparationFormRequest(
    @Json(name = "application_id") val applicationId: Int,
    @Json(name = "site_prep_date") val sitePrepDate: String?,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "customer_contact") val customerContact: String,
    @Json(name = "additional_repairing") val additionalRepairing: String?,
    @Json(name = "extra_payment_required") val extraPaymentRequired: String?,
    @Json(name = "amount_of_extra_payment") val amountOfExtraPayment: String?,
    @Json(name = "applicant_name") val applicantName: String?,
    @Json(name = "applicant_contact") val applicantContact: String?,
    @Json(name = "emptying_purpose") val emptyingPurpose: String?,
    @Json(name = "proposed_emptying_date") val proposedEmptyingDate: String?,
    @Json(name = "last_emptied_date") val lastEmptiedDate: String?,
    @Json(name = "not_emptied_before_reason") val notEmptiedBeforeReason: String?,
    @Json(name = "emptied_nodate_reason") val emptiedNodateReason: String?,
    @Json(name = "free_service_under_pbc") val freeServiceUnderPbc: String?,
    @Json(name = "type_of_storage_tank") val typeOfStorageTank: String?,
    @Json(name = "storage_tank_connection") val storageTankConnection: String?,
    @Json(name = "size_of_storage_tank_m3") val sizeOfStorageTankM3: String?,
    @Json(name = "construction_year") val constructionYear: String?,
    @Json(name = "accessibility") val accessibility: String?,
    @Json(name = "ever_emptied") val everEmptied: String?
)