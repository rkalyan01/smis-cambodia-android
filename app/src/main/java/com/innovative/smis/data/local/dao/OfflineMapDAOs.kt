package com.innovative.smis.data.local.dao

import androidx.room.*
import com.innovative.smis.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineMapTileDao {
    
    @Query("SELECT * FROM offline_map_tiles WHERE zoom = :zoom AND x = :x AND y = :y AND tileType = :tileType")
    suspend fun getTile(zoom: Int, x: Int, y: Int, tileType: String): OfflineMapTileEntity?
    
    @Query("SELECT * FROM offline_map_tiles WHERE zoom BETWEEN :minZoom AND :maxZoom")
    suspend fun getTilesInZoomRange(minZoom: Int, maxZoom: Int): List<OfflineMapTileEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTile(tile: OfflineMapTileEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTiles(tiles: List<OfflineMapTileEntity>)
    
    @Query("UPDATE offline_map_tiles SET lastAccessed = :timestamp WHERE tileId = :tileId")
    suspend fun updateLastAccessed(tileId: String, timestamp: Long)
    
    @Query("DELETE FROM offline_map_tiles WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredTiles(currentTime: Long)
    
    @Query("DELETE FROM offline_map_tiles WHERE tileType = :tileType")
    suspend fun deleteTilesByType(tileType: String)
    
    @Query("SELECT COUNT(*) FROM offline_map_tiles")
    suspend fun getTileCount(): Int
    
    @Query("SELECT SUM(LENGTH(tileData)) FROM offline_map_tiles")
    suspend fun getTotalStorageSize(): Long?
    
    @Query("SELECT DISTINCT tileType FROM offline_map_tiles")
    suspend fun getAvailableTileTypes(): List<String>
    
    @Query("DELETE FROM offline_map_tiles WHERE tileId IN (SELECT tileId FROM offline_map_tiles WHERE lastAccessed < :cutoffTime ORDER BY lastAccessed ASC LIMIT :limit)")
    suspend fun deleteOldestTiles(cutoffTime: Long, limit: Int)

    @Query("DELETE FROM offline_map_tiles")
    suspend fun clearAllTiles()
}

@Dao
interface OfflineBuildingPolygonDao {
    
    @Query("SELECT * FROM offline_building_polygons")
    fun getAllBuildings(): Flow<List<OfflineBuildingPolygonEntity>>
    
    @Query("SELECT * FROM offline_building_polygons WHERE buildingId = :buildingId")
    suspend fun getBuildingById(buildingId: String): OfflineBuildingPolygonEntity?
    
    @Query("SELECT * FROM offline_building_polygons WHERE bin = :bin")
    suspend fun getBuildingByBin(bin: String): OfflineBuildingPolygonEntity?
    
    @Query("SELECT * FROM offline_building_polygons WHERE surveyStatus = :status")
    suspend fun getBuildingsByStatus(status: String): List<OfflineBuildingPolygonEntity>
    
    @Query("SELECT * FROM offline_building_polygons WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedBuildings(): List<OfflineBuildingPolygonEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuilding(building: OfflineBuildingPolygonEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildings(buildings: List<OfflineBuildingPolygonEntity>)
    
    @Update
    suspend fun updateBuilding(building: OfflineBuildingPolygonEntity)
    
    @Query("UPDATE offline_building_polygons SET surveyStatus = :status, lastSurveyDate = :date WHERE buildingId = :buildingId")
    suspend fun updateSurveyStatus(buildingId: String, status: String, date: Long?)
    
    @Query("UPDATE offline_building_polygons SET syncStatus = :status WHERE buildingId = :buildingId")
    suspend fun updateSyncStatus(buildingId: String, status: String)
    
    @Delete
    suspend fun deleteBuilding(building: OfflineBuildingPolygonEntity)
    
    @Query("DELETE FROM offline_building_polygons WHERE buildingId = :buildingId")
    suspend fun deleteBuildingById(buildingId: String)
    
    @Query("SELECT COUNT(*) FROM offline_building_polygons")
    suspend fun getBuildingCount(): Int
    
    @Query("SELECT COUNT(*) FROM offline_building_polygons WHERE surveyStatus = 'COMPLETED'")
    suspend fun getCompletedSurveyCount(): Int

    @Query("DELETE FROM offline_building_polygons")
    suspend fun clearAllBuildings()
}

@Dao
interface OfflineMapAreaDao {
    
    @Query("SELECT * FROM offline_map_areas")
    fun getAllAreas(): Flow<List<OfflineMapAreaEntity>>
    
    @Query("SELECT * FROM offline_map_areas WHERE areaId = :areaId")
    suspend fun getAreaById(areaId: String): OfflineMapAreaEntity?
    
    @Query("SELECT * FROM offline_map_areas WHERE downloadStatus = :status")
    suspend fun getAreasByStatus(status: String): List<OfflineMapAreaEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArea(area: OfflineMapAreaEntity)
    
    @Update
    suspend fun updateArea(area: OfflineMapAreaEntity)
    
    @Query("UPDATE offline_map_areas SET downloadedTiles = :downloadedTiles, downloadStatus = :status WHERE areaId = :areaId")
    suspend fun updateDownloadProgress(areaId: String, downloadedTiles: Int, status: String)
    
    @Delete
    suspend fun deleteArea(area: OfflineMapAreaEntity)
    
    @Query("DELETE FROM offline_map_areas WHERE areaId = :areaId")
    suspend fun deleteAreaById(areaId: String)
    
    @Query("SELECT SUM(sizeInMB) FROM offline_map_areas WHERE downloadStatus = 'COMPLETED'")
    suspend fun getTotalDownloadedSize(): Double?

    @Query("DELETE FROM offline_map_areas")
    suspend fun clearAllAreas()
}

@Dao
interface OfflinePOIDao {
    
    @Query("SELECT * FROM offline_poi_markers")
    fun getAllPOIs(): Flow<List<OfflinePOIEntity>>
    
    @Query("SELECT * FROM offline_poi_markers WHERE category = :category")
    suspend fun getPOIsByCategory(category: String): List<OfflinePOIEntity>
    
    @Query("SELECT * FROM offline_poi_markers WHERE poiId = :poiId")
    suspend fun getPOIById(poiId: String): OfflinePOIEntity?
    
    @Query("SELECT * FROM offline_poi_markers WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedPOIs(): List<OfflinePOIEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPOI(poi: OfflinePOIEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPOIs(pois: List<OfflinePOIEntity>)
    
    @Update
    suspend fun updatePOI(poi: OfflinePOIEntity)
    
    @Query("UPDATE offline_poi_markers SET syncStatus = :status WHERE poiId = :poiId")
    suspend fun updateSyncStatus(poiId: String, status: String)
    
    @Delete
    suspend fun deletePOI(poi: OfflinePOIEntity)
    
    @Query("DELETE FROM offline_poi_markers WHERE poiId = :poiId")
    suspend fun deletePOIById(poiId: String)
    
    @Query("SELECT COUNT(*) FROM offline_poi_markers WHERE category = :category")
    suspend fun getPOICountByCategory(category: String): Int
    
    @Query("SELECT DISTINCT category FROM offline_poi_markers")
    suspend fun getAvailableCategories(): List<String>

    @Query("DELETE FROM offline_poi_markers")
    suspend fun clearAllPOIs()
}