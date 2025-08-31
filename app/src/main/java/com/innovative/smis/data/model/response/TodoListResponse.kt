package com.innovative.smis.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TodoListResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: List<TodoItem>?
)

@JsonClass(generateAdapter = true)
data class TodoItem(
    @Json(name = "id") val applicationId: Int,
    @Json(name = "application_datetime") val applicationDatetime: String?,
    @Json(name = "applicant_name") val applicantName: String?,
    @Json(name = "applicant_contact") val applicantContact: String?,
    @Json(name = "proposed_emptying_date") val proposedEmptyingDate: String?,
    @Json(name = "application_status") val status: String? = "Initiated"
)

enum class TodoStatus(val value: String, val displayName: String) {
    INITIATED("Initiated", "Initiated"),
    SCHEDULED("Scheduled", "Scheduled"),
    RESCHEDULED("Rescheduled", "Rescheduled"),
    SITE_PREPARATION("Site-Preparation", "Site Preparation"),
    EMPTIED("Emptied", "Emptied"),
    COMPLETED("Completed", "Completed"),
    PENDING("Pending", "Pending"),
    CANCELLED("Cancelled", "Cancelled"),
    REASSIGNED("Reassigned", "Reassigned");

    companion object {
        fun fromValue(value: String): TodoStatus? {
            return values().find { it.value.equals(value, ignoreCase = true) }
        }

        fun getAllStatuses(): List<String> {
            return values().map { it.value }
        }
    }
}

@JsonClass(generateAdapter = true)
data class TodoFilter(
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val status: String? = null,
    val isToday: Boolean = false,
    val isThisWeek: Boolean = false,
    val isThisMonth: Boolean = false
)