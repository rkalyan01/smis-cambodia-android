package com.innovative.smis.ui.features.taskmanagement

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
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagementScreen(navController: NavController, onMenuClick: (() -> Unit)? = null) {
    val viewModel: TaskManagementViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        // Load tasks with the default selected status (first status)
        viewModel.setStatusFilter(uiState.selectedStatus)
        val message = navController.currentBackStackEntry
            ?.savedStateHandle
            ?.get<String>("snackbar_message")

        if (message != null) {
            snackbarHostState.showSnackbar(message)
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set("snackbar_message", null)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = uiState.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_task_management), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.setStatusFilter(uiState.selectedStatus)
                    }) {
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
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .pullToRefresh(
                    isRefreshing = isRefreshing,
                    state = pullToRefreshState,
                    onRefresh = { 
                        viewModel.setStatusFilter(uiState.selectedStatus)
                    },
                    enabled = true
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                // Status Filter Row
                item { 
                    TaskStatusFilter(
                        availableStatuses = uiState.availableStatuses,
                        selectedStatus = uiState.selectedStatus,
                        onStatusSelected = viewModel::setStatusFilter
                    )
                }
                
                item { Spacer(Modifier.height(8.dp)) }
                

                
                when {
                    uiState.isLoading && uiState.tasks.isEmpty() -> {
                        item { LoadingState() }
                    }
                    uiState.tasks.isEmpty() -> {
                        item { EmptyState(uiState.selectedStatus) }
                    }
                    else -> {
                        items(uiState.tasks, key = { it.applicationId }) { todoItem ->
                            ApplicationTaskCard(
                                todoItem = todoItem,
                                context = context
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskStatusFilter(
    availableStatuses: List<String>,
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
    
    Column {
        Text(
            text = stringResource(R.string.label_filter_by_status),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(availableStatuses) { status ->
                val stringResId = statusFiltersMap[status]
                if (stringResId != null) {
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { onStatusSelected(status) },
                        label = { Text(stringResource(stringResId)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicationTaskCard(
    todoItem: TodoItem, 
    context: Context
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")
    val statusText = todoItem.status ?: "Unknown"
    val statusColor = when (statusText.lowercase()) {
        "pending" -> Color(0xFFFFC107)
        "cancelled" -> MaterialTheme.colorScheme.error
        "reassigned" -> Color(0xFF6F42C1)
        "rescheduled" -> Color(0xFFFF9800)
        "emptied", "completed" -> Color(0xFF28A745)
        else -> MaterialTheme.colorScheme.outline
    }
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val isToday = todoItem.proposedEmptyingDate == todayStr
    val isUrgent = todoItem.urgency?.equals("yes", ignoreCase = true) == true
    
    // Priority: Urgent (red) > Today (orange) > Normal (gray)
    val cardBorder = when {
        isUrgent -> BorderStroke(3.dp, Color(0xFFDC3545)) // Red border for urgent
        isToday -> BorderStroke(2.dp, Color(0xFFFF9800))  // Orange border for today
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) // Normal border
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Column(modifier = Modifier.clickable { expanded = !expanded }.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Application ID #${todoItem.applicationId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = todoItem.applicantName ?: todoItem.ownerName ?: "Name not provided",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(text = statusText, color = statusColor)
                    Icon(Icons.Default.ExpandMore, if (expanded) "Collapse" else "Expand", modifier = Modifier.rotate(rotationAngle))
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow(
                        icon = Icons.Outlined.DateRange,
                        label = "Proposed Date",
                        text = todoItem.proposedEmptyingDate ?: "Not scheduled"
                    )
                    todoItem.applicationDatetime?.let { date ->
                        InfoRow(
                            icon = Icons.Default.CalendarToday,
                            label = "Applied On",
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
                                android.util.Log.d("TaskManagementDial", "Raw: '$rawPhone' -> Formatted: '$formattedPhone'")
                                if (formattedPhone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$formattedPhone"))
                                    context.startActivity(intent)
                                } else {
                                    android.util.Log.e("TaskManagementDial", "Formatted phone is blank!")
                                }
                            } ?: run {
                                android.util.Log.e("TaskManagementDial", "No phone number available - applicantContact: '${todoItem.applicantContact}', phoneNo: '${todoItem.phoneNo}'")
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
                }
                // No form button for Task Management - just view details
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
                text = "There are no applications matching the current filter criteria.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
