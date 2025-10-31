package com.innovative.smis.ui.features.emptyingservice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
import com.innovative.smis.ui.components.SectionHeader
import com.innovative.smis.ui.components.LoadingDialog
import com.innovative.smis.ui.components.CheckboxField
import com.innovative.smis.ui.components.ImagePickerComponent
import com.innovative.smis.ui.components.DropdownField
import com.innovative.smis.ui.components.MultiSelectCheckboxGroup
import com.innovative.smis.ui.components.RadioButtonGroupField
import com.innovative.smis.ui.components.PostponeDialog
import com.innovative.smis.ui.components.PostponeData
import com.innovative.smis.ui.components.ReadOnlyTextField
import com.innovative.smis.util.common.Resource
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.compose.ui.Alignment.Companion.Center
import com.google.android.gms.location.LocationServices
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.google.android.gms.maps.CameraUpdateFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyingServiceFormScreen(
    navController: androidx.navigation.NavController,
    applicationId: Int,
    viewModel: EmptyingServiceFormViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // State for expandable sections
    var applicantDetailsExpanded by remember { mutableStateOf(false) }
    var serviceDetailsExpanded by remember { mutableStateOf(false) }
    var vehicleDetailsExpanded by remember { mutableStateOf(false) }
    var paymentDocumentationExpanded by remember { mutableStateOf(false) }
    
    // State for map bottom sheet
    var showMapBottomSheet by remember { mutableStateOf(false) }
    
    var showPostponeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(applicationId) {
        viewModel.loadApplicationDetails(applicationId)
        viewModel.loadReadonlyData(applicationId)
    }
    
    // Auto-expand sections when validation errors occur
    LaunchedEffect(uiState.desludgingVehicleIdError, uiState.pumpingPointTypeError) {
        if (uiState.desludgingVehicleIdError != null || uiState.pumpingPointTypeError != null) {
            vehicleDetailsExpanded = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveResult.collect { result ->
            // CRITICAL: Check if we're on a valid destination before popping
            val currentRoute = navController.currentDestination?.route
            android.util.Log.d("NavigationGuard", "EmptyingService save result - current route: $currentRoute, result: $result")
            
            if (currentRoute != null && navController.previousBackStackEntry != null) {
                val message = when (result) {
                    is EmptyingServiceFormViewModel.SaveResult.Success -> result.message
                    is EmptyingServiceFormViewModel.SaveResult.Error -> result.message
                }

                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("snackbar_message", message)

                // If this was a successful form submission, trigger list refresh on previous screen
                if (result is EmptyingServiceFormViewModel.SaveResult.Success && result.shouldRefreshList) {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("should_refresh_list", true)
                }

                // Add small delay to ensure the savedStateHandle is properly set before navigation
                kotlinx.coroutines.delay(100)
                
                android.util.Log.d("NavigationGuard", "EmptyingService executing popBackStack")
                navController.popBackStack()
            } else {
                android.util.Log.d("NavigationGuard", "EmptyingService popBackStack skipped - invalid state")
            }
        }
    }

    LoadingDialog(
        isLoading = uiState.isLoading,
        title = "Loading Application Details",
        message = "Please wait while we load the application information..."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Emptying Service Form",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Postpone icon button
                    IconButton(
                        onClick = { showPostponeDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EventBusy,
                            contentDescription = "Postpone",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Containment icon button
                    IconButton(
                        onClick = { 
                            navController.navigate("containment_form/$applicationId/${uiState.sanitationCustomerId ?: ""}")
                        },
                        enabled = uiState.sanitationCustomerId != null
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inventory,
                            contentDescription = "Containment",
                            tint = if (uiState.sanitationCustomerId != null) 
                                MaterialTheme.colorScheme.onSurface 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Application ID - always visible
            item {
                OutlinedTextField(
                    value = applicationId.toString(),
                    onValueChange = { },
                    label = { Text("Application ID") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors()
                )
            }

            // Applicant Details Section
            item {
                CollapsibleSection(
                    title = "Applicant Details",
                    isExpanded = applicantDetailsExpanded,
                    onToggle = { applicantDetailsExpanded = !applicantDetailsExpanded }
                ) {
                    // Sanitation Customer ID - disabled field
                    ReadOnlyTextField(
                        label = "Sanitation Customer ID",
                        value = uiState.sanitationCustomerId ?: ""
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = uiState.applicantName,
                        onValueChange = { },
                        label = { Text("Applicant Name") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.applicantContact,
                        onValueChange = { },
                        label = { Text("Applicant Contact") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(),
                        trailingIcon = {
                            if (uiState.applicantContact.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${uiState.applicantContact}"))
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = "Call applicant",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    CheckboxField(
                        label = "Service Receiver Same as Applicant",
                        checked = uiState.isServiceReceiverSameAsApplicant,
                        onCheckedChange = viewModel::onServiceReceiverSameAsApplicantChange
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.serviceReceiverName,
                        onValueChange = viewModel::onServiceReceiverNameChange,
                        label = { Text("Service Receiver Name") },
                        enabled = !uiState.isServiceReceiverSameAsApplicant,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.serviceReceiverContact,
                        onValueChange = viewModel::onServiceReceiverContactChange,
                        label = { Text("Service Receiver Contact") },
                        enabled = !uiState.isServiceReceiverSameAsApplicant,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(),
                        trailingIcon = {
                            if (uiState.serviceReceiverContact.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${uiState.serviceReceiverContact}"))
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = "Call service receiver",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // Service Details Section
            item {
                CollapsibleSection(
                    title = "Service Details",
                    isExpanded = serviceDetailsExpanded,
                    onToggle = { serviceDetailsExpanded = !serviceDetailsExpanded }
                ) {
                    // Display emptiedDate in user's preferred format
                    val displayEmptiedDate = remember(uiState.emptiedDate) {
                        try {
                            val apiFormatter = com.innovative.smis.util.helper.DateFormatManager.getApiFormatter()
                            val displayFormatter = com.innovative.smis.util.helper.DateFormatManager.getDisplayFormatter(context)
                            val date = apiFormatter.parse(uiState.emptiedDate)
                            date?.let { displayFormatter.format(it) } ?: uiState.emptiedDate
                        } catch (e: Exception) {
                            uiState.emptiedDate
                        }
                    }
                    
                    OutlinedTextField(
                        value = displayEmptiedDate,
                        onValueChange = { },
                        label = { Text("Emptied Date") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TimePickerField(
                        label = "Start Time",
                        value = uiState.startTime,
                        onValueChange = viewModel::onStartTimeChange,
                        modifier = Modifier.fillMaxWidth(),
                        error = uiState.startTimeError
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TimePickerField(
                        label = "End Time",
                        value = uiState.endTime,
                        onValueChange = viewModel::onEndTimeChange,
                        modifier = Modifier.fillMaxWidth(),
                        error = uiState.endTimeError,
                        minTime = uiState.startTime.takeIf { it.isNotEmpty() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    RadioButtonGroupField(
                        label = "Additional Trip Required",
                        options = listOf("Yes", "No"),
                        selectedValue = uiState.additionalTripRequired.replaceFirstChar { it.uppercase() },
                        onValueSelected = { value -> viewModel.onAdditionalTripRequiredChange(value.lowercase()) },
                        error = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Vehicle & Sludge Details Section
            item {
                CollapsibleSection(
                    title = "Vehicle & Sludge Details",
                    isExpanded = vehicleDetailsExpanded,
                    onToggle = { vehicleDetailsExpanded = !vehicleDetailsExpanded }
                ) {
                    // Desludging Vehicle ID - Dropdown from API
                    DropdownField(
                        label = "Desludging Vehicle ID *",
                        selectedValue = uiState.selectedVehicleLicensePlate,
                        options = uiState.vehicleOptions.map { it.type },
                        onValueSelected = viewModel::onDesludgingVehicleIdChange,
                        error = uiState.desludgingVehicleIdError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Sludge Type - Radio buttons
                    RadioButtonGroupField(
                        label = "Sludge Type",
                        options = listOf("Mixed", "Not Mixed"),
                        selectedValue = uiState.sludgeType,
                        onValueSelected = viewModel::onSludgeTypeChange,
                        error = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Type of Sludge - Show only when "Mixed" is selected
                    if (uiState.sludgeType == "Mixed") {
                        RadioButtonGroupField(
                            label = "Type of Sludge",
                            options = listOf("Processing food", "Oil and fat (restaurant)", "Content of fuel"),
                            selectedValue = uiState.typeOfSludge,
                            onValueSelected = viewModel::onTypeOfSludgeChange,
                            error = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    RadioButtonGroupField(
                        label = "Pumping Point Type *",
                        options = listOf("Cover", "Tube", "Pierce"),
                        selectedValue = uiState.pumpingPointType,
                        onValueSelected = viewModel::onPumpingPointTypeChange,
                        error = uiState.pumpingPointTypeError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Additional Repairing in Emptying - Multi-select Checkboxes
                    MultiSelectCheckboxGroup(
                        label = "Additional Repairing in Emptying",
                        options = uiState.additionalRepairingOptions,
                        selectedKeys = uiState.additionalRepairingKeys,
                        onSelectionChange = viewModel::onAdditionalRepairingChange,
                        enabled = !uiState.isAdditionalRepairingReadonly,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Show "Other Additional Repairing" field if "Others" is selected
                    if (uiState.additionalRepairingKeys.any { key ->
                        val value = uiState.additionalRepairingOptions[key] ?: ""
                        value.contains("Others", ignoreCase = true)
                    }) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = uiState.otherAdditionalRepairing,
                            onValueChange = viewModel::onOtherAdditionalRepairingChange,
                            label = { Text("Other Additional Repairing") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Payment & Documentation Section
            item {
                CollapsibleSection(
                    title = "Payment & Documentation",
                    isExpanded = paymentDocumentationExpanded,
                    onToggle = { paymentDocumentationExpanded = !paymentDocumentationExpanded }
                ) {
                    ReadOnlyTextField(
                        label = "Free Under PBC",
                        value = if (uiState.freeUnderPBC) "Yes" else "No"
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Regular Cost - readonly when loaded from API
                    if (uiState.isRegularCostReadonly) {
                        ReadOnlyTextField(
                            label = "Amount of Regular Cost",
                            value = uiState.regularCost
                        )
                    } else {
                        OutlinedTextField(
                            value = uiState.regularCost,
                            onValueChange = viewModel::onRegularCostChange,
                            label = { Text("Amount of Regular Cost") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.extraCost,
                        onValueChange = viewModel::onExtraCostChange,
                        label = { Text("Amount of Extra Cost") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        readOnly = uiState.isExtraCostReadonly,
                        enabled = !uiState.isExtraCostReadonly,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Only show Receipt Number and Receipt Image when Additional Trip Required is "no"
                    if (uiState.additionalTripRequired == "no") {
                        OutlinedTextField(
                            value = uiState.receiptNumber,
                            onValueChange = viewModel::onReceiptNumberChange,
                            label = { Text("Receipt Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Image Upload Components
                        ImagePickerComponent(
                            label = "Receipt Image",
                            selectedImageUri = if (uiState.receiptImage.isNotBlank()) Uri.parse(uiState.receiptImage) else null,
                            onImageSelected = { uri -> viewModel.onReceiptImageSelected(uri?.toString()) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    ImagePickerComponent(
                        label = "Picture of Emptying",
                        selectedImageUri = if (uiState.pictureOfEmptying.isNotBlank()) Uri.parse(uiState.pictureOfEmptying) else null,
                        onImageSelected = { uri -> viewModel.onEmptyingImageSelected(uri?.toString()) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.comments,
                        onValueChange = viewModel::onCommentsChange,
                        label = { Text("Comments") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    
                    // Only show location fields if building point geometry doesn't exist
                    if (!uiState.buildingPointGeomExist) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Location capture
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.latitude?.toString() ?: "",
                                onValueChange = { },
                                label = { Text("Latitude") },
                                readOnly = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.longitude?.toString() ?: "",
                                onValueChange = { },
                                label = { Text("Longitude") },
                                readOnly = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = viewModel::captureLocation,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Capture GPS")
                            }
                            
                            OutlinedButton(
                                onClick = { 
                                    showMapBottomSheet = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Update Map")
                            }
                        }
                    }
                }
            }

            // Submit Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.saveDraft() },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSubmitting
                    ) {
                        Text("Save Draft")
                    }

                    Button(
                        onClick = { viewModel.submitForm() },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSubmitting
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Update")
                        }
                    }
                }
            }
        }
        
        
        // Postpone Dialog
        if (showPostponeDialog) {
            PostponeDialog(
                applicationId = applicationId,
                currentDate = uiState.emptiedDate, // Already in API format (yyyy-MM-dd)
                onDismiss = { showPostponeDialog = false },
                onPostpone = { postponeData ->
                    // postponeData contains dates already in API format (yyyy-MM-dd)
                    viewModel.postponeApplication(
                        postponeFrom = postponeData.postponeFrom,
                        postponeUntil = postponeData.postponeUntil,
                        reason = postponeData.reason,
                        remark = postponeData.remark,
                        onSuccess = {
                            showPostponeDialog = false
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("snackbar_message", "Application postponed successfully")
                            navController.popBackStack()
                        },
                        onError = { errorMessage ->
                            showPostponeDialog = false
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("snackbar_message", errorMessage)
                            navController.popBackStack()
                        }
                    )
                }
            )
        }
        
        // Map Bottom Sheet
        if (showMapBottomSheet) {
            EmptyingServiceMapBottomSheet(
                initialLatitude = uiState.latitude,
                initialLongitude = uiState.longitude,
                onDismiss = { showMapBottomSheet = false },
                onLocationSelected = { lat, lng ->
                    viewModel.updateLocation(lat, lng)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    minTime: String? = null // Minimum selectable time (HH:mm format)
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val calendar = Calendar.getInstance()
    val timeFormat12Hour = SimpleDateFormat("hh:mm a", Locale.getDefault()) // 12-hour with AM/PM
    val timeFormat24Hour = SimpleDateFormat("HH:mm", Locale.getDefault()) // 24-hour for validation

    // Parse current time if value is not empty
    if (value.isNotEmpty()) {
        try {
            val parts = value.split(":")
            if (parts.size == 2) {
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                calendar.set(Calendar.MINUTE, parts[1].toInt())
            }
        } catch (e: Exception) {
            // Use current time if parsing fails
        }
    }

    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = false // ✅ 12-hour format with AM/PM
    )

    // Convert 24-hour format to 12-hour format for display
    val displayValue = if (value.isNotEmpty()) {
        try {
            val parts = value.split(":")
            if (parts.size == 2) {
                val hour24 = parts[0].toInt()
                val minute = parts[1].toInt()
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour24)
                    set(Calendar.MINUTE, minute)
                }
                timeFormat12Hour.format(cal.time)
            } else value
        } catch (e: Exception) {
            value
        }
    } else value

    OutlinedTextField(
        value = displayValue,
        onValueChange = { },
        label = { Text(label) },
        readOnly = true,
        isError = error != null,
        trailingIcon = {
            IconButton(onClick = { showTimePicker = true }) {
                Icon(Icons.Default.Schedule, contentDescription = "Select time")
            }
        },
        supportingText = {
            error?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        modifier = modifier.clickable { showTimePicker = true }
    )

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.95f) // ✅ Use 95% of screen width for spacious AM/PM display
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp), // ✅ Reduced padding from 24dp to 16dp
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select $label",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 12.dp) // ✅ Reduced from 16dp to 12dp
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
                                showTimePicker = false
                                validationError = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                // Store time in 24-hour format for backend
                                val hour24 = timePickerState.hour.toString().padStart(2, '0')
                                val minute = timePickerState.minute.toString().padStart(2, '0')
                                val selectedTime24Hour = "$hour24:$minute"
                                
                                // Validate against minimum time if provided (compare 24-hour format)
                                if (minTime != null && selectedTime24Hour < minTime) {
                                    // Convert minTime to 12-hour format for error message
                                    val minTimeParts = minTime.split(":")
                                    val minCal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, minTimeParts[0].toInt())
                                        set(Calendar.MINUTE, minTimeParts[1].toInt())
                                    }
                                    val minTime12Hour = timeFormat12Hour.format(minCal.time)
                                    validationError = "Time must be after $minTime12Hour"
                                } else {
                                    onValueChange(selectedTime24Hour)
                                    showTimePicker = false
                                    validationError = null
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header section (always visible)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onToggle() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Content section (collapsible)
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    content = content
                )
            }
        }
    }
}

