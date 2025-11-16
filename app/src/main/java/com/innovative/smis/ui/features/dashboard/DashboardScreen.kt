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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.innovative.smis.R
import com.innovative.smis.data.model.response.TodoItem
import com.innovative.smis.ui.features.logout.LogoutBottomSheet
import com.innovative.smis.ui.features.dashboard.LogoutViewModel
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PhoneNumberFormatter
import com.innovative.smis.util.constants.ScreenName
import com.innovative.smis.util.helper.DateFormatManager
import com.innovative.smis.util.localization.LocalizationManager
import com.innovative.smis.util.localization.StringResources
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    onMenuClick: (() -> Unit)? = null
) {
    android.util.Log.d("DashboardScreen", "DashboardScreen created - onMenuClick is ${if (onMenuClick != null) "not null" else "null"}")

    val viewModel: DashboardViewModel = koinViewModel<DashboardViewModel>()
    val scope = rememberCoroutineScope()

    // ✅ IMPORTANT FIX KEPT: LazyRow inside LazyColumn anti-pattern has been fixed (Row + horizontalScroll)

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
            // ✅ FIX: Removed isSettled delay - no longer needed after fixing nested lazy layout
            // TaskFilters now uses Row + horizontalScroll instead of LazyRow inside LazyColumn
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
                    android.util.Log.d("DashboardScreen", "🎯 Rendering Applications header")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.nav_applications),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                item {
                    android.util.Log.d("DashboardScreen", "🎯 Rendering TaskFilters")
                    TaskFilters(
                        selectedStatus = uiState.selectedStatus,
                        onStatusSelected = viewModel::setStatusFilter
                    )
                }
                when (val state = uiState.applicationLoadingState) {
                    is Resource.Loading -> {
                        android.util.Log.d("DashboardScreen", "🔄 Rendering Loading state - isRefreshing: $isRefreshing")
                        // Always show loading indicator to prevent black screen during rapid navigation
                        item {
                            android.util.Log.d("DashboardScreen", "📱 Adding LoadingState item to LazyColumn")
                            LoadingState()
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
                                
                                // ✅ OPTIMIZATION: Remember the click handler to prevent recreation on every recomposition
                                val onOpenFormClick = remember(todoItem.applicationId, todoItem.status) {
                                    {
                                        val route = when (todoItem.status?.lowercase()) {
                                            "initiated" -> "emptying_scheduling_form/${todoItem.applicationId}"
                                            "scheduled" -> "site_preparation_form/${todoItem.applicationId}"
                                            "site-preparation" -> "emptying_service_form/${todoItem.applicationId}"
                                            else -> null
                                        }
                                        route?.let {
                                            navController.navigate(it)
                                        }
                                        Unit
                                    }
                                }
                                
                                DashboardTodoItemCard(
                                    todoItem = todoItem,
                                    context = context,
                                    navController = navController,
                                    onOpenFormClick = onOpenFormClick
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
                                
                                // ✅ OPTIMIZATION: Remember the click handler to prevent recreation on every recomposition
                                val onOpenFormClick = remember(todoItem.applicationId, todoItem.status) {
                                    {
                                        val route = when (todoItem.status?.lowercase()) {
                                            "initiated" -> "emptying_scheduling_form/${todoItem.applicationId}"
                                            "scheduled" -> "site_preparation_form/${todoItem.applicationId}"
                                            "site-preparation" -> "emptying_service_form/${todoItem.applicationId}"
                                            else -> null
                                        }
                                        route?.let {
                                            navController.navigate(it)
                                        }
                                        Unit
                                    }
                                }
                                
                                DashboardTodoItemCard(
                                    todoItem = todoItem,
                                    context = context,
                                    navController = navController,
                                    onOpenFormClick = onOpenFormClick
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
            } // End of LazyColumn
        } // End of Box
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
        title = { Text(stringResource(R.string.nav_dashboard), fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            // Hamburger menu icon
            android.util.Log.d("TopBar", "NavigationIcon rendering - onMenuClick is ${if (onMenuClick != null) "visible" else "hidden"}")
            onMenuClick?.let { menuClick ->
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
            }
        },
        actions = {
            // Refresh button - always visible
            IconButton(
                onClick = {
                    android.util.Log.i("TopBar", "Dashboard refresh triggered")
                    viewModel.refreshApplications()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurface
                )
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
fun QuickActionsSection(
    navController: NavController
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val context = LocalContext.current
        val languageCode = LocalizationManager.getCurrentLanguage(context)
        Text(stringResource(R.string.section_quick_actions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = stringResource(R.string.card_emptying_scheduling), 
                icon = com.innovative.smis.util.constants.NavigationIcons.EmptyingScheduling, 
                color = MaterialTheme.colorScheme.primary,
                stepNumber = "Step 1",
                modifier = Modifier.weight(1f), 
                onClick = { navController.navigate("emptying_scheduling") }
            )
            QuickActionCard(
                title = stringResource(R.string.card_site_preparation), 
                icon = com.innovative.smis.util.constants.NavigationIcons.SitePreparation, 
                color = Color(0xFF7B1FA2),
                stepNumber = "Step 2",
                modifier = Modifier.weight(1f), 
                onClick = { navController.navigate("site_preparation") }
            )
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = stringResource(R.string.card_emptying_service), 
                icon = com.innovative.smis.util.constants.NavigationIcons.EmptyingService, 
                color = Color(0xFF1976D2),
                stepNumber = "Step 3",
                modifier = Modifier.weight(1f), 
                onClick = { navController.navigate("emptying_service") }
            )
            QuickActionCard(
                title = stringResource(R.string.card_additional_trips), 
                icon = com.innovative.smis.util.constants.NavigationIcons.AdditionalRepairing, 
                color = Color(0xFFFF6F00),
                stepNumber = "Step 4",
                modifier = Modifier.weight(1f), 
                onClick = { navController.navigate("additional_repairing") }
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    title: String, 
    icon: ImageVector, 
    color: Color, 
    stepNumber: String? = null,
    modifier: Modifier = Modifier, 
    onClick: () -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                if (stepNumber != null) {
                    Text(
                        text = stepNumber,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = title, 
                    style = MaterialTheme.typography.bodyMedium, 
                    fontWeight = FontWeight.SemiBold, 
                    lineHeight = 18.sp
                )
            }
        }
    }
}
@Composable
private fun DashboardTodoItemCard(todoItem: TodoItem, context: Context, navController: NavController, onOpenFormClick: () -> Unit) {
    android.util.Log.d("DashboardTodoItemCard", "🎨 Rendering card for application ID: ${todoItem.applicationId}, status: ${todoItem.status}")
    val languageCode = LocalizationManager.getCurrentLanguage(context)
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
    val todayStr = DateFormatManager.getTodayInApiFormat()
    val isToday = todoItem.proposedEmptyingDate == todayStr
    val isUrgent = todoItem.urgency?.equals("yes", ignoreCase = true) == true
    
    // Priority: Urgent (red) > Today (orange) > Normal (gray)
    val cardBorder = when {
        isUrgent -> BorderStroke(3.dp, Color(0xFFDC3545)) // Red border for urgent
        isToday -> BorderStroke(2.dp, Color(0xFFFF9800))  // Orange border for today
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) // Normal border
    }
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
            Column(modifier = Modifier.clickable { expanded = !expanded }.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.label_application_id_hash, todoItem.applicationId), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = todoItem.applicantName ?: todoItem.ownerName ?: stringResource(R.string.message_name_not_provided),
                            style = MaterialTheme.typography.bodyMedium, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    LocalStatusBadge(text = todoItem.status ?: "", color = statusColor)
                    Icon(Icons.Default.ExpandMore, if (expanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand), modifier = Modifier.rotate(rotationAngle))
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoRow(Icons.Outlined.DateRange, stringResource(R.string.label_proposed_date), todoItem.proposedEmptyingDate ?: stringResource(R.string.message_not_scheduled))
                    todoItem.applicationDatetime?.let { InfoRow(Icons.Default.CalendarToday, stringResource(R.string.label_applied_on), it) }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            val contact = todoItem.applicantContact ?: todoItem.phoneNo
                            contact?.let { rawPhone ->
                                val formattedPhone = PhoneNumberFormatter.formatForDialing(rawPhone)
                                android.util.Log.d("DashboardDial", "Raw: '$rawPhone' -> Formatted: '$formattedPhone'")
                                if (formattedPhone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$formattedPhone"))
                                    context.startActivity(intent)
                                } else {
                                    android.util.Log.e("DashboardDial", "Formatted phone is blank!")
                                }
                            } ?: run {
                                android.util.Log.e("DashboardDial", "No phone number available - applicantContact: '${todoItem.applicantContact}', phoneNo: '${todoItem.phoneNo}'")
                            }
                        },
                        enabled = !todoItem.applicantContact.isNullOrBlank() || !todoItem.phoneNo.isNullOrBlank()
                    ) {
                        Icon(
                            Icons.Outlined.Call, 
                            stringResource(R.string.cd_call_applicant), 
                            tint = if (todoItem.applicantContact.isNullOrBlank() && todoItem.phoneNo.isNullOrBlank()) 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            val lat = todoItem.latitude
                            val lon = todoItem.longitude
                            if (!lat.isNullOrBlank() && !lon.isNullOrBlank()) {
                                val mapUri = Uri.parse("http://maps.google.com/maps?daddr=$lat,$lon")
                                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    android.widget.Toast.makeText(context, "No map application found", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = todoItem.buildingPointGeomExist == true && 
                                  !todoItem.latitude.isNullOrBlank() && 
                                  !todoItem.longitude.isNullOrBlank()
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn, 
                            stringResource(R.string.cd_view_on_map), 
                            tint = if (todoItem.buildingPointGeomExist == true && 
                                      !todoItem.latitude.isNullOrBlank() && 
                                      !todoItem.longitude.isNullOrBlank())
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun LocalStatusBadge(text: String, color: Color) {
    android.util.Log.d("StatusBadge", "🎨 Rendering badge: '$text' with color: $color")
    
    // Translate status to localized string
    val localizedStatus = when (text.lowercase()) {
        "initiated" -> stringResource(R.string.filter_initiated)
        "scheduled" -> stringResource(R.string.filter_scheduled)
        "rescheduled" -> stringResource(R.string.filter_rescheduled)
        "site-preparation" -> stringResource(R.string.filter_site_visited)
        "emptied" -> stringResource(R.string.filter_emptied)
        "completed" -> stringResource(R.string.filter_completed)
        "cancelled" -> stringResource(R.string.filter_cancelled)
        "reassigned" -> stringResource(R.string.filter_reassigned)
        else -> text
    }
    
    Text(
        text = localizedStatus.uppercase(),
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
    // Map of status keys to their string resource IDs
    val statusFiltersMap = linkedMapOf(
        "All" to R.string.filter_all,
        "Today" to R.string.filter_today,
        "Urgent" to R.string.filter_urgent,
        "Initiated" to R.string.filter_initiated,
        "Scheduled" to R.string.filter_scheduled,
        "Rescheduled" to R.string.filter_rescheduled,
        "Site-Preparation" to R.string.filter_site_preparation,
        "Emptied" to R.string.filter_emptied,
        "Completed" to R.string.filter_completed
    )

    // ✅ FIX: Changed from LazyRow to Row with horizontalScroll()
    // This eliminates the nested lazy layout anti-pattern (LazyRow inside LazyColumn)
    // which was causing main thread blocking and black screen during rapid clicks
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(Modifier.width(16.dp)) // Start padding
        
        statusFiltersMap.forEach { (statusKey, stringResId) ->
            FilterChip(
                selected = statusKey == selectedStatus,
                onClick = { onStatusSelected(statusKey) },
                label = { Text(stringResource(stringResId)) },
                leadingIcon = if (statusKey == selectedStatus) { { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) } } else null
            )
        }
        
        Spacer(Modifier.width(16.dp)) // End padding
    }
}
@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text(stringResource(R.string.message_loading_tasks), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                stringResource(R.string.message_all_clear),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                stringResource(R.string.message_no_applications_filter_criteria),
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
                Text(stringResource(R.string.action_retry))
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
