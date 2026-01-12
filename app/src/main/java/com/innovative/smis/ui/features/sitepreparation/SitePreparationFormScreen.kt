package com.innovative.smis.ui.features.sitepreparation

import com.innovative.smis.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import com.innovative.smis.ui.components.DropdownMenuField
import com.innovative.smis.ui.components.MultiSelectCheckboxGroup
import com.innovative.smis.ui.components.PhoneNumberField
import com.innovative.smis.ui.components.PostponeDialog
import com.innovative.smis.ui.components.PostponeData
import com.innovative.smis.ui.components.DatePickerField
import com.innovative.smis.ui.components.YesNoRadioGroup
import com.innovative.smis.ui.components.disabledTextFieldColors
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PhoneNumberFormatter
import com.innovative.smis.util.validation.InputValidators
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.innovative.smis.ui.components.ValidatedTextField
import com.innovative.smis.ui.components.ValidatedPhoneNumberField

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
    val listState = rememberLazyListState()
    
    // Create BringIntoViewRequesters for each required field
    val serviceReceiverNameRequester = remember { BringIntoViewRequester() }
    val serviceReceiverContactRequester = remember { BringIntoViewRequester() }
    val extraPaymentRequiredRequester = remember { BringIntoViewRequester() }
    val needRescheduleRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(applicationId) {
        viewModel.loadApplicationDetails(applicationId)
    }
    
    // Scroll to first error field when validation fails using BringIntoViewRequester
    LaunchedEffect(uiState.firstErrorField) {
        uiState.firstErrorField?.let { errorField ->
            when (errorField) {
                "serviceReceiverName" -> serviceReceiverNameRequester.bringIntoView()
                "serviceReceiverContact" -> serviceReceiverContactRequester.bringIntoView()
                "extraPaymentRequired" -> extraPaymentRequiredRequester.bringIntoView()
                "needReschedule" -> needRescheduleRequester.bringIntoView()
            }
        }
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
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inventory,
                            contentDescription = stringResource(R.string.cd_containment),
                            tint = MaterialTheme.colorScheme.onSurface
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
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                keyboardController?.hide()
                            })
                        },
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
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
                        FormSectionHeader(stringResource(R.string.section_applicant_information))
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
                            value = if (uiState.customerContactList.isNotEmpty()) uiState.customerContactList.joinToString(", ") else PhoneNumberFormatter.formatForDisplay(uiState.applicantContact),
                            onValueChange = { },
                            label = { Text(stringResource(R.string.label_applicant_contact)) },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            trailingIcon = {
                                if (uiState.customerContactList.isNotEmpty()) {
                                    Box {
                                        var showCallDropdown by remember { mutableStateOf(false) }

                                        IconButton(
                                            onClick = {
                                                if (uiState.customerContactList.size > 1) {
                                                    showCallDropdown = true
                                                } else {
                                                    val formattedNumber = PhoneNumberFormatter.formatForDialing(uiState.customerContactList.first())
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$formattedNumber"))
                                                    context.startActivity(intent)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Phone,
                                                contentDescription = stringResource(R.string.cd_call_applicant),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showCallDropdown,
                                            onDismissRequest = { showCallDropdown = false }
                                        ) {
                                            uiState.customerContactList.forEach { number ->
                                                DropdownMenuItem(
                                                    text = { Text(number) },
                                                    onClick = {
                                                        showCallDropdown = false
                                                        val formattedNumber = PhoneNumberFormatter.formatForDialing(number)
                                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$formattedNumber"))
                                                        context.startActivity(intent)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else if (uiState.applicantContact.isNotEmpty()) {
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
                        FormSectionHeader(stringResource(R.string.section_additional_services))
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
                    // Show "Others" input field if "Others" (ID: 7) is selected for additional repairing
                    // Note: API returns numeric IDs as keys, e.g., {"7": "Others, specify"} or {"7": "ផ្សេងៗ"}
                    // Show "Others" input field if selected option contains "Other" or "ផ្សេងៗ" or logic implies it
                    val showOtherInput = uiState.additionalRepairingKeys.any { key ->
                        val label = uiState.containmentIssuesList[key]
                        label?.contains("Other", ignoreCase = true) == true || label?.contains("ផ្សេងៗ") == true || key == "7"
                    }
                    if (showOtherInput) {
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
                        FormSectionHeader(stringResource(R.string.section_payment_information))
                    }
                    item {
                        Column(
                            modifier = Modifier.bringIntoViewRequester(extraPaymentRequiredRequester)
                        ) {
                            YesNoRadioGroup(
                                label = stringResource(R.string.label_extra_payment_required),
                                selectedOption = uiState.extraPaymentRequired,
                                onOptionSelected = viewModel::onExtraPaymentRequiredChange,
                                isRequired = true
                            )
                            uiState.extraPaymentRequiredError?.let { error ->
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        }
                    }

                    // Extra Payment Amount (editable if required)
                    if (uiState.extraPaymentRequired == true) {
                        item {
                            OutlinedTextField(
                                value = uiState.amountOfExtraPayment,
                                onValueChange = { value -> 
                                    val validated = InputValidators.validateExtraPaymentAmount(value)
                                    viewModel.onAmountOfExtraPaymentChange(validated)
                                },
                                label = { Text(stringResource(R.string.label_amount_of_extra_payment)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Receiver Information (renamed from Customer Information)
                    item {
                        FormSectionHeader(stringResource(R.string.section_receiver_information))
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
                    val isNameEditable = !uiState.isReceiverSameAsApplicant || uiState.applicantName.isNullOrBlank()
                    val isContactEditable = !uiState.isReceiverSameAsApplicant || uiState.applicantContact.isNullOrBlank()
                    item {
                        ValidatedTextField(
                            value = uiState.serviceReceiverName, // ViewModel logic handles population
                            onValueChange = viewModel::onServiceReceiverNameChange,
                            label = stringResource(R.string.label_service_receiver_name),
                            errorMessage = uiState.serviceReceiverNameError,
                            enabled = isNameEditable,
                            isRequired = true,
                            modifier = Modifier.bringIntoViewRequester(serviceReceiverNameRequester)
                        )
                    }
                    item {
                        Column(modifier = Modifier.bringIntoViewRequester(serviceReceiverContactRequester)) {
                            if (uiState.isReceiverSameAsApplicant && uiState.customerContactList.size > 1) {
                                val contactOptions = uiState.customerContactList.associateWith { it }
                                DropdownMenuField(
                                    label = stringResource(R.string.label_service_receiver_contact),
                                    selectedValue = uiState.serviceReceiverContact,
                                    selectedKey = uiState.serviceReceiverContact,
                                    options = contactOptions,
                                    onOptionSelected = { _, value -> viewModel.onServiceReceiverContactChange(value) },
                                    modifier = Modifier.fillMaxWidth(),
                                    isRequired = true
                                )
                                uiState.serviceReceiverContactError?.let { error ->
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                    )
                                }
                            } else {
                                ValidatedPhoneNumberField(
                                    value = uiState.serviceReceiverContact,
                                    onValueChange = viewModel::onServiceReceiverContactChange,
                                    label = stringResource(R.string.label_service_receiver_contact),
                                    errorMessage = uiState.serviceReceiverContactError,
                                    enabled = isContactEditable,
                                    isRequired = true
                                )
                            }
                        }
                    }

                    // Scheduling Information
                    item {
                        FormSectionHeader(stringResource(R.string.section_scheduling_information))
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
                        Column(
                            modifier = Modifier.bringIntoViewRequester(needRescheduleRequester)
                        ) {
                            YesNoRadioGroup(
                                label = stringResource(R.string.label_need_reschedule),
                                selectedOption = uiState.needReschedule,
                                onOptionSelected = viewModel::onNeedRescheduleChange,
                                isRequired = true
                            )
                            uiState.needRescheduleError?.let { error ->
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        }
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
                                onClick = { viewModel.submitForm() },
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
                    postponeType = postponeData.postponeType,
                    postponeFrom = postponeData.postponeFrom,
                    postponeTo = postponeData.postponeTo,
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp)
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
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
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
