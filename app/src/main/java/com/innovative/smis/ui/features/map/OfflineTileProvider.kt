package com.innovative.smis.ui.features.map

import android.util.Log
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.innovative.smis.data.local.offline.OfflineMapManager
import kotlinx.coroutines.runBlocking

/**
 * Custom TileProvider that integrates with OfflineMapManager to serve cached tiles
 * or falls back to online tiles when offline data is not available
 */
class OfflineTileProvider(
    private val offlineMapManager: OfflineMapManager,
    private val tileType: String = "roadmap"
) : TileProvider {
    
    companion object {
        private const val TAG = "OfflineTileProvider"
        const val TILE_SIZE = 256
    }
    
    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        return try {
            // Try to get tile from offline cache
            val tileData = runBlocking {
                offlineMapManager.getTileOffline(zoom, x, y, tileType)
            }
            
            if (tileData != null && tileData.isNotEmpty()) {
                Log.d(TAG, "Serving offline tile: z=$zoom, x=$x, y=$y")
                Tile(TILE_SIZE, TILE_SIZE, tileData)
            } else {
                Log.d(TAG, "No offline tile available: z=$zoom, x=$x, y=$y")
                // Return NO_TILE to let GoogleMap handle with default tiles
                TileProvider.NO_TILE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting offline tile", e)
            TileProvider.NO_TILE
        }
    }
}