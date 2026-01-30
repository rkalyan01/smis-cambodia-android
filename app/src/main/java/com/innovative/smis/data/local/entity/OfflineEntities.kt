package com.innovative.smis.data.local.entity

import androidx.room.*
import com.innovative.smis.data.model.response.TodoItem

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val applicationId: String,
    val title: String,
    val description: String?,
    val status: String,
    val priority: String,
    val assignedTo: String?,
    val createdAt: String,
    val updatedAt: String,
    val dueDate: String?,
    val location: String?,
    val customerName: String?,
    val customerContact: String?,
    val estimatedDuration: Int?,
    val actualDuration: Int?,
    val notes: String?,
    val syncStatus: String = "SYNCED", // SYNCED, PENDING, FAILED
    val isDeleted: Boolean = false
)

@Entity(tableName = "workflow_steps")
data class WorkflowStepEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val stepName: String,
    val stepOrder: Int,
    val isCompleted: Boolean,
    val completedAt: String?,
    val completedBy: String?,
    val formData: String?, // JSON string
    val notes: String?,
    val syncStatus: String = "SYNCED",
    val isDeleted: Boolean = false
)

// BuildingSurveyEntity moved to separate file in this package

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String?,
    val name: String,
    val role: String,
    val phoneNumber: String?,
    val department: String?,
    val isActive: Boolean,
    val createdAt: String,
    val lastLoginAt: String?,
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "survey_dropdowns")
data class SurveyDropdownEntity(
    @PrimaryKey val id: String,
    val type: String, // structure_types, functional_uses, etc.
    val value: String,
    val displayName: String,
    val category: String,
    val isActive: Boolean = true,
    val syncedAt: String
)

@Entity(tableName = "wfs_buildings")
data class WfsBuildingEntity(
    @PrimaryKey val id: String,
    val bin: String?,
    val sangkat: String?,
    val isSurveyed: Boolean?,
    val isAuxiliary: Boolean?,
    val geometryType: String,
    val coordinates: String, // JSON string of coordinates
    val syncedAt: String
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val entityType: String, // task, survey, etc.
    val entityId: String,
    val operation: String, // CREATE, UPDATE, DELETE
    val data: String, // JSON data to sync
    val retryCount: Int,
    val maxRetries: Int,
    val lastAttempt: Long?,
    val createdAt: Long,
    val errorMessage: String?,
    val priority: Int // Higher numbers = higher priority
)

@Entity(tableName = "applications")
data class TodoItemEntity(
    @PrimaryKey
    val applicationId: Int,
    val applicantName: String?,
    val applicantContact: String?,
    val ownerName: String?,
    val phoneNo: String?,
    val proposedEmptyingDate: String?,
    val status: String?,
    val applicationDatetime: String?,
    val buildingPointGeomExist: Boolean?,
    val longitude: String?,
    val latitude: String?,
    val lastUpdated: Long = System.currentTimeMillis(), // Track when record was last updated
    val cacheExpiry: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours cache validity
)

fun TodoItem.toEntity(): TodoItemEntity {
    val currentTime = System.currentTimeMillis()
    return TodoItemEntity(
        applicationId = this.applicationId,
        applicantName = this.applicantName,
        applicantContact = this.applicantContact,
        ownerName = this.ownerName,
        phoneNo = this.phoneNo,
        proposedEmptyingDate = this.proposedEmptyingDate,
        status = this.status ?: "",
        applicationDatetime = this.applicationDatetime,
        buildingPointGeomExist = this.buildingPointGeomExist,
        longitude = this.longitude,
        latitude = this.latitude,
        lastUpdated = currentTime,
        cacheExpiry = currentTime + (24 * 60 * 60 * 1000) + 1000 // Add 1 second buffer to prevent timing issues
    )
}

fun TodoItemEntity.toDomainModel(): TodoItem {
    return TodoItem(
        applicationId = this.applicationId,
        applicantName = this.applicantName,
        applicantContact = this.applicantContact,
        ownerName = this.ownerName,
        phoneNo = this.phoneNo,
        proposedEmptyingDate = this.proposedEmptyingDate,
        status = this.status,
        applicationDatetime = this.applicationDatetime,
        buildingPointGeomExist = this.buildingPointGeomExist,
        longitude = this.longitude,
        latitude = this.latitude
    )
}

// Relation classes for complex queries
data class TaskWithSteps(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val steps: List<WorkflowStepEntity>
)

data class SurveyWithLocation(
    @Embedded val survey: BuildingSurveyEntity,
    @Relation(
        parentColumn = "bin",
        entityColumn = "bin"
    )
    val building: WfsBuildingEntity?
)

enum class EntityType {
    TASK,
    WORKFLOW_STEP,
    BUILDING_SURVEY,
    EMPTYING_FORM,
    USER,
    EMPTYING_SCHEDULING_FORM,
    SITE_PREPARATION_FORM,
    EMPTYING_SERVICE_FORM
}

enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}

// Enums for database constants
enum class SyncStatus {
    SYNCED, PENDING, FAILED, DELETED
}
