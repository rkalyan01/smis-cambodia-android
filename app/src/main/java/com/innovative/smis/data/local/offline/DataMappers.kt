package com.innovative.smis.data.local.offline

import com.innovative.smis.data.local.entity.*
import com.innovative.smis.domain.model.*

// Task conversions
fun TaskEntity.toTask(): Task {
    return Task(
        id = this.id,
        title = this.title,
        description = this.description ?: "",
        status = com.innovative.smis.domain.model.TaskStatus.valueOf(this.status.uppercase()),
        priority = com.innovative.smis.domain.model.TaskPriority.valueOf(this.priority.uppercase()),
        location = this.location ?: "",
        estimatedDuration = this.estimatedDuration ?: 0,
        assignedToUserId = this.assignedTo ?: "",
        scheduledDate = this.createdAt,
        completedDate = this.updatedAt,
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = this.id,
        applicationId = this.id, // Default mapping
        title = this.title,
        description = this.description,
        status = this.status.name,
        priority = this.priority.name,
        assignedTo = this.assignedToUserId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        dueDate = this.scheduledDate,
        location = this.location,
        customerName = null,
        customerContact = null,
        estimatedDuration = this.estimatedDuration,
        actualDuration = null,
        notes = this.notes,
        syncStatus = "PENDING",
        isDeleted = false
    )
}

// WorkflowStep conversions
fun WorkflowStepEntity.toWorkflowStep(): WorkflowStep {
    return WorkflowStep(
        id = this.id,
        taskId = this.taskId,
        stepType = WorkflowStepType.valueOf(
            this.stepName.uppercase().replace(" ", "_")
        ),
        stepOrder = this.stepOrder,
        status = if (this.isCompleted) 
            WorkflowStepStatus.COMPLETED
        else 
            WorkflowStepStatus.PENDING,
        formData = this.formData?.let { 
            try { 
                mapOf("data" to it)
            } catch (e: Exception) { 
                null 
            }
        },
        completedBy = this.completedBy,
        completedAt = this.completedAt,
        notes = this.notes,
        createdAt = System.currentTimeMillis().toString(),
        updatedAt = System.currentTimeMillis().toString()
    )
}

fun WorkflowStep.toEntity(): WorkflowStepEntity {
    return WorkflowStepEntity(
        id = this.id,
        taskId = this.taskId,
        stepName = this.stepType.displayName,
        stepOrder = this.stepOrder,
        isCompleted = this.status == WorkflowStepStatus.COMPLETED,
        formData = this.formData?.toString(),
        completedBy = this.completedBy,
        completedAt = this.completedAt,
        notes = this.notes,
        syncStatus = "PENDING",
        isDeleted = false
    )
}

fun WorkflowStep.toWorkflowStepEntity(): WorkflowStepEntity = this.toEntity()

fun BuildingSurveyEntity.toBuildingSurvey(): BuildingSurvey {
    return BuildingSurvey(
        id = this.id,
        bin = this.bin,
        sangkat = this.sangkat,
        village = this.village,
        roadCode = this.roadCode,
        respondentName = this.respondentName,
        respondentGender = this.respondentGender,
        respondentContact = this.respondentContact,
        ownerName = this.ownerName,
        ownerContact = this.ownerContact,
        structureTypeId = this.structureTypeId,
        functionalUseId = this.functionalUseId,
        buildingUseId = this.buildingUseId,
        numberOfFloors = this.numberOfFloors,
        householdServed = this.householdServed,
        populationServed = this.populationServed,
        isMainBuilding = this.isMainBuilding,
        floorArea = this.floorArea,
        constructionYear = this.constructionYear,
        waterSupply = this.waterSupply,
        defecationPlaceId = this.defecationPlaceId,
        numberOfToilets = this.numberOfToilets,
        toiletConnectionId = this.toiletConnectionId,
        toiletCount = this.toiletCount,
        containmentPresentOnsite = this.containmentPresentOnsite,
        typeOfStorageTank = this.typeOfStorageTank,
        storageTankConnection = this.storageTankConnection,
        numberOfTanks = this.numberOfTanks,
        sizeOfTank = this.sizeOfTank,
        distanceFromWell = this.distanceFromWell,
        constructionDate = this.constructionDate,
        lastEmptiedDate = this.lastEmptiedDate,
        vacutugAccessible = this.vacutugAccessible,
        containmentLocation = this.containmentLocation,
        accessToContainment = this.accessToContainment,
        distanceHouseToContainment = this.distanceHouseToContainment,
        waterCustomerId = this.waterCustomerId,
        meterSerialNumber = this.meterSerialNumber,
        sanitationSystem = this.sanitationSystem,
        technology = this.technology,
        compliance = this.compliance,
        comments = this.comments,
        surveyedBy = this.surveyedBy ?: "",
        surveyDate = this.surveyDate ?: "",
        createdAt = this.createdAt ?: System.currentTimeMillis(),
        updatedAt = this.updatedAt ?: System.currentTimeMillis()
    )
}

