package com.innovative.smis.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EmptyingSchedulingFormRequest(
    @Json(name = "sanitation_customer_id") val sanitationCustomerId: String?,
    @Json(name = "customer_name") val customerName: String?,
    @Json(name = "customer_phone") val customerPhone: String?,
    @Json(name = "customer_address") val customerAddress: String?,
    @Json(name = "applicant_name") val applicantName: String?,
    @Json(name = "applicant_contact") val applicantContact: String?,
    @Json(name = "emptying_purpose") val emptyingPurpose: String?,
    @Json(name = "other_emptying_purpose") val otherEmptyingPurpose: String?,
    @Json(name = "proposed_emptying_date") val proposedEmptyingDate: String?,
    @Json(name = "last_emptied_date") val lastEmptiedDate: String?,
    @Json(name = "emptied_nodate_reason") val emptiedNodateReason: String?,
    @Json(name = "not_emptied_before_reason") val notEmptiedBeforeReason: String?,
    @Json(name = "other_not_emptied_before_reason") val otherNotEmptiedBeforeReason: String?,
    @Json(name = "additional_repairing") val additionalRepairing: String?,
    @Json(name = "other_additional_repairing") val otherAdditionalRepairing: String?,
    @Json(name = "free_service_under_pbc") val freeServiceUnderPbc: String?,
    @Json(name = "extra_payment_required") val extraPaymentRequired: String?,
    @Json(name = "amount_of_extra_payment") val amountOfExtraPayment: String?,
    @Json(name = "amount_of_regular_payment") val amountOfRegularPayment: String?,
    @Json(name = "site_visit_required") val siteVisitRequired: String?,
    @Json(name = "size_of_storage_tank_m3") val sizeOfStorageTankM3: String?,
    @Json(name = "construction_year") val constructionYear: String?,
    @Json(name = "accessibility") val accessibility: String?,
    @Json(name = "ever_emptied") val everEmptied: String?,
    @Json(name = "last_emptied_year") val lastEmptiedYear: String?,
    @Json(name = "issues_with_containment") val issuesWithContainment: String?,
    @Json(name = "other_issues_with_containment") val otherIssuesWithContainment: String?
)