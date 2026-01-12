package com.innovative.smis.ui.features.emptyingscheduling

import com.innovative.smis.R

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.innovative.smis.ui.components.CheckboxWithLabel
import com.innovative.smis.ui.components.DatePickerField
import com.innovative.smis.ui.components.PhoneNumberField
import com.innovative.smis.ui.components.RadioGroup
import com.innovative.smis.ui.components.ReadOnlyTextField
import com.innovative.smis.ui.components.SectionHeader
import com.innovative.smis.ui.components.YesNoRadioGroup
import com.innovative.smis.ui.components.disabledTextFieldColors
import com.innovative.smis.ui.components.labelWithAsterisk
// Import your new components
import com.innovative.smis.ui.components.ValidatedTextField
import com.innovative.smis.ui.components.ValidatedPhoneNumberField

import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PhoneNumberFormatter
import com.innovative.smis.util.validation.InputValidators
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
    val listState = rememberLazyListState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // State for expandable sections
    var customerDetailsExpanded by remember { mutableStateOf(false) }
    var emptyingDetailsExpanded by remember { mutableStateOf(false) }
    var containmentDetailsExpanded by remember { mutableStateOf(false) }
    var paymentVisitDetailsExpanded by remember { mutableStateOf(false) }

    // Auto-scroll to first error field when validation fails
    LaunchedEffect(uiState.firstErrorField) {
        uiState.firstErrorField?.let { errorField ->
            val scrollToIndex = when (errorField) {
                "applicant_name", "applicant_contact" -> {
                    customerDetailsExpanded = true
                    1
                }
                "purpose_of_emptying", "purpose_of_emptying_other", "propose_emptying_date", "ever_emptied" -> {
                    emptyingDetailsExpanded = true
                    2
                }
                "extra_payment_required", "site_visit_required" -> {
                    paymentVisitDetailsExpanded = true
                    4
                }
                else -> 0
            }
            listState.animateScrollToItem(scrollToIndex)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveResult.collect { result ->
            val message = when (result) {
                is SaveResult.Success -> result.message
                is SaveResult.Error -> result.message
            }

            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("snackbar_message", message)

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
                            value = uiState.customerContactList.joinToString(", "),
                            onValueChange = { },
                            label = { Text(stringResource(R.string.label_customer_phone)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = disabledTextFieldColors(),
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
                                                contentDescription = "Call customer",
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
                                }
                            }
                        )

                        val isOnDemand = uiState.applicationType?.equals("On-Demand", ignoreCase = true) == true
                        if (!isOnDemand) {
                            CheckboxWithLabel(
                                label = stringResource(R.string.checkbox_applicant_same_as_customer),
                                checked = uiState.isApplicantSameAsCustomer,
                                onCheckedChange = viewModel::onApplicantSameAsCustomerChange
                            )
                        }

                        // --- OPTIMIZED LOGIC START ---
                        val isNameEditable = isOnDemand || !uiState.isApplicantSameAsCustomer || uiState.sanitationCustomerName.isNullOrBlank()
                        val isContactEditable = isOnDemand || !uiState.isApplicantSameAsCustomer || uiState.sanitationCustomerContact.isNullOrBlank()

                        // 1. Applicant Name (Using Reusable Component)
                        ValidatedTextField(
                            value = uiState.applicantName,
                            onValueChange = viewModel::onApplicantNameChange,
                            label = stringResource(R.string.label_applicant_name),
                            errorMessage = uiState.applicantNameError,
                            enabled = isNameEditable,
                            isRequired = true
                        )

                        // 2. Applicant Contact (Using Reusable Component)
                        if (uiState.isApplicantSameAsCustomer && uiState.customerContactList.size > 1) {
                            val contactOptions = uiState.customerContactList.associateWith { it }
                            DropdownMenuField(
                                label = stringResource(R.string.label_applicant_contact),
                                selectedValue = uiState.applicantContact,
                                options = contactOptions,
                                onValueSelected = viewModel::onApplicantContactChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = "Select Contact",
                                isRequired = true
                            )
                            uiState.applicantContactError?.let { error ->
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        } else {
                            ValidatedPhoneNumberField(
                                value = uiState.applicantContact,
                                onValueChange = viewModel::onApplicantContactChange,
                                label = stringResource(R.string.label_applicant_contact),
                                errorMessage = uiState.applicantContactError,
                                enabled = isContactEditable,
                                isRequired = true
                            )
                        }
                        // --- OPTIMIZED LOGIC END ---
                    }
                }

                item {
                    ExpandableSection(
                        title = stringResource(R.string.section_emptying_details),
                        isExpanded = emptyingDetailsExpanded,
                        onExpandedChange = { emptyingDetailsExpanded = it }
                    ) {
                        if (uiState.isPurposeOfEmptyingReadonly && !uiState.purposeOfEmptying.isNullOrBlank()) {
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
                            val filteredReasons = uiState.emptyingReasons.filterValues { value ->
                                !value.contains("Additional emptying to period service", ignoreCase = true) &&
                                        !value.equals("Scheduled", ignoreCase = true)
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                DropdownMenuField(
                                    label = stringResource(R.string.label_purpose_of_emptying_request),
                                    selectedValue = uiState.purposeOfEmptying ?: "",
                                    options = filteredReasons,
                                    onValueSelected = viewModel::onPurposeOfEmptyingChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = "Select purpose",
                                    isRequired = true
                                )
                                uiState.purposeOfEmptyingError?.let { error ->
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                    )
                                }
                            }
                        }

                        if (!uiState.isPurposeOfEmptyingReadonly &&
                            !uiState.purposeOfEmptying.isNullOrBlank() &&
                            uiState.purposeOfEmptying == "7") {
                            // You can also optimize this "Other" field later if desired
                            OutlinedTextField(
                                value = uiState.purposeOfEmptyingOther,
                                onValueChange = viewModel::onPurposeOfEmptyingOtherChange,
                                label = { Text(stringResource(R.string.label_please_specify_other_reason) + " *") },
                                isError = uiState.purposeOfEmptyingOtherError != null,
                                supportingText = uiState.purposeOfEmptyingOtherError?.let { { Text(it) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            DatePickerField(
                                label = stringResource(R.string.label_propose_emptying_date),
                                selectedDate = uiState.proposeEmptyingDate,
                                onDateSelected = viewModel::onProposeEmptyingDateChange,
                                isRequired = true
                            )
                            uiState.proposeEmptyingDateError?.let { error ->
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        }

                        YesNoRadioGroup(
                            label = stringResource(R.string.label_ever_emptied_before),
                            selectedOption = uiState.everEmptied,
                            onOptionSelected = viewModel::onEverEmptiedChange,
                            enabled = !uiState.isEverEmptiedReadonly,
                            isRequired = true
                        )
                        uiState.everEmptiedError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }

                        if (uiState.everEmptied == true) {
                            if (uiState.lastEmptiedYear != null) {
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
                                if (uiState.lastEmptiedDate.isBlank()) {
                                    if (!uiState.isEverEmptiedReadonly) {
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
                                            isFutureDateAllowed = false
                                        )

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
                                    }
                                } else {
                                    if (uiState.isEverEmptiedReadonly) {
                                        OutlinedTextField(
                                            value = uiState.lastEmptiedDate,
                                            onValueChange = { },
                                            label = { Text(stringResource(R.string.label_last_emptied_date_required)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = false,
                                            colors = disabledTextFieldColors()
                                        )
                                    } else {
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

                            if (uiState.notEmptiedBeforeReason == "7") {
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
                            onValueChange = { value ->
                                val validated = InputValidators.validateStorageTankSize(value)
                                viewModel.onSizeOfContainmentChange(validated)
                            },
                            label = { Text(stringResource(R.string.label_storage_tank_size)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.constructionYear?.toString() ?: "",
                            onValueChange = { value ->
                                val validated = InputValidators.validateConstructionYear(value)
                                if (validated.isEmpty()) {
                                    viewModel.onConstructionYearChange(null)
                                } else {
                                    validated.toIntOrNull()?.let { year ->
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
                                "Accessible" to stringResource(R.string.option_accessible),
                                "Not Accessible" to stringResource(R.string.option_not_accessible)
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

                        // Presence of Pumping Point
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(
                                text = stringResource(R.string.label_presence_pumping_point),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            val valYes = "Yes (Cover, Tube, Pierce)"
                            val valNo = "No (need to pierce the tank)"

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                                    .clickable { viewModel.onPumpingPointPresenceChange(valYes) }
                            ) {
                                RadioButton(
                                    selected = uiState.pumpingPointPresence == valYes,
                                    onClick = { viewModel.onPumpingPointPresenceChange(valYes) }
                                )
                                Text(
                                    text = valYes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 0.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                                    .clickable { viewModel.onPumpingPointPresenceChange(valNo) }
                            ) {
                                RadioButton(
                                    selected = uiState.pumpingPointPresence == valNo,
                                    onClick = { viewModel.onPumpingPointPresenceChange(valNo) }
                                )
                                Text(
                                    text = valNo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 0.dp)
                                )
                            }
                        }

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

                        if (uiState.containmentIssues == "99") {
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

                        if (uiState.extraPaymentRequired == true) {
                            OutlinedTextField(
                                value = uiState.extraPaymentAmount,
                                onValueChange = { value ->
                                    val validated = InputValidators.validateExtraPaymentAmount(value)
                                    viewModel.onExtraPaymentAmountChange(validated)
                                },
                                label = { Text(stringResource(R.string.label_amount_extra_payment_estimation)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        YesNoRadioGroup(
                            label = stringResource(R.string.label_site_visit_required),
                            selectedOption = uiState.siteVisitRequired,
                            onOptionSelected = viewModel::onSiteVisitRequiredChange,
                            isRequired = true
                        )
                        uiState.siteVisitRequiredError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

// ... (Rest of your helper functions: ExpandableSection, CheckboxWithLabel, etc. remain here) ...
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
    placeholder: String = "",
    isRequired: Boolean = false
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
            label = { Text(labelWithAsterisk(label, isRequired)) },
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