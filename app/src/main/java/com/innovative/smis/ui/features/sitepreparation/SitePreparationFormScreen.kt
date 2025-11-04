package com.innovative.smis.ui.features.sitepreparation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.innovative.smis.R
import com.innovative.smis.ui.components.DropdownMenuField
import com.innovative.smis.ui.components.MultiSelectCheckboxGroup
import com.innovative.smis.ui.components.PhoneNumberField
import com.innovative.smis.ui.components.PostponeDialog
import com.innovative.smis.ui.components.PostponeData
import com.innovative.smis.ui.components.DatePickerField
import com.innovative.smis.ui.components.disabledTextFieldColors
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PhoneNumberFormatter
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitePreparationFormScreen(
    applicationId: Int,
    navController: NavController,
    onNavigateToContainment: (Int, String?) -> Unit = { _, _ -> },
    viewModel: SitePreparationFormViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPostponeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(applicationId) {
        viewModel.loadApplicationDetails(applicationId)
    }

    val saveResult by viewModel.saveResult.collectAsState(null)
    
    LaunchedEffect(saveResult) {
        saveResult?.let { result ->
            // CRITICAL: Check if we're on a valid destination before popping
            val currentRoute = navController.currentDestination?.route
            android.util.Log.d("NavigationGuard", "SitePreparation save result - current route: $currentRoute, result: $result")
            
            when (result) {
                is SaveResult.Success -> {
                    if (currentRoute != null && navController.previousBackStackEntry != null) {
                        // Show success message and navigate back
                        if (result.shouldRefreshList) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("snackbar_message", result.message)
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("should_refresh", true)
                        }
                        android.util.Log.d("NavigationGuard", "SitePreparation executing popBackStack")
                        navController.popBackStack()
                    } else {
                        android.util.Log.d("NavigationGuard", "SitePreparation popBackStack skipped - invalid state")
                    }
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
                title = { Text(stringResource(R.string.screen_site_preparation_form), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    // Postpone icon button
                    IconButton(
                        onClick = { showPostponeDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EventBusy,
                            contentDescription = stringResource(R.string.cd_postpone),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Containment icon button
                    IconButton(
                        onClick = { 
                            onNavigateToContainment(applicationId, uiState.sanitationCustomerId)
                        },
                        enabled = uiState.sanitationCustomerId != null
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inventory,
                            contentDescription = stringResource(R.string.cd_containment),
                            tint = if (uiState.sanitationCustomerId != null) 
                                MaterialTheme.colorScheme.onSurface 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
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
                    Text(stringResource(R.string.message_error_format, loadingState.message ?: ""))
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
                            label = stringResource(R.string.label_application_id),
                            value = uiState.applicationId
                        )
                    }

                    // Applicant Information
                    item {
                        FormSectionHeader("Applicant Information")
                    }
                    
                    // Sanitation Customer ID (readonly)
                    item {
                        ReadOnlyTextField(
                            label = stringResource(R.string.label_sanitation_customer_id),
                            value = uiState.sanitationCustomerId ?: ""
                        )
                    }
                    
                    item {
                        ReadOnlyTextField(
                            label = stringResource(R.string.label_applicant_name),
                            value = uiState.applicantName
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = PhoneNumberFormatter.formatForDisplay(uiState.applicantContact),
                            onValueChange = { },
                            label = { Text(stringResource(R.string.label_applicant_contact)) },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            trailingIcon = {
                                if (uiState.applicantContact.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            val formattedNumber = PhoneNumberFormatter.formatForDialing(uiState.applicantContact)
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$formattedNumber"))
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = stringResource(R.string.cd_call_applicant),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        )
                    }

                    // Free Service Under PBC (readonly from API)
                    item {
                        ReadOnlyTextField(
                            label = stringResource(R.string.label_free_service_under_pbc),
                            value = if (uiState.freeServiceUnderPbc) "Yes" else "No"
                        )
                    }

                    // Additional Repairing (editable dropdown)
                    item {
                        FormSectionHeader("Additional Services")
                    }
                    item {
                        if (uiState.isLoadingDropdowns) {
                            OutlinedTextField(
                                value = "Loading...",
                                onValueChange = {},
                                label = { Text(stringResource(R.string.label_additional_repairing)) },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            MultiSelectCheckboxGroup(
                                label = stringResource(R.string.label_additional_repairing),
                                options = uiState.containmentIssuesList,
                                selectedKeys = uiState.additionalRepairingKeys,
                                onSelectionChange = viewModel::onAdditionalRepairingChange,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    // Show "Others" input field if "Others" is selected for additional repairing
                    if (uiState.additionalRepairingKeys.any { key ->
                        val value = uiState.containmentIssuesList[key] ?: ""
                        value.contains("Others", ignoreCase = true)
                    }) {
                        item {
                            OutlinedTextField(
                                value = uiState.otherAdditionalRepairing,
                                onValueChange = viewModel::onOtherAdditionalRepairingChange,
                                label = { Text(stringResource(R.string.label_other_additional_repairing)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Payment Information (editable)
                    item {
                        FormSectionHeader("Payment Information")
                    }
                    item {
                        YesNoRadioGroup(
                            label = stringResource(R.string.label_extra_payment_required),
                            selectedOption = uiState.extraPaymentRequired,
                            onOptionSelected = viewModel::onExtraPaymentRequiredChange
                        )
                    }

                    // Extra Payment Amount (editable if required)
                    if (uiState.extraPaymentRequired == true) {
                        item {
                            OutlinedTextField(
                                value = uiState.amountOfExtraPayment,
                                onValueChange = viewModel::onAmountOfExtraPaymentChange,
                                label = { Text(stringResource(R.string.label_amount_of_extra_payment)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
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
                            Text(stringResource(R.string.checkbox_receiver_same_as_applicant))
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = if (uiState.isReceiverSameAsApplicant) uiState.applicantName else uiState.serviceReceiverName,
                            onValueChange = viewModel::onServiceReceiverNameChange,
                            label = { Text(stringResource(R.string.label_service_receiver_name)) },
                            enabled = !uiState.isReceiverSameAsApplicant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        if (uiState.isReceiverSameAsApplicant) {
                            // Disabled state - show existing contact
                            OutlinedTextField(
                                value = uiState.applicantContact,
                                onValueChange = {},
                                label = { Text(stringResource(R.string.label_service_receiver_contact)) },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                colors = disabledTextFieldColors()
                            )
                        } else {
                            PhoneNumberField(
                                value = uiState.serviceReceiverContact,
                                onValueChange = viewModel::onServiceReceiverContactChange,
                                label = stringResource(R.string.label_service_receiver_contact),
                                modifier = Modifier,
                                enabled = true,
                                isRequired = false
                            )
                        }
                    }

                    // Scheduling Information
                    item {
                        FormSectionHeader("Scheduling Information")
                    }
                    item {
                        // Display proposedEmptyingDate in user's preferred format
                        val displayProposedDate = remember(uiState.proposedEmptyingDate) {
                            try {
                                if (uiState.proposedEmptyingDate.isNotEmpty()) {
                                    val apiFormatter = com.innovative.smis.util.helper.DateFormatManager.getApiFormatter()
                                    val displayFormatter = com.innovative.smis.util.helper.DateFormatManager.getDisplayFormatter(context)
                                    val date = apiFormatter.parse(uiState.proposedEmptyingDate)
                                    date?.let { displayFormatter.format(it) } ?: uiState.proposedEmptyingDate
                                } else {
                                    uiState.proposedEmptyingDate
                                }
                            } catch (e: Exception) {
                                uiState.proposedEmptyingDate
                            }
                        }
                        
                        ReadOnlyTextField(
                            label = stringResource(R.string.label_propose_emptying_date),
                            value = displayProposedDate
                        )
                    }
                    item {
                        YesNoRadioGroup(
                            label = stringResource(R.string.label_need_reschedule),
                            selectedOption = uiState.needReschedule,
                            onOptionSelected = viewModel::onNeedRescheduleChange
                        )
                    }

                    // New Proposed Emptying Date (if reschedule is yes)
                    if (uiState.needReschedule == true) {
                        item {
                            val selectedMillis = com.innovative.smis.util.helper.DateFormatManager
                                .parseDisplayDate(context, uiState.newProposedEmptyingDate)
                            
                            DatePickerField(
                                label = stringResource(R.string.label_new_proposed_emptying_date),
                                selectedDate = selectedMillis,
                                onDateSelected = { millis ->
                                    val formattedDate = millis?.let {
                                        com.innovative.smis.util.helper.DateFormatManager
                                            .formatTimestampForDisplay(context, it)
                                    } ?: ""
                                    viewModel.onNewProposedEmptyingDateChange(formattedDate)
                                },
                                isFutureDateAllowed = true  // Allow future dates for rescheduling
                            )
                        }
                    }

                    // Submit and Draft buttons
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Save Draft Button
                            OutlinedButton(
                                onClick = { viewModel.saveDraft() },
                                enabled = !uiState.isSubmitting,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (uiState.isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.button_saving))
                                } else {
                                    Text(stringResource(R.string.button_draft))
                                }
                            }
                            
                            // Submit Button
                            Button(
                                onClick = { viewModel.saveForm() },
                                enabled = !uiState.isSubmitting,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (uiState.isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.button_submitting))
                                } else {
                                    Text(stringResource(R.string.button_submit))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Postpone Dialog
    if (showPostponeDialog) {
        PostponeDialog(
            applicationId = applicationId,
            currentDate = uiState.proposedEmptyingDate,
            onDismiss = { showPostponeDialog = false },
            onPostpone = { postponeData ->
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
                Text(stringResource(R.string.label_yes), modifier = Modifier.padding(start = 4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedOption == false,
                    onClick = { onOptionSelected(false) }
                )
                Text(stringResource(R.string.label_no), modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
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