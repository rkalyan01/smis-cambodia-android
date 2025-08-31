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
import android.net.Uri
import com.innovative.smis.ui.components.SectionHeader
import com.innovative.smis.ui.components.LoadingDialog
import com.innovative.smis.ui.components.CheckboxField
import com.innovative.smis.ui.components.ImagePickerComponent
import com.innovative.smis.ui.components.DropdownField
import com.innovative.smis.ui.components.RadioButtonGroupField
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyingServiceFormScreen(
    navController: androidx.navigation.NavController,
    applicationId: Int,
    viewModel: EmptyingServiceFormViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // State for expandable sections
    var applicantDetailsExpanded by remember { mutableStateOf(false) }
    var serviceDetailsExpanded by remember { mutableStateOf(false) }
    var vehicleDetailsExpanded by remember { mutableStateOf(false) }
    var paymentDocumentationExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(applicationId) {
        viewModel.loadApplicationDetails(applicationId)
        viewModel.loadReadonlyData(applicationId)
    }

    val saveResult by viewModel.saveResult.collectAsStateWithLifecycle(null)

    LaunchedEffect(saveResult) {
        saveResult?.let { result ->
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
            navController.popBackStack()
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
                    OutlinedButton(
                        onClick = { navController.navigate("containment_form/$applicationId") }
                    ) {
                        Text("Containment")
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
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Applicant Details Section
            item {
                CollapsibleSection(
                    title = "Applicant Details",
                    isExpanded = applicantDetailsExpanded,
                    onToggle = { applicantDetailsExpanded = !applicantDetailsExpanded }
                ) {
                    OutlinedTextField(
                        value = uiState.applicantName,
                        onValueChange = { },
                        label = { Text("Applicant Name") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.applicantContact,
                        onValueChange = { },
                        label = { Text("Applicant Contact") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.serviceReceiverContact,
                        onValueChange = viewModel::onServiceReceiverContactChange,
                        label = { Text("Service Receiver Contact") },
                        enabled = !uiState.isServiceReceiverSameAsApplicant,
                        modifier = Modifier.fillMaxWidth()
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
                    OutlinedTextField(
                        value = uiState.emptiedDate,
                        onValueChange = { },
                        label = { Text("Emptied Date") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TimePickerField(
                        label = "Start Time",
                        value = uiState.startTime,
                        onValueChange = viewModel::onStartTimeChange,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TimePickerField(
                        label = "End Time",
                        value = uiState.endTime,
                        onValueChange = viewModel::onEndTimeChange,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.noOfTrips,
                        onValueChange = viewModel::onNoOfTripsChange,
                        label = { Text("No of Trips") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        label = "Desludging Vehicle ID",
                        selectedValue = uiState.selectedVehicleLicensePlate,
                        options = uiState.vehicleOptions.map { it.type },
                        onValueSelected = viewModel::onDesludgingVehicleIdChange,
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

                    // Pumping Point Presence - Radio buttons
                    RadioButtonGroupField(
                        label = "Is there a pumping point",
                        options = listOf("Yes", "No"),
                        selectedValue = uiState.pumpingPointPresence,
                        onValueSelected = viewModel::onPumpingPointPresenceChange,
                        error = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Pumping Point Type - Show only when "Yes" is selected
                    if (uiState.pumpingPointPresence == "Yes") {
                        RadioButtonGroupField(
                            label = "Pumping Point Type",
                            options = listOf("Cover", "Tube", "Pierce"),
                            selectedValue = uiState.pumpingPointType,
                            onValueSelected = viewModel::onPumpingPointTypeChange,
                            error = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Additional Repairing in Emptying - Dropdown from API
                    DropdownField(
                        label = "Additional Repairing in Emptying",
                        selectedValue = uiState.additionalRepairingInEmptying,
                        options = uiState.additionalRepairingOptions.values.toList(),
                        onValueSelected = viewModel::onAdditionalRepairingChange,
                        enabled = !uiState.isAdditionalRepairingReadonly,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Payment & Documentation Section
            item {
                CollapsibleSection(
                    title = "Payment & Documentation",
                    isExpanded = paymentDocumentationExpanded,
                    onToggle = { paymentDocumentationExpanded = !paymentDocumentationExpanded }
                ) {
                    CheckboxField(
                        label = "Free Under PBC",
                        checked = uiState.freeUnderPBC,
                        onCheckedChange = if (uiState.isFreeUnderPBCReadonly) { {} } else viewModel::onFreeUnderPBCChange,
                        enabled = !uiState.isFreeUnderPBCReadonly
                    )
                    Spacer(modifier = Modifier.height(16.dp))



                    OutlinedTextField(
                        value = uiState.regularCost,
                        onValueChange = viewModel::onRegularCostChange,
                        label = { Text("Regular Cost") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.extraCost,
                        onValueChange = viewModel::onExtraCostChange,
                        label = { Text("Extra Cost") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        readOnly = uiState.isExtraCostReadonly,
                        enabled = !uiState.isExtraCostReadonly,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

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

                    Button(
                        onClick = viewModel::captureLocation,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture Location")
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance()

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
        is24Hour = true
    )

    OutlinedTextField(
        value = value,
        onValueChange = { },
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showTimePicker = true }) {
                Icon(Icons.Default.Schedule, contentDescription = "Select time")
            }
        },
        modifier = modifier.clickable { showTimePicker = true }
    )

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.wrapContentSize()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select $label",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.padding(16.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { showTimePicker = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val hour = timePickerState.hour.toString().padStart(2, '0')
                                val minute = timePickerState.minute.toString().padStart(2, '0')
                                onValueChange("$hour:$minute")
                                showTimePicker = false
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
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header section (always visible)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray.copy(alpha = 0.1f))
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
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            // Content section (collapsible)
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp),
                    content = content
                )
            }
        }
    }
}