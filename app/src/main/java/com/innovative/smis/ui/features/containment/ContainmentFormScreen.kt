package com.innovative.smis.ui.features.containment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.innovative.smis.R
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import com.innovative.smis.ui.components.*
import com.innovative.smis.util.localization.LocalizationManager
import com.innovative.smis.util.localization.StringResources
import com.innovative.smis.util.validation.InputValidators

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainmentFormScreen(
    navController: NavController,
    applicationId: String,
    sanitationCustomerId: String,
    modifier: Modifier = Modifier,
    viewModel: ContainmentFormViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val currentLanguage = remember { LocalizationManager.getCurrentLanguage(context) }
    val languageCode = remember(currentLanguage) { LocalizationManager.getLanguageCode(currentLanguage) }

    LaunchedEffect(sanitationCustomerId) {
        viewModel.loadContainmentData(sanitationCustomerId)
    }

    // Handle save result for success/error messages
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

            // Add small delay to ensure the savedStateHandle is properly set before navigation
            kotlinx.coroutines.delay(100)
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.section_containment_details)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
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
                // Form Header
                item {
                    SectionHeader(stringResource(R.string.section_storage_tank_information))
                }

            // Toilet Connection (Read-only)
            item {
                OutlinedTextField(
                    value = uiState.toiletConnection,
                    onValueChange = { },
                    label = { Text(stringResource(R.string.label_toilet_connection)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = disabledTextFieldColors()
                )
            }

            // Storage Tank Type
            item {
                if (uiState.isLoadingDropdowns) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    DropdownMenuField(
                        label = stringResource(R.string.label_storage_tank_type),
                        selectedValue = uiState.selectedStorageType,
                        selectedKey = uiState.selectedStorageTypeKey,
                        options = uiState.storageTypeOptions,
                        onOptionSelected = { key, value ->
                            viewModel.onStorageTypeSelected(key, value)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Other Type of Storage Tank (conditional)
            if (uiState.selectedStorageType == "Other") {
                item {
                    OutlinedTextField(
                        value = uiState.otherTypeOfStorageTank,
                        onValueChange = viewModel::onOtherStorageTypeChange,
                        label = { Text(stringResource(R.string.label_other_type_storage_tank)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Storage Tank Outlet Connection
            item {
                if (uiState.isLoadingDropdowns) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    DropdownMenuField(
                        label = stringResource(R.string.label_storage_tank_outlet_connection),
                        selectedValue = uiState.selectedStorageConnection,
                        selectedKey = uiState.selectedStorageConnectionKey,
                        options = uiState.storageConnectionOptions,
                        onOptionSelected = { key, value ->
                            viewModel.onStorageConnectionSelected(key, value)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Other Storage Tank Connection (conditional)
            if (uiState.selectedStorageConnection == "Other") {
                item {
                    OutlinedTextField(
                        value = uiState.otherStorageTankConnection,
                        onValueChange = viewModel::onOtherStorageConnectionChange,
                        label = { Text(stringResource(R.string.label_other_storage_tank_connection)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Tank Specifications
            item {
                SectionHeader(stringResource(R.string.section_tank_specifications))
            }

            // Storage Tank Size
            item {
                OutlinedTextField(
                    value = uiState.sizeOfStorageTankM3,
                    onValueChange = { value -> 
                        val validated = InputValidators.validateStorageTankSize(value)
                        viewModel.onSizeOfStorageTankM3Change(validated)
                    },
                    label = { Text(stringResource(R.string.label_storage_tank_size)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Construction Year of Storage Tank
            item {
                OutlinedTextField(
                    value = uiState.constructionYear,
                    onValueChange = { value -> 
                        val validated = InputValidators.validateConstructionYear(value)
                        viewModel.onConstructionYearChange(validated)
                    },
                    label = { Text(stringResource(R.string.label_construction_year_storage_tank)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Accessibility & History
            item {
                SectionHeader(stringResource(R.string.section_accessibility_history))
            }

            // Accessible to Desludging Vehicle (Yes/No)
            item {
                DropdownMenuField(
                    label = stringResource(R.string.label_accessible_to_desludging_vehicle),
                    selectedValue = uiState.accessibility,
                    selectedKey = uiState.accessibilityKey,
                    options = mapOf(
                        "yes" to stringResource(R.string.label_yes),
                        "no" to stringResource(R.string.label_no)
                    ),
                    onOptionSelected = { key, value ->
                        viewModel.onAccessibilitySelected(key, value)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Ever Emptied the Storage Tank (Yes/No)
            item {
                DropdownMenuField(
                    label = stringResource(R.string.label_ever_emptied_storage_tank),
                    selectedValue = uiState.everEmptied,
                    selectedKey = uiState.everEmptiedKey,
                    options = mapOf(
                        "yes" to stringResource(R.string.label_yes),
                        "no" to stringResource(R.string.label_no)
                    ),
                    onOptionSelected = { key, value ->
                        viewModel.onEverEmptiedSelected(key, value)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Last Emptied Year (conditional)
            if (uiState.everEmptiedKey == "yes") {
                item {
                    OutlinedTextField(
                        value = uiState.lastEmptiedYear,
                        onValueChange = viewModel::onLastEmptiedYearChange,
                        label = { Text(stringResource(R.string.label_last_emptied_year)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Submit Button
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.submitForm() },
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.action_update))
                }
            }

            // Error Message
            if (uiState.errorMessage != null) {
                item {
                    FormErrorCard(uiState.errorMessage!!)
                }
            }
            }
        }
    }
}