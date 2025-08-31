package com.innovative.smis.ui.features.emptying

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.innovative.smis.data.model.EmptyingFormState
import com.innovative.smis.data.model.LocationOfContainment
import com.innovative.smis.data.model.PumpingPointPresence
import com.innovative.smis.ui.components.DatePickerField
import com.innovative.smis.ui.components.DropdownField
import com.innovative.smis.ui.components.LoadingButton
import com.innovative.smis.ui.components.OutlinedTextFieldWithError
import com.innovative.smis.ui.components.RadioButtonGroup
import com.innovative.smis.ui.components.SectionHeader
import com.innovative.smis.util.localization.LocalizationManager
import com.innovative.smis.util.localization.StringResources
import androidx.compose.ui.platform.LocalContext
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyingFormScreen(
    navController: NavController,
    applicationId: String?,
    viewModel: EmptyingFormViewModel = koinViewModel()
) {
    val state by viewModel.formState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(applicationId) {
        if (!applicationId.isNullOrEmpty()) {
            viewModel.loadApplicationData(applicationId)
        }
        viewModel.loadFormOptions()
    }

    val context = LocalContext.current
    val currentLanguage = remember { LocalizationManager.getCurrentLanguage(context) }
    val languageCode = remember(currentLanguage) { LocalizationManager.getLanguageCode(currentLanguage) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = StringResources.getString(StringResources.EMPTYING_REQUEST_FORM, languageCode),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            EmptyingFormContent(
                state = state,
                onStateChange = viewModel::updateFormState,
                onSubmit = { viewModel.submitForm(applicationId) },
                languageCode = languageCode
            )
        }

        state.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
            ) {
                Text(
                    text = error,
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyingFormContent(
    state: EmptyingFormState,
    onStateChange: (EmptyingFormState) -> Unit,
    onSubmit: () -> Unit,
    languageCode: String
) {
    // Auto-generated Application Date
    SectionHeader(StringResources.getString(StringResources.APPLICATION_INFORMATION, languageCode))
    
    OutlinedTextField(
        value = state.applicationDate.ifEmpty { 
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        },
        onValueChange = { },
        label = { Text(StringResources.getString(StringResources.APPLICATION_DATE, languageCode)) },
        enabled = false,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(16.dp))

    SectionHeader("Sanitation Customer Details")
    
    OutlinedTextField(
        value = state.sanitationCustomerName,
        onValueChange = { },
        label = { Text("Sanitation Customer Name") },
        enabled = false,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    OutlinedTextField(
        value = state.sanitationCustomerContact,
        onValueChange = { },
        label = { Text("Sanitation Customer Contact") },
        enabled = false,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    OutlinedTextField(
        value = state.sanitationCustomerAddress,
        onValueChange = { },
        label = { Text("Sanitation Customer Address") },
        enabled = false,
        maxLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(16.dp))

    SectionHeader("Applicant Details")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = state.isSamePersonAsCustomer,
            onCheckedChange = { isChecked ->
                onStateChange(
                    if (isChecked) {
                        state.copy(
                            isSamePersonAsCustomer = true,
                            applicantName = state.sanitationCustomerName,
                            applicantContact = state.sanitationCustomerContact
                        )
                    } else {
                        state.copy(
                            isSamePersonAsCustomer = false,
                            applicantName = "",
                            applicantContact = ""
                        )
                    }
                )
            }
        )
        Text(
            text = "Same person as Sanitation Customer",
            modifier = Modifier.padding(start = 8.dp)
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    OutlinedTextFieldWithError(
        value = state.applicantName,
        onValueChange = { onStateChange(state.copy(applicantName = it)) },
        label = "Applicant Name",
        error = state.applicantNameError,
        enabled = !state.isSamePersonAsCustomer,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    OutlinedTextFieldWithError(
        value = state.applicantContact,
        onValueChange = { onStateChange(state.copy(applicantContact = it)) },
        label = "Applicant Contact",
        error = state.applicantContactError,
        enabled = !state.isSamePersonAsCustomer,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(16.dp))

    SectionHeader("Request Details")
    
    com.innovative.smis.ui.components.DropdownField(
        label = "Purpose of Emptying Request",
        options = state.purposeOptions.map { it.type },
        selectedValue = state.purposeOfEmptyingRequest,
        onValueSelected = { selectedPurpose: String ->
            onStateChange(state.copy(purposeOfEmptyingRequest = selectedPurpose))
        },
        modifier = Modifier.fillMaxWidth()
    )
    
    if (state.purposeOfEmptyingRequest.contains("other", ignoreCase = true)) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.otherEmptyingPurpose,
            onValueChange = { onStateChange(state.copy(otherEmptyingPurpose = it)) },
            label = { Text("Please specify") },
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    DatePickerField(
        label = "Propose Emptying Date",
        selectedDate = state.proposeEmptyingDate,
        onDateSelected = { onStateChange(state.copy(proposeEmptyingDate = it)) },
        minDate = LocalDate.now(),
        modifier = Modifier.fillMaxWidth()
    )
    
    state.proposeEmptyingDateError?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
    }
    
    Spacer(modifier = Modifier.height(16.dp))

    SectionHeader("Emptying History")
    
    RadioButtonGroup(
        title = "Ever Emptied",
        options = listOf("Yes", "No"),
        selectedValue = when (state.everEmptied) {
            true -> "Yes"
            false -> "No"
            null -> ""
        },
        onValueSelected = { value ->
            val everEmptied = when (value) {
                "Yes" -> true
                "No" -> false
                else -> null
            }
            onStateChange(
                state.copy(
                    everEmptied = everEmptied,
                    lastEmptiedDate = if (everEmptied == false) "" else state.lastEmptiedDate,
                    notEmptiedBeforeReason = if (everEmptied == true) "" else state.notEmptiedBeforeReason
                )
            )
        }
    )
    
    if (state.everEmptied == true) {
        Spacer(modifier = Modifier.height(8.dp))
        DatePickerField(
            label = "Last Emptied Date",
            selectedDate = state.lastEmptiedDate,
            onDateSelected = { onStateChange(state.copy(lastEmptiedDate = it)) },
            maxDate = LocalDate.now(), // Cannot be in future
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    if (state.everEmptied == false) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.notEmptiedBeforeReason,
            onValueChange = { onStateChange(state.copy(notEmptiedBeforeReason = it)) },
            label = { Text("Reason for Not Being Emptied Before") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    if (state.everEmptied == true && state.lastEmptiedDate.isEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.reasonForNoEmptiedDate,
            onValueChange = { onStateChange(state.copy(reasonForNoEmptiedDate = it)) },
            label = { Text("Reason for No Emptied Date") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    Spacer(modifier = Modifier.height(16.dp))

    SectionHeader("Service Details")
    
    RadioButtonGroup(
        title = "Free Service Under PBC",
        options = listOf("Yes", "No"),
        selectedValue = when (state.freeServiceUnderPbc) {
            true -> "Yes"
            false -> "No"
            null -> ""
        },
        onValueSelected = { value ->
            val freeService = when (value) {
                "Yes" -> true
                "No" -> false
                else -> null
            }
            onStateChange(state.copy(freeServiceUnderPbc = freeService))
        }
    )
    
    Spacer(modifier = Modifier.height(16.dp))

    SectionHeader("Containment Information")
    
    OutlinedTextField(
        value = state.sizeOfContainmentM3,
        onValueChange = { onStateChange(state.copy(sizeOfContainmentM3 = it)) },
        label = { Text("Size of Containment (m³)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    OutlinedTextField(
        value = state.yearOfInstallation,
        onValueChange = { onStateChange(state.copy(yearOfInstallation = it)) },
        label = { Text("Year of Installation") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    RadioButtonGroup(
        title = "Containment Accessibility",
        options = listOf("Accessible", "Not Accessible"),
        selectedValue = when (state.containmentAccessibility) {
            true -> "Accessible"
            false -> "Not Accessible"
            null -> ""
        },
        onValueSelected = { value ->
            val accessibility = when (value) {
                "Accessible" -> true
                "Not Accessible" -> false
                else -> null
            }
            onStateChange(state.copy(containmentAccessibility = accessibility))
        }
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = "Location of Containment",
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    
    Column(modifier = Modifier.selectableGroup()) {
        LocationOfContainment.values().forEach { location ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = state.locationOfContainment == location,
                    onClick = { onStateChange(state.copy(locationOfContainment = location)) }
                )
                Text(
                    text = location.displayName,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        text = "Presence of Pumping Point",
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    
    Column(modifier = Modifier.selectableGroup()) {
        PumpingPointPresence.values().forEach { presence ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = state.presenceOfPumpingPoint == presence,
                    onClick = { onStateChange(state.copy(presenceOfPumpingPoint = presence)) }
                )
                Text(
                    text = presence.displayName,
                    modifier = Modifier.padding(start = 8.dp),
                    fontSize = 14.sp
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))

    SectionHeader("Issues and Additional Information")
    
    com.innovative.smis.ui.components.DropdownField(
        label = "Experience Issues with Containment",
        options = state.experienceIssuesOptions.map { it.type },
        selectedValue = state.experienceIssuesWithContainment,
        onValueSelected = { onStateChange(state.copy(experienceIssuesWithContainment = it)) },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    RadioButtonGroup(
        title = "Extra Payment Required?",
        options = listOf("Yes", "No"),
        selectedValue = when (state.extraPaymentRequired) {
            true -> "Yes"
            false -> "No"
            null -> ""
        },
        onValueSelected = { value ->
            val extraPayment = when (value) {
                "Yes" -> true
                "No" -> false
                else -> null
            }
            onStateChange(state.copy(extraPaymentRequired = extraPayment))
        }
    )
    
    if (state.extraPaymentRequired == true) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.amountOfExtraPayment,
            onValueChange = { onStateChange(state.copy(amountOfExtraPayment = it)) },
            label = { Text("Amount of Extra Payment (Estimation)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    RadioButtonGroup(
        title = "Site Visit Required?",
        options = listOf("Yes", "No"),
        selectedValue = when (state.siteVisitRequired) {
            true -> "Yes"
            false -> "No"
            null -> ""
        },
        onValueSelected = { value ->
            val siteVisit = when (value) {
                "Yes" -> true
                "No" -> false
                else -> null
            }
            onStateChange(state.copy(siteVisitRequired = siteVisit))
        }
    )
    
    Spacer(modifier = Modifier.height(24.dp))

    OutlinedButton(
        onClick = { /* TODO: Navigate to Containments screen */ },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Containments")
    }
    
    Spacer(modifier = Modifier.height(16.dp))

    LoadingButton(
        text = "Submit Emptying Form",
        isLoading = state.isLoading,
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(32.dp))
}