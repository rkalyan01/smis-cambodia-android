package com.innovative.smis.ui.features.map

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.CancellationTokenSource
import com.innovative.smis.data.repository.BuildingSurveyRepository
import com.innovative.smis.util.common.Resource
import com.innovative.smis.data.model.SurveyAlertState
import com.innovative.smis.data.model.response.WmsRoadResponse
import com.innovative.smis.data.model.response.WmsSewerResponse
import com.innovative.smis.data.model.response.WmsSangkatResponse
import com.innovative.smis.data.model.response.WmsBuildingResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class WfsFeature(
    val id: String?,
    val bin: String?,
    val geometry: Geometry?,
    val is_surveyed: Boolean?,
    val is_auxiliary: Boolean?
)

data class Geometry(
    val type: String,
    val coordinates: List<List<List<List<Double>>>>
)

class MapViewModel(private val buildingSurveyRepository: BuildingSurveyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState = _uiState.asStateFlow()

    private val _locationState = MutableSharedFlow<CameraUpdateEvent>(replay = 1, extraBufferCapacity = 0)
    val locationState = _locationState.asSharedFlow()

    private val _isLocatingUser = MutableStateFlow(false)
    val isLocatingUser = _isLocatingUser.asStateFlow()

    private val _isAnimatingToData = MutableStateFlow(false)
    val isAnimatingToData = _isAnimatingToData.asStateFlow()

    private val _surveyAlert = MutableStateFlow(SurveyAlertState())
    val surveyAlert = _surveyAlert.asStateFlow()

    private val _dataRegionMessage = MutableStateFlow<String?>(null)
    val dataRegionMessage = _dataRegionMessage.asStateFlow()

    private var filterJob: Job? = null

    fun clearDataRegionMessage() {
        _dataRegionMessage.value = null
    }

    init {
        fetchWfsData()
        fetchLayerData()
    }

    private fun fetchWfsData() {
        Log.i("MapViewModel", "START: fetchWfsData() called")
        viewModelScope.launch {
            Log.i("MapViewModel", "START: collect() starting")
            // Use getWFSLayerBuildings() as per the existing code
            buildingSurveyRepository.getWFSLayerBuildings().collect { result ->
                Log.i("MapViewModel", "COLLECT: Received emission: ${result::class.simpleName}")
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(loading = true, debugInfo = "Fetching WFS data...") }
                    }
                    is Resource.Success -> {
                        val rawFeatures = result.data?.features ?: emptyList()
                        Log.i("MapViewModel", "WFS Data Success: Received ${rawFeatures.size} raw features")
                        
                        if (rawFeatures.isNotEmpty()) {
                            val firstRaw = rawFeatures.first()
                            Log.i("MapViewModel", "First Raw Feature Type: ${firstRaw.geometry?.type}")
                            Log.i("MapViewModel", "First Raw Feature Coords Type: ${firstRaw.geometry?.coordinates?.javaClass?.simpleName}")
                        }

                        // Move mapping to background thread
                        launch(Dispatchers.Default) {
                            try {
                                val features = rawFeatures.mapNotNull { feature ->
                                    val geometry = feature.geometry
                                    val normalizedCoords = normalizeCoordinates(geometry?.coordinates)

                                    // Debug properties
                                    if (feature == rawFeatures.first()) {
                                        Log.d("MapViewModel", "First Feature Props: ${feature.properties}")
                                    }

                                    if (normalizedCoords.isNotEmpty()) {
                                        val props = feature.properties
                                    // Robust BIN lookup: Check direct field first, then properties map
                                    val binValue = feature.bin ?: props?.get("bin") ?: props?.get("BIN") ?: props?.get("Bin")
                                    val binString = binValue as? String ?: binValue?.toString()
                                    
                                    // Robust Boolean parsing
                                    fun parseBoolean(key: String, directValue: Any?): Boolean? {
                                        val value = directValue ?: props?.get(key)
                                        return when (value) {
                                            is Boolean -> value
                                            is String -> value.toBoolean()
                                            is Number -> value.toInt() != 0
                                            else -> null
                                        }
                                    }

                                    WfsFeature(
                                        id = feature.id,
                                        bin = binString,
                                        geometry = Geometry(
                                            type = "MultiPolygon",
                                            coordinates = normalizedCoords
                                        ),
                                        is_surveyed = parseBoolean("is_surveyed", feature.is_surveyed),
                                        is_auxiliary = parseBoolean("is_auxiliary", feature.is_auxiliary)
                                    )
                                } else {
                                    null
                                }
                            }
                            _uiState.update {
                                it.copy(loading = false, wfsData = features, filteredData = emptyList(), debugInfo = "Loaded ${features.size} features")
                            }
                                Log.i("MapViewModel", "SUCCESS: Mapped ${features.size} features.")
                            } catch (e: Exception) {
                                Log.e("MapViewModel", "Mapping Error: ${e.message}", e)
                            }
                        }
                    }
                    is Resource.Error -> {
                        Log.e("MapViewModel", "API Error: ${result.message}")
                        _uiState.update { it.copy(loading = false) }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(loading = true) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun normalizeCoordinates(coords: Any?): List<List<List<List<Double>>>> {
        if (coords == null || coords !is List<*> || coords.isEmpty()) return emptyList()

        try {
            val level1 = coords[0] // Polygon or Ring?
            if (level1 !is List<*>) return emptyList()

            val level2 = level1[0] // Ring or Point?
            if (level2 !is List<*>) return emptyList()

            val level3 = level2[0] // Point or Double?

            if (level3 is Number) {
                // Coords is Polygon (Depth 3): List<List<List<Double>>>
                // Wrap in List to make it MultiPolygon
                @Suppress("UNCHECKED_CAST")
                val polygon = coords as List<List<List<Double>>>
                return listOf(polygon)
            } else if (level3 is List<*>) {
                // Coords is MultiPolygon (Depth 4): List<List<List<List<Double>>>>
                @Suppress("UNCHECKED_CAST")
                return coords as List<List<List<List<Double>>>>
            }
        } catch (e: Exception) {
            Log.e("MapViewModel", "Normalization Error: ${e.message}")
        }
        return emptyList()
    }

    private fun fetchLayerData() {
        viewModelScope.launch {
            // Fetch road layer
            launch {
                buildingSurveyRepository.getRoadWms().collect { result ->
                    when (result) {
                        is Resource.Success<*> -> {
                            val roadResponse = result.data as? WmsRoadResponse
                            Log.d("MapViewModel", "Road WMS Response: $roadResponse")
                            if (roadResponse?.success == true && roadResponse.baseUrl != null && roadResponse.data != null) {
                                val layerName = roadResponse.data.road_networks
                                if (!layerName.isNullOrEmpty()) {
                                    // Strip STYLES param to force default (backend sends _none)
                                    val cleanLayer = layerName.replace(Regex("STYLES=[^&]*"), "STYLES=")
                                    val fullUrl = "${roadResponse.baseUrl}$cleanLayer"
                                    Log.d("MapViewModel", "Road WMS URL (Fixed): $fullUrl")
                                    _uiState.update { it.copy(roadWmsUrl = fullUrl) }
                                }
                            }
                        }
                        is Resource.Error<*> -> {
                            Log.e("MapViewModel", "Error fetching Road WMS: ${result.message}")
                        }
                        else -> {}
                    }
                }
            }
            
            // Fetch sewer layer
            launch {
                buildingSurveyRepository.getSewerWms().collect { result ->
                    when (result) {
                        is Resource.Success<*> -> {
                            val sewerResponse = result.data as? WmsSewerResponse
                            Log.d("MapViewModel", "Sewer WMS Response: $sewerResponse")
                            if (sewerResponse?.success == true && sewerResponse.baseUrl != null && sewerResponse.data != null) {
                                val layerName = sewerResponse.data.sewer_networks
                                if (!layerName.isNullOrEmpty()) {
                                    val cleanLayer = layerName.replace(Regex("STYLES=[^&]*"), "STYLES=")
                                    val fullUrl = "${sewerResponse.baseUrl}$cleanLayer"
                                    Log.d("MapViewModel", "Sewer WMS URL (Fixed): $fullUrl")
                                    _uiState.update { it.copy(sewerWmsUrl = fullUrl) }
                                }
                            }
                        }
                        is Resource.Error<*> -> {
                            Log.e("MapViewModel", "Error fetching Sewer WMS: ${result.message}")
                        }
                        else -> {}
                    }
                }
            }
            
            // Fetch sangkat layer
            launch {
                buildingSurveyRepository.getSangkatWms().collect { result ->
                    when (result) {
                        is Resource.Success<*> -> {
                            val sangkatResponse = result.data as? WmsSangkatResponse
                            Log.d("MapViewModel", "Sangkat WMS Response: $sangkatResponse")
                            if (sangkatResponse?.success == true && sangkatResponse.baseUrl != null && sangkatResponse.data != null) {
                                val layerName = sangkatResponse.data.communes_sangkats
                                if (!layerName.isNullOrEmpty()) {
                                    val cleanLayer = layerName.replace(Regex("STYLES=[^&]*"), "STYLES=")
                                    val fullUrl = "${sangkatResponse.baseUrl}$cleanLayer"
                                    Log.d("MapViewModel", "Sangkat WMS URL (Fixed): $fullUrl")
                                    _uiState.update { it.copy(sangkatWmsUrl = fullUrl) }
                                }
                            }
                        }
                        is Resource.Error<*> -> {
                            Log.e("MapViewModel", "Error fetching Sangkat WMS: ${result.message}")
                        }
                        else -> {}
                    }
                }
            }
            
            // Fetch building layer
            launch {
                buildingSurveyRepository.getBuildingWms().collect { result ->
                    when (result) {
                        is Resource.Success<*> -> {
                            val buildingResponse = result.data as? WmsBuildingResponse
                            Log.d("MapViewModel", "Building WMS Response: $buildingResponse")
                            if (buildingResponse?.success == true && buildingResponse.baseUrl != null && buildingResponse.data != null) {
                                val layerName = buildingResponse.data.building_surveys ?: buildingResponse.data.buildings
                                if (!layerName.isNullOrEmpty()) {
                                    // Replace STYLES param with empty value to force server default
                                    val cleanLayer = layerName.replace(Regex("STYLES=[^&]*"), "STYLES=")
                                    val fullUrl = "${buildingResponse.baseUrl}$cleanLayer"
                                    Log.d("MapViewModel", "Building WMS URL (Fixed): $fullUrl")
                                    _uiState.update { it.copy(buildingWmsUrl = fullUrl) }
                                }
                            }
                        }
                        is Resource.Error<*> -> {
                            Log.e("MapViewModel", "Error fetching Building WMS: ${result.message}")
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun filterDataByViewport(bounds: LatLngBounds?, zoomLevel: Float) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            // Show WFS building polygons at all zoom levels (viewport filter only; no zoom-based hiding)
            if (bounds == null) return@launch

            val filteredList = _uiState.value.wfsData.filter { item ->
                item.geometry?.coordinates?.firstOrNull()?.firstOrNull()?.any { coord ->
                    val latLng = LatLng(coord[1], coord[0])
                    bounds.contains(latLng)
                } ?: false
            }

            _uiState.update { it.copy(filteredData = filteredList) }
        }
    }

    fun animateToDataRegion() {
        viewModelScope.launch {
            if (_uiState.value.wfsData.isEmpty()) {
                Log.w("MapDebug", "Animate button pressed but no data is available.")
                _dataRegionMessage.value = "No building data yet. Please wait for the map to load."
                return@launch
            }

            Log.d("MapDebug", "Animating to data. Total features: ${_uiState.value.wfsData.size}")
            
            val firstFeature = _uiState.value.wfsData.first()
            val firstCoordinate = firstFeature.geometry?.coordinates?.firstOrNull()?.firstOrNull()?.firstOrNull()

            Log.d("MapDebug", "First feature geometry type: ${firstFeature.geometry?.type}")
            Log.d("MapDebug", "Extracted coordinate: $firstCoordinate")

            if (firstCoordinate != null && firstCoordinate.size >= 2) {
                _isAnimatingToData.value = true
                val targetLocation = LatLng(firstCoordinate[1], firstCoordinate[0])
                Log.d("MapDebug", "Emitting CameraUpdateEvent to $targetLocation with zoom 20f")
                
                // Zoom level 20f for viewing building details (bit layer)
                _locationState.emit(CameraUpdateEvent(targetLocation, 20f))
                delay(1500) // Match animation duration
                _isAnimatingToData.value = false
            } else {
                Log.e("MapDebug", "Failed to extract valid coordinate from first feature.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun animateToCurrentLocation(context: Context) {
        viewModelScope.launch {
            _isLocatingUser.value = true
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await()

                if (location != null) {
                    // Zoom level 18f for user location
                    _locationState.emit(CameraUpdateEvent(LatLng(location.latitude, location.longitude), 18f))
                }
            } catch (e: Exception) {
                // Handle exceptions
            } finally {
                _isLocatingUser.value = false
            }
        }
    }

    fun onPolygonPress(item: WfsFeature) {
        // Highlight logic for auxiliary polygons
        val currentHighlighted = _uiState.value.highlightedBin
        _uiState.update {
            it.copy(
                highlightedBin = if (currentHighlighted == item.bin) null else item.bin
            )
        }
    }

    fun getCenterOfPolygon(polygon: List<List<Double>>): LatLng {
        val latLngs = polygon.map { LatLng(it[1], it[0]) }
        val builder = com.google.android.gms.maps.model.LatLngBounds.builder()
        latLngs.forEach { builder.include(it) }
        return builder.build().center
    }

    fun onMapClick() {
        _uiState.update { it.copy(highlightedBin = null) }
    }

    fun toggleBuildingLayer(isVisible: Boolean) {
        _uiState.update { it.copy(isBuildingLayerVisible = isVisible) }
    }

    fun toggleRoadLayer(isVisible: Boolean) {
        _uiState.update { it.copy(isRoadLayerVisible = isVisible) }
    }
    
    fun toggleSewerLayer(isVisible: Boolean) {
        _uiState.update { it.copy(isSewerLayerVisible = isVisible) }
    }
    
    fun toggleSangkatLayer(isVisible: Boolean) {
        _uiState.update { it.copy(isSangkatLayerVisible = isVisible) }
    }

    fun showSurveyAlert(title: String, message: String) {
        _surveyAlert.update { SurveyAlertState(show = true, title = title, message = message) }
    }

    fun dismissSurveyAlert() {
        _surveyAlert.update { SurveyAlertState() }
    }
}

data class MapUiState(
    val loading: Boolean = true,
    val wfsData: List<WfsFeature> = emptyList(),
    val filteredData: List<WfsFeature> = emptyList(),
    val highlightedBin: String? = null,
    val permissions: Map<String, Boolean> = mapOf("View Map" to true, "Edit Building Survey" to true),
    val isBuildingLayerVisible: Boolean = true,
    val isRoadLayerVisible: Boolean = true,
    val isSewerLayerVisible: Boolean = true,
    val isSangkatLayerVisible: Boolean = true,
    val roadWmsUrl: String? = null,
    val sewerWmsUrl: String? = null,
    val sangkatWmsUrl: String? = null,
    val buildingWmsUrl: String? = null,
    val debugInfo: String? = null
)

data class CameraUpdateEvent(
    val location: LatLng,
    val zoomLevel: Float
)