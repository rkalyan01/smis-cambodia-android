package com.innovative.smis.ui.features.buildingsurvey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.innovative.smis.data.model.SurveyFormState
import com.innovative.smis.ui.components.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprehensiveSurveyScreen(
    navController: NavController,
    bin: String?,
    viewModel: ComprehensiveSurveyViewModel = koinViewModel()
) {
    val state by viewModel.formState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(bin) {
        if (!bin.isNullOrEmpty()) {
            viewModel.loadSurvey(bin)
        }
    }

    // Success navigation
    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            navController.popBackStack()
        }
    }

    val focusManager = LocalFocusManager.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Building Survey") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding() // Fix status bar overlap
            .imePadding() // Handle keyboard padding
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus() // Dismiss keyboard on tap
                    })
                }
        ) {
            // Progress indicator
            SurveyProgressIndicator(state)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // BIN header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Building Survey",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "BIN: ${bin ?: "New Building"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Survey sections based on current section
                when (state.currentSection) {
                    0 -> BuildingLocationSection(state, viewModel::updateFormState)
                    1 -> BuildingInformationSection(state, viewModel::updateFormState)
                    2 -> ToiletInformationSection(state, viewModel::updateFormState)
                    3 -> WaterInformationSection(state, viewModel::updateFormState)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Navigation buttons
                SurveyNavigationButtons(
                    state = state,
                    onPrevious = viewModel::previousSection,
                    onNext = viewModel::nextSection,
                    onSubmit = { viewModel.submitSurvey(bin) }
                )
            }

            // Error message
            state.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                com.innovative.smis.ui.components.FormErrorCard(message = error)
            }
        }
    }
}

