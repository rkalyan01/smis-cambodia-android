package com.innovative.smis.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WorkflowStep(
    @Json(name = "id")
    val id: String,

    @Json(name = "taskId")
    val taskId: String,

    @Json(name = "stepType")
    val stepType: WorkflowStepType,

    @Json(name = "stepOrder")
    val stepOrder: Int,

    @Json(name = "status")
    val status: WorkflowStepStatus,

    @Json(name = "formData")
    val formData: Map<String, Any>?,

    @Json(name = "completedBy")
    val completedBy: String?,

    @Json(name = "completedAt")
    val completedAt: String?,

    @Json(name = "notes")
    val notes: String?,

    @Json(name = "createdAt")
    val createdAt: String,

    @Json(name = "updatedAt")
    val updatedAt: String
)

enum class WorkflowStepType(val displayName: String, val description: String) {
    @Json(name = "empty_scheduling")
    EMPTY_SCHEDULING("Empty Scheduling", "Schedule and prepare for sewer emptying"),
    
    @Json(name = "site_preparation")
    SITE_PREPARATION("Site Preparation", "Prepare the site and equipment for emptying"),
    
    @Json(name = "emptying")
    EMPTYING("Emptying", "Execute the sewer emptying process"),
    
    @Json(name = "reassignment")
    REASSIGNMENT("Reassignment", "Reassign task due to complications or issues"),

    @Json(name = "completion_verification")
    COMPLETION_VERIFICATION("Completion Verification", "Verify and document completion"),

    @Json(name = "quality_check")
    QUALITY_CHECK("Quality Check", "Quality assurance and final inspection")
}

enum class WorkflowStepStatus(val displayName: String) {
    @Json(name = "pending")
    PENDING("Pending"),
    
    @Json(name = "in_progress")
    IN_PROGRESS("In Progress"),
    
    @Json(name = "completed")
    COMPLETED("Completed"),
    
    @Json(name = "skipped")
    SKIPPED("Skipped"),

    @Json(name = "blocked")
    BLOCKED("Blocked")
}

sealed class WorkflowFormData {
    @JsonClass(generateAdapter = true)
    data class EmptySchedulingData(
        val scheduledDateTime: String,
        val equipmentRequired: List<String>,
        val estimatedDuration: Int,
        val specialInstructions: String?
    ) : WorkflowFormData()

    @JsonClass(generateAdapter = true)
    data class SitePreparationData(
        val accessConfirmed: Boolean,
        val equipmentSetup: Boolean,
        val safetyMeasures: Boolean,
        val preWorkPhotos: List<String>
    ) : WorkflowFormData()

    @JsonClass(generateAdapter = true)
    data class EmptyingData(
        val startTime: String,
        val endTime: String,
        val volumeEmptied: Double?,
        val equipmentUsed: List<String>,
        val issuesEncountered: String?,
        val postWorkPhotos: List<String>
    ) : WorkflowFormData()

    @JsonClass(generateAdapter = true)
    data class CompletionVerificationData(
        val workCompleted: Boolean,
        val clientSignature: String?,
        val qualityRating: Int,
        val finalNotes: String?
    ) : WorkflowFormData()
}