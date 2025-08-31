package com.innovative.smis.ui.features.sitepreparation

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.innovative.smis.util.common.Resource
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitePreparationFormScreen(
    applicationId: Int,
    navController: NavController,
    onNavigateToContainment: (Int) -> Unit = { },
    viewModel: SitePreparationFormViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(applicationId) {
        viewModel.loadApplicationDetails(applicationId)
    }

    val saveResult by viewModel.saveResult.collectAsState(null)
    
    LaunchedEffect(saveResult) {
        saveResult?.let { result ->
            when (result) {
                is SaveResult.Success -> {
                    // Show success message and navigate back
                    if (result.shouldRefreshList) {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("snackbar_message", result.message)
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("should_refresh", true)
                    }
                    navController.popBackStack()
                }
                is SaveResult.Error -> {
                    // Handle error - could show snackbar or dialog
                }
            }
        }
    }

    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Site Preparation Form", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { 
                            onNavigateToContainment(applicationId)
                        }
                    ) {
                        Text("CONTAINMENT", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        
        when (val loadingState = uiState.loadingState) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: ${loadingState.message}")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                keyboardController?.hide()
                            })
                        },
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {

                    // Application ID (readonly at top)
                    item {
                        ReadOnlyTextField(
                            label = "Application ID",
                            value = uiState.applicationId
                        )
                    }

                    // Applicant Information
                    item {
                        FormSectionHeader("Applicant Information")
                    }
                    item {
                        ReadOnlyTextField(
                            label = "Applicant Name",
                            value = uiState.applicantName
                        )
                    }
                    item {
                        ReadOnlyTextField(
                            label = "Applicant Contact",
                            value = uiState.applicantContact
                        )
                    }



                    // Purpose of Emptying Request (readonly from API)
                    item {
                        FormSectionHeader("Purpose of Emptying Request")
                    }
                    item {
                        ReadOnlyTextField(
                            label = "Purpose of Emptying Request",
                            value = uiState.purposeOfEmptying
                        )
                    }
                    
                    // Show "Other" field if purpose contains "Others, specify" (readonly)
                    if (uiState.otherEmptyingPurpose.isNotBlank()) {
                        item {
                            ReadOnlyTextField(
                                label = "Other Emptying Purpose",
                                value = uiState.otherEmptyingPurpose
                            )
                        }
                    }

                    // Ever Emptied Section (readonly from API)
                    item {
                        FormSectionHeader("Emptying History")
                    }
                    item {
                        ReadOnlyTextField(
                            label = "Ever Emptied",
                            value = if (uiState.everEmptied == true) "Yes" else if (uiState.everEmptied == false) "No" else ""
                        )
                    }

                    // Show Last Emptied Year if Ever Emptied is Yes (readonly)
                    if (uiState.everEmptied == true && uiState.lastEmptiedYear.isNotBlank()) {
                        item {
                            ReadOnlyTextField(
                                label = "Last Emptied Date",
                                value = "${uiState.lastEmptiedYear}-01-01"
                            )
                        }
                    }

                    // Show reason field if Ever Emptied is No (readonly if provided by API)
                    if (uiState.everEmptied == false && uiState.notEmptiedBeforeReason.isNotBlank()) {
                        item {
                            ReadOnlyTextField(
                                label = "Reason for no Emptied Date",
                                value = uiState.notEmptiedBeforeReason
                            )
                        }
                    }

                    // Show reason field if Last Emptied Date is null but Ever Emptied is Yes (readonly if provided by API)
                    if (uiState.everEmptied == true && uiState.lastEmptiedYear.isBlank() && uiState.reasonForNoEmptiedDate.isNotBlank()) {
                        item {
                            ReadOnlyTextField(
                                label = "Reason for no Emptied Date",
                                value = uiState.reasonForNoEmptiedDate
                            )
                        }
                    }

                    // Free Service Under PBC (readonly from API)
                    item {
                        ReadOnlyTextField(
                            label = "Free Service Under PBC",
                            value = if (uiState.freeServiceUnderPbc) "Yes" else "No"
                        )
                    }

                    // Additional Repairing (readonly from API)
                    item {
                        FormSectionHeader("Additional Services")
                    }
                    item {
                        ReadOnlyTextField(
                            label = "Additional repairing",
                            value = uiState.additionalRepairing
                        )
                    }
                    if (uiState.otherAdditionalRepairing.isNotBlank()) {
                        item {
                            ReadOnlyTextField(
                                label = "Other Additional Repairing",
                                value = uiState.otherAdditionalRepairing
                            )
                        }
                    }

                    // Payment Information (readonly from API)
                    item {
                        FormSectionHeader("Payment Information")
                    }
                    item {
                        ReadOnlyTextField(
                            label = "Extra payment required",
                            value = if (uiState.extraPaymentRequired == true) "Yes" else if (uiState.extraPaymentRequired == false) "No" else ""
                        )
                    }

                    // Extra Payment Amount (readonly from API if required)
                    if (uiState.extraPaymentRequired == true && uiState.amountOfExtraPayment.isNotBlank()) {
                        item {
                            ReadOnlyTextField(
                                label = "Amount of Extra Payment",
                                value = uiState.amountOfExtraPayment
                            )
                        }
                    }

                    // Receiver Information (renamed from Customer Information)
                    item {
                        FormSectionHeader("Receiver Information")
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = uiState.isReceiverSameAsApplicant,
                                onCheckedChange = viewModel::onReceiverSameAsApplicantChange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Receiver is same as applicant")
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = if (uiState.isReceiverSameAsApplicant) uiState.applicantName else uiState.serviceReceiverName,
                            onValueChange = viewModel::onServiceReceiverNameChange,
                            label = { Text("Service Receiver Name") },
                            enabled = !uiState.isReceiverSameAsApplicant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = if (uiState.isReceiverSameAsApplicant) uiState.applicantContact else uiState.serviceReceiverContact,
                            onValueChange = viewModel::onServiceReceiverContactChange,
                            label = { Text("Service Receiver Contact") },
                            enabled = !uiState.isReceiverSameAsApplicant,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Scheduling Information
                    item {
                        FormSectionHeader("Scheduling Information")
                    }
                    item {
                        ReadOnlyTextField(
                            label = "Propose Emptying Date",
                            value = uiState.proposedEmptyingDate
                        )
                    }
                    item {
                        YesNoRadioGroup(
                            label = "need reschedule?",
                            selectedOption = uiState.needReschedule,
                            onOptionSelected = viewModel::onNeedRescheduleChange
                        )
                    }

                    // New Proposed Emptying Date (if reschedule is yes)
                    if (uiState.needReschedule == true) {
                        item {
                            DatePickerField(
                                label = "New Proposed Emptying Date",
                                selectedDateMillis = if (uiState.newProposedEmptyingDate.isNotBlank()) {
                                    try {
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            .parse(uiState.newProposedEmptyingDate)?.time
                                    } catch (e: Exception) {
                                        null
                                    }
                                } else null,
                                onDateSelected = { dateMillis ->
                                    val formattedDate = if (dateMillis != null) {
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            .format(Date(dateMillis))
                                    } else ""
                                    viewModel.onNewProposedEmptyingDateChange(formattedDate)
                                },
                                minDate = System.currentTimeMillis() // Prevent selecting past dates
                            )
                        }
                    }

                    // Submit Button
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.saveForm() },
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submitting...")
                            } else {
                                Text("Submit Form")
                            }
                        }
                        
                        // Save Draft Button
                        OutlinedButton(
                            onClick = { viewModel.saveDraft() },
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving...")
                            } else {
                                Text("Save as Draft")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FormSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ReadOnlyTextField(label: String, value: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { },
        label = { Text(label) },
        enabled = false,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    )
}

