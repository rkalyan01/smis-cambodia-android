package com.innovative.smis.ui.features.additionalrepairing

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.innovative.smis.R
import com.innovative.smis.data.model.request.TripEntryUiState
import com.innovative.smis.ui.components.ImagePickerComponent
import com.innovative.smis.ui.components.RadioButtonGroupField
import com.innovative.smis.ui.components.ReadOnlyTextField
import com.innovative.smis.ui.components.SectionHeader
import com.innovative.smis.util.common.Resource
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdditionalRepairingFormScreen(
    navController: NavController,
    emptyingId: Int
) {
    val viewModel: AdditionalRepairingFormViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val draftState by viewModel.draftState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    // Expandable section states
    var applicationDetailsExpanded by remember { mutableStateOf(true) }
    var tripDetailsExpanded by remember { mutableStateOf(true) }
    var paymentDetailsExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(emptyingId) {
        viewModel.loadEmptyingDetails(emptyingId)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(saveState) {
        when (saveState) {
            is Resource.Success -> {
                // CRITICAL: Check if we're on a valid destination before popping
                val currentRoute = navController.currentDestination?.route
                android.util.Log.d("NavigationGuard", "AdditionalRepairing save success - current route: $currentRoute")
                
                if (currentRoute != null && navController.previousBackStackEntry != null) {
                    snackbarHostState.showSnackbar("Form submitted successfully!")
                    viewModel.clearSaveState()
                    android.util.Log.d("NavigationGuard", "AdditionalRepairing executing popBackStack")
                    navController.popBackStack()
                } else {
                    android.util.Log.d("NavigationGuard", "AdditionalRepairing popBackStack skipped - invalid state")
                    viewModel.clearSaveState()
                }
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(saveState.message ?: "Failed to submit")
                viewModel.clearSaveState()
            }
            else -> {}
        }
    }
    
    LaunchedEffect(draftState) {
        when (draftState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Draft saved successfully!")
                viewModel.clearDraftState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(draftState.message ?: "Failed to save draft")
                viewModel.clearDraftState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_additional_trips), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Application Details Section
            item {
                ExpandableSection(
                    title = stringResource(R.string.section_application_details),
                    isExpanded = applicationDetailsExpanded,
                    onExpandedChange = { applicationDetailsExpanded = it }
                ) {
                    ReadOnlyTextField(
                        value = uiState.applicationId?.toString() ?: "",
                        label = stringResource(R.string.label_application_id),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Trip Entries Section
            item {
                ExpandableSection(
                    title = stringResource(R.string.section_trip_entries),
                    isExpanded = tripDetailsExpanded,
                    onExpandedChange = { tripDetailsExpanded = it }
                ) {
                    uiState.tripEntries.forEachIndexed { index, trip ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        TripEntryContent(
                            trip = trip,
                            tripIndex = index,
                            onStartTimeChange = { viewModel.onTripStartTimeChange(index, it) },
                            onEndTimeChange = { viewModel.onTripEndTimeChange(index, it) },
                            onAdditionalRequiredChange = { viewModel.onTripAdditionalRequiredChange(index, it) }
                        )
                    }
                }
            }

            // Payment Details Section
            if (uiState.showPaymentSection) {
                item {
                    ExpandableSection(
                        title = stringResource(R.string.section_payment_details),
                        isExpanded = paymentDetailsExpanded,
                        onExpandedChange = { paymentDetailsExpanded = it }
                    ) {
                        ReadOnlyTextField(
                            value = uiState.amountOfRegularPayment,
                            label = stringResource(R.string.label_total_amount_regular_payment),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.amountOfExtraPayment,
                            onValueChange = viewModel::onExtraPaymentChange,
                            label = { Text(stringResource(R.string.label_total_extra_payment)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.receiptNumber,
                            onValueChange = viewModel::onReceiptNumberChange,
                            label = { Text(stringResource(R.string.label_receipt_number)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        ImagePickerComponent(
                            label = stringResource(R.string.label_receipt_image),
                            selectedImageUri = uiState.receiptImageUri,
                            onImageSelected = viewModel::onReceiptImageSelected
                        )

                        OutlinedTextField(
                            value = uiState.comments,
                            onValueChange = viewModel::onCommentsChange,
                            label = { Text(stringResource(R.string.label_comments)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }
            }
            
            // ✅ Two-Step Progressive Buttons
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                val lastTrip = uiState.tripEntries.lastOrNull()
                val isAdditionalTripNo = lastTrip?.additionalTripRequired?.equals("no", ignoreCase = true) == true
                val isAdditionalTripYes = lastTrip?.additionalTripRequired?.equals("yes", ignoreCase = true) == true
                val hasAdditionalTripSelection = isAdditionalTripNo || isAdditionalTripYes
                
                when (uiState.formStep) {
                    // Initial state - Show "Next" if "No" selected, or "Submit" if "Yes" selected
                    is FormStep.TripDetailsReady -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.saveDraft() },
                                modifier = Modifier.weight(1f),
                                enabled = draftState !is Resource.Loading,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (draftState is Resource.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(stringResource(R.string.button_save_draft))
                            }
                            
                            Button(
                                onClick = {
                                    if (isAdditionalTripNo) {
                                        // "No" selected → Go to payment section
                                        viewModel.submitTripAndProceedToPayment()
                                    } else if (isAdditionalTripYes) {
                                        // "Yes" selected → Submit trip only and finish
                                        viewModel.submitForm()
                                    }
                                    // ✅ No action if no selection (button is disabled)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = hasAdditionalTripSelection && draftState !is Resource.Loading, // ✅ Disabled until Yes/No selected
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAdditionalTripNo) Icons.Default.ArrowForward else Icons.Default.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isAdditionalTripNo) stringResource(R.string.button_next) else stringResource(R.string.button_submit))
                            }
                        }
                    }
                    
                    // Submitting trip to API
                    is FormStep.TripSubmitting, is FormStep.TripSubmitted -> {
                        Button(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.formStep is FormStep.TripSubmitting) stringResource(R.string.message_submitting_trip) else stringResource(R.string.message_loading_payment_details))
                        }
                    }
                    
                    // Payment section ready - Show payment submit button
                    is FormStep.PaymentReady -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.saveDraft() },
                                modifier = Modifier.weight(1f),
                                enabled = draftState !is Resource.Loading,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (draftState is Resource.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(stringResource(R.string.button_draft))
                            }
                            
                            Button(
                                onClick = { viewModel.submitPaymentDetails() },
                                modifier = Modifier.weight(1f),
                                enabled = draftState !is Resource.Loading,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payment,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.button_submit))
                            }
                        }
                    }
                    
                    // Submitting payment to API
                    is FormStep.SubmittingPayment -> {
                        Button(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.message_submitting_payment))
                        }
                    }
                    
                    // Completed - Show success (will auto-navigate)
                    is FormStep.Completed -> {
                        Button(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.message_completed))
                        }
                    }
                }
            }
            
        }
    }
}

