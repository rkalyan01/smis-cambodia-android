package com.innovative.smis.ui.features.map

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Dummy data classes to represent your WFS data structure
data class WfsFeature(
    val id: String,
    val bin: String?,
    val geometry: Geometry?,
    val is_surveyed: Boolean,
    val is_auxiliary: Boolean
)

data class Geometry(
    val type: String,
    val coordinates: List<List<List<List<Double>>>>
)


class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState = _uiState.asStateFlow()

    // This flow will emit the new location for the UI to observe.
    private val _locationState = MutableSharedFlow<LatLng>()
    val locationState = _locationState.asSharedFlow()

    // This state tracks if we are currently fetching the location.
    private val _isLocatingUser = MutableStateFlow(false)
    val isLocatingUser = _isLocatingUser.asStateFlow()

    init {
        fetchWfsData()
    }

    private fun fetchWfsData() {
        val dummyData = listOf(
            WfsFeature("1", "B-101", Geometry("MultiPolygon", listOf(listOf(listOf(listOf(103.87190, 13.36710), listOf(103.87200, 13.36710), listOf(103.87200, 13.36720), listOf(103.87190, 13.36720), listOf(103.87190, 13.36710))))), is_surveyed = false, is_auxiliary = false),
            WfsFeature("2", "B-102", Geometry("MultiPolygon", listOf(listOf(listOf(listOf(103.87210, 13.36730), listOf(103.87220, 13.36730), listOf(103.87220, 13.36740), listOf(103.87210, 13.36740), listOf(103.87210, 13.36730))))), is_surveyed = true, is_auxiliary = false),
            WfsFeature("3", "A-201", Geometry("MultiPolygon", listOf(listOf(listOf(listOf(103.87150, 13.36750), listOf(103.87160, 13.36750), listOf(103.87160, 13.36760), listOf(103.87150, 13.36760), listOf(103.87150, 13.36750))))), is_surveyed = false, is_auxiliary = true)
        )
        _uiState.value = _uiState.value.copy(loading = false, wfsData = dummyData, filteredData = dummyData)
    }

    /**
     * Fetches the user's current location and emits it to the locationState flow.
     * Permissions are checked in the Composable before this is called.
     */
    @SuppressLint("MissingPermission")
    fun animateToCurrentLocation(context: Context) {
        viewModelScope.launch {
            _isLocatingUser.value = true
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                // Request the current location with high accuracy.
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await()

                if (location != null) {
                    _locationState.emit(LatLng(location.latitude, location.longitude))
                }
            } catch (e: Exception) {
                // Handle exceptions, e.g., location services are disabled.
                // You could emit an error state here for the UI to show a message.
            } finally {
                _isLocatingUser.value = false
            }
        }
    }

    fun onPolygonPress(item: WfsFeature) {
        if (item.is_auxiliary) {
            val currentHighlighted = _uiState.value.highlightedBin
            _uiState.value = _uiState.value.copy(
                highlightedBin = if (currentHighlighted == item.bin) null else item.bin
            )
        } else {
            _uiState.value = _uiState.value.copy(highlightedBin = null)
        }
    }

    fun getCenterOfPolygon(polygon: List<List<Double>>): LatLng {
        val latLngs = polygon.map { LatLng(it[1], it[0]) }
        val builder = com.google.android.gms.maps.model.LatLngBounds.builder()
        latLngs.forEach { builder.include(it) }
        return builder.build().center
    }

    fun onMapClick() {
        _uiState.value = _uiState.value.copy(highlightedBin = null)
    }
}

data class MapUiState(
    val loading: Boolean = true,
    val wfsData: List<WfsFeature> = emptyList(),
    val filteredData: List<WfsFeature> = emptyList(),
    val highlightedBin: String? = null,
    val permissions: Map<String, Boolean> = mapOf("View Map" to true, "Edit Building Survey" to true)
)