@Composable
fun YesNoRadioGroup(
    label: String,
    selectedOption: Boolean?,
    onOptionSelected: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedOption == true,
                    onClick = { onOptionSelected(true) }
                )
                Text("Yes", modifier = Modifier.padding(start = 4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedOption == false,
                    onClick = { onOptionSelected(false) }
                )
                Text("No", modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
fun DatePickerField(
    label: String,
    selectedDateMillis: Long?,
    onDateSelected: (Long?) -> Unit,
    minDate: Long? = null,
    maxDate: Long? = null
) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    OutlinedTextField(
        value = selectedDateMillis?.let { dateFormatter.format(Date(it)) } ?: "",
        onValueChange = { },
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            IconButton(
                onClick = {
                    val calendar = Calendar.getInstance()
                    if (selectedDateMillis != null) {
                        calendar.timeInMillis = selectedDateMillis
                    }
                    
                    val datePickerDialog = DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selectedCalendar = Calendar.getInstance()
                            selectedCalendar.set(year, month, dayOfMonth)
                            onDateSelected(selectedCalendar.timeInMillis)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
                    
                    minDate?.let { datePickerDialog.datePicker.minDate = it }
                    maxDate?.let { datePickerDialog.datePicker.maxDate = it }
                    
                    datePickerDialog.show()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select Date"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}