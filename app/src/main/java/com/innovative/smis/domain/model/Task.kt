package com.innovative.smis.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
data class Task(
    @Json(name = "id")
    val id: String,

    @Json(name = "title")
    val title: String,

    @Json(name = "description")
    val description: String?,

    @Json(name = "location")
    val location: String,

    @Json(name = "priority")
    val priority: TaskPriority,

    @Json(name = "status")
    val status: TaskStatus,

    @Json(name = "estimatedDuration")
    val estimatedDuration: Int?,

    @Json(name = "assignedToUserId")
    val assignedToUserId: String,

    @Json(name = "scheduledDate")
    val scheduledDate: String,

    @Json(name = "completedDate")
    val completedDate: String?,

    @Json(name = "notes")
    val notes: String?,

    @Json(name = "createdAt")
    val createdAt: String,

    @Json(name = "updatedAt")
    val updatedAt: String
)

enum class TaskPriority(val displayName: String, val colorHex: String) {
    @Json(name = "low")
    LOW("Low", "#28a745"),
    
    @Json(name = "medium")
    MEDIUM("Medium", "#ffc107"),
    
    @Json(name = "high")
    HIGH("High", "#fd7e14"),
    
    @Json(name = "urgent")
    URGENT("Urgent", "#dc3545")
}

enum class TaskStatus(val displayName: String) {
    @Json(name = "pending")
    PENDING("Pending"),
    
    @Json(name = "in_progress")
    IN_PROGRESS("In Progress"),
    
    @Json(name = "completed")
    COMPLETED("Completed"),
    
    @Json(name = "cancelled")
    CANCELLED("Cancelled"),
    
    @Json(name = "rescheduled")
    RESCHEDULED("Rescheduled")
}