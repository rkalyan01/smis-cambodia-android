package com.innovative.smis.ui.features.map

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController) {
    val viewModel: MapViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val isLocatingUser by viewModel.isLocatingUser.collectAsState()
    val context = LocalContext.current
    var mapType by remember { mutableStateOf(MapType.NORMAL) }


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

    // This effect observes the location state from the ViewModel and animates the camera.
    LaunchedEffect(Unit) {
        viewModel.locationState.collectLatest { latLng ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(latLng, 18f),
                durationMs = 1500
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMIS Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (locationPermissionsState.allPermissionsGranted) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FloatingActionButton(onClick = {
                        mapType = if (mapType == MapType.NORMAL) MapType.SATELLITE else MapType.NORMAL
                    }) {
                        Icon(Icons.Default.Layers, contentDescription = "Toggle Map Layer")
                    }

                    FloatingActionButton(onClick = {
                        navController.navigate("data_screen")
                    }) {
                        Icon(Icons.Default.Business, contentDescription = "Navigate to Data Region")
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
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
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
                else -> {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = true, mapType = mapType),
                        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                        onMapClick = { viewModel.onMapClick() }
                    ) {
                        uiState.filteredData.forEach { item ->
                            val coordinates = item.geometry?.coordinates?.firstOrNull()?.firstOrNull() ?: emptyList()
                            if (coordinates.isNotEmpty()) {
                                val latLngs = coordinates.map { LatLng(it[1], it[0]) }
                                val isHighlighted = uiState.highlightedBin == item.bin

                                val (strokeColor, fillColor) = when {
                                    isHighlighted -> Color(0xFFFFD700) to Color(0x66FFFF00)
                                    item.is_auxiliary -> Color.Gray to Color(0x33000000)
                                    item.is_surveyed -> Color(0xFF198754) to Color(0x0D198754)
                                    else -> Color(0xFF007BFF) to Color(0x0D007BFF)
                                }

                                Polygon(
                                    points = latLngs,
                                    clickable = true,
                                    onClick = { viewModel.onPolygonPress(item) },
                                    strokeWidth = 2f,
                                    strokeColor = strokeColor,
                                    fillColor = fillColor,
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
                                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
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
