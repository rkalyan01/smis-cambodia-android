package com.innovative.smis.data.local.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.innovative.smis.data.local.dao.*
import com.innovative.smis.data.local.entity.*
// Extension functions are imported automatically from the entity classes
import com.innovative.smis.data.api.*
import com.innovative.smis.data.model.response.*
import com.innovative.smis.data.model.request.SitePreparationFormRequest
import com.innovative.smis.domain.model.Task
import com.innovative.smis.domain.model.TaskPriority
import com.innovative.smis.domain.model.TaskStatus
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonAdapter
// Removed LocalDateTime and DateTimeFormatter - using Long timestamps
import java.util.UUID

class OfflineManager(
    private val context: Context,
    private val taskDao: TaskDao,
    private val buildingSurveyDao: BuildingSurveyDao,
    private val workflowStepDao: WorkflowStepDao,

    private val syncQueueDao: SyncQueueDao,
    private val surveyDropdownDao: SurveyDropdownDao,
    private val wfsBuildingDao: WfsBuildingDao,
    private val emptyingSchedulingFormDao: EmptyingSchedulingFormDao,
    private val sitePreparationFormDao: SitePreparationFormDao,
    private val emptyingServiceFormDao: EmptyingServiceFormDao,
    private val emptyingApiService: EmptyingApiService,
    private val buildingSurveyApiService: BuildingSurveyApiService,
    private val emptyingSchedulingApiService: EmptyingSchedulingApiService,
    private val sitePreparationApiService: SitePreparationApiService,
    private val emptyingServiceApiService: EmptyingServiceApiService,
    private val moshi: Moshi
) {

    // Removed dateFormatter - now using Long timestamps throughout

    /**
     * Check if device has internet connectivity
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Save task offline and queue for sync
     */
    suspend fun saveTaskOffline(task: Task): Flow<Resource<Task>> = flow {
        emit(Resource.Loading())

        try {
            val taskEntity = task.toEntity().copy(
                syncStatus = "PENDING"
            )

            taskDao.insertTask(taskEntity)

            // Queue for sync
            queueForSync(
                entityType = EntityType.TASK.name,
                entityId = task.id,
                operation = SyncOperation.CREATE.name,
                data = taskToJson(task)
            )

            emit(Resource.Success(task))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to save task offline: ${e.message}"))
        }
    }

    /**
     * Save building survey offline and queue for sync
     */
    suspend fun saveBuildingSurveyOffline(bin: String, request: BuildingSurveyRequest): Flow<Resource<BuildingSurveyResponse>> = flow {
        emit(Resource.Loading())

        try {
            val surveyId = UUID.randomUUID().toString()
            val currentTime = System.currentTimeMillis()

            val surveyEntity = BuildingSurveyEntity(
                id = surveyId,
                bin = request.bin,
                sangkat = request.sangkat,
                village = request.village,
                roadCode = request.roadCode,
                respondentName = request.respondentName,
                respondentGender = request.respondentGender,
                respondentContact = request.respondentContact,
                ownerName = request.ownerName,
                ownerContact = request.ownerContact,
                structureTypeId = request.structureTypeId,
                functionalUseId = request.functionalUseId,
                buildingUseId = request.buildingUseId,
                numberOfFloors = request.numberOfFloors,
                householdServed = request.householdServed,
                populationServed = request.populationServed,
                isMainBuilding = request.isMainBuilding,
                floorArea = request.floorArea,
                constructionYear = request.constructionYear,
                waterSupply = request.waterSupply,
                defecationPlaceId = request.defecationPlaceId,
                numberOfToilets = request.numberOfToilets,
                toiletConnectionId = request.toiletConnectionId,
                toiletCount = request.toiletCount,
                containmentPresentOnsite = request.containmentPresentOnsite,
                typeOfStorageTank = request.typeOfStorageTank,
                storageTankConnection = request.storageTankConnection,
                numberOfTanks = request.numberOfTanks,
                sizeOfTank = request.sizeOfTank,
                distanceFromWell = request.distanceFromWell,
                constructionDate = request.constructionDate,
                lastEmptiedDate = request.lastEmptiedDate,
                vacutugAccessible = request.vacutugAccessible,
                containmentLocation = request.containmentLocation,
                accessToContainment = request.accessToContainment,
                distanceHouseToContainment = request.distanceHouseToContainment,
                waterCustomerId = request.waterCustomerId,
                meterSerialNumber = request.meterSerialNumber,
                sanitationSystem = request.sanitationSystem,
                technology = request.technology,
                compliance = request.compliance,
                comments = request.comments,
                surveyedBy = request.surveyedBy,
                surveyDate = request.surveyDate,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING",
                isDeleted = false
            )

            buildingSurveyDao.insertSurvey(surveyEntity)

            // Queue for sync
            queueForSync(
                entityType = EntityType.BUILDING_SURVEY.name,
                entityId = surveyId,
                operation = SyncOperation.CREATE.name,
                data = buildingSurveyRequestToJson(request)
            )

            val response = BuildingSurveyResponse(
                success = true,
                message = "Survey saved offline successfully",
                data = BuildingSurveyData(
                    id = surveyId,
                    bin = request.bin,
                    surveyData = buildingSurveyRequestToJson(request),
                    surveyedBy = request.surveyedBy,
                    surveyDate = request.surveyDate
                )
            )

            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to save survey offline: ${e.message}"))
        }
    }

    /**
     * Get tasks from local database (offline-first)
     */
    suspend fun getTasksOffline(): Flow<Resource<List<Task>>> = flow {
        emit(Resource.Loading())

        try {
            taskDao.getAllTasks().collect { taskEntities ->
                val tasks = taskEntities.map { it.toTask() }
                emit(Resource.Success(tasks))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Failed to load offline tasks: ${e.message}"))
        }
    }

    /**
     * Get building surveys from local database (offline-first)
     */
    suspend fun getBuildingSurveysOffline(): Flow<Resource<List<BuildingSurveyEntity>>> = flow {
        emit(Resource.Loading())

        try {
            buildingSurveyDao.getAllSurveys().collect { surveys ->
                emit(Resource.Success(surveys))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Failed to load offline surveys: ${e.message}"))
        }
    }

    /**
     * Sync all pending data to server when network is available
     */
    suspend fun syncAllData(): Flow<Resource<String>> = flow {
        if (!isNetworkAvailable()) {
            emit(Resource.Error("No network connection available for sync"))
            return@flow
        }

        emit(Resource.Loading())

        try {
            val pendingSyncs = syncQueueDao.getRetryableSyncs()
            var successCount = 0
            var failureCount = 0

            for (sync in pendingSyncs) {
                try {
                    when (sync.entityType) {
                        EntityType.TASK.name -> {
                            syncTask(sync)
                            successCount++
                        }
                        EntityType.BUILDING_SURVEY.name -> {
                            syncBuildingSurvey(sync)
                            successCount++
                        }
                        EntityType.WORKFLOW_STEP.name -> {
                            syncWorkflowStep(sync)
                            successCount++
                        }
                        EntityType.EMPTYING_FORM.name -> {
                            syncEmptyingForm(sync)
                            successCount++
                        }
                        EntityType.EMPTYING_SCHEDULING_FORM.name -> {
                            syncEmptyingSchedulingForm(sync)
                            successCount++
                        }
                        EntityType.SITE_PREPARATION_FORM.name -> {
                            syncSitePreparationForm(sync)
                            successCount++
                        }
                        EntityType.EMPTYING_SERVICE_FORM.name -> {
                            syncEmptyingServiceForm(sync)
                            successCount++
                        }
                    }

                    // Remove from sync queue on success
                    syncQueueDao.deleteSync(sync.id)

                } catch (e: Exception) {
                    failureCount++
                    // Update retry info
                    syncQueueDao.updateRetryInfo(
                        syncId = sync.id,
                        lastAttempt = System.currentTimeMillis(),
                        errorMessage = e.message
                    )
                }
            }

            val message = "Sync completed: $successCount successful, $failureCount failed"
            emit(Resource.Success(message))

        } catch (e: Exception) {
            emit(Resource.Error("Sync failed: ${e.message}"))
        }
    }

    /**
     * Cache dropdown options for offline use
     */
    suspend fun cacheDropdownOptions() {
        if (!isNetworkAvailable()) return

        try {
            val currentTime = System.currentTimeMillis()

            // Cache structure types
            val structureTypesResponse = buildingSurveyApiService.getStructureTypes()
            if (structureTypesResponse.isSuccessful) {
                structureTypesResponse.body()?.data?.let { options ->
                    val entities = options.map { option ->
                        SurveyDropdownEntity(
                            id = option.id,
                            type = "structure_types",
                            value = option.id,
                            displayName = option.type ?: option.id,
                            category = "structure_types",
                            syncedAt = currentTime.toString()
                        )
                    }
                    surveyDropdownDao.deleteDropdownsByCategory("structure_types")
                    surveyDropdownDao.insertDropdowns(entities)
                }
            }

            // Cache other dropdown types similarly...
            cacheDropdownCategory("functional_uses") { buildingSurveyApiService.getFunctionalUses() }
            cacheDropdownCategory("building_uses") { buildingSurveyApiService.getBuildingUses() }
            cacheDropdownCategory("defecation_places") { buildingSurveyApiService.getDefecationPlaces() }
            cacheDropdownCategory("toilet_connections") { buildingSurveyApiService.getToiletConnections() }
            cacheDropdownCategory("storage_tank_types") { buildingSurveyApiService.getStorageTankTypes() }
            cacheDropdownCategory("storage_tank_connections") { buildingSurveyApiService.getStorageTankConnections() }

        } catch (e: Exception) {
            // Log error but don't fail - offline mode should still work
        }
    }

    /**
     * Cache WFS building data for offline map use
     */
    suspend fun cacheBuildingData() {
        if (!isNetworkAvailable()) return

        try {
            // Note: WFS building data caching would need a specific API service
            // For now, skip this functionality until proper API service is provided
            return
        } catch (e: Exception) {
            // Log error but continue
        }
    }

    /**
     * Queue item for background sync
     */
    private suspend fun queueForSync(
        entityType: String,
        entityId: String,
        operation: String,
        data: String,
        priority: Int = 0
    ) {
        val syncEntity = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            data = data,
            retryCount = 0,
            maxRetries = 3,
            lastAttempt = null,
            createdAt = System.currentTimeMillis(),
            errorMessage = null,
            priority = priority
        )

        syncQueueDao.insert(syncEntity)
    }

    // Private helper methods for specific sync operations
    private suspend fun syncTask(sync: SyncQueueEntity) {
        // Implementation for syncing tasks
        val taskJson = sync.data
        // Parse and send to API
    }

    private suspend fun syncBuildingSurvey(sync: SyncQueueEntity) {
        // Implementation for syncing building surveys
        val surveyRequest = buildingSurveyRequestFromJson(sync.data)
        val response = buildingSurveyApiService.updateBuildingSurvey(sync.entityId, surveyRequest)

        if (response.isSuccessful) {
            // Update local record as synced
            buildingSurveyDao.updateSyncStatus(sync.entityId, "SYNCED")
        } else {
            throw Exception("API error: ${response.code()}")
        }
    }

    private suspend fun syncWorkflowStep(sync: SyncQueueEntity) {
        // Implementation for syncing workflow steps
    }

    private suspend fun syncEmptyingForm(sync: SyncQueueEntity) {
        // Implementation for syncing emptying forms
    }

    private suspend fun syncEmptyingSchedulingForm(sync: SyncQueueEntity) {
        try {
            val formEntity = emptyingSchedulingFormDao.getFormById(sync.entityId)
            if (formEntity != null) {
                val request = formEntity.toApiRequest()
                val response = emptyingSchedulingApiService.updateEmptyingScheduling(formEntity.applicationId, request)

                if (response.isSuccessful) {
                    emptyingSchedulingFormDao.markAsSynced(formEntity.id)
                } else {
                    throw Exception("API error: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Failed to sync emptying scheduling form: ${e.message}")
        }
    }

    private suspend fun syncSitePreparationForm(sync: SyncQueueEntity) {
        try {
            val formEntity = sitePreparationFormDao.getFormById(sync.entityId)
            if (formEntity != null) {
                val request = formEntity.toApiRequest(formEntity.applicationId)
                val response = sitePreparationApiService.updateSitePreparation(formEntity.applicationId, request)

                if (response.isSuccessful) {
                    sitePreparationFormDao.markAsSynced(formEntity.id)
                } else {
                    throw Exception("API error: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Failed to sync site preparation form: ${e.message}")
        }
    }

    private suspend fun syncEmptyingServiceForm(sync: SyncQueueEntity) {
        try {
            val formEntity = emptyingServiceFormDao.getFormById(sync.entityId)
            if (formEntity != null) {
                val request = formEntity.toApiRequest()
                val response = emptyingServiceApiService.submitEmptyingService(formEntity.applicationId, request)

                if (response.isSuccessful) {
                    emptyingServiceFormDao.markAsSynced(formEntity.id)
                } else {
                    throw Exception("API error: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Failed to sync emptying service form: ${e.message}")
        }
    }

    private suspend fun cacheDropdownCategory(
        category: String,
        apiCall: suspend () -> retrofit2.Response<SurveyDropdownResponse>
    ) {
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                response.body()?.data?.let { options ->
                    val entities = options.map { option ->
                        SurveyDropdownEntity(
                            id = option.id,
                            type = category,
                            value = option.id,
                            displayName = option.type ?: option.id,
                            category = category,
                            syncedAt = System.currentTimeMillis().toString()
                        )
                    }
                    surveyDropdownDao.deleteDropdownsByCategory(category)
                    surveyDropdownDao.insertDropdowns(entities)
                }
            }
        } catch (e: Exception) {
            // Continue with other categories
        }
    }

    // JSON serialization helpers
    private fun taskToJson(task: Task): String {
        val adapter: JsonAdapter<Task> = moshi.adapter(Task::class.java)
        return adapter.toJson(task)
    }

    private fun buildingSurveyRequestToJson(request: BuildingSurveyRequest): String {
        val adapter: JsonAdapter<BuildingSurveyRequest> = moshi.adapter(BuildingSurveyRequest::class.java)
        return adapter.toJson(request)
    }

    private fun buildingSurveyRequestFromJson(json: String): BuildingSurveyRequest {
        val adapter: JsonAdapter<BuildingSurveyRequest> = moshi.adapter(BuildingSurveyRequest::class.java)
        return adapter.fromJson(json) ?: throw Exception("Failed to parse BuildingSurveyRequest JSON")
    }

    /**
     * Save Emptying Scheduling form offline and queue for sync
     */
    suspend fun saveEmptyingSchedulingFormOffline(form: EmptyingSchedulingFormEntity): Flow<Resource<String>> = flow {
        emit(Resource.Loading())

        try {
            val formWithSyncStatus = form.copy(syncStatus = "PENDING")
            emptyingSchedulingFormDao.upsert(formWithSyncStatus)

            // Queue for sync
            queueForSync(
                entityType = EntityType.EMPTYING_SCHEDULING_FORM.name,
                entityId = form.id,
                operation = SyncOperation.UPDATE.name,
                data = emptyingSchedulingFormToJson(form)
            )

            emit(Resource.Success("Emptying scheduling form saved offline"))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to save emptying scheduling form: ${e.message}"))
        }
    }

    /**
     * Save Site Preparation form offline and queue for sync
     */
    suspend fun saveSitePreparationFormOffline(form: SitePreparationFormEntity): Flow<Resource<String>> = flow {
        emit(Resource.Loading())

        try {
            val formWithSyncStatus = form.copy(syncStatus = "PENDING")
            sitePreparationFormDao.upsert(formWithSyncStatus)

            // Queue for sync
            queueForSync(
                entityType = EntityType.SITE_PREPARATION_FORM.name,
                entityId = form.id,
                operation = SyncOperation.UPDATE.name,
                data = sitePreparationFormToJson(form)
            )

            emit(Resource.Success("Site preparation form saved offline"))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to save site preparation form: ${e.message}"))
        }
    }

    /**
     * Save Emptying Service form offline and queue for sync
     */
    suspend fun saveEmptyingServiceFormOffline(form: EmptyingServiceFormEntity): Flow<Resource<String>> = flow {
        emit(Resource.Loading())

        try {
            val formWithSyncStatus = form.copy(syncStatus = "PENDING")
            emptyingServiceFormDao.upsert(formWithSyncStatus)

            // Queue for sync
            queueForSync(
                entityType = EntityType.EMPTYING_SERVICE_FORM.name,
                entityId = form.id,
                operation = SyncOperation.UPDATE.name,
                data = emptyingServiceFormToJson(form)
            )

            emit(Resource.Success("Emptying service form saved offline"))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to save emptying service form: ${e.message}"))
        }
    }

    // JSON serialization helpers for new forms
    private fun emptyingSchedulingFormToJson(form: EmptyingSchedulingFormEntity): String {
        val adapter: JsonAdapter<EmptyingSchedulingFormEntity> = moshi.adapter(EmptyingSchedulingFormEntity::class.java)
        return adapter.toJson(form)
    }

    private fun sitePreparationFormToJson(form: SitePreparationFormEntity): String {
        val adapter: JsonAdapter<SitePreparationFormEntity> = moshi.adapter(SitePreparationFormEntity::class.java)
        return adapter.toJson(form)
    }

    private fun emptyingServiceFormToJson(form: EmptyingServiceFormEntity): String {
        val adapter: JsonAdapter<EmptyingServiceFormEntity> = moshi.adapter(EmptyingServiceFormEntity::class.java)
        return adapter.toJson(form)
    }
}
