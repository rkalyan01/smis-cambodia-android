package com.innovative.smis.ui.features.emptyingscheduling

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.innovative.smis.ui.components.CheckboxWithLabel
import com.innovative.smis.ui.components.DatePickerField
import com.innovative.smis.ui.components.RadioGroup
import com.innovative.smis.ui.components.ReadOnlyTextField
import com.innovative.smis.ui.components.SectionHeader
import com.innovative.smis.ui.components.YesNoRadioGroup
import com.innovative.smis.ui.components.disabledTextFieldColors
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.localization.LocalizationManager
import com.innovative.smis.util.localization.StringResources
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
    val currentLanguage = remember { LocalizationManager.getCurrentLanguage(context) }
    val languageCode = remember(currentLanguage) { LocalizationManager.getLanguageCode(currentLanguage) }

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
                        Text(StringResources.getString(StringResources.EMPTYING_SCHEDULING, languageCode),style = MaterialTheme.typography.titleMedium)
                        // Sync status indicator
                        when (val loadingState = uiState.loadingState) {
                            is Resource.Success -> {
                                val syncStatus = loadingState.data?.syncStatus ?: ""
                                val statusText = when (syncStatus) {
                                    "DRAFT" -> StringResources.getString(StringResources.DRAFT, languageCode)
                                    "PENDING" -> StringResources.getString(StringResources.SYNCING, languageCode)
                                    "FAILED" -> StringResources.getString(StringResources.SYNC_FAILED, languageCode)
                                    "SYNCED" -> StringResources.getString(StringResources.SYNCED, languageCode)
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
                Text("Loading details...", modifier = Modifier.padding(top = 60.dp))
            }
        } else if (loadingState is Resource.Error && loadingState.data == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Error: ${loadingState.message}")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { applicationId?.let { viewModel.loadApplicationDetails(it) } }) {
                    Text("Retry")
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
                        label = StringResources.getString(StringResources.APPLICATION_ID, languageCode), 
                        value = applicationId?.toString() ?: ""
                    ) 
                }
                
                item {
                    ExpandableSection(
                        title = "Customer Details",
                        isExpanded = customerDetailsExpanded,
                        onExpandedChange = { customerDetailsExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.sanitationCustomerName ?: "",
                            onValueChange = { },
                            label = { Text(StringResources.getString(StringResources.CUSTOMER_NAME, languageCode)) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = disabledTextFieldColors()
                        )
                        
                        OutlinedTextField(
                            value = uiState.sanitationCustomerContact ?: "",
                            onValueChange = { },
                            label = { Text(StringResources.getString(StringResources.CUSTOMER_PHONE, languageCode)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = disabledTextFieldColors()
                        )
                        
                        CheckboxWithLabel(
                            label = "Applicant is same as customer",
                            checked = uiState.isApplicantSameAsCustomer,
                            onCheckedChange = viewModel::onApplicantSameAsCustomerChange
                        )
                        
                        OutlinedTextField(
                            value = uiState.applicantName,
                            onValueChange = viewModel::onApplicantNameChange,
                            label = { Text("Applicant Name") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isApplicantSameAsCustomer,
                            colors = if (uiState.isApplicantSameAsCustomer) disabledTextFieldColors() else OutlinedTextFieldDefaults.colors()
                        )
                        
                        OutlinedTextField(
                            value = uiState.applicantContact,
                            onValueChange = viewModel::onApplicantContactChange,
                            label = { Text("Applicant Contact") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isApplicantSameAsCustomer,
                            colors = if (uiState.isApplicantSameAsCustomer) disabledTextFieldColors() else OutlinedTextFieldDefaults.colors()
                        )
                    }
                }

                item {
                    ExpandableSection(
                        title = "Emptying Details",
                        isExpanded = emptyingDetailsExpanded,
                        onExpandedChange = { emptyingDetailsExpanded = it }
                    ) {
                        // Dynamic dropdown for Purpose of Emptying Request
                        if (uiState.isLoadingDropdowns) {
                            OutlinedTextField(
                                value = "Loading...",
                                onValueChange = {},
                                label = { Text("Purpose of Emptying Request") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            DropdownMenuField(
                                label = "Purpose of Emptying Request",
                                selectedValue = uiState.purposeOfEmptying,
                                options = uiState.emptyingReasons,
                                onValueSelected = viewModel::onPurposeOfEmptyingChange,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        // Show "Others" input field if "Others, specify" is selected
                        if (uiState.emptyingReasons.any { it.value.contains("Others", ignoreCase = true) } &&
                            uiState.emptyingReasons.entries.find { it.value.contains("Others", ignoreCase = true) }?.key == uiState.purposeOfEmptying) {
                            OutlinedTextField(
                                value = uiState.purposeOfEmptyingOther,
                                onValueChange = viewModel::onPurposeOfEmptyingOtherChange,
                                label = { Text("Please specify other reason") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        DatePickerField(
                            label = "Propose Emptying Date",
                            selectedDate = uiState.proposeEmptyingDate,
                            onDateSelected = viewModel::onProposeEmptyingDateChange
                        )
                        
                        YesNoRadioGroup(
                            label = "Ever Emptied Before?",
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
                                    label = { Text("Last Emptied Date") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    colors = disabledTextFieldColors()
                                )
                            } else {
                                // Show Date Picker when lastEmptiedYear is null
                                DatePickerField(
                                    label = "Last Emptied Date",
                                    selectedDate = uiState.lastEmptiedDate,
                                    onDateSelected = viewModel::onLastEmptiedDateChange
                                )
                                
                                // Also show reason field if no date is selected
                                OutlinedTextField(
                                    value = uiState.reasonForNoEmptiedDate,
                                    onValueChange = viewModel::onReasonForNoEmptiedDateChange,
                                    label = { Text("Reason for no Emptied Date (if applicable)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        if (uiState.everEmptied == false) {
                            OutlinedTextField(
                                value = uiState.reasonForNoEmptiedDate,
                                onValueChange = viewModel::onReasonForNoEmptiedDateChange,
                                label = { Text("Reason if not emptied before") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        ReadOnlyTextField(
                            label = "Free Service Under PBC",
                            value = if (uiState.freeServiceUnderPBC == true) "Yes" else "No"
                        )
                    }
                }

                item {
                    ExpandableSection(
                        title = "Containment Details",
                        isExpanded = containmentDetailsExpanded,
                        onExpandedChange = { containmentDetailsExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.sizeOfStorageTankM3 ?: "",
                            onValueChange = viewModel::onSizeOfContainmentChange,
                            label = { Text("Size of Storage Tank (m³)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = uiState.constructionYear?.toString() ?: "",
                            onValueChange = { value -> 
                                value.toIntOrNull()?.let { year -> viewModel.onConstructionYearChange(year) }
                            },
                            label = { Text("Construction Year") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        YesNoRadioGroup(
                            label = "Accessibility",
                            selectedOption = uiState.accessibility,
                            onOptionSelected = viewModel::onAccessibilityChange
                        )
                        
                        RadioGroup(
                            title = "Location of Containment",
                            options = listOf("Around the house", "Ground floor"),
                            selectedOption = uiState.locationOfContainment ?: "",
                            onOptionSelected = viewModel::onLocationOfContainmentChange
                        )
                        
                        YesNoRadioGroup(
                            label = "Presence of Pumping Point",
                            selectedOption = uiState.pumpingPointPresence,
                            onOptionSelected = viewModel::onPumpingPointPresenceChange
                        )
                        
                        // Dynamic dropdown for Experience issues with containment
                        if (uiState.isLoadingDropdowns) {
                            OutlinedTextField(
                                value = "Loading...",
                                onValueChange = {},
                                label = { Text("Experience issues with containment?") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            DropdownMenuField(
                                label = "Experience issues with containment?",
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
                                label = { Text("Please specify other issue") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    ExpandableSection(
                        title = "Payment & Visit Details",
                        isExpanded = paymentVisitDetailsExpanded,
                        onExpandedChange = { paymentVisitDetailsExpanded = it }
                    ) {
                        YesNoRadioGroup(
                            label = StringResources.getString(StringResources.EXTRA_PAYMENT_REQUIRED, languageCode),
                            selectedOption = uiState.extraPaymentRequired,
                            onOptionSelected = viewModel::onExtraPaymentRequiredChange
                        )
                        
                        if (uiState.extraPaymentRequired == true) {
                            OutlinedTextField(
                                value = uiState.extraPaymentAmount,
                                onValueChange = viewModel::onExtraPaymentAmountChange,
                                label = { Text("Amount of Extra Payment (Estimation)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        YesNoRadioGroup(
                            label = StringResources.getString(StringResources.SITE_VISIT_REQUIRED, languageCode),
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
                            Text(StringResources.getString(StringResources.SUBMITTING, languageCode))
                        } else {
                            Text(StringResources.getString(StringResources.SUBMIT_FORM, languageCode))
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
                            Text(StringResources.getString(StringResources.SAVING, languageCode))
                        } else {
                            Text(StringResources.getString(StringResources.SAVE_AS_DRAFT, languageCode))
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
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Column {
            // Header section with grey background
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
            
            // Content section with white background
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
fun DatePickerField(
    label: String,
    selectedDate: Long?,
    onDateSelected: (Long?) -> Unit,
    isFutureDateAllowed: Boolean = true,
    enabled: Boolean = true
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = SimpleDateFormat("MMM dd, yy", Locale.getDefault())
    val dateText = selectedDate?.let { formatter.format(Date(it)) } ?: ""

    OutlinedTextField(
        value = dateText,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(
                onClick = { showDatePicker = true },
                enabled = enabled
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
            }
        },
        colors = if (!enabled) disabledTextFieldColors() else OutlinedTextFieldDefaults.colors()
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate,
            selectableDates = remember(isFutureDateAllowed) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return if (isFutureDateAllowed) {
                            utcTimeMillis >= Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        } else {
                            true
                        }
                    }
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuField(
    label: String,
    selectedValue: String,
    options: Map<String, String>,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier
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