fun BuildingSurvey.toEntity(): BuildingSurveyEntity {
    return BuildingSurveyEntity(
        id = this.id,
        bin = this.bin,
        sangkat = this.sangkat,
        village = this.village,
        roadCode = this.roadCode,
        respondentName = this.respondentName,
        respondentGender = this.respondentGender,
        respondentContact = this.respondentContact,
        ownerName = this.ownerName,
        ownerContact = this.ownerContact,
        structureTypeId = this.structureTypeId,
        functionalUseId = this.functionalUseId,
        buildingUseId = this.buildingUseId,
        numberOfFloors = this.numberOfFloors,
        householdServed = this.householdServed,
        populationServed = this.populationServed,
        isMainBuilding = this.isMainBuilding,
        floorArea = this.floorArea,
        constructionYear = this.constructionYear,
        waterSupply = this.waterSupply,
        defecationPlaceId = this.defecationPlaceId,
        numberOfToilets = this.numberOfToilets,
        toiletConnectionId = this.toiletConnectionId,
        toiletCount = this.toiletCount,
        containmentPresentOnsite = this.containmentPresentOnsite,
        typeOfStorageTank = this.typeOfStorageTank,
        storageTankConnection = this.storageTankConnection,
        numberOfTanks = this.numberOfTanks,
        sizeOfTank = this.sizeOfTank,
        distanceFromWell = this.distanceFromWell,
        constructionDate = this.constructionDate,
        lastEmptiedDate = this.lastEmptiedDate,
        vacutugAccessible = this.vacutugAccessible,
        containmentLocation = this.containmentLocation,
        accessToContainment = this.accessToContainment,
        distanceHouseToContainment = this.distanceHouseToContainment,
        waterCustomerId = this.waterCustomerId,
        meterSerialNumber = this.meterSerialNumber,
        sanitationSystem = this.sanitationSystem,
        technology = this.technology,
        compliance = this.compliance,
        comments = this.comments,
        surveyedBy = this.surveyedBy ?: "",
        surveyDate = this.surveyDate ?: "",
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        syncStatus = "PENDING",
        isDeleted = false
    )
}

fun List<TaskEntity>.toTasks(): List<Task> = this.map { it.toTask() }
fun List<Task>.toTaskEntities(): List<TaskEntity> = this.map { it.toEntity() }

fun List<WorkflowStepEntity>.toWorkflowSteps(): List<WorkflowStep> = this.map { it.toWorkflowStep() }
fun List<WorkflowStep>.toWorkflowStepEntities(): List<WorkflowStepEntity> = this.map { it.toEntity() }

fun List<BuildingSurveyEntity>.toBuildingSurveys(): List<com.innovative.smis.domain.model.BuildingSurvey> = this.map { it.toBuildingSurvey() }
fun List<com.innovative.smis.domain.model.BuildingSurvey>.toBuildingSurveyEntities(): List<BuildingSurveyEntity> = this.map { it.toEntity() }

fun TaskEntity.markForSync(): TaskEntity = this.copy(syncStatus = "PENDING")
fun WorkflowStepEntity.markForSync(): WorkflowStepEntity = this.copy(syncStatus = "PENDING")
fun BuildingSurveyEntity.markForSync(): BuildingSurveyEntity = this.copy(syncStatus = "PENDING")
