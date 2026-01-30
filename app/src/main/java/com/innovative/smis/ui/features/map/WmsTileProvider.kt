package com.innovative.smis.ui.features.map

import com.google.android.gms.maps.model.UrlTileProvider
import java.net.MalformedURLException
import java.net.URL
import kotlin.math.pow

class WmsTileProvider(
    private val urlTemplate: String,
    private val width: Int = 256,
    private val height: Int = 256
) : UrlTileProvider(width, height) {

    // Web Mercator (EPSG:3857) bounds
    private val MAX_EXTENT = 20037508.342789244

    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
        try {
            val bbox = getBoundingBox(x, y, zoom)
            
            // Calculate min/max values
            val minX = bbox[0]
            val minY = bbox[1]
            val maxX = bbox[2]
            val maxY = bbox[3]

            val urlString = urlTemplate
                .replace("{minX}", String.format(java.util.Locale.US, "%.6f", minX))
                .replace("{minY}", String.format(java.util.Locale.US, "%.6f", minY))
                .replace("{maxX}", String.format(java.util.Locale.US, "%.6f", maxX))
                .replace("{maxY}", String.format(java.util.Locale.US, "%.6f", maxY))
                .replace("{width}", width.toString())
                .replace("{height}", height.toString())
                .replace("{x}", x.toString())
                .replace("{y}", y.toString())
                .replace("{z}", zoom.toString())

            android.util.Log.d("WmsTileProvider", "Generated URL: $urlString")
            return URL(urlString)
        } catch (e: MalformedURLException) {
            return null
        }
    }

    private fun getBoundingBox(x: Int, y: Int, zoom: Int): DoubleArray {
        val tileSize = (MAX_EXTENT * 2) / 2.0.pow(zoom.toDouble())
        val minX = -MAX_EXTENT + x * tileSize
        val maxX = -MAX_EXTENT + (x + 1) * tileSize
        
        // Google Maps tiles have origin at top-left
        // Y goes down. 
        // Global Mercator Y range: -MAX to +MAX
        // Tile Y=0 is at +MAX (top)
        
        val maxY = MAX_EXTENT - y * tileSize
        val minY = MAX_EXTENT - (y + 1) * tileSize

        return doubleArrayOf(minX, minY, maxX, maxY)
    }
}
