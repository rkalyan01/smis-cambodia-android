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
import com.innovative.smis.data.api.WmsRoadResponse
import com.innovative.smis.data.api.WmsSewerResponse  
import com.innovative.smis.data.api.WmsSangkatResponse
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

    private val _locationState = MutableSharedFlow<LatLng>()
    val locationState = _locationState.asSharedFlow()

    private val _isLocatingUser = MutableStateFlow(false)
    val isLocatingUser = _isLocatingUser.asStateFlow()

    private val _isAnimatingToData = MutableStateFlow(false)
    val isAnimatingToData = _isAnimatingToData.asStateFlow()

    private val _surveyAlert = MutableStateFlow(SurveyAlertState())
    val surveyAlert = _surveyAlert.asStateFlow()

    private var filterJob: Job? = null

    init {
        fetchWfsData()
        fetchLayerData()
    }

    private fun fetchWfsData() {
        viewModelScope.launch {
            buildingSurveyRepository.getWFSLayerBuildings().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val rawFeatures = result.data?.features ?: emptyList()
                        val features = rawFeatures.map { feature ->
                            WfsFeature(
                                id = feature.id,
                                bin = feature.properties?.bin,
                                geometry = feature.geometry?.let { geo ->
                                    Geometry(
                                        type = geo.type,
                                        coordinates = geo.coordinates?.map { 
                                            listOf(it)
                                        } ?: emptyList()
                                    )
                                },
                                is_surveyed = feature.properties?.is_surveyed,
                                is_auxiliary = feature.properties?.is_auxiliary
                            )
                        }
                        _uiState.update {
                            it.copy(loading = false, wfsData = features, filteredData = emptyList())
                        }
                        Log.d("MapDebug", "SUCCESS: Loaded ${features.size} total features.")
                    }
                    is Resource.Error -> {
                        Log.e("MapDebug", "API Error: ${result.message}")
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

    private fun fetchLayerData() {
        viewModelScope.launch {
            // Fetch road layer
            launch {
                buildingSurveyRepository.getRoadWms().collect { result ->
                    when (result) {
                        is Resource.Success<*> -> {
                            val roadResponse = result.data as? WmsRoadResponse
                            Log.d("MapViewModel", "Road WMS Response: $roadResponse")
                            if (roadResponse?.success == true && !roadResponse.data.isNullOrEmpty()) {
                                // Roads layer successfully fetched - layers should be visible now
                                Log.d("MapViewModel", "Road data loaded: ${roadResponse.data.size} road segments")
                                // Note: Layers are displayed as building polygons from local building data
                                // WMS integration would require actual tile server URLs from backend
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
                            if (sewerResponse?.success == true && !sewerResponse.data.isNullOrEmpty()) {
                                // Sewer layer successfully fetched
                                Log.d("MapViewModel", "Sewer data loaded: ${sewerResponse.data.size} sewer segments")
                                // Note: Layers are displayed as building polygons from local building data
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
                            if (sangkatResponse?.success == true && !sangkatResponse.data.isNullOrEmpty()) {
                                // Sangkat layer successfully fetched
                                Log.d("MapViewModel", "Sangkat data loaded: ${sangkatResponse.data.size} sangkat boundaries")
                                // Note: Layers are displayed as building polygons from local building data
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
                            if (buildingResponse?.success == true) {
                                // Building layer successfully fetched
                                Log.d("MapViewModel", "Building WMS data loaded successfully")
                                // Note: Layers are displayed as building polygons from local building data
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
            if (zoomLevel < 18f) {
                if (_uiState.value.filteredData.isNotEmpty()) {
                    _uiState.update { it.copy(filteredData = emptyList()) }
                }
                return@launch
            }

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
                return@launch
            }

            val firstCoordinate = _uiState.value.wfsData.first()
                .geometry?.coordinates?.firstOrNull()?.firstOrNull()?.firstOrNull()

            if (firstCoordinate != null && firstCoordinate.size >= 2) {
                _isAnimatingToData.value = true
                val targetLocation = LatLng(firstCoordinate[1], firstCoordinate[0])
                _locationState.emit(targetLocation)
                delay(1500) // Match animation duration
                _isAnimatingToData.value = false
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
                    _locationState.emit(LatLng(location.latitude, location.longitude))
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
    val isRoadLayerVisible: Boolean = false,
    val isSewerLayerVisible: Boolean = false,
    val isSangkatLayerVisible: Boolean = false,
    val roadWmsUrl: String? = null,
    val sewerWmsUrl: String? = null,
    val sangkatWmsUrl: String? = null,
    val buildingWmsUrl: String? = null
)