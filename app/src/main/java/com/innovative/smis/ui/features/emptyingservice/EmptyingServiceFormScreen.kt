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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.innovative.smis.R
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
import com.innovative.smis.ui.components.PhoneNumberField
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PhoneNumberFormatter
import com.innovative.smis.util.validation.InputValidators
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester

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
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // BringIntoViewRequesters for scroll-to-error functionality
    val fieldRequesters = remember {
        mapOf(
            "serviceReceiverName" to BringIntoViewRequester(),
            "serviceReceiverContact" to BringIntoViewRequester(),
            "emptiedDate" to BringIntoViewRequester(),
            "startTime" to BringIntoViewRequester(),
            "endTime" to BringIntoViewRequester(),
            "additionalTripRequired" to BringIntoViewRequester(),
            "desludgingVehicle" to BringIntoViewRequester(),
            "sludgeType" to BringIntoViewRequester(),
            "additionalRepairing" to BringIntoViewRequester(),
            "extraCost" to BringIntoViewRequester(),
            "receiptNumber" to BringIntoViewRequester(),
            "receiptImage" to BringIntoViewRequester()
        )
    }

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
    
    // Auto-scroll to first error field when validation fails
    LaunchedEffect(uiState.firstErrorField) {
        uiState.firstErrorField?.let { errorField ->
            // Auto-expand the relevant section based on error field
            when (errorField) {
                "serviceReceiverName", "serviceReceiverContact" -> serviceDetailsExpanded = true
                "emptiedDate", "startTime", "endTime", "additionalTripRequired" -> serviceDetailsExpanded = true
                "desludgingVehicle", "sludgeType", "additionalRepairing" -> vehicleDetailsExpanded = true
                "extraCost", "receiptNumber", "receiptImage" -> paymentDocumentationExpanded = true
            }
            
            // Scroll to the error field using BringIntoViewRequester
            fieldRequesters[errorField]?.let { requester ->
                coroutineScope.launch {
                    requester.bringIntoView()
                }
            }
            
            // Clear the error field indicator after scrolling
            viewModel.clearFirstErrorField()
        }
    }
    
    // Auto-expand sections when validation errors occur
    LaunchedEffect(
        uiState.serviceReceiverNameError,
        uiState.serviceReceiverContactError,
        uiState.emptiedDateError,
        uiState.startTimeError,
        uiState.endTimeError,
        uiState.additionalTripRequiredError,
        uiState.desludgingVehicleIdError,
        uiState.sludgeTypeError,
        uiState.additionalRepairingError,
        uiState.extraCostError,
        uiState.receiptNumberError,
        uiState.receiptImageError
    ) {
        if (uiState.serviceReceiverNameError != null || uiState.serviceReceiverContactError != null ||
            uiState.emptiedDateError != null || uiState.startTimeError != null || uiState.endTimeError != null ||
            uiState.additionalTripRequiredError != null) {
            serviceDetailsExpanded = true
        }
        if (uiState.desludgingVehicleIdError != null || uiState.sludgeTypeError != null ||
            uiState.additionalRepairingError != null) {
            vehicleDetailsExpanded = true
        }
        if (uiState.extraCostError != null || uiState.receiptNumberError != null || uiState.receiptImageError != null) {
            paymentDocumentationExpanded = true
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
        title = stringResource(R.string.title_loading_application),
        message = stringResource(R.string.message_loading_application)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.form_emptying_service_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
                            navController.navigate("containment_form/$applicationId/${uiState.sanitationCustomerId ?: ""}")
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
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
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
            // Application ID - Standalone at top (not in collapsible section)
            item {
                ReadOnlyTextField(
                    label = stringResource(R.string.label_application_id),
                    value = applicationId.toString()
                )
            }

            // Applicant Details Section
            item {
                CollapsibleSection(
                    title = stringResource(R.string.section_applicant_details),
                    isExpanded = applicantDetailsExpanded,
                    onToggle = { applicantDetailsExpanded = !applicantDetailsExpanded }
                ) {
                    // Sanitation Customer ID - disabled field
                    ReadOnlyTextField(
                        label = stringResource(R.string.label_sanitation_customer_id),
                        value = uiState.sanitationCustomerId ?: ""
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = uiState.applicantName,
                        onValueChange = { },
                        label = { Text(stringResource(R.string.label_applicant_name)) },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = PhoneNumberFormatter.formatForDisplay(uiState.applicantContact),
                        onValueChange = { },
                        label = { Text(stringResource(R.string.label_applicant_contact)) },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(),
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
                    Spacer(modifier = Modifier.height(16.dp))

                    CheckboxField(
                        label = stringResource(R.string.label_service_receiver_same_as_applicant),
                        checked = uiState.isServiceReceiverSameAsApplicant,
                        onCheckedChange = viewModel::onServiceReceiverSameAsApplicantChange
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.serviceReceiverName,
                        onValueChange = viewModel::onServiceReceiverNameChange,
                        label = { Text(stringResource(R.string.label_service_receiver_name) + " *") },
                        enabled = !uiState.isServiceReceiverSameAsApplicant,
                        isError = uiState.serviceReceiverNameError != null,
                        supportingText = uiState.serviceReceiverNameError?.let { { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(fieldRequesters["serviceReceiverName"]!!),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(fieldRequesters["serviceReceiverContact"]!!)
                    ) {
                        PhoneNumberField(
                            value = uiState.serviceReceiverContact,
                            onValueChange = viewModel::onServiceReceiverContactChange,
                            label = stringResource(R.string.label_service_receiver_contact),
                            modifier = Modifier,
                            enabled = !uiState.isServiceReceiverSameAsApplicant,
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
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Customer Type Dropdown
                    DropdownField(
                        label = stringResource(R.string.label_customer_type),
                        options = uiState.customerTypeOptions.values.toList(),
                        selectedValue = uiState.customerTypeOptions[uiState.customerType] ?: "",
                        onValueSelected = { selectedDisplayValue ->
                            // Find the key for the selected display value
                            val selectedKey = uiState.customerTypeOptions.entries
                                .find { it.value == selectedDisplayValue }?.key ?: ""
                            viewModel.onCustomerTypeChange(selectedKey)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Show "Other Customer Type" field if "Other" is selected
                    // Check display value for both English ("other") and Khmer ("ផ្សេង")
                    val customerTypeDisplay = uiState.customerTypeOptions[uiState.customerType] ?: ""
                    if (customerTypeDisplay.contains("other", ignoreCase = true) || 
                        customerTypeDisplay.contains("ផ្សេង")) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = uiState.otherCustomerType,
                            onValueChange = viewModel::onOtherCustomerTypeChange,
                            label = { Text(stringResource(R.string.label_other_customer_type)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Service Details Section
            item {
                CollapsibleSection(
                    title = stringResource(R.string.section_service_details),
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
                        label = { Text(stringResource(R.string.label_emptied_date) + " *") },
                        readOnly = true,
                        isError = uiState.emptiedDateError != null,
                        supportingText = uiState.emptiedDateError?.let { { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(fieldRequesters["emptiedDate"]!!),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TimePickerField(
                        label = stringResource(R.string.label_start_time) + " *",
                        value = uiState.startTime,
                        onValueChange = viewModel::onStartTimeChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(fieldRequesters["startTime"]!!),
                        error = uiState.startTimeError
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TimePickerField(
                        label = stringResource(R.string.label_end_time) + " *",
                        value = uiState.endTime,
                        onValueChange = viewModel::onEndTimeChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(fieldRequesters["endTime"]!!),
                        error = uiState.endTimeError,
                        minTime = uiState.startTime.takeIf { it.isNotEmpty() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    RadioButtonGroupField(
                        label = stringResource(R.string.label_additional_trip_required) + " *",
                        options = listOf(
                            stringResource(R.string.label_yes),
                            stringResource(R.string.label_no)
                        ),
                        selectedValue = uiState.additionalTripRequired.replaceFirstChar { it.uppercase() },
                        onValueSelected = { value -> viewModel.onAdditionalTripRequiredChange(value.lowercase()) },
                        error = uiState.additionalTripRequiredError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(fieldRequesters["additionalTripRequired"]!!)
                    )
                }
            }

            // Vehicle & Sludge Details Section
            item {
                CollapsibleSection(
                    title = stringResource(R.string.section_vehicle_sludge_details),
                    isExpanded = vehicleDetailsExpanded,
                    onToggle = { vehicleDetailsExpanded = !vehicleDetailsExpanded }
                ) {
                    // Desludging Vehicle ID - Dropdown from API (Required field)
                    DropdownField(
                        label = stringResource(R.string.label_desludging_vehicle_id) + " *",
                        selectedValue = uiState.selectedVehicleLicensePlate,
                        options = uiState.vehicleOptions.map { it.type },
                        onValueSelected = viewModel::onDesludgingVehicleIdChange,
                        error = uiState.desludgingVehicleIdError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(fieldRequesters["desludgingVehicle"]!!)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Sludge Type - Radio buttons
                    RadioButtonGroupField(
                        label = stringResource(R.string.label_sludge_type) + " *",
                        options = listOf(
                            stringResource(R.string.option_mixed),
                            stringResource(R.string.option_not_mixed)
                        ),
                        selectedValue = uiState.sludgeType,
                        onValueSelected = viewModel::onSludgeTypeChange,
                        error = uiState.sludgeTypeError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(fieldRequesters["sludgeType"]!!)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Type of Sludge - Show only when "Mixed" is selected (handles both English and Khmer values)
                    if (uiState.sludgeType == "Mixed" || uiState.sludgeType == "លាយ") {
                        RadioButtonGroupField(
                            label = stringResource(R.string.label_type_of_sludge),
                            options = listOf(
                                stringResource(R.string.option_processing_food),
                                stringResource(R.string.option_oil_and_fat),
                                stringResource(R.string.option_content_of_fuel)
                            ),
                            selectedValue = uiState.typeOfSludge,
                            onValueSelected = viewModel::onTypeOfSludgeChange,
                            error = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    RadioButtonGroupField(
                        label = stringResource(R.string.label_pumping_point_type),
                        options = listOf(
                            stringResource(R.string.option_cover),
                            stringResource(R.string.option_tube),
                            stringResource(R.string.option_pierce)
                        ),
                        selectedValue = uiState.pumpingPointType,
                        onValueSelected = viewModel::onPumpingPointTypeChange,
                        error = uiState.pumpingPointTypeError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Additional Repairing in Emptying - Multi-select Checkboxes
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(fieldRequesters["additionalRepairing"]!!)
                    ) {
                        MultiSelectCheckboxGroup(
                            label = stringResource(R.string.label_additional_repairing_in_emptying),
                            options = uiState.additionalRepairingOptions,
                            selectedKeys = uiState.additionalRepairingKeys,
                            onSelectionChange = viewModel::onAdditionalRepairingChange,
                            enabled = !uiState.isAdditionalRepairingReadonly,
                            modifier = Modifier.fillMaxWidth(),
                            isRequired = true
                        )
                        uiState.additionalRepairingError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }
                    
                    // Show "Other Additional Repairing" field if "Others" (ID: 7) is selected
                    // Note: API returns numeric IDs as keys, e.g., {"7": "Others, specify"} or {"7": "ផ្សេងៗ"}
                    if (uiState.additionalRepairingKeys.contains("7")) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = uiState.otherAdditionalRepairing,
                            onValueChange = viewModel::onOtherAdditionalRepairingChange,
                            label = { Text(stringResource(R.string.label_other_additional_repairing)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Payment & Documentation Section
            item {
                CollapsibleSection(
                    title = stringResource(R.string.section_payment_documentation),
                    isExpanded = paymentDocumentationExpanded,
                    onToggle = { paymentDocumentationExpanded = !paymentDocumentationExpanded }
                ) {
                    ReadOnlyTextField(
                        label = stringResource(R.string.label_free_under_pbc),
                        value = if (uiState.freeUnderPBC) "Yes" else "No"
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Regular Cost - readonly when loaded from API
                    if (uiState.isRegularCostReadonly) {
                        ReadOnlyTextField(
                            label = stringResource(R.string.label_amount_regular_cost),
                            value = uiState.regularCost
                        )
                    } else {
                        OutlinedTextField(
                            value = uiState.regularCost,
                            onValueChange = viewModel::onRegularCostChange,
                            label = { Text(stringResource(R.string.label_amount_of_regular_cost)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.extraCost,
                        onValueChange = { value ->
                            if (!uiState.isExtraCostReadonly) {
                                val validated = InputValidators.validateExtraPaymentAmount(value)
                                viewModel.onExtraCostChange(validated)
                            }
                        },
                        label = { Text(stringResource(R.string.label_amount_of_extra_cost)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        readOnly = uiState.isExtraCostReadonly,
                        enabled = !uiState.isExtraCostReadonly,
                        isError = uiState.extraCostError != null,
                        supportingText = uiState.extraCostError?.let { { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(fieldRequesters["extraCost"]!!)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Only show Receipt Number and Receipt Image when Additional Trip Required is "no"
                    if (uiState.additionalTripRequired == "no") {
                        OutlinedTextField(
                            value = uiState.receiptNumber,
                            onValueChange = viewModel::onReceiptNumberChange,
                            label = { Text(stringResource(R.string.label_receipt_number)) },
                            isError = uiState.receiptNumberError != null,
                            supportingText = uiState.receiptNumberError?.let { { Text(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(fieldRequesters["receiptNumber"]!!)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Image Upload Components
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(fieldRequesters["receiptImage"]!!)
                        ) {
                            ImagePickerComponent(
                                label = stringResource(R.string.label_receipt_image),
                                selectedImageUri = if (uiState.receiptImage.isNotBlank()) Uri.parse(uiState.receiptImage) else null,
                                onImageSelected = { uri -> viewModel.onReceiptImageSelected(uri?.toString()) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            uiState.receiptImageError?.let { error ->
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    ImagePickerComponent(
                        label = stringResource(R.string.label_picture_of_emptying),
                        selectedImageUri = if (uiState.pictureOfEmptying.isNotBlank()) Uri.parse(uiState.pictureOfEmptying) else null,
                        onImageSelected = { uri -> viewModel.onEmptyingImageSelected(uri?.toString()) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.comments,
                        onValueChange = viewModel::onCommentsChange,
                        label = { Text(stringResource(R.string.label_comments)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // GPS Location - always show
                    // Check if coordinates already exist
                    val hasCoordinates = uiState.latitude != null && uiState.longitude != null

                    if (hasCoordinates) {
                        // Show as disabled read-only fields when coordinates exist
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReadOnlyTextField(
                                label = stringResource(R.string.label_latitude),
                                value = String.format("%.6f", uiState.latitude ?: 0.0),
                                modifier = Modifier.weight(1f)
                            )
                            ReadOnlyTextField(
                                label = stringResource(R.string.label_longitude),
                                value = String.format("%.6f", uiState.longitude ?: 0.0),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Show editable fields and buttons when no coordinates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.latitude?.toString() ?: "",
                                onValueChange = { },
                                label = { Text(stringResource(R.string.label_latitude)) },
                                readOnly = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.longitude?.toString() ?: "",
                                onValueChange = { },
                                label = { Text(stringResource(R.string.label_longitude)) },
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
                                Text(stringResource(R.string.button_capture_gps))
                            }
                            
                            OutlinedButton(
                                onClick = { 
                                    showMapBottomSheet = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.button_update_map))
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
                        Text(stringResource(R.string.button_save_draft))
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
                            Text(stringResource(R.string.button_submit))
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
                            Text(stringResource(R.string.action_cancel))
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
                            Text(stringResource(R.string.action_ok))
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