@Composable
private fun TripEntryContent(
    trip: TripEntryUiState,
    tripIndex: Int,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onAdditionalRequiredChange: (String) -> Unit
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeFormat12Hour = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val calendar = remember { Calendar.getInstance() }

    // Convert 24-hour format to 12-hour format for display
    fun convert24HourTo12Hour(time24: String): String {
        return if (time24.isNotEmpty()) {
            try {
                val parts = time24.split(":")
                if (parts.size == 2) {
                    val hour24 = parts[0].toInt()
                    val minute = parts[1].toInt()
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour24)
                        set(Calendar.MINUTE, minute)
                    }
                    timeFormat12Hour.format(cal.time)
                } else time24
            } catch (e: Exception) {
                time24
            }
        } else time24
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.label_trip_number, trip.tripNumber),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = convert24HourTo12Hour(trip.startTime),
            onValueChange = {},
            label = { Text(stringResource(R.string.label_start_time)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showStartTimePicker = true }) {
                    Icon(Icons.Default.Schedule, contentDescription = "Select Time")
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = convert24HourTo12Hour(trip.endTime),
            onValueChange = {},
            label = { Text(stringResource(R.string.label_end_time)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            isError = trip.timeError != null,
            trailingIcon = {
                IconButton(onClick = { showEndTimePicker = true }) {
                    Icon(Icons.Default.Schedule, contentDescription = "Select Time")
                }
            },
            supportingText = {
                trip.timeError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        ReadOnlyTextField(
            value = trip.amountOfRegularPayment,
            label = stringResource(R.string.label_amount_regular_payment),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        RadioButtonGroupField(
            label = stringResource(R.string.label_additional_trip_required),
            options = listOf(stringResource(R.string.option_yes), stringResource(R.string.option_no)),
            selectedValue = trip.additionalTripRequired,
            onValueSelected = onAdditionalRequiredChange,
            error = null,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                onStartTimeChange(timeFormat.format(calendar.time))
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                onEndTimeChange(timeFormat.format(calendar.time))
                showEndTimePicker = false
            },
            minTime = trip.startTime.takeIf { it.isNotEmpty() }
        )
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Column {
            // Header section with background
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExpandedChange(!isExpanded) }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Content section
            if (isExpanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    minTime: String? = null // Minimum selectable time (HH:mm format)
) {
    val timePickerState = rememberTimePickerState(is24Hour = false) // ✅ 12-hour format with AM/PM
    var validationError by remember { mutableStateOf<String?>(null) }
    val timeFormat12Hour = SimpleDateFormat("hh:mm a", Locale.getDefault())

    // ✅ Using Dialog + Surface for full control (matches Emptying Service form)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f) // ✅ Use 95% of screen width for spacious AM/PM display
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(16.dp), // ✅ Reduced padding from default 24dp
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.label_select_time),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp) // ✅ Reduced from default 16dp
                )

                TimePicker(
                    state = timePickerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp) // ✅ Minimal horizontal padding
                )

                // Show validation error if present
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            validationError = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    Button(
                        onClick = {
                            val hour = timePickerState.hour.toString().padStart(2, '0')
                            val minute = timePickerState.minute.toString().padStart(2, '0')
                            val selectedTime = "$hour:$minute"
                            
                            // Validate against minimum time if provided
                            if (minTime != null && selectedTime < minTime) {
                                // Convert minTime to 12-hour format for error message
                                val minTimeParts = minTime.split(":")
                                val minCal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, minTimeParts[0].toInt())
                                    set(Calendar.MINUTE, minTimeParts[1].toInt())
                                }
                                val minTime12Hour = timeFormat12Hour.format(minCal.time)
                                validationError = "Time must be after $minTime12Hour"
                            } else {
                                onConfirm(timePickerState.hour, timePickerState.minute)
                                validationError = null
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            }
        }
    }
}
