package com.innovative.smis.ui.features.todolist

import com.innovative.smis.R

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.innovative.smis.data.model.response.TodoItem
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PhoneNumberFormatter
import com.innovative.smis.util.helper.DateFormatManager
import com.innovative.smis.util.localization.LocalizationManager
import com.innovative.smis.util.localization.StringResources
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(navController: NavController, onMenuClick: (() -> Unit)? = null) {
    val viewModel: TodoListViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val currentLanguage = remember { LocalizationManager.getCurrentLanguage(context) }
    val languageCode = remember(currentLanguage) { LocalizationManager.getLanguageCode(currentLanguage) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Handle refresh signals from form submissions
    LaunchedEffect(Unit) {
        val message = navController.currentBackStackEntry
            ?.savedStateHandle
            ?.get<String>("snackbar_message")

        val shouldRefresh = navController.currentBackStackEntry
            ?.savedStateHandle
            ?.get<Boolean>("should_refresh_list")

        if (message != null) {
            snackbarHostState.showSnackbar(message)
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set("snackbar_message", null)
        }

        if (shouldRefresh == true) {
            viewModel.refreshList()
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set("should_refresh_list", null)
        }
    }

    // Refresh when returning to this screen from navigation
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            if (backStackEntry.destination.route?.contains("todo_list") == true) {
                viewModel.refreshList()
            }
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
                title = { Text(stringResource(R.string.nav_todo_list), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshList() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
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
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .pullToRefresh(
                    isRefreshing = isRefreshing,
                    state = pullToRefreshState,
                    onRefresh = viewModel::refreshList,
                    enabled = true
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                item {
                    TaskFilters(
                        selectedStatus = uiState.selectedStatus,
                        onStatusSelected = viewModel::setStatusFilter,
                        dateFilterText = uiState.dateRangeText,
                        isDateFilterApplied = uiState.startDate != null,
                        onDateFilterClick = { showDatePicker = true },
                        onClearDateFilterClick = viewModel::clearDateFilter
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }

                when (val state = uiState.listState) {
                    is Resource.Loading -> {
                        if (!isRefreshing) {
                            item { LoadingState() }
                        }
                    }
                    is Resource.Success, is Resource.Error -> {
                        if (uiState.todoItems.isEmpty()) {
                            item { EmptyState(uiState.selectedStatus) }
                        } else {
                            items(uiState.todoItems, key = { it.applicationId }) { todoItem ->
                                ApplicationTaskCard(
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
                        item { IdleState() }
                    }
                }
            }


        }
    }
}

@Composable
private fun ApplicationTaskCard(todoItem: TodoItem, context: Context, navController: NavController, onOpenFormClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")

    val statusText = todoItem.status ?: stringResource(R.string.status_unknown)

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
    val todayStr = DateFormatManager.getTodayInApiFormat()
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
                Icon(Icons.Default.ExpandMore, if (expanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand), modifier = Modifier.rotate(rotationAngle))
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), // Added bottom padding
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow(
                        icon = Icons.Outlined.DateRange,
                        label = stringResource(R.string.label_proposed_date),
                        text = todoItem.proposedEmptyingDate ?: stringResource(R.string.message_not_scheduled)
                    )
                    todoItem.applicationDatetime?.let { date ->
                        InfoRow(
                            icon = Icons.Default.CalendarToday,
                            label = stringResource(R.string.label_applied_on),
                            text = date
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
                                android.util.Log.d("TodoListDial", "Raw: '$rawPhone' -> Formatted: '$formattedPhone'")
                                if (formattedPhone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$formattedPhone"))
                                    context.startActivity(intent)
                                } else {
                                    android.util.Log.e("TodoListDial", "Formatted phone is blank!")
                                }
                            } ?: run {
                                android.util.Log.e("TodoListDial", "No phone number available - applicantContact: '${todoItem.applicantContact}', phoneNo: '${todoItem.phoneNo}'")
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
                        Icon(Icons.Outlined.EditNote, stringResource(R.string.cd_open_form))
                    }
                }
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
    val statusFilters = listOf(
        stringResource(R.string.filter_all), 
        stringResource(R.string.filter_today), 
        stringResource(R.string.filter_initiated),
        stringResource(R.string.filter_scheduled),
        stringResource(R.string.filter_rescheduled),
        stringResource(R.string.filter_site_preparation),
        stringResource(R.string.filter_emptied),
        stringResource(R.string.filter_completed),
        stringResource(R.string.filter_cancelled),
        stringResource(R.string.filter_reassigned)
    )
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
            Icon(Icons.Outlined.EditCalendar, stringResource(R.string.action_filter_by_date), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = dateFilterText, modifier = Modifier.weight(1f))
            if (isDateFilterApplied) {
                IconButton(onClick = onClearDateFilterClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, stringResource(R.string.cd_clear_date_filter))
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
        Text(stringResource(R.string.message_pull_to_refresh_tasks), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
