package com.innovative.smis.ui.features.emptyingservice

import com.innovative.smis.R

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.innovative.smis.data.model.response.TodoItem
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PhoneNumberFormatter
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyingServiceScreen(navController: NavController, onMenuClick: (() -> Unit)? = null) {
    val viewModel: EmptyingServiceViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    // Observe snackbar messages from other screens
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    
    val snackbarMessage by savedStateHandle?.getStateFlow<String?>("snackbar_message", null)
        ?.collectAsStateWithLifecycle() ?: mutableStateOf(null)

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            savedStateHandle?.set("snackbar_message", null)
        }
    }

    // Observe refresh trigger from other screens
    val shouldRefresh by savedStateHandle?.getStateFlow("should_refresh_list", false)
        ?.collectAsStateWithLifecycle() ?: mutableStateOf(false)

    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            viewModel.refreshList()
            savedStateHandle?.set("should_refresh_list", false)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showDatePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = uiState.startDate,
            initialSelectedEndDateMillis = uiState.endDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    viewModel.setDateFilter(dateRangePickerState.selectedStartDateMillis, dateRangePickerState.selectedEndDateMillis)
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) {
            DateRangePicker(state = dateRangePickerState, modifier = Modifier.weight(1f))
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = uiState.isRefreshing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.screen_emptying_service), 
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshList() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = pullToRefreshState,
            onRefresh = viewModel::refreshList,
            modifier = Modifier.padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                // TaskFilters hidden as per user request - default filter: "Emptied"
                item { Spacer(Modifier.height(8.dp)) }
                when (uiState.listState) {
                    is Resource.Loading -> if (!isRefreshing) item { LoadingState() }
                    is Resource.Success, is Resource.Error -> {
                        if (uiState.applications.isEmpty()) {
                            item { EmptyState(uiState.selectedStatus) }
                        } else {
                            items(uiState.applications, key = { it.applicationId }) { todoItem ->
                                ApplicationTaskCard(
                                    todoItem = todoItem,
                                    context = context,
                                    onOpenFormClick = {
                                        navController.navigate("emptying_service_form/${todoItem.applicationId}")
                                    }
                                )
                            }
                        }
                    }
                    is Resource.Idle -> item { IdleState() }
                }
            }
        }
    }
}

@Composable
private fun ApplicationTaskCard(
    todoItem: TodoItem, 
    context: Context,
    onOpenFormClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")
    val statusText = todoItem.status ?: "Unknown"
    val statusColor = when (statusText.lowercase()) {
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
        modifier = Modifier.fillMaxWidth(),
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
                    Text(stringResource(R.string.label_application_id_hash, todoItem.applicationId), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = todoItem.applicantName ?: todoItem.ownerName ?: stringResource(R.string.message_name_not_provided),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusBadge(text = statusText, color = statusColor)
                Icon(Icons.Default.ExpandMore, if (expanded) "Collapse" else "Expand", modifier = Modifier.rotate(rotationAngle))
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow(
                        icon = Icons.Outlined.DateRange,
                        label = stringResource(R.string.label_proposed_date),
                        text = todoItem.proposedEmptyingDate ?: "Not scheduled"
                    )
                    todoItem.applicationDatetime?.let { date ->
                        InfoRow(
                            icon = Icons.Default.CalendarToday,
                            label = stringResource(R.string.label_applied_on),
                            text = date
                        )
                    }
                    val contact = todoItem.applicantContact ?: todoItem.phoneNo
                            contact?.let {
                        InfoRow(
                            icon = Icons.Outlined.Phone,
                            label = "Contact",
                            text = contact
                        )
                    }
                }
            }
            HorizontalDivider()
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
                                android.util.Log.d("EmptyingServiceDial", "Raw: '$rawPhone' -> Formatted: '$formattedPhone'")
                                if (formattedPhone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$formattedPhone"))
                                    context.startActivity(intent)
                                } else {
                                    android.util.Log.e("EmptyingServiceDial", "Formatted phone is blank!")
                                }
                            } ?: run {
                                android.util.Log.e("EmptyingServiceDial", "No phone number available - applicantContact: '${todoItem.applicantContact}', phoneNo: '${todoItem.phoneNo}'")
                            }
                        },
                        enabled = !todoItem.applicantContact.isNullOrBlank() || !todoItem.phoneNo.isNullOrBlank()
                    ) { 
                        Icon(
                            Icons.Outlined.Call, 
                            "Call Applicant", 
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
                            "Get Directions", 
                            tint = if (todoItem.buildingPointGeomExist == true && 
                                      !todoItem.latitude.isNullOrBlank() && 
                                      !todoItem.longitude.isNullOrBlank())
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ) 
                    }
                }
                FilledIconButton(onClick = onOpenFormClick) { Icon(Icons.Outlined.EditNote, "Open Form") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFilters(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    dateFilterText: String,
    isDateFilterApplied: Boolean,
    onDateFilterClick: () -> Unit,
    onClearDateFilterClick: () -> Unit
) {
    val statusFilters = listOf("All", "Today", "Scheduled", "Pending", "Completed", "Cancelled")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        OutlinedButton(
            onClick = onDateFilterClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Outlined.EditCalendar, "Filter by Date", modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = dateFilterText, modifier = Modifier.weight(1f))
            if (isDateFilterApplied) {
                IconButton(onClick = onClearDateFilterClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Clear date filter")
                }
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
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
fun IdleState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.message_pull_to_refresh), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyState(filter: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Filled.Assignment, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Text(stringResource(R.string.message_no_applications_found), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(R.string.message_no_applications_filter_criteria),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
