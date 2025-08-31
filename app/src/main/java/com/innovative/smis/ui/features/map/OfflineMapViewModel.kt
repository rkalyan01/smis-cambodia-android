package com.innovative.smis.ui.features.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*
import com.innovative.smis.data.local.entity.*
import com.innovative.smis.data.local.offline.MapCacheStats
import com.innovative.smis.data.local.offline.OfflineMapManager
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OfflineMapViewModel(
    private val offlineMapManager: OfflineMapManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineMapUiState())
    val uiState: StateFlow<OfflineMapUiState> = _uiState.asStateFlow()

    private var currentMapType = MapType.NORMAL

    init {
        loadInitialData()
        checkNetworkStatus()
    }

    data class OfflineMapUiState(
        val isLoading: Boolean = false,
        val isOnline: Boolean = true,
        val cameraPositionState: CameraPositionState = CameraPositionState(
            position = CameraPosition.fromLatLngZoom(
                LatLng(11.555, 104.904),
                13f
            )
        ),
        val mapProperties: MapProperties = MapProperties(
            mapType = MapType.NORMAL,
            isMyLocationEnabled = true,
            mapStyleOptions = null
        ),
        val mapUiSettings: MapUiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            myLocationButtonEnabled = true,
            mapToolbarEnabled = false
        ),
        val buildingPolygons: List<OfflineBuildingPolygonEntity> = emptyList(),
        val poiMarkers: List<OfflinePOIEntity> = emptyList(),
        val downloadedAreas: List<OfflineMapAreaEntity> = emptyList(),
        val cacheStats: MapCacheStats? = null,
        val errorMessage: String? = null,
        val downloadProgress: Pair<Int, Int>? = null
    )

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load building polygons
            offlineMapManager.getBuildingPolygonsOffline().collect { buildings ->
                _uiState.value = _uiState.value.copy(buildingPolygons = buildings)
            }
        }

        viewModelScope.launch {
            loadCacheStats()
        }
    }

    private fun checkNetworkStatus() {
        _uiState.value = _uiState.value.copy(
            isOnline = true
        )
    }

    fun onMapLoaded() {
        loadCacheStats()
    }

    fun onMapClick(latLng: LatLng) {
        viewModelScope.launch {
            val clickedBuilding = _uiState.value.buildingPolygons.find { building ->
                val distance = calculateDistance(
                    latLng.latitude, latLng.longitude,
                    building.coordinates.firstOrNull()?.latitude ?: 0.0,
                    building.coordinates.firstOrNull()?.longitude ?: 0.0
                )
                distance < 0.001 // ~100 meters
            }

            clickedBuilding?.let { building ->
                if (building.surveyStatus == "NOT_SURVEYED") {
                    // TODO: Navigate to survey or show options
                }
            }
        }
    }

    fun toggleMapType() {
        currentMapType = when (currentMapType) {
            MapType.NORMAL -> MapType.SATELLITE
            MapType.SATELLITE -> MapType.HYBRID
            MapType.HYBRID -> MapType.TERRAIN
            else -> MapType.NORMAL
        }

        _uiState.value = _uiState.value.copy(
            mapProperties = _uiState.value.mapProperties.copy(mapType = currentMapType)
        )
    }

    fun downloadMapArea(bounds: MapBounds, zoomLevels: List<Int>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val area = OfflineMapAreaEntity(
                areaId = "area_${System.currentTimeMillis()}",
                name = "Downloaded Area ${System.currentTimeMillis()}",
                bounds = bounds,
                zoomLevels = zoomLevels
            )

            offlineMapManager.downloadMapArea(area) { downloaded, total ->
                _uiState.value = _uiState.value.copy(
                    downloadProgress = Pair(downloaded, total)
                )
            }.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            downloadProgress = null
                        )
                        loadCacheStats()
                        loadDownloadedAreas()
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            downloadProgress = null,
                            errorMessage = resource.message
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadCacheStats() {
        viewModelScope.launch {
            try {
                val stats = offlineMapManager.getCacheStats()
                _uiState.value = _uiState.value.copy(cacheStats = stats)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load cache stats: ${e.message}"
                )
            }
        }
    }

    fun clearOfflineCache() {
        viewModelScope.launch {
            try {
                offlineMapManager.clearAllOfflineData()
                loadCacheStats()
                _uiState.value = _uiState.value.copy(
                    buildingPolygons = emptyList(),
                    downloadedAreas = emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to clear cache: ${e.message}"
                )
            }
        }
    }

    private fun loadDownloadedAreas() {
        // TODO: Load downloaded areas from database
        // TODO: Implementation would use Flow from DAO
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon/2) * Math.sin(dLon/2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
        return earthRadius * c
    }
}