@Composable
private fun SurveyProgressIndicator(state: SurveyFormState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Section ${state.currentSection + 1} of ${state.totalSections}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { (state.currentSection + 1).toFloat() / state.totalSections },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = getSectionTitle(state.currentSection),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BuildingLocationSection(
    state: SurveyFormState,
    onStateChange: (SurveyFormState) -> Unit
) {
    SectionCard(
        title = "A. Building Location Information",
        icon = Icons.Default.LocationOn
    ) {
        SurveyDropdownField(
            label = "Sangkat *",
            options = state.sangkatOptions,
            selectedValue = state.sangkat,
            onValueSelected = { onStateChange(state.copy(sangkat = it)) },
            error = state.validationErrors["sangkat"],
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SurveyTextFieldWithError(
            value = state.village,
            onValueChange = { onStateChange(state.copy(village = it)) },
            label = "Village",
            error = null,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SurveyTextFieldWithError(
            value = state.roadCode,
            onValueChange = { onStateChange(state.copy(roadCode = it)) },
            label = "Road Code *",
            error = state.validationErrors["roadCode"],
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BuildingInformationSection(
    state: SurveyFormState,
    onStateChange: (SurveyFormState) -> Unit
) {
    SectionCard(
        title = "B. Building Information",
        icon = Icons.Default.Home
    ) {
        SurveyTextFieldWithError(
            value = state.respondentName,
            onValueChange = { onStateChange(state.copy(respondentName = it)) },
            label = "Respondent Name *",
            error = state.validationErrors["respondentName"],
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SurveyDropdownField(
            label = "Respondent Gender *",
            options = state.genderOptions.map { it.replaceFirstChar { char -> char.uppercase() } },
            selectedValue = state.respondentGender.replaceFirstChar { it.uppercase() },
            onValueSelected = { onStateChange(state.copy(respondentGender = it.lowercase())) },
            error = state.validationErrors["respondentGender"],
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SurveyTextFieldWithError(
            value = state.respondentContact,
            onValueChange = { onStateChange(state.copy(respondentContact = it)) },
            label = "Respondent Contact *",
            error = state.validationErrors["respondentContact"],
            keyboardType = KeyboardType.Phone,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        RadioButtonGroupField(
            label = "Is Respondent Owner? *",
            options = state.yesNoOptions.map { it.replaceFirstChar { char -> char.uppercase() } },
            selectedValue = state.respondentIsOwner.replaceFirstChar { it.uppercase() },
            onValueSelected = { onStateChange(state.copy(respondentIsOwner = it.lowercase())) },
            error = state.validationErrors["respondentIsOwner"]
        )
        
        // Show owner fields only if respondent is not owner
        if (state.respondentIsOwner == "no") {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Owner Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            SurveyTextFieldWithError(
                value = state.ownerName,
                onValueChange = { onStateChange(state.copy(ownerName = it)) },
                label = "Owner Name (English) *",
                error = state.validationErrors["ownerName"],
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SurveyTextFieldWithError(
                value = state.ownerNameKhmer,
                onValueChange = { onStateChange(state.copy(ownerNameKhmer = it)) },
                label = "Owner Name (Khmer) *",
                error = state.validationErrors["ownerNameKhmer"],
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SurveyDropdownField(
                label = "Owner Gender *",
                options = state.genderOptions.map { it.replaceFirstChar { char -> char.uppercase() } },
                selectedValue = state.ownerGender.replaceFirstChar { it.uppercase() },
                onValueSelected = { onStateChange(state.copy(ownerGender = it.lowercase())) },
                error = state.validationErrors["ownerGender"],
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SurveyTextFieldWithError(
                value = state.ownerContact,
                onValueChange = { onStateChange(state.copy(ownerContact = it)) },
                label = "Owner Contact *",
                error = state.validationErrors["ownerContact"],
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SurveyDropdownField(
            label = "Structure Type *",
            options = state.structureTypeOptions.map { it.replace("-", " ").replaceFirstChar { char -> char.uppercase() } },
            selectedValue = state.structureType.replace("-", " ").replaceFirstChar { it.uppercase() },
            onValueSelected = { 
                val formatted = it.lowercase().replace(" ", "-")
                onStateChange(state.copy(structureType = formatted)) 
            },
            error = state.validationErrors["structureType"],
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SurveyTextFieldWithError(
            value = state.floorCount,
            onValueChange = { onStateChange(state.copy(floorCount = it)) },
            label = "Floor Count *",
            error = state.validationErrors["floorCount"],
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SurveyTextFieldWithError(
            value = state.householdServed,
            onValueChange = { onStateChange(state.copy(householdServed = it)) },
            label = "Households Served *",
            error = state.validationErrors["householdServed"],
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        var buildingPhotoUri by remember { mutableStateOf<Uri?>(null) }
        ImagePickerComponent(
            label = "Building Photo *",
            selectedImageUri = buildingPhotoUri,
            onImageSelected = { uri ->
                buildingPhotoUri = uri
                onStateChange(state.copy(buildingPhoto = uri?.toString() ?: ""))
            }
        )
    }
}

@Composable
private fun ToiletInformationSection(
    state: SurveyFormState,
    onStateChange: (SurveyFormState) -> Unit
) {
    SectionCard(
        title = "C. Toilet and Containment Information",
        icon = Icons.Default.Wc
    ) {
        RadioButtonGroupField(
            label = "Presence of Toilet *",
            options = state.yesNoOptions.map { it.replaceFirstChar { char -> char.uppercase() } },
            selectedValue = state.presenceOfToilet.replaceFirstChar { it.uppercase() },
            onValueSelected = { onStateChange(state.copy(presenceOfToilet = it.lowercase())) },
            error = state.validationErrors["presenceOfToilet"]
        )
        
        if (state.presenceOfToilet == "yes") {
            Spacer(modifier = Modifier.height(16.dp))
            
            SurveyDropdownField(
                label = "Toilet Connection *",
                options = state.toiletConnectionOptions.map { 
                    it.replace("_", " ").replaceFirstChar { char -> char.uppercase() } 
                },
                selectedValue = state.toiletConnection.replace("_", " ").replaceFirstChar { it.uppercase() },
                onValueSelected = { 
                    val formatted = it.lowercase().replace(" ", "_")
                    onStateChange(state.copy(toiletConnection = formatted)) 
                },
                error = state.validationErrors["toiletConnection"],
                modifier = Modifier.fillMaxWidth()
            )
            
            if (state.toiletConnection == "other") {
                Spacer(modifier = Modifier.height(12.dp))
                SurveyTextFieldWithError(
                    value = state.toiletConnectionOther,
                    onValueChange = { onStateChange(state.copy(toiletConnectionOther = it)) },
                    label = "Specify Other Connection *",
                    error = state.validationErrors["toiletConnectionOther"],
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (state.toiletConnection in listOf("shared_sewer", "shared_storage_tank")) {
                Spacer(modifier = Modifier.height(12.dp))
                SurveyTextFieldWithError(
                    value = state.sharedConnectionBin,
                    onValueChange = { onStateChange(state.copy(sharedConnectionBin = it)) },
                    label = "Shared Connection BIN",
                    error = null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Storage tank specific fields
            if (state.toiletConnection in listOf("storage_tank", "shared_storage_tank")) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Storage Tank Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SurveyDropdownField(
                    label = "Storage Tank Type *",
                    options = state.storageTankTypeOptions.map { 
                        it.replace("_", " ").replaceFirstChar { char -> char.uppercase() } 
                    },
                    selectedValue = state.storageTankType.replace("_", " ").replaceFirstChar { it.uppercase() },
                    onValueSelected = { 
                        val formatted = it.lowercase().replace(" ", "_")
                        onStateChange(state.copy(storageTankType = formatted)) 
                    },
                    error = state.validationErrors["storageTankType"],
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (state.storageTankType == "other") {
                    Spacer(modifier = Modifier.height(12.dp))
                    SurveyTextFieldWithError(
                        value = state.storageTankTypeOther,
                        onValueChange = { onStateChange(state.copy(storageTankTypeOther = it)) },
                        label = "Specify Other Tank Type *",
                        error = state.validationErrors["storageTankTypeOther"],
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SurveyDropdownField(
                    label = "Storage Tank Outlet *",
                    options = state.storageTankOutletOptions.map { 
                        it.replace("_", " ").replaceFirstChar { char -> char.uppercase() } 
                    },
                    selectedValue = state.storageTankOutlet.replace("_", " ").replaceFirstChar { it.uppercase() },
                    onValueSelected = { 
                        val formatted = it.lowercase().replace(" ", "_")
                        onStateChange(state.copy(storageTankOutlet = formatted)) 
                    },
                    error = state.validationErrors["storageTankOutlet"],
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (state.storageTankOutlet == "other") {
                    Spacer(modifier = Modifier.height(12.dp))
                    SurveyTextFieldWithError(
                        value = state.storageTankOutletOther,
                        onValueChange = { onStateChange(state.copy(storageTankOutletOther = it)) },
                        label = "Specify Other Outlet *",
                        error = state.validationErrors["storageTankOutletOther"],
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SurveyTextFieldWithError(
                    value = state.storageTankSize,
                    onValueChange = { onStateChange(state.copy(storageTankSize = it)) },
                    label = "Tank Size (m³)",
                    error = null,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SurveyTextFieldWithError(
                    value = state.storageTankYear,
                    onValueChange = { onStateChange(state.copy(storageTankYear = it)) },
                    label = "Construction Year",
                    error = null,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                RadioButtonGroupField(
                    label = "Tank Accessible",
                    options = state.yesNoOptions.map { it.replaceFirstChar { char -> char.uppercase() } },
                    selectedValue = state.storageTankAccessible.replaceFirstChar { it.uppercase() },
                    onValueSelected = { onStateChange(state.copy(storageTankAccessible = it.lowercase())) },
                    error = null
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                RadioButtonGroupField(
                    label = "Ever Emptied",
                    options = state.yesNoOptions.map { it.replaceFirstChar { char -> char.uppercase() } },
                    selectedValue = state.storageTankEmptied.replaceFirstChar { it.uppercase() },
                    onValueSelected = { onStateChange(state.copy(storageTankEmptied = it.lowercase())) },
                    error = null
                )
                
                if (state.storageTankEmptied == "yes") {
                    Spacer(modifier = Modifier.height(12.dp))
                    SurveyTextFieldWithError(
                        value = state.storageTankLastEmptied,
                        onValueChange = { onStateChange(state.copy(storageTankLastEmptied = it)) },
                        label = "Last Emptied Year",
                        error = null,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            if (state.toiletConnection == "sewer" || state.toiletConnection == "shared_sewer") {
                Spacer(modifier = Modifier.height(16.dp))
                
                RadioButtonGroupField(
                    label = "Sewer Bill Available",
                    options = state.yesNoOptions.map { it.replaceFirstChar { char -> char.uppercase() } },
                    selectedValue = state.sewerBill.replaceFirstChar { it.uppercase() },
                    onValueSelected = { onStateChange(state.copy(sewerBill = it.lowercase())) },
                    error = null
                )
                
                if (state.sewerBill == "yes") {
                    Spacer(modifier = Modifier.height(12.dp))
                    var sewerBillPhotoUri by remember { mutableStateOf<Uri?>(null) }
                    ImagePickerComponent(
                        label = "Sewer Bill Photo",
                        selectedImageUri = sewerBillPhotoUri,
                        onImageSelected = { uri ->
                            sewerBillPhotoUri = uri
                            onStateChange(state.copy(sewerBillPhoto = uri?.toString() ?: ""))
                        }
                    )
                }
            }
        } else if (state.presenceOfToilet == "no") {
            Spacer(modifier = Modifier.height(16.dp))
            
            SurveyDropdownField(
                label = "Place of Defecation *",
                options = state.placeOfDefecationOptions.map { 
                    it.replace("_", " ").replaceFirstChar { char -> char.uppercase() } 
                },
                selectedValue = state.placeOfDefecation.replace("_", " ").replaceFirstChar { it.uppercase() },
                onValueSelected = { 
                    val formatted = it.lowercase().replace(" ", "_")
                    onStateChange(state.copy(placeOfDefecation = formatted)) 
                },
                error = state.validationErrors["placeOfDefecation"],
                modifier = Modifier.fillMaxWidth()
            )
            
            if (state.placeOfDefecation == "other") {
                Spacer(modifier = Modifier.height(12.dp))
                SurveyTextFieldWithError(
                    value = state.placeOfDefecationOther,
                    onValueChange = { onStateChange(state.copy(placeOfDefecationOther = it)) },
                    label = "Specify Other Place *",
                    error = state.validationErrors["placeOfDefecationOther"],
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (state.placeOfDefecation == "shared_toilet") {
                Spacer(modifier = Modifier.height(12.dp))
                SurveyTextFieldWithError(
                    value = state.sharedToiletBin,
                    onValueChange = { onStateChange(state.copy(sharedToiletBin = it)) },
                    label = "Shared Toilet BIN",
                    error = null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun WaterInformationSection(
    state: SurveyFormState,
    onStateChange: (SurveyFormState) -> Unit
) {
    SectionCard(
        title = "D. Water Source Information",
        icon = Icons.Default.Water
    ) {
        RadioButtonGroupField(
            label = "Water Connection *",
            options = state.yesNoOptions.map { it.replaceFirstChar { char -> char.uppercase() } },
            selectedValue = state.waterConnection.replaceFirstChar { it.uppercase() },
            onValueSelected = { onStateChange(state.copy(waterConnection = it.lowercase())) },
            error = state.validationErrors["waterConnection"]
        )
        
        if (state.waterConnection == "yes") {
            Spacer(modifier = Modifier.height(16.dp))
            
            SurveyTextFieldWithError(
                value = state.waterCustomerId,
                onValueChange = { onStateChange(state.copy(waterCustomerId = it)) },
                label = "Water Customer ID *",
                error = state.validationErrors["waterCustomerId"],
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SurveyTextFieldWithError(
                value = state.waterMeterNumber,
                onValueChange = { onStateChange(state.copy(waterMeterNumber = it)) },
                label = "Water Meter Number *",
                error = state.validationErrors["waterMeterNumber"],
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            var waterMeterPhotoUri by remember { mutableStateOf<Uri?>(null) }
            ImagePickerComponent(
                label = "Water Meter Photo",
                selectedImageUri = waterMeterPhotoUri,
                onImageSelected = { uri ->
                    waterMeterPhotoUri = uri
                    onStateChange(state.copy(waterMeterPhoto = uri?.toString() ?: ""))
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            var waterBillPhotoUri by remember { mutableStateOf<Uri?>(null) }
            ImagePickerComponent(
                label = "Water Bill Photo",
                selectedImageUri = waterBillPhotoUri,
                onImageSelected = { uri ->
                    waterBillPhotoUri = uri
                    onStateChange(state.copy(waterBillPhoto = uri?.toString() ?: ""))
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            RadioButtonGroupField(
                label = "Is Connection Shared",
                options = state.yesNoOptions.map { it.replaceFirstChar { char -> char.uppercase() } },
                selectedValue = state.waterShared.replaceFirstChar { it.uppercase() },
                onValueSelected = { onStateChange(state.copy(waterShared = it.lowercase())) },
                error = null
            )
            
            if (state.waterShared == "yes") {
                Spacer(modifier = Modifier.height(12.dp))
                SurveyTextFieldWithError(
                    value = state.waterSharedBin,
                    onValueChange = { onStateChange(state.copy(waterSharedBin = it)) },
                    label = "Main Connection BIN",
                    error = null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
private fun SurveyNavigationButtons(
    state: SurveyFormState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.currentSection > 0) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Previous")
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        
        if (state.currentSection < state.totalSections - 1) {
            Button(
                onClick = onNext,
                enabled = state.isCurrentSectionValid(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Next")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        } else {
            Button(
                onClick = onSubmit,
                enabled = state.canSubmit() && !state.isSubmitting,
                modifier = Modifier.weight(1f)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit Survey")
                }
            }
        }
    }
}



@Composable
private fun PhotoField(
    label: String,
    photoPath: String,
    onPhotoSelected: (String) -> Unit,
    error: String?
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Button(
            onClick = { 
                // For now, simulate photo selection
                onPhotoSelected("photo_${System.currentTimeMillis()}")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (photoPath.isEmpty()) "Take Photo" else "Photo Selected")
        }
        
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun getSectionTitle(section: Int): String = when (section) {
    0 -> "Building Location Information"
    1 -> "Building Information"
    2 -> "Toilet and Containment Information"
    3 -> "Water Source Information"
    else -> "Unknown Section"
}