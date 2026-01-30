package com.innovative.smis.ui.features.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.innovative.smis.util.constants.ScreenName
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingMapScreen(
    navController: NavController,
    onMenuClick: (() -> Unit)? = null
) {
    val viewModel: MapViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val surveyAlert by viewModel.surveyAlert.collectAsState()
    val dataRegionMessage by viewModel.dataRegionMessage.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Cambodia coordinates (Phnom Penh area)
    val cambodia = LatLng(11.5564, 104.9282)
    val cameraPositionState = rememberCameraPositionState {
        // Default to Siem Reap to see WMS/WFS data
        position = CameraPosition.fromLatLngZoom(LatLng(13.3633, 103.8564), 14f)
    }

    // WMS Tile Providers
    val roadTileProvider = remember(uiState.roadWmsUrl) {
        uiState.roadWmsUrl?.let { WmsTileProvider(it) }
    }
    
    val sewerTileProvider = remember(uiState.sewerWmsUrl) {
        uiState.sewerWmsUrl?.let { WmsTileProvider(it) }
    }
    
    val sangkatTileProvider = remember(uiState.sangkatWmsUrl) {
        uiState.sangkatWmsUrl?.let { WmsTileProvider(it) }
    }

    val buildingTileProvider = remember(uiState.buildingWmsUrl) {
        uiState.buildingWmsUrl?.let { WmsTileProvider(it) }
    }

    // State for user-placed markers (for drawing buildings)

    var isMapLoaded by remember { mutableStateOf(false) }
    var showLayerDialog by remember { mutableStateOf(false) }
    var showMapTypeDialog by remember { mutableStateOf(false) }
    var currentMapType by remember { mutableStateOf(MapType.NORMAL) }
    var hasAutoNavigatedToData by remember { mutableStateOf(false) }

    // On first open: when WFS building data is available, animate camera to building data region (same as tapping Building FAB)
    LaunchedEffect(uiState.wfsData.size) {
        if (uiState.wfsData.isNotEmpty() && !hasAutoNavigatedToData) {
            hasAutoNavigatedToData = true
            viewModel.animateToDataRegion()
        }
    }

    // Animate camera to location/data region when user taps My Location or Building FAB (same pattern as MapScreen / RN DashboardScreen)
    LaunchedEffect(Unit) {
        viewModel.locationState.collectLatest { event ->
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(event.location, event.zoomLevel), 1000)
        }
    }

    // Filter WFS building polygons by viewport so they render and are clickable (same as MapScreen / RN handleRegionChange)
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val newBounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
            val zoom = cameraPositionState.position.zoom
            viewModel.filterDataByViewport(newBounds, zoom)
        }
    }

    // Check location permission
    val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    // Theme logic
    val preferenceHelper = remember { com.innovative.smis.util.helper.PreferenceHelper(context) }
    val themeMode = preferenceHelper.themeMode
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMIS", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Loading overlay drawn first so FABs (drawn later) stay clickable
            if (!isMapLoaded || uiState.loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLoaded = { isMapLoaded = true },

                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false // Using custom FAB
                ),
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission,
                    mapType = currentMapType,
                    // Apply Dark Mode style if system is in dark theme
                    // Apply Dark Mode style based on App Preferences
                    mapStyleOptions = remember(themeMode) {
                        val isDark = when (themeMode) {
                            com.innovative.smis.util.helper.PreferenceHelper.ThemeMode.DARK -> true
                            com.innovative.smis.util.helper.PreferenceHelper.ThemeMode.LIGHT -> false
                            com.innovative.smis.util.helper.PreferenceHelper.ThemeMode.AUTO -> isSystemInDarkTheme
                        }
                        if (isDark) {
                            com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(context, com.innovative.smis.R.raw.map_style_dark)
                        } else {
                            null
                        }
                    }
                )
            ) {
                // Render WMS Layers (Context Layers)
                // Sangkat layer (lowest)
                if (uiState.isSangkatLayerVisible && sangkatTileProvider != null) {
                    TileOverlay(
                        tileProvider = sangkatTileProvider,
                        zIndex = 1f,
                        transparency = 0.0f
                    )
                }

                // Building layer (WMS) (Already 0.0f, keep it)
                if (uiState.isBuildingLayerVisible && buildingTileProvider != null) {
                    TileOverlay(
                        tileProvider = buildingTileProvider,
                        zIndex = 1.5f,
                        transparency = 0.0f // Fully opaque for visibility
                    )
                }
                
                // Road layer
                if (uiState.isRoadLayerVisible && roadTileProvider != null) {
                    TileOverlay(
                        tileProvider = roadTileProvider,
                        zIndex = 2f,
                        transparency = 0.0f
                    )
                }
                
                // Sewer layer
                if (uiState.isSewerLayerVisible && sewerTileProvider != null) {
                    TileOverlay(
                        tileProvider = sewerTileProvider,
                        zIndex = 3f,
                        transparency = 0.0f
                    )
                }


                // Draw Polygon if enough points


                // Render WFS Building Polygons (from ViewModel)
                if (uiState.isBuildingLayerVisible) {
                    uiState.filteredData.forEach { feature ->
                        feature.geometry?.coordinates?.firstOrNull()?.firstOrNull()?.let { coords ->
                            val points = coords.map { LatLng(it[1], it[0]) }
                            val isSurveyed = feature.is_surveyed == true
                            val isAuxiliary = feature.is_auxiliary == true
                            val isHighlighted = feature.bin == uiState.highlightedBin

                            // Same colors as DashboardScreen.js (RN)
                            Polygon(
                                points = points,
                                fillColor = when {
                                    isHighlighted -> MapLayerColors.highlightFill()
                                    isAuxiliary -> MapLayerColors.auxiliaryFill()
                                    isSurveyed -> MapLayerColors.surveyedFill()
                                    else -> MapLayerColors.unsurveyedFill()
                                },
                                strokeColor = when {
                                    isHighlighted -> MapLayerColors.highlightStroke()
                                    isAuxiliary -> MapLayerColors.auxiliaryStroke()
                                    isSurveyed -> MapLayerColors.surveyedStroke()
                                    else -> MapLayerColors.unsurveyedStroke()
                                },
                                strokeWidth = if (isHighlighted) 3f else 1f, // Default 0.5 in RN, approx 1-2dp in Android
                                zIndex = 10f, // Ensure polygon is above TileOverlays (zIndex 2f, 3f)
                                clickable = true,
                                onClick = {
                                    Log.d("MapClick", "Polygon clicked! BIN: ${feature.bin}, Surveyed: $isSurveyed, Auxiliary: $isAuxiliary")
                                    if (isAuxiliary) {
                                        viewModel.onPolygonPress(feature)
                                    } else {
                                        viewModel.onMapClick() // Clear highlight
                                        
                                        val hasEditPermission = uiState.permissions["Edit Building Survey"] == true
                                        Log.d("MapClick", "Permissions: Edit=$hasEditPermission")
                                        
                                        if (hasEditPermission && !isSurveyed) {
                                            feature.bin?.let { bin ->
                                                Log.d("MapClick", "Navigating to: building_survey_comprehensive/$bin")
                                                navController.navigate("building_survey_comprehensive/$bin")
                                            } ?: run {
                                                Log.e("MapClick", "BIN is null, cannot navigate!")
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Building ID (BIN) missing for this building. Cannot open survey form.",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        } else if (hasEditPermission && isSurveyed) {
                                            Log.d("MapClick", "Building already surveyed")
                                            viewModel.showSurveyAlert("Already Surveyed", "This building has already been surveyed and cannot be edited.")
                                        } else if (!hasEditPermission) {
                                            Log.d("MapClick", "Permission denied")
                                            viewModel.showSurveyAlert("Permission Denied", "You don't have permission to edit building surveys.")
                                        }
                                    }
                                }
                            )

                            // Show BIN Label (centered on polygon)
                            // Lowered zoom threshold to 16f to ensure visibility
                            if (uiState.isBuildingLayerVisible && !feature.bin.isNullOrEmpty() && points.isNotEmpty() && cameraPositionState.position.zoom >= 16f) {
                                val centerLat = points.map { it.latitude }.average()
                                val centerLng = points.map { it.longitude }.average()
                                
                                if (!centerLat.isNaN() && !centerLng.isNaN()) {
                                    val center = LatLng(centerLat, centerLng)
                                    val markerKey = feature.bin + "_" + feature.id

                                    MarkerComposable(
                                        keys = arrayOf(markerKey),
                                        state = rememberMarkerState(key = markerKey, position = center),
                                        zIndex = 20f,
                                        onClick = { 
                                            // Pass click through to polygon logic
                                            if (isAuxiliary) {
                                                viewModel.onPolygonPress(feature)
                                            } else {
                                                viewModel.onMapClick()
                                                // Simplified nav logic copy for now to ensure action works if marker is hit
                                                val hasEditPermission = uiState.permissions["Edit Building Survey"] == true
                                                if (hasEditPermission && !isSurveyed) {
                                                    feature.bin?.let { bin -> navController.navigate("building_survey_comprehensive/$bin") }
                                                }
                                            }
                                            true 
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .border(width = 1.dp, color = Color.DarkGray, shape = RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = feature.bin,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FABs on LEFT side like RN DashboardScreen
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 15.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // My Location FAB (bottom position like RN: bottom: 30)
                FloatingActionButton(
                    onClick = { viewModel.animateToCurrentLocation(context) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location", tint = Color.White)
                }
            }
            
            // Navigate to Data FAB (middle position like RN: bottom: 100)
            FloatingActionButton(
                onClick = { viewModel.animateToDataRegion() },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 15.dp, bottom = 100.dp)
                    .size(56.dp)
            ) {
                Icon(Icons.Default.Business, contentDescription = "Navigate to Data", tint = Color.White)
            }
            
            // Layers FAB (position: bottom: 170)
            FloatingActionButton(
                onClick = { showLayerDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 15.dp, bottom = 170.dp)
                    .size(56.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Layers", tint = Color.White)
            }
            
            // Map Type FAB (position: bottom: 240)
            FloatingActionButton(
                onClick = { showMapTypeDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 15.dp, bottom = 240.dp)
                    .size(56.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = "Map Type", tint = Color.White)
            }

            // Add Survey FAB (bottom right)
            /* FloatingActionButton(
                onClick = {
                    navController.navigate("building_survey_new")
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 30.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Survey", tint = Color.White)
            } */

            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))

            // Debug Info Overlay
            // Debug Info Overlay REMOVED
            // uiState.debugInfo?.let { ... }
        }
    }

    // Show Snackbar when user taps building FAB but no data is available yet
    LaunchedEffect(dataRegionMessage) {
        dataRegionMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearDataRegionMessage()
        }
    }

    // Layer Toggle Dialog
    if (showLayerDialog) {
        AlertDialog(
            onDismissRequest = { showLayerDialog = false },
            title = { Text("Map Layers") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.isBuildingLayerVisible,
                            onCheckedChange = { viewModel.toggleBuildingLayer(it) }
                        )
                        Text("Buildings", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.isRoadLayerVisible,
                            onCheckedChange = { viewModel.toggleRoadLayer(it) }
                        )
                        Text("Roads", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.isSangkatLayerVisible,
                            onCheckedChange = { viewModel.toggleSangkatLayer(it) }
                        )
                        Text("Commune/Sangkat", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.isSewerLayerVisible,
                            onCheckedChange = { viewModel.toggleSewerLayer(it) }
                        )
                        Text("Sewer Networks", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLayerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Map Type Dialog
    if (showMapTypeDialog) {
        AlertDialog(
            onDismissRequest = { showMapTypeDialog = false },
            title = { Text("Map Style") },
            text = {
                Column {
                    listOf(
                        MapType.NORMAL to "Normal",
                        MapType.SATELLITE to "Satellite",
                        MapType.TERRAIN to "Terrain",
                        MapType.HYBRID to "Hybrid"
                    ).forEach { (type, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentMapType = type
                                    showMapTypeDialog = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentMapType == type,
                                onClick = {
                                    currentMapType = type
                                    showMapTypeDialog = false
                                }
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMapTypeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Survey Alert Dialog
    if (surveyAlert.show) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSurveyAlert() },
            title = {
                Text(
                    text = surveyAlert.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = surveyAlert.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.dismissSurveyAlert() }
                ) {
                    Text(stringResource(com.innovative.smis.R.string.action_ok))
                }
            }
        )
    }
}
