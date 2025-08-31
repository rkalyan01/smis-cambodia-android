package com.innovative.smis.data.local.offline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.android.gms.maps.model.LatLngBounds
import com.innovative.smis.data.local.dao.*
import com.innovative.smis.data.local.entity.*
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.*

class OfflineMapManager(
    private val context: Context,
    private val mapTileDao: OfflineMapTileDao,
    private val buildingPolygonDao: OfflineBuildingPolygonDao,
    private val mapAreaDao: OfflineMapAreaDao,
    private val poiDao: OfflinePOIDao
) {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val TAG = "OfflineMapManager"
        private const val MAX_CACHE_SIZE_MB = 500.0
        private const val TILE_SIZE = 256
        private const val GOOGLE_MAPS_API_KEY = ""
        
        // Google Maps tile URL templates
        private const val ROADMAP_URL = "https://mt1.google.com/vt/lyrs=m&x=%d&y=%d&z=%d"
        private const val SATELLITE_URL = "https://mt1.google.com/vt/lyrs=s&x=%d&y=%d&z=%d"
        private const val HYBRID_URL = "https://mt1.google.com/vt/lyrs=y&x=%d&y=%d&z=%d"
        private const val TERRAIN_URL = "https://mt1.google.com/vt/lyrs=t&x=%d&y=%d&z=%d"
    }
    
    /**
     * Download and cache map area for offline use
     */
    suspend fun downloadMapArea(
        area: OfflineMapAreaEntity,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        
        try {
            val bounds = area.bounds
            var totalTiles = 0
            var downloadedTiles = 0
            
            // Calculate total tiles needed
            for (zoom in area.zoomLevels) {
                val tilesAtZoom = calculateTileCount(bounds, zoom)
                totalTiles += tilesAtZoom
            }
            
            // Update area with total tile count
            mapAreaDao.updateArea(area.copy(
                totalTiles = totalTiles,
                downloadStatus = "DOWNLOADING"
            ))
            
            // Download tiles for each zoom level
            for (zoom in area.zoomLevels) {
                val tiles = getTileCoordinatesForBounds(bounds, zoom)
                
                for (tile in tiles) {
                    try {
                        // Download different map types
                        val mapTypes = listOf("roadmap", "satellite", "hybrid", "terrain")
                        for (mapType in mapTypes) {
                            val tileData = downloadMapTile(zoom, tile.first, tile.second, mapType)
                            if (tileData != null) {
                                val tileEntity = OfflineMapTileEntity(
                                    tileId = "${zoom}_${tile.first}_${tile.second}_$mapType",
                                    zoom = zoom,
                                    x = tile.first,
                                    y = tile.second,
                                    tileData = tileData,
                                    tileType = mapType
                                )
                                mapTileDao.insertTile(tileEntity)
                            }
                        }
                        
                        downloadedTiles++
                        onProgress(downloadedTiles, totalTiles)
                        
                        // Update progress
                        mapAreaDao.updateDownloadProgress(
                            area.areaId, 
                            downloadedTiles, 
                            "DOWNLOADING"
                        )
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to download tile ${tile.first},${tile.second} at zoom $zoom", e)
                    }
                }
            }
            
            // Mark as completed
            val completedArea = area.copy(
                downloadedTiles = downloadedTiles,
                downloadStatus = "COMPLETED",
                sizeInMB = calculateAreaSize(area.areaId)
            )
            mapAreaDao.updateArea(completedArea)
            
            emit(Resource.Success("Downloaded $downloadedTiles tiles for ${area.name}"))
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download map area", e)
            mapAreaDao.updateArea(area.copy(downloadStatus = "FAILED"))
            emit(Resource.Error("Failed to download map area: ${e.message}"))
        }
    }
    
    /**
     * Get cached tile or download if not available
     */
    suspend fun getTileOffline(
        zoom: Int, 
        x: Int, 
        y: Int, 
        tileType: String = "roadmap"
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // Try to get from cache first
            val cachedTile = mapTileDao.getTile(zoom, x, y, tileType)
            if (cachedTile != null && !isTileExpired(cachedTile)) {
                // Update last accessed time
                mapTileDao.updateLastAccessed(cachedTile.tileId, System.currentTimeMillis())
                return@withContext cachedTile.tileData
            }
            
            // If not cached or expired, try to download
            val tileData = downloadMapTile(zoom, x, y, tileType)
            if (tileData != null) {
                val tileEntity = OfflineMapTileEntity(
                    tileId = "${zoom}_${x}_${y}_$tileType",
                    zoom = zoom,
                    x = x,
                    y = y,
                    tileData = tileData,
                    tileType = tileType
                )
                mapTileDao.insertTile(tileEntity)
                return@withContext tileData
            }
            
            // Return cached tile even if expired as fallback
            cachedTile?.tileData
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get tile $x,$y at zoom $zoom", e)
            null
        }
    }
    
    /**
     * Download individual map tile
     */
    private suspend fun downloadMapTile(
        zoom: Int, 
        x: Int, 
        y: Int, 
        tileType: String
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val url = when (tileType.lowercase()) {
                "roadmap" -> ROADMAP_URL.format(x, y, zoom)
                "satellite" -> SATELLITE_URL.format(x, y, zoom)
                "hybrid" -> HYBRID_URL.format(x, y, zoom)
                "terrain" -> TERRAIN_URL.format(x, y, zoom)
                else -> ROADMAP_URL.format(x, y, zoom)
            }
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "SMIS Mobile App")
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()
            } else {
                Log.w(TAG, "Failed to download tile: HTTP ${response.code}")
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error downloading tile", e)
            null
        }
    }
    
    /**
     * Save building polygons for offline use
     */
    suspend fun saveBuildingPolygonsOffline(buildings: List<OfflineBuildingPolygonEntity>) {
        try {
            buildingPolygonDao.insertBuildings(buildings)
            Log.i(TAG, "Saved ${buildings.size} building polygons offline")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save building polygons", e)
        }
    }
    
    /**
     * Get building polygons for map display
     */
    fun getBuildingPolygonsOffline(): Flow<List<OfflineBuildingPolygonEntity>> {
        return buildingPolygonDao.getAllBuildings()
    }
    
    /**
     * Cache management - clean expired tiles
     */
    suspend fun cleanExpiredTiles() {
        try {
            val currentTime = System.currentTimeMillis()
            mapTileDao.deleteExpiredTiles(currentTime)
            
            // Also clean oldest tiles if cache is too large
            val cacheSize = getCurrentCacheSize()
            if (cacheSize > MAX_CACHE_SIZE_MB) {
                val tilesToDelete = ((cacheSize - MAX_CACHE_SIZE_MB) * 1024 * 1024 / (TILE_SIZE * TILE_SIZE)).toInt()
                val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago
                mapTileDao.deleteOldestTiles(cutoffTime, tilesToDelete)
            }
            
            Log.i(TAG, "Cleaned expired tiles, current cache size: ${getCurrentCacheSize()}MB")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean expired tiles", e)
        }
    }
    
    /**
     * Get current cache statistics
     */
    suspend fun getCacheStats(): MapCacheStats {
        return try {
            MapCacheStats(
                totalTiles = mapTileDao.getTileCount(),
                cacheSizeMB = getCurrentCacheSize(),
                buildingCount = buildingPolygonDao.getBuildingCount(),
                completedSurveys = buildingPolygonDao.getCompletedSurveyCount(),
                availableTileTypes = mapTileDao.getAvailableTileTypes(),
                downloadedAreas = mapAreaDao.getAreasByStatus("COMPLETED").size,
                totalDownloadSizeMB = mapAreaDao.getTotalDownloadedSize() ?: 0.0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache stats", e)
            MapCacheStats()
        }
    }
    
    /**
     * Clear all offline map data
     */
    suspend fun clearAllOfflineData() {
        try {
            // Clear all DAOs
            mapTileDao.deleteTilesByType("roadmap")
            mapTileDao.deleteTilesByType("satellite") 
            mapTileDao.deleteTilesByType("hybrid")
            mapTileDao.deleteTilesByType("terrain")
            
            Log.i(TAG, "Cleared all offline map data")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear offline data", e)
        }
    }
    
    // Helper functions
    private suspend fun getCurrentCacheSize(): Double {
        return try {
            val sizeBytes = mapTileDao.getTotalStorageSize() ?: 0L
            sizeBytes / (1024.0 * 1024.0) // Convert to MB
        } catch (e: Exception) {
            0.0
        }
    }
    
    private suspend fun calculateAreaSize(areaId: String): Double {
        return try {
            // Estimate size based on tile count and average tile size
            val area = mapAreaDao.getAreaById(areaId)
            if (area != null) {
                (area.downloadedTiles * 15.0) / 1024.0 // Assume ~15KB per tile average
            } else {
                0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }
    
    private fun isTileExpired(tile: OfflineMapTileEntity): Boolean {
        return System.currentTimeMillis() > tile.expiresAt
    }
    
    private fun calculateTileCount(bounds: MapBounds, zoom: Int): Int {
        val swTile = latLngToTileCoordinates(bounds.southwest.latitude, bounds.southwest.longitude, zoom)
        val neTile = latLngToTileCoordinates(bounds.northeast.latitude, bounds.northeast.longitude, zoom)
        
        val minX = min(swTile.first, neTile.first)
        val maxX = max(swTile.first, neTile.first)
        val minY = min(swTile.second, neTile.second)
        val maxY = max(swTile.second, neTile.second)
        
        return (maxX - minX + 1) * (maxY - minY + 1)
    }
    
    private fun getTileCoordinatesForBounds(bounds: MapBounds, zoom: Int): List<Pair<Int, Int>> {
        val tiles = mutableListOf<Pair<Int, Int>>()
        
        val swTile = latLngToTileCoordinates(bounds.southwest.latitude, bounds.southwest.longitude, zoom)
        val neTile = latLngToTileCoordinates(bounds.northeast.latitude, bounds.northeast.longitude, zoom)
        
        val minX = min(swTile.first, neTile.first)
        val maxX = max(swTile.first, neTile.first)
        val minY = min(swTile.second, neTile.second)
        val maxY = max(swTile.second, neTile.second)
        
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                tiles.add(Pair(x, y))
            }
        }
        
        return tiles
    }
    
    private fun latLngToTileCoordinates(lat: Double, lng: Double, zoom: Int): Pair<Int, Int> {
        val latRad = Math.toRadians(lat)
        val n = 2.0.pow(zoom)
        val x = ((lng + 180.0) / 360.0 * n).toInt()
        val y = ((1.0 - asinh(tan(latRad)) / PI) / 2.0 * n).toInt()
        return Pair(x, y)
    }
}

/**
 * Cache statistics data class
 */
data class MapCacheStats(
    val totalTiles: Int = 0,
    val cacheSizeMB: Double = 0.0,
    val buildingCount: Int = 0,
    val completedSurveys: Int = 0,
    val availableTileTypes: List<String> = emptyList(),
    val downloadedAreas: Int = 0,
    val totalDownloadSizeMB: Double = 0.0
)