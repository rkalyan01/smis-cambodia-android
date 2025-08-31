package com.innovative.smis.ui.features.dashboard
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.innovative.smis.data.model.response.TodoItem
import com.innovative.smis.ui.features.logout.LogoutBottomSheet
import com.innovative.smis.ui.features.dashboard.LogoutViewModel
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.constants.ScreenName
import com.innovative.smis.util.localization.LocalizationManager
import com.innovative.smis.util.localization.StringResources
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(navController: NavController, onMenuClick: (() -> Unit)? = null) {
    android.util.Log.d("DashboardScreen", "DashboardScreen created - onMenuClick is ${if (onMenuClick != null) "not null" else "null"}")

    val viewModel: DashboardViewModel = koinViewModel<DashboardViewModel>()

    // Log ViewModel creation success
    LaunchedEffect(Unit) {
        android.util.Log.d("DashboardScreen", "✅ DashboardViewModel created successfully")
    }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val currentLanguage = remember { LocalizationManager.getCurrentLanguage(context) }
    val languageCode = remember(currentLanguage) { LocalizationManager.getLanguageCode(currentLanguage) }

    // Debug: Log UI state changes
    LaunchedEffect(uiState) {
        android.util.Log.d("DashboardScreen", "📊 UI State Update: applicationLoadingState=${uiState.applicationLoadingState}, applications.size=${uiState.applications.size}, selectedStatus=${uiState.selectedStatus}, isRefreshing=${uiState.isRefreshing}")

        // Only trigger manual refresh once for empty Success state
        if (uiState.applicationLoadingState is com.innovative.smis.util.common.Resource.Success &&
            uiState.applications.isEmpty() &&
            uiState.selectedStatus == "All" &&
            !uiState.isRefreshing) {
            android.util.Log.w("DashboardScreen", "🔄 Manual data refresh - Success state with 0 items")
            viewModel.refreshApplications()
        }
    }

    var showLogoutSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    val logoutViewModel: LogoutViewModel = koinViewModel()
    val logoutState by logoutViewModel.logoutState.collectAsState()
    LaunchedEffect(logoutState.syncComplete) {
        if (logoutState.syncComplete && !logoutState.isLoading) {
            navController.navigate("login") { popUpTo(0) { inclusive = true } }
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = uiState.isRefreshing
//    // This effect triggers the refresh in the ViewModel when the user pulls
//    if (pullToRefreshState.isRefreshing) {
//        LaunchedEffect(true) {
//            viewModel.refreshApplications()
//        }
//    }
//
//    // This effect stops the UI indicator when the ViewModel is no longer refreshing
//    LaunchedEffect(uiState.isRefreshing) {
//        if (!uiState.isRefreshing) {
//            pullToRefreshState.endRefresh()
//        }
//    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding() // Fix status bar overlap
            .imePadding() // Handle keyboard padding
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus() // Dismiss keyboard on tap
                })
            },
        topBar = {
            android.util.Log.d("DashboardScreen", "Rendering TopBar with onMenuClick: ${if (onMenuClick != null) "not null" else "null"}")
            TopBar(
                navController = navController,
                viewModel = viewModel,
                scrollBehavior = scrollBehavior,
                onLogoutClick = { showLogoutSheet = true },
                onMenuClick = onMenuClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .pullToRefresh(
                    isRefreshing = isRefreshing,
                    state = pullToRefreshState,
                    onRefresh = viewModel::refreshApplications,
                    enabled = true
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {

                item {
                    android.util.Log.d("DashboardScreen", "🎯 Rendering QuickActionsSection")
                    QuickActionsSection(navController = navController)
                }
                item {
                    android.util.Log.d("DashboardScreen", "🎯 Rendering TaskFilters")
                    TaskFilters(
                        selectedStatus = uiState.selectedStatus,
                        onStatusSelected = viewModel::setStatusFilter
                    )
                }
                item {
                    android.util.Log.d("DashboardScreen", "🎯 Rendering Task List header")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(StringResources.getString(StringResources.TASK_LIST, languageCode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { navController.navigate(ScreenName.TodoList) }) {
                            Text(StringResources.getString(StringResources.VIEW_ALL, languageCode))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                when (val state = uiState.applicationLoadingState) {
                    is Resource.Loading -> {
                        android.util.Log.d("DashboardScreen", "🔄 Rendering Loading state - isRefreshing: $isRefreshing")
                        if (!isRefreshing) {
                            item {
                                android.util.Log.d("DashboardScreen", "📱 Adding LoadingState item to LazyColumn")
                                LoadingState()
                            }
                        }
                    }
                    is Resource.Success -> {
                        android.util.Log.d("DashboardScreen", "✅ Resource.Success - applications count: ${uiState.applications.size}")
                        if (uiState.applications.isEmpty()) {
                            android.util.Log.d("DashboardScreen", "🟦 Adding EmptyApplicationsState item to LazyColumn")
                            item { EmptyApplicationsState(uiState.selectedStatus) }
                        } else {
                            android.util.Log.d("DashboardScreen", "📋 Adding ${uiState.applications.size} application items to LazyColumn")
                            items(uiState.applications, key = { it.applicationId }) { todoItem ->
                                android.util.Log.d("DashboardScreen", "🎯 Rendering item for application ${todoItem.applicationId}")
                                DashboardTodoItemCard(
                                    todoItem = todoItem,
                                    context = context,
                                    navController = navController,
                                    onOpenFormClick = {
                                        // Navigate based on application status to form screens
                                        when (todoItem.status?.lowercase()) {
                                            "initiated" -> navController.navigate("emptying_scheduling_form/${todoItem.applicationId}")
                                            "scheduled" -> navController.navigate("site_preparation_form/${todoItem.applicationId}")
                                            "site-preparation" -> navController.navigate("emptying_service_form/${todoItem.applicationId}")
                                            else -> { /* Don't show form for other statuses */ }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        android.util.Log.d("DashboardScreen", "❌ Resource.Error - applications count: ${uiState.applications.size}")
                        if (uiState.applications.isEmpty()) {
                            android.util.Log.d("DashboardScreen", "❌ Adding ErrorState item to LazyColumn")
                            item { ErrorState(state.message, viewModel::refreshApplications) }
                        } else {
                            android.util.Log.d("DashboardScreen", "📋 Error state but showing cached ${uiState.applications.size} applications")
                            items(uiState.applications, key = { it.applicationId }) { todoItem ->
                                DashboardTodoItemCard(
                                    todoItem = todoItem,
                                    context = context,
                                    navController = navController,
                                    onOpenFormClick = {
                                        // Navigate based on application status to form screens
                                        when (todoItem.status?.lowercase()) {
                                            "initiated" -> navController.navigate("emptying_scheduling_form/${todoItem.applicationId}")
                                            "scheduled" -> navController.navigate("site_preparation_form/${todoItem.applicationId}")
                                            "site-preparation" -> navController.navigate("emptying_service_form/${todoItem.applicationId}")
                                            else -> { /* Don't show form for other statuses */ }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    is Resource.Idle -> {
                        item {
                            ErrorState(
                                message = "No data available. Reloading...",
                                onRetry = viewModel::refreshApplications
                            )
                        }
                    }
                }
            }
        }
        LogoutBottomSheet(
            isVisible = showLogoutSheet,
            pendingSyncCount = logoutState.pendingSyncCount,
            onConfirmLogout = { logoutViewModel.logoutWithoutSync(); showLogoutSheet = false },
            onCancel = { showLogoutSheet = false; logoutViewModel.resetState() },
            onSyncAndLogout = { logoutViewModel.syncAndLogout(); showLogoutSheet = false }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    navController: NavController,
    viewModel: DashboardViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    onLogoutClick: () -> Unit,
    onMenuClick: (() -> Unit)? = null
) {
    val preferenceHelper = remember { viewModel.getPreferenceHelper() }
    val context = LocalContext.current
    val languageCode = LocalizationManager.getCurrentLanguage(context)
    CenterAlignedTopAppBar(
        title = { Text(StringResources.getString(StringResources.DASHBOARD, languageCode), fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            android.util.Log.d("TopBar", "NavigationIcon rendering - onMenuClick is ${if (onMenuClick != null) "not null" else "null"}")
            onMenuClick?.let { menuClick ->
                android.util.Log.d("TopBar", "Rendering menu IconButton")
                IconButton(
                    onClick = {
                        android.util.Log.d("TopBar", "🔥 HAMBURGER MENU CLICKED! 🔥")
                        try {
                            menuClick()
                            android.util.Log.d("TopBar", "✅ Menu callback executed successfully")
                        } catch (e: Exception) {
                            android.util.Log.e("TopBar", "❌ Menu click error: ${e.message}", e)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } ?: run {
                android.util.Log.w("TopBar", "onMenuClick is null - no navigation icon rendered")
            }
        },
        actions = {
            // Emergency recovery button (only show if needed)
            if (android.util.Log.isLoggable("DEBUG", android.util.Log.DEBUG)) {
                IconButton(
                    onClick = {
                        android.util.Log.i("TopBar", "Emergency dashboard reload triggered")
                        try {
                            navController.navigate(ScreenName.Dashboard) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TopBar", "Emergency reload failed: ${e.message}")
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Dashboard",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        ),
        scrollBehavior = scrollBehavior
    )
}
@Composable
fun QuickActionsSection(navController: NavController) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val context = LocalContext.current
        val languageCode = LocalizationManager.getCurrentLanguage(context)
        Text(StringResources.getString(StringResources.QUICK_ACTIONS, languageCode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(title = "Emptying Scheduling", icon = Icons.Default.Schedule, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f), onClick = { navController.navigate("emptying_scheduling") })
            QuickActionCard(title = "Site Preparation", icon = Icons.Default.Construction, color = Color(0xFF7B1FA2), modifier = Modifier.weight(1f), onClick = { navController.navigate("site_preparation") })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(title = "Emptying Service", icon = Icons.Default.Build, color = Color(0xFF1976D2), modifier = Modifier.weight(1f), onClick = { navController.navigate("emptying_service") })
            QuickActionCard(title = "Task Management", icon = Icons.Default.ManageAccounts, color = Color(0xFFE53935), modifier = Modifier.weight(1f), onClick = { navController.navigate("task_management") })
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
            }
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
        }
    }
}
@Composable
private fun DashboardTodoItemCard(todoItem: TodoItem, context: Context, navController: NavController, onOpenFormClick: () -> Unit) {
    android.util.Log.d("DashboardTodoItemCard", "🎨 Rendering card for application ID: ${todoItem.applicationId}, status: ${todoItem.status}")
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")
    val statusColor = when (todoItem.status?.lowercase()) {
        "initiated" -> Color(0xFF6C757D)
        "scheduled" -> MaterialTheme.colorScheme.primary
        "rescheduled" -> Color(0xFFFF9800)
        "site-preparation" -> Color(0xFF17A2B8)
        "emptied", "completed" -> Color(0xFF28A745)
        "pending" -> Color(0xFFFFC107)
        "cancelled" -> MaterialTheme.colorScheme.error
        "reassigned" -> Color(0xFF6F42C1)
        else -> MaterialTheme.colorScheme.outline
    }
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val isToday = todoItem.proposedEmptyingDate == todayStr
    val cardBorder = if (isToday) BorderStroke(2.dp, Color(0xFFFF9800)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier.clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Application ID #${todoItem.applicationId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(todoItem.applicantName ?: "Name not provided", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                LocalStatusBadge(text = todoItem.status ?: "", color = statusColor)
                Icon(Icons.Default.ExpandMore, if (expanded) "Collapse" else "Expand", modifier = Modifier.rotate(rotationAngle))
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoRow(Icons.Outlined.DateRange, "Proposed Date", todoItem.proposedEmptyingDate ?: "Not scheduled")
                    todoItem.applicationDatetime?.let { InfoRow(Icons.Default.CalendarToday, "Applied On", it) }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${todoItem.applicantContact}"))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Outlined.Call, "Call Applicant", tint = MaterialTheme.colorScheme.primary)
                    }
//                    IconButton(onClick = { /* TODO: Open map with location */ }) {
//                        Icon(Icons.Outlined.LocationOn, "View on Map", tint = MaterialTheme.colorScheme.primary)
//                    }
                }
                // Only show form button for specific statuses
                val shouldShowFormButton = when (todoItem.status?.lowercase()) {
                    "initiated", "scheduled", "site-preparation" -> true
                    else -> false
                }
                if (shouldShowFormButton) {
                    FilledIconButton(onClick = onOpenFormClick) {
                        Icon(Icons.Outlined.EditNote, "Open Form")
                    }
                }
            }
        }
    }
}
@Composable
fun LocalStatusBadge(text: String, color: Color) {
    android.util.Log.d("StatusBadge", "🎨 Rendering badge: '$text' with color: $color")
    Text(
        text = text.uppercase(),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
@Composable
fun InfoRow(icon: ImageVector, label: String, text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFilters(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit
) {
    val statusFilters = listOf("All", "Today", "Initiated", "Scheduled", "Rescheduled", "Site-Preparation", "Emptied", "Completed", "Pending", "Cancelled", "Reassigned")

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(statusFilters) { status ->
            FilterChip(
                selected = status == selectedStatus,
                onClick = { onStatusSelected(status) },
                label = { Text(status) },
                leadingIcon = if (status == selectedStatus) { { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) } } else null
            )
        }
    }
}
@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Loading tasks...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable
fun EmptyApplicationsState(filter: String) {
    android.util.Log.d("DashboardScreen", "🟦 EmptyApplicationsState is being rendered for filter: $filter")
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(32.dp)
        ) {
            Icon(
                Icons.Filled.AssignmentTurnedIn,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Text(
                "All Clear!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "No applications match the filter \"$filter\". You're all caught up!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
fun ErrorState(message: String?, onRetry: () -> Unit) {
    android.util.Log.d("DashboardScreen", "🔴 ErrorState is being rendered with message: $message")
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.CloudOff,
                null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Text(
                "Failed to Load Tasks",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
            Text(
                message ?: "Please check your connection and try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}
@Composable
fun IdleState() {
    android.util.Log.d("DashboardScreen", "🔧 IdleState composable is being rendered!")
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Dashboard Ready",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Pull to refresh or use the menu to load tasks.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
