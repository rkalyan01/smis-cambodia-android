package com.innovative.smis.ui.features.buildingsurvey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import com.innovative.smis.data.model.response.*
import com.innovative.smis.data.model.BuildingSurveyFormState
import com.innovative.smis.data.model.RespondentGender
import com.innovative.smis.data.model.WaterSupply
import com.innovative.smis.data.model.VacutugAccessible
import com.innovative.smis.data.model.SanitationSystem
import com.innovative.smis.data.model.Technology
import com.innovative.smis.ui.components.*
import com.innovative.smis.ui.components.BooleanField
import com.innovative.smis.ui.components.DatePickerField
import com.innovative.smis.ui.components.LoadingButton
import com.innovative.smis.util.validation.FormValidation
import com.innovative.smis.util.localization.LocalizationManager
import com.innovative.smis.util.localization.StringResources
import androidx.compose.ui.platform.LocalContext
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingSurveyScreen(
    navController: NavController,
    bin: String?,
    viewModel: BuildingSurveyViewModel = koinViewModel()
) {
    val state by viewModel.formState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val currentLanguage = remember { LocalizationManager.getCurrentLanguage(context) }
    val languageCode = remember(currentLanguage) { LocalizationManager.getLanguageCode(currentLanguage) }

    LaunchedEffect(bin) {
        if (!bin.isNullOrEmpty()) {
            viewModel.loadBuildingSurvey(bin)
        }
        viewModel.loadDropdownOptions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = StringResources.getString(StringResources.BUILDING_SURVEY, languageCode),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "BIN: ${bin ?: StringResources.getString(StringResources.NEW_BUILDING, languageCode)}",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
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
            BuildingSurveyFormContent(
                state = state,
                bin = bin,
                onStateChange = viewModel::updateFormState,
                onSubmit = { viewModel.submitSurvey(bin) }
            )
        }

        state.errorMessage?.let { error ->
            com.innovative.smis.ui.components.FormErrorCard(
                message = error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun BuildingSurveyFormContent(
    state: BuildingSurveyFormState,
    bin: String?,
    onStateChange: (BuildingSurveyFormState) -> Unit,
    onSubmit: () -> Unit
) {
    // A. Building Location Information
    SectionHeader("A. Building Location Information")
    
    OutlinedTextFieldWithError(
        value = state.bin.ifEmpty { bin ?: "" },
        onValueChange = { onStateChange(state.copy(bin = it)) },
        label = "BIN (Building Identification Number)",
        error = state.binError,
        enabled = bin.isNullOrEmpty(), // Read-only if BIN provided
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    com.innovative.smis.ui.components.DropdownField(
        label = "Sangkat",
        options = state.sangkats.map { it.sangkatName },
        selectedValue = state.sangkat,
        onValueSelected = { onStateChange(state.copy(sangkat = it)) },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    OutlinedTextField(
        value = state.village,
        onValueChange = { onStateChange(state.copy(village = it)) },
        label = { Text("Village (Optional)") },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    com.innovative.smis.ui.components.DropdownField(
        label = "Road Code",
        options = state.roadCodes.map { it.roadCode },
        selectedValue = state.roadCode,
        onValueSelected = { onStateChange(state.copy(roadCode = it)) },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(16.dp))

    // B. Building Information
    SectionHeader("B. Building Information")
    
    OutlinedTextFieldWithError(
        value = state.respondentName,
        onValueChange = { onStateChange(state.copy(respondentName = it)) },
        label = "Name of Respondent (English)",
        error = state.respondentNameError,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    RadioButtonGroup(
        title = "Gender of Respondent",
        options = RespondentGender.values().map { it.displayName },
        selectedValue = state.respondentGender?.displayName ?: "",
        onValueSelected = { selectedGender ->
            val gender = RespondentGender.values().find { it.displayName == selectedGender }
            onStateChange(state.copy(respondentGender = gender))
        }
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    OutlinedTextField(
        value = state.respondentContact,
        onValueChange = { onStateChange(state.copy(respondentContact = it)) },
        label = { Text("Respondent Contact") },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Phone
        ),
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    OutlinedTextField(
        value = state.ownerName,
        onValueChange = { onStateChange(state.copy(ownerName = it)) },
        label = { Text("Owner Name") },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    OutlinedTextField(
        value = state.ownerContact,
        onValueChange = { onStateChange(state.copy(ownerContact = it)) },
        label = { Text("Owner Contact") },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Phone
        ),
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    com.innovative.smis.ui.components.DropdownField(
        label = "Structure Type",
        options = state.structureTypes.map { it.type },
        selectedValue = state.structureType,
        onValueSelected = { onStateChange(state.copy(structureType = it)) },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    com.innovative.smis.ui.components.DropdownField(
        label = "Functional Use",
        options = state.functionalUses.map { it.type },
        selectedValue = state.functionalUse,
        onValueSelected = { onStateChange(state.copy(functionalUse = it)) },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    com.innovative.smis.ui.components.DropdownField(
        label = "Building Use",
        options = state.buildingUses.map { it.type },
        selectedValue = state.buildingUse,
        onValueSelected = { onStateChange(state.copy(buildingUse = it)) },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = state.numberOfFloors,
            onValueChange = { onStateChange(state.copy(numberOfFloors = it)) },
            label = { Text("No. of Floors") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.weight(1f)
        )
        
        OutlinedTextField(
            value = state.householdServed,
            onValueChange = { onStateChange(state.copy(householdServed = it)) },
            label = { Text("Household Served") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.weight(1f)
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = state.populationServed,
            onValueChange = { onStateChange(state.copy(populationServed = it)) },
            label = { Text("Population Served") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.weight(1f)
        )
        
        OutlinedTextField(
            value = state.floorArea,
            onValueChange = { onStateChange(state.copy(floorArea = it)) },
            label = { Text("Floor Area (m²)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            modifier = Modifier.weight(1f)
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = state.isMainBuilding,
            onCheckedChange = { onStateChange(state.copy(isMainBuilding = it)) }
        )
        Text(
            text = "Is Main Building",
            modifier = Modifier.padding(start = 8.dp)
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = state.constructionYear,
            onValueChange = { onStateChange(state.copy(constructionYear = it)) },
            label = { Text("Construction Year") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.weight(1f)
        )
        
        // Water Supply dropdown using enum
        com.innovative.smis.ui.components.DropdownField(
            label = "Water Supply",
            options = WaterSupply.values().map { it.displayName },
            selectedValue = state.waterSupply?.displayName ?: "",
            onValueSelected = { selectedSupply: String ->
                val supply = WaterSupply.values().find { it.displayName == selectedSupply }
                onStateChange(state.copy(waterSupply = supply))
            },
            modifier = Modifier.weight(1f)
        )
    }
    
    Spacer(modifier = Modifier.height(16.dp))

    // C. Toilet Information
    SectionHeader("C. Toilet Information")
    
    com.innovative.smis.ui.components.DropdownField(
        label = "Defecation Place",
        options = state.defecationPlaces.map { it.type },
        selectedValue = state.defecationPlace,
        onValueSelected = { onStateChange(state.copy(defecationPlace = it)) },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = state.numberOfToilets,
            onValueChange = { onStateChange(state.copy(numberOfToilets = it)) },
            label = { Text("No. of Toilets") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.weight(1f)
        )
        
        OutlinedTextField(
            value = state.toiletCount,
            onValueChange = { onStateChange(state.copy(toiletCount = it)) },
            label = { Text("Toilet Count") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.weight(1f)
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    com.innovative.smis.ui.components.DropdownField(
        label = "Toilet Connection",
        options = state.toiletConnections.map { it.type },
        selectedValue = state.toiletConnection,
        onValueSelected = { onStateChange(state.copy(toiletConnection = it)) },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(16.dp))

    // D. Containment Information
    SectionHeader("D. Containment Information")
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = state.containmentPresentOnsite,
            onCheckedChange = { onStateChange(state.copy(containmentPresentOnsite = it)) }
        )
        Text(
            text = "Containment Present Onsite",
            modifier = Modifier.padding(start = 8.dp)
        )
    }
    
    if (state.containmentPresentOnsite) {
        Spacer(modifier = Modifier.height(8.dp))
        
        DropdownField(
            label = "Type of Storage Tank",
            options = state.storageTankTypes.map { it.type },
            selectedValue = state.typeOfStorageTank,
            onValueSelected = { onStateChange(state.copy(typeOfStorageTank = it)) },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        DropdownField(
            label = "Storage Tank Connection",
            options = state.storageTankConnections.map { it.type },
            selectedValue = state.storageTankConnection,
            onValueSelected = { onStateChange(state.copy(storageTankConnection = it)) },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.numberOfTanks,
                onValueChange = { onStateChange(state.copy(numberOfTanks = it)) },
                label = { Text("No. of Tanks") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.weight(1f)
            )
            
            OutlinedTextField(
                value = state.sizeOfTank,
                onValueChange = { onStateChange(state.copy(sizeOfTank = it)) },
                label = { Text("Size of Tank (m³)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = state.distanceFromWell,
            onValueChange = { onStateChange(state.copy(distanceFromWell = it)) },
            label = { Text("Distance from Well (m)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        DatePickerField(
            label = "Construction Date",
            selectedDate = state.constructionDate,
            onDateSelected = { onStateChange(state.copy(constructionDate = it)) },
            maxDate = LocalDate.now(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        DatePickerField(
            label = "Last Emptied Date",
            selectedDate = state.lastEmptiedDate,
            onDateSelected = { onStateChange(state.copy(lastEmptiedDate = it)) },
            maxDate = LocalDate.now(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        RadioButtonGroup(
            title = "Vacutug Accessible",
            options = VacutugAccessible.values().map { it.displayName },
            selectedValue = state.vacutugAccessible?.displayName ?: "",
            onValueSelected = { selectedAccessible ->
                val accessible = VacutugAccessible.values().find { it.displayName == selectedAccessible }
                onStateChange(state.copy(vacutugAccessible = accessible))
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = state.containmentLocation,
            onValueChange = { onStateChange(state.copy(containmentLocation = it)) },
            label = { Text("Containment Location") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = state.distanceHouseToContainment,
            onValueChange = { onStateChange(state.copy(distanceHouseToContainment = it)) },
            label = { Text("Distance House to Containment (m)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
    
    Spacer(modifier = Modifier.height(16.dp))

    // E. Water Supply and System Information
    SectionHeader("E. Water Supply and System Information")
    
    OutlinedTextField(
        value = state.waterCustomerId,
        onValueChange = { onStateChange(state.copy(waterCustomerId = it)) },
        label = { Text("Water Customer ID") },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    OutlinedTextField(
        value = state.meterSerialNumber,
        onValueChange = { onStateChange(state.copy(meterSerialNumber = it)) },
        label = { Text("Meter Serial Number") },
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    RadioButtonGroup(
        title = "Sanitation System",
        options = SanitationSystem.values().map { it.displayName },
        selectedValue = state.sanitationSystem?.displayName ?: "",
        onValueSelected = { selectedSystem ->
            val system = SanitationSystem.values().find { it.displayName == selectedSystem }
            onStateChange(state.copy(sanitationSystem = system))
        }
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    RadioButtonGroup(
        title = "Technology",
        options = Technology.values().map { it.displayName },
        selectedValue = state.technology?.displayName ?: "",
        onValueSelected = { selectedTechnology ->
            val technology = Technology.values().find { it.displayName == selectedTechnology }
            onStateChange(state.copy(technology = technology))
        }
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = state.compliance,
            onCheckedChange = { onStateChange(state.copy(compliance = it)) }
        )
        Text(
            text = "Compliance",
            modifier = Modifier.padding(start = 8.dp)
        )
    }
    
    Spacer(modifier = Modifier.height(16.dp))

    // Additional Information
    SectionHeader("Additional Information")
    
    OutlinedTextField(
        value = state.comments,
        onValueChange = { onStateChange(state.copy(comments = it)) },
        label = { Text("Comments") },
        maxLines = 4,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(24.dp))

    // Submit Button
    LoadingButton(
        text = if (bin.isNullOrEmpty()) "Create Survey" else "Update Survey",
        isLoading = state.isSubmitting,
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(32.dp))
}