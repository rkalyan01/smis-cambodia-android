package com.innovative.smis.ui.features.buildingsurvey

import com.innovative.smis.R

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.innovative.smis.data.model.SurveyFormState
import com.innovative.smis.ui.components.*
import org.koin.androidx.compose.koinViewModel

// Color palette - dynamic theme support
private val PrimaryGradient = listOf(Color(0xFF0077B6), Color(0xFF00B4D8)) // RN-like teal/blue
private val AccentColor = Color(0xFF00D4AA)
// Remove hardcoded CardBackground/CardContentColor/SurfaceLight to use MaterialTheme instead

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
                title = { Text(stringResource(R.string.screen_building_survey)) },
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus() // Dismiss keyboard on tap
                    })
                }
                .padding(horizontal = 16.dp)
        ) {
            // Top margin to separate from header
            Spacer(modifier = Modifier.height(12.dp))
            
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
    val stepIcons = listOf(
        Icons.Default.LocationOn,
        Icons.Default.Home,
        Icons.Default.WaterDrop,
        Icons.Default.Water
    )
    val stepLabels = listOf("Location", "Building", "Toilet", "Water")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Step circles row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until state.totalSections) {
                    val isCompleted = i < state.currentSection
                    val isCurrent = i == state.currentSection
                    
                    // Step circle
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .then(
                                if (isCurrent || isCompleted) {
                                    Modifier.background(
                                        brush = Brush.linearGradient(PrimaryGradient),
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                        .background(if(isSystemInDarkTheme()) Color.DarkGray else Color(0xFFE0E0E0), CircleShape)
                                        .border(2.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = stepIcons[i],
                                contentDescription = stepLabels[i],
                                tint = if (isCurrent) Color.White else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    
                    // Connecting line (except after last)
                    if (i < state.totalSections - 1) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .padding(horizontal = 4.dp)
                                .background(
                                    if (i < state.currentSection) 
                                        Brush.linearGradient(PrimaryGradient)
                                    else 
                                        Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f))),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Current step label
            Text(
                text = getSectionTitle(state.currentSection),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Step ${state.currentSection + 1} of ${state.totalSections}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF0077B6),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
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
        RadioButtonGroupField(
            label = "Is Main Building?",
            options = state.yesNoOptions.map { it.replaceFirstChar { char -> char.uppercase() } },
            selectedValue = state.isMainBuilding.replaceFirstChar { it.uppercase() },
            onValueSelected = { onStateChange(state.copy(isMainBuilding = it.lowercase())) },
            error = null
        )
        
        if (state.isMainBuilding == "no") {
            Spacer(modifier = Modifier.height(12.dp))
            SurveyTextFieldWithError(
                value = state.buildingNo,
                onValueChange = { onStateChange(state.copy(buildingNo = it)) },
                label = "Building No *",
                error = state.validationErrors["buildingNo"],
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SurveyTextFieldWithError(
            value = state.taxCode,
            onValueChange = { onStateChange(state.copy(taxCode = it)) },
            label = "Tax Code",
            error = null,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
    
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
        
        PhoneNumberField(
            value = state.respondentContact,
            onValueChange = { onStateChange(state.copy(respondentContact = it)) },
            label = "Respondent Contact *",
            modifier = Modifier,
            enabled = true,
            isRequired = true
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
            
            PhoneNumberField(
                value = state.ownerContact,
                onValueChange = { onStateChange(state.copy(ownerContact = it)) },
                label = "Owner Contact *",
                modifier = Modifier,
                enabled = true,
                isRequired = true
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
        
        SurveyTextFieldWithError(
            value = state.populationServed,
            onValueChange = { onStateChange(state.copy(populationServed = it)) },
            label = "Population Served",
            error = null,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SurveyDropdownField(
            label = "Functional Use",
            options = state.functionalUseOptions.map { it.replaceFirstChar { char -> char.uppercase() } },
            selectedValue = state.functionalUse.replaceFirstChar { it.uppercase() },
            onValueSelected = { onStateChange(state.copy(functionalUse = it.lowercase())) },
            error = null,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (state.functionalUse.isNotEmpty() && state.functionalUse != "residental") {
             Spacer(modifier = Modifier.height(12.dp))
             SurveyTextFieldWithError(
                value = state.officeName,
                onValueChange = { onStateChange(state.copy(officeName = it)) },
                label = "Office/Business Name *",
                error = state.validationErrors["officeName"],
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SurveyTextFieldWithError(
            value = state.constructionDate,
            onValueChange = { onStateChange(state.copy(constructionDate = it)) },
            label = "Construction Year",
            error = null,
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
                label = "Sanitation System Technology *",
                options = state.toiletConnectionOptions,
                selectedValue = state.toiletConnection,
                onValueSelected = { 
                    onStateChange(state.copy(toiletConnection = it)) 
                },
                error = state.validationErrors["toiletConnection"],
                modifier = Modifier.fillMaxWidth()
            )
            
            // Tech 13: Septic tank with soak away pit -> Pre-connected BIN
            if (state.toiletConnection == "13") {
                Spacer(modifier = Modifier.height(12.dp))
                SurveyTextFieldWithError(
                    value = state.sharedConnectionBin,
                    onValueChange = { onStateChange(state.copy(sharedConnectionBin = it)) },
                    label = "BIN of pre-connected buildings",
                    error = state.validationErrors["sharedConnectionBin"],
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Tech 8: Directly to stormwater drain -> Drain Code
            if (state.toiletConnection == "8") {
                 Spacer(modifier = Modifier.height(12.dp))
                 SurveyTextFieldWithError(
                    value = state.drainCode,
                    onValueChange = { onStateChange(state.copy(drainCode = it)) },
                    label = "Drain Code",
                    error = state.validationErrors["drainCode"],
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Tech 11: Septic tank connected to sewerage network -> Sewer Code
            if (state.toiletConnection == "11") {
                 Spacer(modifier = Modifier.height(12.dp))
                 SurveyTextFieldWithError(
                    value = state.sewerCode,
                    onValueChange = { onStateChange(state.copy(sewerCode = it)) },
                    label = "Sewer Code",
                    error = state.validationErrors["sewerCode"],
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Tech 2, 11, 12 -> Containment Info (Tank Style)
            if (state.toiletConnection in listOf("2", "11", "12")) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Containment Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                SurveyTextFieldWithError(
                    value = state.containmentLength,
                    onValueChange = { onStateChange(state.copy(containmentLength = it)) },
                    label = "Length",
                    error = state.validationErrors["containmentLength"],
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
                 Spacer(modifier = Modifier.height(12.dp))
                 SurveyTextFieldWithError(
                    value = state.containmentWidth,
                    onValueChange = { onStateChange(state.copy(containmentWidth = it)) },
                    label = "Width",
                    error = state.validationErrors["containmentWidth"],
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
                 Spacer(modifier = Modifier.height(12.dp))
                 SurveyTextFieldWithError(
                    value = state.containmentDepth,
                    onValueChange = { onStateChange(state.copy(containmentDepth = it)) },
                    label = "Depth",
                    error = state.validationErrors["containmentDepth"],
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                SurveyDropdownField(
                    label = "Containment Location",
                    options = state.containmentLocationOptions.map { it.replace("_", " ").replaceFirstChar { char -> char.uppercase() } },
                    selectedValue = state.containmentLocation.replace("_", " ").replaceFirstChar { it.uppercase() },
                    onValueSelected = { onStateChange(state.copy(containmentLocation = it.lowercase().replace(" ", "_"))) },
                    error = state.validationErrors["containmentLocation"],
                    modifier = Modifier.fillMaxWidth()
                )
                 
                Spacer(modifier = Modifier.height(12.dp))
                
                // Septic Tank Compliance (Using yes/no options for boolean logic)
                RadioButtonGroupField(
                    label = "Septic Tank Compliance",
                    options = state.yesNoOptions.map { it.replaceFirstChar { char -> char.uppercase() } },
                    selectedValue = state.compliance.replaceFirstChar { it.uppercase() },
                    onValueSelected = { onStateChange(state.copy(compliance = it.lowercase())) },
                    error = null
                )
            }
            
            // Tech 10, 15 -> Pit Info
             if (state.toiletConnection in listOf("10", "15")) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Containment Information (Pit)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                SurveyTextFieldWithError(
                    value = state.pitDiameter,
                    onValueChange = { onStateChange(state.copy(pitDiameter = it)) },
                    label = "Pit Diameter",
                    error = state.validationErrors["pitDiameter"],
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
                 Spacer(modifier = Modifier.height(12.dp))
                 SurveyTextFieldWithError(
                    value = state.pitDepth,
                    onValueChange = { onStateChange(state.copy(pitDepth = it)) },
                    label = "Pit Depth",
                    error = state.validationErrors["pitDepth"],
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                SurveyDropdownField(
                    label = "Containment Location",
                    options = state.containmentLocationOptions.map { it.replace("_", " ").replaceFirstChar { char -> char.uppercase() } },
                    selectedValue = state.containmentLocation.replace("_", " ").replaceFirstChar { it.uppercase() },
                    onValueSelected = { onStateChange(state.copy(containmentLocation = it.lowercase().replace(" ", "_"))) },
                    error = state.validationErrors["containmentLocation"],
                    modifier = Modifier.fillMaxWidth()
                )
                 
                Spacer(modifier = Modifier.height(12.dp))
                
                RadioButtonGroupField(
                    label = "Septic Tank Compliance",
                    options = state.yesNoOptions.map { it.replaceFirstChar { char -> char.uppercase() } },
                    selectedValue = state.compliance.replaceFirstChar { it.uppercase() },
                    onValueSelected = { onStateChange(state.copy(compliance = it.lowercase())) },
                    error = null
                )
            }
            
            // Vacutug Accessible logic (Tech 2, 10-15) - simplified range check
            if (state.toiletConnection in listOf("2", "10", "11", "12", "13", "14", "15")) {
                 Spacer(modifier = Modifier.height(12.dp))
                 RadioButtonGroupField(
                    label = "Is Building Vacutug accessible",
                    options = state.dontKnowOptions.map { it.replace("_", " ").replaceFirstChar { char -> char.uppercase() } },
                    selectedValue = state.storageTankAccessible.replace("_", " ").replaceFirstChar { it.uppercase() },
                    onValueSelected = { onStateChange(state.copy(storageTankAccessible = it.lowercase().replace(" ", "_"))) },
                    error = null
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Gradient header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(PrimaryGradient),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            // Content area
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                content()
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Previous button
        if (state.currentSection > 0) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(PrimaryGradient)
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, 
                    contentDescription = null,
                    tint = Color(0xFF00B4DB)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.button_previous),
                    color = Color(0xFF00B4DB),
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // Next / Submit button
        if (state.currentSection < state.totalSections - 1) {
            Button(
                onClick = onNext,
                enabled = state.isCurrentSectionValid(),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00B4DB),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    stringResource(R.string.button_next),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        } else {
            Button(
                onClick = onSubmit,
                enabled = state.canSubmit() && !state.isSubmitting,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentColor,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                )
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.button_submit_survey),
                        fontWeight = FontWeight.Bold
                    )
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
    0 -> "Building Location (1/4)"
    1 -> "Building Information (2/4)"
    2 -> "Toilet & Containment (3/4)"
    3 -> "Water Source (4/4)"
    else -> "Unknown Section"
}
