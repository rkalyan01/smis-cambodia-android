package com.innovative.smis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "offline_map_tiles")
@TypeConverters(MapTileConverters::class)
data class OfflineMapTileEntity(
    @PrimaryKey
    val tileId: String, // Format: "zoom_x_y" (e.g., "15_1024_768")
    val zoom: Int,
    val x: Int,
    val y: Int,
    val tileData: ByteArray, // Actual tile image data
    val tileType: String, // "satellite", "terrain", "roadmap", "hybrid"
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessed: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as OfflineMapTileEntity
        return tileId == other.tileId
    }

    override fun hashCode(): Int {
        return tileId.hashCode()
    }
}

@Entity(tableName = "offline_building_polygons")
@TypeConverters(PolygonConverters::class)
data class OfflineBuildingPolygonEntity(
    @PrimaryKey
    val buildingId: String,
    val bin: String?,
    val coordinates: List<LatLng>, // Building polygon coordinates
    val properties: Map<String, String>, // Building properties (height, type, etc.)
    val surveyStatus: String = "NOT_SURVEYED", // "NOT_SURVEYED", "IN_PROGRESS", "COMPLETED"
    val lastSurveyDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED" // "PENDING", "SYNCING", "SYNCED", "FAILED"
)

@Entity(tableName = "offline_map_areas")
@TypeConverters(BoundsConverters::class)
data class OfflineMapAreaEntity(
    @PrimaryKey
    val areaId: String,
    val name: String,
    val bounds: MapBounds, // Southwest and northeast corners
    val zoomLevels: List<Int>, // Which zoom levels are downloaded
    val downloadedAt: Long = System.currentTimeMillis(),
    val totalTiles: Int = 0,
    val downloadedTiles: Int = 0,
    val downloadStatus: String = "PENDING", // "PENDING", "DOWNLOADING", "COMPLETED", "FAILED"
    val sizeInMB: Double = 0.0
)

@Entity(tableName = "offline_poi_markers")
@TypeConverters(LatLngConverters::class)
data class OfflinePOIEntity(
    @PrimaryKey
    val poiId: String,
    val title: String,
    val description: String?,
    val position: LatLng,
    val category: String, // "task", "survey", "landmark", "utility"
    val icon: String, // Icon identifier
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
)

// Data classes for complex types
@JsonClass(generateAdapter = true)
data class LatLng(
    val latitude: Double,
    val longitude: Double
)

@JsonClass(generateAdapter = true)
data class MapBounds(
    val southwest: LatLng,
    val northeast: LatLng
)

// Type Converters for Room
class MapTileConverters {
    @TypeConverter
    fun fromByteArray(value: ByteArray?): String? {
        return value?.let { android.util.Base64.encodeToString(it, android.util.Base64.DEFAULT) }
    }

    @TypeConverter
    fun toByteArray(value: String?): ByteArray? {
        return value?.let { android.util.Base64.decode(it, android.util.Base64.DEFAULT) }
    }
}

class PolygonConverters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val listType = Types.newParameterizedType(List::class.java, LatLng::class.java)
    private val coordsAdapter: JsonAdapter<List<LatLng>> = moshi.adapter(listType)
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    private val mapAdapter: JsonAdapter<Map<String, String>> = moshi.adapter(mapType)

    @TypeConverter
    fun fromCoordinatesList(coordinates: List<LatLng>?): String? {
        return coordinates?.let { coordsAdapter.toJson(it) }
    }

    @TypeConverter
    fun toCoordinatesList(json: String?): List<LatLng>? {
        return json?.let { coordsAdapter.fromJson(it) } ?: emptyList()
    }

    @TypeConverter
    fun fromPropertiesMap(properties: Map<String, String>?): String? {
        return properties?.let { mapAdapter.toJson(it) }
    }

    @TypeConverter
    fun toPropertiesMap(json: String?): Map<String, String>? {
        return json?.let { mapAdapter.fromJson(it) } ?: emptyMap()
    }
}

class BoundsConverters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val boundsAdapter: JsonAdapter<MapBounds> = moshi.adapter(MapBounds::class.java)
    private val intListType = Types.newParameterizedType(List::class.java, Integer::class.java)
    private val intListAdapter: JsonAdapter<List<Int>> = moshi.adapter(intListType)

    @TypeConverter
    fun fromMapBounds(bounds: MapBounds?): String? {
        return bounds?.let { boundsAdapter.toJson(it) }
    }

    @TypeConverter
    fun toMapBounds(json: String?): MapBounds? {
        return json?.let { boundsAdapter.fromJson(it) }
    }

    @TypeConverter
    fun fromIntList(list: List<Int>?): String? {
        return list?.let { intListAdapter.toJson(it) }
    }

    @TypeConverter
    fun toIntList(json: String?): List<Int>? {
        return json?.let { intListAdapter.fromJson(it) } ?: emptyList()
    }
}

class LatLngConverters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val latLngAdapter: JsonAdapter<LatLng> = moshi.adapter(LatLng::class.java)
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    private val mapAdapter: JsonAdapter<Map<String, String>> = moshi.adapter(mapType)

    @TypeConverter
    fun fromLatLng(latLng: LatLng?): String? {
        return latLng?.let { latLngAdapter.toJson(it) }
    }

    @TypeConverter
    fun toLatLng(json: String?): LatLng? {
        return json?.let { latLngAdapter.fromJson(it) }
    }

    @TypeConverter
    fun fromMetadataMap(metadata: Map<String, String>?): String? {
        return metadata?.let { mapAdapter.toJson(it) }
    }

    @TypeConverter
    fun toMetadataMap(json: String?): Map<String, String>? {
        return json?.let { mapAdapter.fromJson(it) } ?: emptyMap()
    }
}