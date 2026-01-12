package com.innovative.smis.ui.features.desludgingvehicle

import com.innovative.smis.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.innovative.smis.data.model.response.VehicleResponse
import com.innovative.smis.util.localization.LocalizationManager
import org.koin.androidx.compose.koinViewModel

enum class VehicleStatus(val color: Color, val apiValue: String) {
    ACTIVE(Color(0xFF28A745), "active"),
    UNDER_MAINTENANCE(Color(0xFFFFC107), "under-maintenance"),
    INACTIVE(Color(0xFFDC3545), "inactive");

    companion object {
        fun fromApiValue(value: String?): VehicleStatus {
            return values().find { it.apiValue == value } ?: INACTIVE
        }
    }
}

fun VehicleStatus.labelResId(): Int {
    return when (this) {
        VehicleStatus.ACTIVE -> R.string.status_active
        VehicleStatus.UNDER_MAINTENANCE -> R.string.status_under_maintenance
        VehicleStatus.INACTIVE -> R.string.status_inactive
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesludgingVehicleScreen(
    navController: NavController,
    onMenuClick: (() -> Unit)? = null,
    viewModel: DesludgingVehicleViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val languageCode = LocalizationManager.getCurrentLanguage(context)
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Handle update success
    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            snackbarHostState.showSnackbar(context.getString(R.string.message_vehicle_status_updated))
            viewModel.clearUpdateSuccess()
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    // Handle pull to refresh
    LaunchedEffect(uiState.isRefreshing) {
        if (uiState.isRefreshing) {
            // Refreshing handled by viewModel
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.nav_desludging_vehicles),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refreshVehicles) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            state = pullToRefreshState,
            onRefresh = viewModel::refreshVehicles,
            modifier = Modifier.padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Status Overview Card
                if (uiState.vehicles.isNotEmpty()) {
                    item {
                        val stats = viewModel.getVehicleStats()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.label_vehicle_status_overview),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VehicleStatusChip(
                                        title = stringResource(R.string.status_active),
                                        count = stats["active"] ?: 0,
                                        icon = Icons.Default.CheckCircle,
                                        color = VehicleStatus.ACTIVE.color,
                                        modifier = Modifier.weight(1f)
                                    )
                                    VehicleStatusChip(
                                        title = stringResource(R.string.status_under_maintenance),
                                        count = stats["under-maintenance"] ?: 0,
                                        icon = Icons.Default.Build,
                                        color = VehicleStatus.UNDER_MAINTENANCE.color,
                                        modifier = Modifier.weight(1f)
                                    )
                                    VehicleStatusChip(
                                        title = stringResource(R.string.status_inactive),
                                        count = stats["inactive"] ?: 0,
                                        icon = Icons.Default.Block,
                                        color = VehicleStatus.INACTIVE.color,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Loading State
                if (uiState.isLoading) {
                    items(3) {
                        VehicleCardSkeleton()
                    }
                }

                // Vehicle List
                items(uiState.vehicles, key = { it.id }) { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        onStatusUpdate = { newStatus ->
                            viewModel.updateVehicleStatus(vehicle.id, newStatus)
                        },
                        isUpdating = uiState.isUpdatingVehicle
                    )
                }

                // Empty State
                if (!uiState.isLoading && uiState.vehicles.isEmpty()) {
                    item {
                        EmptyVehiclesState(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp)
                        )
                    }
                }
            }
        }
    }
}

// Support composables for the screen
@Composable
fun VehicleStatusChip(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun VehicleCard(
    vehicle: VehicleResponse,
    onStatusUpdate: (String) -> Unit,
    isUpdating: Boolean,
    modifier: Modifier = Modifier
) {
    val vehicleStatus = VehicleStatus.fromApiValue(vehicle.status)
    var showStatusDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with license plate and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = vehicle.licensePlateNo,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    modifier = Modifier.clickable { showStatusDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = vehicleStatus.color.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, vehicleStatus.color)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = vehicleStatus.color
                            )
                        } else {
                            Icon(
                                imageVector = when (vehicleStatus) {
                                    VehicleStatus.ACTIVE -> Icons.Default.CheckCircle
                                    VehicleStatus.UNDER_MAINTENANCE -> Icons.Default.Build
                                    VehicleStatus.INACTIVE -> Icons.Default.Block
                                },
                                contentDescription = null,
                                tint = vehicleStatus.color,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = stringResource(vehicleStatus.labelResId()),
                            style = MaterialTheme.typography.labelSmall,
                            color = vehicleStatus.color,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Vehicle details
            vehicle.driverName?.let { driverName ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = driverName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            vehicle.capacity?.let { capacity ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.label_capacity, capacity),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            vehicle.currentLocation?.let { location ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // Status update dialog
    if (showStatusDialog) {
        StatusUpdateDialog(
            currentStatus = vehicleStatus,
            onStatusSelected = { newStatus ->
                onStatusUpdate(newStatus.apiValue)
                showStatusDialog = false
            },
            onDismiss = { showStatusDialog = false }
        )
    }
}

@Composable
fun StatusUpdateDialog(
    currentStatus: VehicleStatus,
    onStatusSelected: (VehicleStatus) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_update_vehicle_status)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VehicleStatus.values().forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStatusSelected(status) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = status == currentStatus,
                            onClick = { onStatusSelected(status) }
                        )
                        Icon(
                            imageVector = when (status) {
                                VehicleStatus.ACTIVE -> Icons.Default.CheckCircle
                                VehicleStatus.UNDER_MAINTENANCE -> Icons.Default.Build
                                VehicleStatus.INACTIVE -> Icons.Default.Block
                            },
                            contentDescription = null,
                            tint = status.color,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(status.labelResId()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun VehicleCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
            }

            // Details skeleton
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
            }
        }
    }
}

@Composable
fun EmptyVehiclesState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalShipping,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.message_no_vehicles),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.message_no_vehicles_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
