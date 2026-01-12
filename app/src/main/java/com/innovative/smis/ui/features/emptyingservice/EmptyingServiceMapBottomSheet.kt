package com.innovative.smis.ui.features.emptyingservice

import com.innovative.smis.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import androidx.compose.ui.res.stringResource
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyingServiceMapBottomSheet(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onDismiss: () -> Unit,
    onLocationSelected: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Map type state - toggle between normal and satellite
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    
    // Get current location or use default
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    
    // Try to get current location on first launch
    LaunchedEffect(Unit) {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                }
            }
        } catch (e: SecurityException) {
            // Permission denied, will use default or initial location
        }
    }
    
    // Determine initial position: provided location > current location > default Phnom Penh
    val initialPosition = when {
        initialLatitude != null && initialLongitude != null -> LatLng(initialLatitude, initialLongitude)
        currentLocation != null -> currentLocation!!
        else -> LatLng(11.5564, 104.9282) // Default Phnom Penh
    }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 18f) // Zoom level 18 for close view
    }
    
    // Auto-animate to current location ONLY if no coordinates were provided
    LaunchedEffect(currentLocation) {
        if (initialLatitude == null || initialLongitude == null) {
            currentLocation?.let { location ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(location, 18f),
                    durationMs = 1000
                )
            }
        }
    }
    
    // Get center position from camera
    val centerPosition = cameraPositionState.position.target

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Select Location",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close"
                            )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Map container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = true,
                            mapType = mapType
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            myLocationButtonEnabled = false,
                            zoomGesturesEnabled = true,
                            scrollGesturesEnabled = true,
                            rotationGesturesEnabled = true,
                            tiltGesturesEnabled = true
                        )
                    )
                    
                    // Fixed center marker icon
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location Pin",
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                            .offset(y = (-24).dp), // Offset to point at center
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    // Map Type Toggle FAB - top left
                    FloatingActionButton(
                        onClick = {
                            mapType = if (mapType == MapType.NORMAL) {
                                MapType.SATELLITE
                            } else {
                                MapType.NORMAL
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = if (mapType == MapType.NORMAL) "Switch to Satellite" else "Switch to Map",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    
                    // My Location FAB - positioned to avoid overlap with zoom controls
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                        location?.let {
                                            scope.launch {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(
                                                        LatLng(it.latitude, it.longitude),
                                                        18f
                                                    )
                                                )
                                            }
                                        }
                                    }
                                } catch (e: SecurityException) {
                                    // Handle permission denied
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "My Location",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // Bottom info and actions panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Text(
                        "Latitude: ${String.format("%.6f", centerPosition.latitude)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Longitude: ${String.format("%.6f", centerPosition.longitude)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Drag the map to change location",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }

                        Button(
                            onClick = {
                                onLocationSelected(centerPosition.latitude, centerPosition.longitude)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.button_update))
                        }
                    }
                }
            }
        }
    }
}
