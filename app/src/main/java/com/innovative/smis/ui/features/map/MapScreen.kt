package com.innovative.smis.ui.features.map

import android.Manifest
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.net.URL
import androidx.compose.runtime.collectAsState
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.PatternItem
import com.innovative.smis.util.helper.PreferenceHelper
import com.innovative.smis.util.constants.PrefConstant
import com.innovative.smis.data.local.offline.OfflineMapManager
import com.innovative.smis.util.localization.LocalizationManager
import com.innovative.smis.util.localization.StringResources
import org.koin.compose.koinInject

// Helper function to parse permissions from SharedPreferences
private fun getUserPermissions(preferenceHelper: PreferenceHelper): Map<String, Boolean> {
    val permissionsJson = preferenceHelper.getString(PrefConstant.USER_PERMISSIONS, "") ?: ""
    if (permissionsJson.isEmpty()) return emptyMap()
    
    val permissions = mutableMapOf<String, Boolean>()
    try {
        val cleanJson = permissionsJson
            .replace("[{", "")
            .replace("}]", "")
            .replace("\"", "")
        
        if (cleanJson.isNotEmpty()) {
            cleanJson.split(",").forEach { pair ->
                val keyValue = pair.split(":")
                if (keyValue.size == 2) {
                    val key = keyValue[0].trim()
                    val value = keyValue[1].trim().toBoolean()
                    permissions[key] = value
                }
            }
        }
    } catch (e: Exception) {
        return emptyMap()
    }
    
    return permissions
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController, onMenuClick: (() -> Unit)? = null) {
    val viewModel: MapViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val isLocatingUser by viewModel.isLocatingUser.collectAsState()
    val isAnimatingToData by viewModel.isAnimatingToData.collectAsState()
    val surveyAlert by viewModel.surveyAlert.collectAsState()
    val context = LocalContext.current
    val currentLanguage = remember { LocalizationManager.getCurrentLanguage(context) }
    val languageCode = remember(currentLanguage) { LocalizationManager.getLanguageCode(currentLanguage) }

    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var showLayersDialog by remember { mutableStateOf(false) }

    val dashedPattern: List<PatternItem> = listOf(Dash(30f), Gap(20f))

    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )

    LaunchedEffect(Unit) {
        locationPermissionsState.launchMultiplePermissionRequest()
    }

    val defaultCameraPosition = CameraPosition.fromLatLngZoom(LatLng(13.36718, 103.87195), 18f)
    val cameraPositionState = rememberCameraPositionState { position = defaultCameraPosition }

    LaunchedEffect(Unit) {
        viewModel.locationState.collectLatest { latLng ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(latLng, 20f),
                durationMs = 1500
            )
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val newBounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
            val zoom = cameraPositionState.position.zoom
            viewModel.filterDataByViewport(newBounds, zoom)
        }
    }

    if (showLayersDialog) {
        LayerSelectionDialog(
            mapType = mapType,
            isRoadLayerVisible = uiState.isRoadLayerVisible,
            isBuildingLayerVisible = uiState.isBuildingLayerVisible,
            isSewerLayerVisible = uiState.isSewerLayerVisible,
            isSangkatLayerVisible = uiState.isSangkatLayerVisible,
            onDismiss = { showLayersDialog = false },
            onMapTypeChange = { mapType = it },
            onRoadLayerToggle = { viewModel.toggleRoadLayer(it) },
            onBuildingLayerToggle = { viewModel.toggleBuildingLayer(it) },
            onSewerLayerToggle = { viewModel.toggleSewerLayer(it) },
            onSangkatLayerToggle = { viewModel.toggleSangkatLayer(it) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringResources.getString(StringResources.SMIS_MAP, languageCode)) },
                navigationIcon = {
                    onMenuClick?.let { menuClick ->
                        IconButton(onClick = menuClick) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(
                        onClick = { 
                            navController.navigate("comprehensive_survey_new")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = StringResources.getString(StringResources.NEW_SURVEY, languageCode),
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (locationPermissionsState.allPermissionsGranted) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // FAB to open the new layers dialog
                    FloatingActionButton(onClick = { showLayersDialog = true }) {
                        Icon(Icons.Default.Layers, contentDescription = StringResources.getString(StringResources.TOGGLE_MAP_LAYERS, languageCode))
                    }

                    FloatingActionButton(
                        onClick = { viewModel.animateToDataRegion() },
                        containerColor = if (isAnimatingToData) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.primary // Always enabled when location permissions granted
                        }
                    ) {
                        if (isAnimatingToData) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Business, contentDescription = "Navigate to Data Region")
                        }
                    }


                    FloatingActionButton(
                        onClick = { viewModel.animateToCurrentLocation(context) },
                        containerColor = if (isLocatingUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    ) {
                        if (isLocatingUser) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                uiState.loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                !locationPermissionsState.allPermissionsGranted || uiState.permissions["View Map"] != true -> {
                    PermissionDeniedContent()
                }
                else -> {
                    val roadTileProvider = remember(uiState.roadWmsUrl) {
                        uiState.roadWmsUrl?.let { createTileProvider(it) }
                    }
                    
                    val sewerTileProvider = remember(uiState.sewerWmsUrl) {
                        uiState.sewerWmsUrl?.let { createTileProvider(it) }
                    }
                    
                    val sangkatTileProvider = remember(uiState.sangkatWmsUrl) {
                        uiState.sangkatWmsUrl?.let { createTileProvider(it) }
                    }
                    
                    val buildingWmsTileProvider = remember(uiState.buildingWmsUrl) {
                        uiState.buildingWmsUrl?.let { createTileProvider(it) }
                    }
                    
                    // Get offline map manager for custom tile provider
                    val offlineMapManager: OfflineMapManager = koinInject()
                    val offlineTileProvider = remember(mapType) {
                        val tileTypeString = when (mapType) {
                            MapType.SATELLITE -> "satellite"
                            MapType.HYBRID -> "hybrid"
                            MapType.TERRAIN -> "terrain"
                            else -> "roadmap"
                        }
                        OfflineTileProvider(offlineMapManager, tileTypeString)
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = true, 
                            mapType = MapType.NONE // Use NONE to let TileOverlay handle the base map
                        ),
                        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                        onMapClick = { viewModel.onMapClick() }
                    ) {
                        // Add offline tile overlay as base map
                        TileOverlay(
                            tileProvider = offlineTileProvider,
                            zIndex = 0f, // Lowest z-index for base map
                            transparency = 0f
                        )
                        // Sangkat layer (lowest WMS layer)
                        if (uiState.isSangkatLayerVisible && sangkatTileProvider != null) {
                            TileOverlay(
                                tileProvider = sangkatTileProvider,
                                zIndex = 1f,
                                transparency = 0.3f
                            )
                        }
                        
                        // Building WMS layer 
                        if (buildingWmsTileProvider != null) {
                            TileOverlay(
                                tileProvider = buildingWmsTileProvider,
                                zIndex = 2f,
                                transparency = 0.4f
                            )
                        }
                        
                        // Road layer
                        if (uiState.isRoadLayerVisible && roadTileProvider != null) {
                            TileOverlay(
                                tileProvider = roadTileProvider,
                                zIndex = 3f,
                                transparency = 0.3f
                            )
                        }
                        
                        // Sewer layer (highest WMS layer)
                        if (uiState.isSewerLayerVisible && sewerTileProvider != null) {
                            TileOverlay(
                                tileProvider = sewerTileProvider,
                                zIndex = 4f,
                                transparency = 0.3f
                            )
                        }

                        if (uiState.isBuildingLayerVisible) {
                            uiState.filteredData.forEach { item ->
                                val coordinates = item.geometry?.coordinates?.firstOrNull()?.firstOrNull() ?: emptyList()
                                if (coordinates.isNotEmpty()) {
                                    val latLngs = coordinates.map { LatLng(it[1], it[0]) }
                                    val isHighlighted = uiState.highlightedBin == item.bin

                                    val (strokeColor, fillColor) = when {
                                        isHighlighted -> Color(0xFFFFD700) to Color(0x66FFFF00)
                                        item.is_auxiliary == true -> Color.Gray to Color(0x33000000)
                                        item.is_surveyed == true -> Color(0xFF198754) to Color(0x0D198754)
                                        else -> Color(0xFF007BFF) to Color(0x0D007BFF)
                                    }

                                    Polygon(
                                        points = latLngs,
                                        clickable = true,
                                        onClick = { 
                                            viewModel.onPolygonPress(item)
                                            // Check permissions before navigating to survey
                                            val preferenceHelper = PreferenceHelper(context)
                                            val permissions = getUserPermissions(preferenceHelper)
                                            val hasEditPermission = permissions["Edit Building Survey"] == true
                                            
                                            if (hasEditPermission && item.is_surveyed != true) {
                                                item.bin?.let { bin ->
                                                    navController.navigate("comprehensive_survey/$bin")
                                                }
                                            } else if (hasEditPermission && item.is_surveyed == true) {
                                                // Show alert for already surveyed building
                                                // In Android, we'll handle this in the ViewModel
                                                viewModel.showSurveyAlert("Already Surveyed", "This building has already been surveyed and cannot be edited.")
                                            } else if (!hasEditPermission) {
                                                // Show permission denied alert
                                                viewModel.showSurveyAlert("Permission Denied", "You don't have permission to edit building surveys.")
                                            }
                                        },
                                        strokeWidth = 5f,
                                        strokeColor = strokeColor,
                                        fillColor = fillColor,
                                        strokePattern = dashedPattern,
                                        zIndex = 10f
                                    )

                                    item.bin?.let { bin ->
                                        val center = viewModel.getCenterOfPolygon(coordinates)
                                        MarkerComposable(
                                            state = MarkerState(position = center),
                                            title = bin,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(text = bin, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = strokeColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun PermissionDeniedContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Location permission is required to view the map. Please grant the permission in your device settings.",
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LayerSelectionDialog(
    mapType: MapType,
    isRoadLayerVisible: Boolean,
    isBuildingLayerVisible: Boolean,
    isSewerLayerVisible: Boolean,
    isSangkatLayerVisible: Boolean,
    onDismiss: () -> Unit,
    onMapTypeChange: (MapType) -> Unit,
    onRoadLayerToggle: (Boolean) -> Unit,
    onBuildingLayerToggle: (Boolean) -> Unit,
    onSewerLayerToggle: (Boolean) -> Unit,
    onSangkatLayerToggle: (Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Layer Options", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))

                Text("Map Type", style = MaterialTheme.typography.titleMedium)
                Column {
                    val mapTypes = listOf(
                        "Normal" to MapType.NORMAL,
                        "Hybrid" to MapType.HYBRID,
                        "Satellite" to MapType.SATELLITE,
                        "Terrain" to MapType.TERRAIN
                    )

                    mapTypes.forEach { (name, type) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMapTypeChange(type) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (mapType == type),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Overlays", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Building Layer (Local)")
                    Switch(checked = isBuildingLayerVisible, onCheckedChange = onBuildingLayerToggle)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sangkat Boundaries")
                    Switch(checked = isSangkatLayerVisible, onCheckedChange = onSangkatLayerToggle)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Road Networks")
                    Switch(checked = isRoadLayerVisible, onCheckedChange = onRoadLayerToggle)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sewer Networks")
                    Switch(checked = isSewerLayerVisible, onCheckedChange = onSewerLayerToggle)
                }

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

private fun createTileProvider(urlTemplate: String): TileProvider {
    return object : UrlTileProvider(256, 256) {
        override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
            val s = urlTemplate
                .replace("{x}", x.toString(), true)
                .replace("{y}", y.toString(), true)
                .replace("{z}", zoom.toString(), true)
            return try {
                URL(s)
            } catch (e: Exception) {
                null
            }
        }
    }
}