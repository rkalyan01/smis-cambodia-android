package com.innovative.smis.ui.features.emptyingscheduling

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.innovative.smis.R
import com.innovative.smis.ui.components.CheckboxWithLabel
import com.innovative.smis.ui.components.DatePickerField
import com.innovative.smis.ui.components.PhoneNumberField
import com.innovative.smis.ui.components.RadioGroup
import com.innovative.smis.ui.components.ReadOnlyTextField
import com.innovative.smis.ui.components.SectionHeader
import com.innovative.smis.ui.components.YesNoRadioGroup
import com.innovative.smis.ui.components.disabledTextFieldColors
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PhoneNumberFormatter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyingSchedulingFormScreen(
    navController: NavController,
    applicationId: Int?
) {
    val viewModel: EmptyingSchedulingFormViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    // State for expandable sections
    var customerDetailsExpanded by remember { mutableStateOf(false) }
    var emptyingDetailsExpanded by remember { mutableStateOf(false) }
    var containmentDetailsExpanded by remember { mutableStateOf(false) }
    var paymentVisitDetailsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.saveResult.collect { result ->
            val message = when (result) {
                is SaveResult.Success -> result.message
                is SaveResult.Error -> result.message
            }

            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("snackbar_message", message)

            // If this was a successful form submission, trigger list refresh on previous screen
            if (result is SaveResult.Success && result.shouldRefreshList) {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("should_refresh_list", true)
            }

            navController.popBackStack()
        }
    }

    val context = LocalContext.current

    LaunchedEffect(applicationId) {
        if (applicationId != null) {
            viewModel.loadApplicationDetails(applicationId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.nav_emptying_scheduling),style = MaterialTheme.typography.titleMedium)
                        // Sync status indicator
                        when (val loadingState = uiState.loadingState) {
                            is Resource.Success -> {
                                val syncStatus = loadingState.data?.syncStatus ?: ""
                                val statusText = when (syncStatus) {
                                    "DRAFT" -> stringResource(R.string.status_draft)
                                    "PENDING" -> stringResource(R.string.status_syncing)
                                    "FAILED" -> stringResource(R.string.status_sync_failed)
                                    "SYNCED" -> stringResource(R.string.status_synced)
                                    else -> ""
                                }
                                if (statusText.isNotEmpty()) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (syncStatus) {
                                            "DRAFT" -> MaterialTheme.colorScheme.onSurfaceVariant
                                            "PENDING" -> MaterialTheme.colorScheme.primary
                                            "FAILED" -> MaterialTheme.colorScheme.error
                                            "SYNCED" -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {}
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val loadingState = uiState.loadingState

        if (loadingState is Resource.Loading && loadingState.data == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Text(stringResource(R.string.message_loading_details), modifier = Modifier.padding(top = 60.dp))
            }
        } else if (loadingState is Resource.Error && loadingState.data == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.message_error_format, loadingState.message ?: ""))
                Spacer(Modifier.height(16.dp))
                Button(onClick = { applicationId?.let { viewModel.loadApplicationDetails(it) } }) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        } else {
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
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                item { 
                    ReadOnlyTextField(
                        label = stringResource(R.string.label_application_id), 
                        value = applicationId?.toString() ?: ""
                    ) 
                }
                
                item {
                    ExpandableSection(
                        title = stringResource(R.string.section_customer_details),
                        isExpanded = customerDetailsExpanded,
                        onExpandedChange = { customerDetailsExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.sanitationCustomerId ?: "",
                            onValueChange = { },
                            label = { Text(stringResource(R.string.label_sanitation_customer_id)) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = disabledTextFieldColors()
                        )
                        
                        OutlinedTextField(
                            value = uiState.sanitationCustomerName ?: "",
                            onValueChange = { },
                            label = { Text(stringResource(R.string.label_customer_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = disabledTextFieldColors()
                        )
                        
                        OutlinedTextField(
                            value = PhoneNumberFormatter.formatForDisplay(uiState.sanitationCustomerContact),
                            onValueChange = { },
                            label = { Text(stringResource(R.string.label_customer_phone)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = disabledTextFieldColors(),
                            trailingIcon = {
                                if (!uiState.sanitationCustomerContact.isNullOrEmpty()) {
                                    IconButton(
                                        onClick = {
                                            val formattedNumber = PhoneNumberFormatter.formatForDialing(uiState.sanitationCustomerContact)
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$formattedNumber"))
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = "Call customer",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        )
                        
                        // Only show checkbox if NOT "On-Demand"
                        val isOnDemand = uiState.applicationType?.equals("On-Demand", ignoreCase = true) == true
                        if (!isOnDemand) {
                            CheckboxWithLabel(
                                label = stringResource(R.string.checkbox_applicant_same_as_customer),
                                checked = uiState.isApplicantSameAsCustomer,
                                onCheckedChange = viewModel::onApplicantSameAsCustomerChange
                            )
                        }
                        
                        OutlinedTextField(
                            value = uiState.applicantName,
                            onValueChange = viewModel::onApplicantNameChange,
                            label = { Text(stringResource(R.string.label_applicant_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isOnDemand || !uiState.isApplicantSameAsCustomer,
                            colors = if (!isOnDemand && uiState.isApplicantSameAsCustomer) disabledTextFieldColors() else OutlinedTextFieldDefaults.colors()
                        )
                        
                        if (isOnDemand || !uiState.isApplicantSameAsCustomer) {
                            PhoneNumberField(
                                value = uiState.applicantContact,
                                onValueChange = viewModel::onApplicantContactChange,
                                label = stringResource(R.string.label_applicant_contact),
                                modifier = Modifier,
                                enabled = true,
                                isRequired = false
                            )
                        } else {
                            // Disabled state - show OutlinedTextField with disabled colors
                            OutlinedTextField(
                                value = uiState.applicantContact,
                                onValueChange = {},
                                label = { Text(stringResource(R.string.label_applicant_contact)) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = disabledTextFieldColors()
                            )
                        }
                    }
                }

                item {
                    ExpandableSection(
                        title = stringResource(R.string.section_emptying_details),
                        isExpanded = emptyingDetailsExpanded,
                        onExpandedChange = { emptyingDetailsExpanded = it }
                    ) {
                        // Purpose of Emptying Request - Read-only if already has value, else dropdown
                        if (uiState.isPurposeOfEmptyingReadonly && !uiState.purposeOfEmptying.isNullOrBlank()) {
                            // Show read-only field with existing value (displays ALL options including "Scheduled")
                            val displayValue = uiState.emptyingReasons[uiState.purposeOfEmptying] 
                                ?: uiState.purposeOfEmptying
                            
                            OutlinedTextField(
                                value = displayValue ?: "Not specified",
                                onValueChange = {},
                                label = { Text(stringResource(R.string.label_purpose_of_emptying)) },
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                colors = disabledTextFieldColors()
                            )
                        } else if (uiState.isLoadingDropdowns) {
                            OutlinedTextField(
                                value = "Loading...",
                                onValueChange = {},
                                label = { Text(stringResource(R.string.label_purpose_of_emptying)) },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // For new applications: Filter out "Scheduled" and "Additional emptying to period service"
                            val filteredReasons = uiState.emptyingReasons.filterValues { value ->
                                !value.contains("Additional emptying to period service", ignoreCase = true) &&
                                !value.equals("Scheduled", ignoreCase = true)
                            }
                            
                            DropdownMenuField(
                                label = stringResource(R.string.label_purpose_of_emptying_request),
                                selectedValue = uiState.purposeOfEmptying ?: "", // ✅ Convert null to "" only when passing to dropdown
                                options = filteredReasons,
                                onValueSelected = viewModel::onPurposeOfEmptyingChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = "Select purpose" // ✅ Show placeholder when nothing is selected
                            )
                        }
                        
                        // Show "Others" input field if "Others, specify" is selected
                        if (!uiState.isPurposeOfEmptyingReadonly && 
                            !uiState.purposeOfEmptying.isNullOrBlank() &&
                            uiState.emptyingReasons.any { it.value.contains("Others", ignoreCase = true) } &&
                            uiState.emptyingReasons.entries.find { it.value.contains("Others", ignoreCase = true) }?.key == uiState.purposeOfEmptying) {
                            OutlinedTextField(
                                value = uiState.purposeOfEmptyingOther,
                                onValueChange = viewModel::onPurposeOfEmptyingOtherChange,
                                label = { Text(stringResource(R.string.label_please_specify_other_reason)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        DatePickerField(
                            label = stringResource(R.string.label_propose_emptying_date),
                            selectedDate = uiState.proposeEmptyingDate,
                            onDateSelected = viewModel::onProposeEmptyingDateChange
                        )
                        
                        YesNoRadioGroup(
                            label = stringResource(R.string.label_ever_emptied_before),
                            selectedOption = uiState.everEmptied,
                            onOptionSelected = viewModel::onEverEmptiedChange
                        )
                        
                        if (uiState.everEmptied == true) {
                            if (uiState.lastEmptiedYear != null) {
                                // Show Last Emptied Date as read-only text field showing year in format 1-1-YYYY
                                val lastEmptiedDisplayText = "${uiState.lastEmptiedYear}-01-01"
                                
                                OutlinedTextField(
                                    value = lastEmptiedDisplayText,
                                    onValueChange = { },
                                    label = { Text(stringResource(R.string.label_last_emptied_date)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    colors = disabledTextFieldColors()
                                )
                            } else {
                                // Either/Or: Show Last Emptied Date OR Reason field
                                if (uiState.lastEmptiedDate.isBlank()) {
                                    // Show Date Picker when no date is selected
                                    val lastEmptiedDateMillis = if (uiState.lastEmptiedDate.isNotBlank()) {
                                        com.innovative.smis.util.helper.DateFormatManager
                                            .parseDisplayDate(context, uiState.lastEmptiedDate)
                                    } else null
                                    
                                    DatePickerField(
                                        label = stringResource(R.string.label_last_emptied_date_required),
                                        selectedDate = lastEmptiedDateMillis,
                                        onDateSelected = { millis ->
                                            val formattedDate = millis?.let {
                                                com.innovative.smis.util.helper.DateFormatManager
                                                    .formatTimestampForDisplay(context, it)
                                            } ?: ""
                                            viewModel.onLastEmptiedDateChange(formattedDate)
                                        },
                                        isFutureDateAllowed = false // Restrict to past dates only
                                    )
                                    
                                    // Reason dropdown when no date is selected
                                    if (uiState.isLoadingDropdowns) {
                                        OutlinedTextField(
                                            value = "Loading...",
                                            onValueChange = {},
                                            label = { Text(stringResource(R.string.label_reason_no_emptied_date)) },
                                            readOnly = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        DropdownMenuField(
                                            label = stringResource(R.string.label_reason_no_emptied_date),
                                            selectedValue = uiState.reasonForNoEmptiedDate,
                                            options = uiState.emptiedNoDateReasons,
                                            onValueSelected = viewModel::onReasonForNoEmptiedDateChange,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                } else {
                                    // Show selected date with clear button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        OutlinedTextField(
                                            value = uiState.lastEmptiedDate,
                                            onValueChange = { },
                                            label = { Text(stringResource(R.string.label_last_emptied_date_required)) },
                                            modifier = Modifier.weight(1f),
                                            readOnly = true
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(
                                            onClick = { viewModel.onLastEmptiedDateChange("") },
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        ) {
                                            Text(stringResource(R.string.action_clear))
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (uiState.everEmptied == false) {
                            if (uiState.isLoadingDropdowns) {
                                OutlinedTextField(
                                    value = "Loading...",
                                    onValueChange = {},
                                    label = { Text(stringResource(R.string.label_reason_not_emptied_before)) },
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                DropdownMenuField(
                                    label = stringResource(R.string.label_reason_if_not_emptied),
                                    selectedValue = uiState.notEmptiedBeforeReason,
                                    options = uiState.notEmptiedReasons,
                                    onValueSelected = viewModel::onNotEmptiedBeforeReasonChange,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            // Show "Others" input field if "Others, specify" is selected
                            if (uiState.notEmptiedReasons.any { it.value.contains("Others", ignoreCase = true) } &&
                                uiState.notEmptiedReasons.entries.find { it.value.contains("Others", ignoreCase = true) }?.key == uiState.notEmptiedBeforeReason) {
                                OutlinedTextField(
                                    value = uiState.notEmptiedReasonOther,
                                    onValueChange = viewModel::onNotEmptiedReasonOtherChange,
                                    label = { Text(stringResource(R.string.label_please_specify_other_reason)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                item {
                    ExpandableSection(
                        title = stringResource(R.string.section_containment_details),
                        isExpanded = containmentDetailsExpanded,
                        onExpandedChange = { containmentDetailsExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.sizeOfStorageTankM3 ?: "",
                            onValueChange = viewModel::onSizeOfContainmentChange,
                            label = { Text(stringResource(R.string.label_storage_tank_size)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = uiState.constructionYear?.toString() ?: "",
                            onValueChange = { value -> 
                                // Allow user to type freely, expansion happens in ViewModel
                                if (value.isEmpty()) {
                                    viewModel.onConstructionYearChange(null)
                                } else {
                                    value.toIntOrNull()?.let { year -> 
                                        viewModel.onConstructionYearChange(year)
                                    }
                                }
                            },
                            label = { Text(stringResource(R.string.label_construction_year_storage_tank)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        DropdownMenuField(
                            label = stringResource(R.string.label_accessible_desludging_vehicle),
                            selectedValue = uiState.accessibility ?: "",
                            options = mapOf(
                                "Accessible" to "Accessible",
                                "Not Accessible" to "Not Accessible"
                            ),
                            onValueSelected = viewModel::onAccessibilityChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "-- Accessibility --"
                        )
                        
                        RadioGroup(
                            title = stringResource(R.string.label_location_of_containment),
                            options = listOf(
                                stringResource(R.string.option_around_house), 
                                stringResource(R.string.option_ground_floor)
                            ),
                            selectedOption = uiState.locationOfContainment ?: "",
                            onOptionSelected = viewModel::onLocationOfContainmentChange
                        )
                        
                        YesNoRadioGroup(
                            label = stringResource(R.string.label_presence_pumping_point),
                            selectedOption = uiState.pumpingPointPresence,
                            onOptionSelected = viewModel::onPumpingPointPresenceChange
                        )
                        
                        // Dynamic dropdown for Experience issues with containment
                        if (uiState.isLoadingDropdowns) {
                            OutlinedTextField(
                                value = "Loading...",
                                onValueChange = {},
                                label = { Text(stringResource(R.string.label_experience_containment_issues)) },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            DropdownMenuField(
                                label = stringResource(R.string.label_experience_issues_containment),
                                selectedValue = uiState.containmentIssues,
                                options = uiState.containmentIssuesList,
                                onValueSelected = viewModel::onContainmentIssuesChange,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        // Show "Others" input field if "Others" is selected for containment issues
                        if (uiState.containmentIssuesList.any { it.value.contains("Others", ignoreCase = true) } &&
                            uiState.containmentIssuesList.entries.find { it.value.contains("Others", ignoreCase = true) }?.key == uiState.containmentIssues) {
                            OutlinedTextField(
                                value = uiState.containmentIssuesOther,
                                onValueChange = viewModel::onContainmentIssuesOtherChange,
                                label = { Text(stringResource(R.string.label_please_specify_other_issue)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    ExpandableSection(
                        title = stringResource(R.string.section_payment_visit_details),
                        isExpanded = paymentVisitDetailsExpanded,
                        onExpandedChange = { paymentVisitDetailsExpanded = it }
                    ) {
                        ReadOnlyTextField(
                            label = stringResource(R.string.label_free_service_under_pbc),
                            value = if (uiState.freeServiceUnderPBC == true) "Yes" else "No"
                        )
                        
                        ReadOnlyTextField(
                            label = stringResource(R.string.label_amount_regular_payment),
                            value = uiState.amountOfRegularPayment
                        )
                        
                        YesNoRadioGroup(
                            label = stringResource(R.string.label_extra_payment_required),
                            selectedOption = uiState.extraPaymentRequired,
                            onOptionSelected = viewModel::onExtraPaymentRequiredChange
                        )
                        
                        if (uiState.extraPaymentRequired == true) {
                            OutlinedTextField(
                                value = uiState.extraPaymentAmount,
                                onValueChange = viewModel::onExtraPaymentAmountChange,
                                label = { Text(stringResource(R.string.label_amount_extra_payment_estimation)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        YesNoRadioGroup(
                            label = stringResource(R.string.label_site_visit_required),
                            selectedOption = uiState.siteVisitRequired,
                            onOptionSelected = viewModel::onSiteVisitRequiredChange
                        )
                    }
                }

                // Submit button at bottom
                item {
                    Spacer(Modifier.height(24.dp))
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
                                Text(stringResource(R.string.action_saving))
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
                                Text(stringResource(R.string.action_submitting))
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



@Composable
fun ExpandableSection(
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

@Composable
fun disabledTextFieldColors() = OutlinedTextFieldDefaults.colors(
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)



@Composable
fun CheckboxWithLabel(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(8.dp))
        Text(text = label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuField(
    label: String,
    selectedValue: String,
    options: Map<String, String>,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDisplayText = options[selectedValue] ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedDisplayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = if (selectedDisplayText.isEmpty() && placeholder.isNotEmpty()) {
                { Text(placeholder) }
            } else null,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (key, value) ->
                DropdownMenuItem(
                    text = { Text(value) },
                    onClick = {
                        onValueSelected(key)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun MultiCheckboxGroup(
    title: String,
    options: List<String>,
    selectedOptions: Set<String>,
    onOptionSelected: (String, Boolean) -> Unit
) {
    Column {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        options.forEach { option ->
            CheckboxWithLabel(
                label = option,
                checked = selectedOptions.contains(option),
                onCheckedChange = { isChecked -> onOptionSelected(option, isChecked) }
            )
        }
    }
}
