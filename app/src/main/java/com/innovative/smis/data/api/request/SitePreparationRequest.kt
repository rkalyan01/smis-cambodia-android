package com.innovative.smis.data.api.request

data class SitePreparationRequest(
    val applicant_name: String?,
    val applicant_contact: String?,
    val customer_name: String?,
    val customer_contact: String?,
    val is_customer_same_as_applicant: Boolean?,
    val purpose_of_emptying_request: String?,
    val other_emptying_purpose: String?,
    val ever_emptied: Boolean?,
    val last_emptied_date: String?,
    val not_emptied_before_reason: String?,
    val reason_for_no_emptied_date: String?,
    val free_service_under_pbc: Boolean?,
    val additional_repairing: String?,
    val other_additional_repairing: String?,
    val extra_payment_required: Boolean?,
    val amount_of_extra_payment: String?,
    val propose_emptying_date: String?,
    val need_reschedule: Boolean?,
    val new_propose_emptying_date: String?,
    val proposed_emptying_date: String?
)