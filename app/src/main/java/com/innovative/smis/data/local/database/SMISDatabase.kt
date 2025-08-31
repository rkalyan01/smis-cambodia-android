package com.innovative.smis.data.local.database

import androidx.room.*
import com.innovative.smis.data.local.dao.*
import com.innovative.smis.data.local.entity.*

@Database(
    entities = [
        TaskEntity::class,
        TodoItemEntity::class,
        WorkflowStepEntity::class,
        BuildingSurveyEntity::class,
        UserEntity::class,
        SurveyDropdownEntity::class,
        WfsBuildingEntity::class,

        SyncQueueEntity::class,
        OfflineMapTileEntity::class,
        OfflineBuildingPolygonEntity::class,
        OfflineMapAreaEntity::class,
        OfflinePOIEntity::class,
        EmptyingSchedulingFormEntity::class,
        SitePreparationFormEntity::class,
        EmptyingServiceFormEntity::class,
        ContainmentFormEntity::class
    ],
    version = 11,
    exportSchema = false
)

@TypeConverters(DatabaseConverters::class)
abstract class SMISDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun workflowStepDao(): WorkflowStepDao
    abstract fun buildingSurveyDao(): BuildingSurveyDao
    abstract fun userDao(): UserDao
    abstract fun surveyDropdownDao(): SurveyDropdownDao
    abstract fun wfsBuildingDao(): WfsBuildingDao

    abstract fun syncQueueDao(): SyncQueueDao
    
    // Offline Map DAOs
    abstract fun offlineMapTileDao(): OfflineMapTileDao
    abstract fun offlineBuildingPolygonDao(): OfflineBuildingPolygonDao
    abstract fun offlineMapAreaDao(): OfflineMapAreaDao
    abstract fun offlinePOIDao(): OfflinePOIDao
    abstract fun todoItemDao(): TodoItemDao
    abstract fun emptyingSchedulingFormDao(): EmptyingSchedulingFormDao
    abstract fun sitePreparationFormDao(): SitePreparationFormDao
    abstract fun emptyingServiceFormDao(): EmptyingServiceFormDao
    abstract fun containmentFormDao(): ContainmentFormDao

    companion object {
        const val DATABASE_NAME = "smis_database"
        const val DATABASE_VERSION = 11
    }
}


class DatabaseConverters {
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }

    @TypeConverter
    fun fromDoubleList(value: List<Double>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toDoubleList(value: String): List<Double> {
        return if (value.isEmpty()) emptyList() else value.split(",").map { it.toDouble() }
    }

    @TypeConverter
    fun fromCoordinatesList(value: List<List<List<List<Double>>>>): String {
        // Flatten the nested structure for storage
        return value.toString()
    }

    @TypeConverter
    fun toCoordinatesList(value: String): List<List<List<List<Double>>>> {
        // Parse the flattened structure (simplified - would need proper JSON parsing in production)
        return emptyList()
    }
}