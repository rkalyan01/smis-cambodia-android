package com.innovative.smis.ui.features.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.innovative.smis.data.local.entity.MapBounds
import com.innovative.smis.data.local.entity.OfflineMapAreaEntity
import com.innovative.smis.data.local.offline.MapCacheStats
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.localization.LocalizationManager
import com.innovative.smis.util.localization.StringResources
import androidx.compose.ui.platform.LocalContext
import org.koin.androidx.compose.koinViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapScreen(navController: NavController) {
    val viewModel: OfflineMapViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currentLanguage = remember { LocalizationManager.getCurrentLanguage(context) }
    val languageCode = remember(currentLanguage) { LocalizationManager.getLanguageCode(currentLanguage) }
    
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showCacheStatsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(StringResources.getString(StringResources.OFFLINE_MAP, languageCode)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        viewModel.loadCacheStats()
                        showCacheStatsDialog = true 
                    }) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = "Cache Stats",
                            tint = Color.White
                        )
                    }
                    
                    IconButton(onClick = { showDownloadDialog = true }) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = "Download Area",
                            tint = Color.White
                        )
                    }
                    
                    IconButton(onClick = { /* Open settings */ }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.toggleMapType() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = "Switch Map Type"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!uiState.isOnline) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = "Offline",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline Mode - Using Cached Maps",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                cameraPositionState = uiState.cameraPositionState,
                properties = uiState.mapProperties,
                uiSettings = uiState.mapUiSettings,
                onMapLoaded = {
                    viewModel.onMapLoaded()
                },
                onMapClick = { latLng ->
                    viewModel.onMapClick(latLng)
                }
            ) {
                uiState.buildingPolygons.forEach { building ->
                    val coordinates = building.coordinates.map {
                        com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
                    }
                    
                    if (coordinates.isNotEmpty()) {
                        Polygon(
                            points = coordinates,
                            strokeColor = when (building.surveyStatus) {
                                "COMPLETED" -> Color.Green
                                "IN_PROGRESS" -> Color.Yellow
                                else -> Color.Red
                            },
                            fillColor = when (building.surveyStatus) {
                                "COMPLETED" -> Color.Green.copy(alpha = 0.3f)
                                "IN_PROGRESS" -> Color.Yellow.copy(alpha = 0.3f)
                                else -> Color.Red.copy(alpha = 0.2f)
                            },
                            strokeWidth = 2.0f,
                            clickable = true,
                            tag = building.buildingId
                        )
                    }
                }
                
                uiState.poiMarkers.forEach { poi ->
                    Marker(
                        state = MarkerState(
                            position = com.google.android.gms.maps.model.LatLng(
                                poi.position.latitude, 
                                poi.position.longitude
                            )
                        ),
                        title = poi.title,
                        snippet = poi.description,
                        tag = poi.poiId
                    )
                }
                
                uiState.downloadedAreas.forEach { area ->
                    if (area.downloadStatus == "COMPLETED") {
                        Polygon(
                            points = listOf(
                                LatLng(area.bounds.southwest.latitude, area.bounds.southwest.longitude),
                                LatLng(area.bounds.northeast.latitude, area.bounds.southwest.longitude),
                                LatLng(area.bounds.northeast.latitude, area.bounds.northeast.longitude),
                                LatLng(area.bounds.southwest.latitude, area.bounds.northeast.longitude)
                            ),

                            strokeColor = Color.Blue,
                            fillColor = Color.Blue.copy(alpha = 0.1f),
                            strokeWidth = 2.0f
                        )
                    }
                }
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Buildings", uiState.cacheStats?.buildingCount ?: 0)
                    StatItem("Tiles", uiState.cacheStats?.totalTiles ?: 0) 
                    StatItem("Cache", "${String.format("%.1f", uiState.cacheStats?.cacheSizeMB ?: 0.0)}MB")
                    StatItem("Areas", uiState.downloadedAreas.count { it.downloadStatus == "COMPLETED" })
                }
            }
        }
    }
    
    if (showDownloadDialog) {
        DownloadAreaDialog(
            onDismiss = { showDownloadDialog = false },
            onDownload = { bounds, zoomLevels ->
                viewModel.downloadMapArea(bounds, zoomLevels)
                showDownloadDialog = false
            }
        )
    }
    
    if (showCacheStatsDialog) {
        CacheStatsDialog(
            stats = uiState.cacheStats,
            onDismiss = { showCacheStatsDialog = false },
            onClearCache = {
                viewModel.clearOfflineCache()
                showCacheStatsDialog = false
            }
        )
    }
}

@Composable
fun StatItem(label: String, value: Any) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadAreaDialog(
    onDismiss: () -> Unit,
    onDownload: (MapBounds, List<Int>) -> Unit
) {
    var areaName by remember { mutableStateOf("") }
    var selectedZoomLevels by remember { mutableStateOf(listOf(15, 16, 17, 18)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Map Area") },
        text = {
            Column {
                Text("Download map tiles for offline use")
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = areaName,
                    onValueChange = { areaName = it },
                    label = { Text("Area Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Zoom Levels (15-18 recommended):")
                Row {
                    for (zoom in 12..20) {
                        FilterChip(
                            onClick = { 
                                selectedZoomLevels = if (selectedZoomLevels.contains(zoom)) {
                                    selectedZoomLevels - zoom
                                } else {
                                    selectedZoomLevels + zoom
                                }
                            },
                            label = { Text(zoom.toString()) },
                            selected = selectedZoomLevels.contains(zoom)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bounds = MapBounds(
                        southwest = com.innovative.smis.data.local.entity.LatLng(11.544, 104.892), // Phnom Penh area
                        northeast = com.innovative.smis.data.local.entity.LatLng(11.566, 104.916)
                    )
                    onDownload(bounds, selectedZoomLevels)
                }
            ) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CacheStatsDialog(
    stats: MapCacheStats?,
    onDismiss: () -> Unit,
    onClearCache: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offline Cache Statistics") },
        text = {
            LazyColumn {
                stats?.let { cacheStats ->
                    item {
                        StatsRow("Total Tiles", cacheStats.totalTiles.toString())
                        StatsRow("Cache Size", "${String.format("%.1f", cacheStats.cacheSizeMB)} MB")
                        StatsRow("Buildings", cacheStats.buildingCount.toString())
                        StatsRow("Completed Surveys", cacheStats.completedSurveys.toString())
                        StatsRow("Downloaded Areas", cacheStats.downloadedAreas.toString())
                        StatsRow("Total Download Size", "${String.format("%.1f", cacheStats.totalDownloadSizeMB)} MB")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Available Map Types:", fontWeight = FontWeight.Bold)
                        cacheStats.availableTileTypes.forEach { type ->
                            Text("• $type", fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onClearCache,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear Cache")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}