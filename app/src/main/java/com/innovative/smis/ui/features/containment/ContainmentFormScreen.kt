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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import com.innovative.smis.ui.components.*
import com.innovative.smis.util.localization.LocalizationManager
import com.innovative.smis.util.localization.StringResources

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainmentFormScreen(
    navController: NavController,
    applicationId: String,
    modifier: Modifier = Modifier,
    viewModel: ContainmentFormViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val currentLanguage = remember { LocalizationManager.getCurrentLanguage(context) }
    val languageCode = remember(currentLanguage) { LocalizationManager.getLanguageCode(currentLanguage) }

    LaunchedEffect(applicationId) {
        viewModel.loadContainmentData(applicationId)
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
                title = { Text(StringResources.getString(StringResources.CONTAINMENT_DETAILS, languageCode)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = StringResources.getString(StringResources.BACK, languageCode))
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
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
                SectionHeader("Storage Tank Information")
            }

            // Where is Your Toilet/Latrine Connected?
            item {
                OutlinedTextField(
                    value = uiState.toiletConnection,
                    onValueChange = viewModel::onToiletConnectionChange,
                    label = { Text("Where is Your Toilet/Latrine Connected?") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // What Type of Storage Tank?
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
                        label = "What Type of Storage Tank?",
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
                        label = { Text("Other Type of Storage Tank") },
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
                        label = "Storage Tank Outlet Connection",
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
                        label = { Text("Other Storage Tank Connection") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Tank Specifications
            item {
                SectionHeader("Tank Specifications")
            }

            // Size of Storage Tank (m3)
            item {
                OutlinedTextField(
                    value = uiState.sizeOfStorageTankM3,
                    onValueChange = viewModel::onSizeOfStorageTankM3Change,
                    label = { Text("Size of Storage Tank (m³)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Construction Year
            item {
                OutlinedTextField(
                    value = uiState.constructionYear,
                    onValueChange = viewModel::onConstructionYearChange,
                    label = { Text("Construction Year") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Accessibility & History
            item {
                SectionHeader("Accessibility & History")
            }

            // Accessibility to Desludging Vehicle
            item {
                DropdownMenuField(
                    label = "Accessibility to Desludging Vehicle",
                    selectedValue = uiState.accessibility,
                    selectedKey = uiState.accessibilityKey,
                    options = mapOf(
                        "yes" to "Yes",
                        "no" to "No"
                    ),
                    onOptionSelected = { key, value ->
                        viewModel.onAccessibilitySelected(key, value)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Ever Emptied the Storage Tank
            item {
                DropdownMenuField(
                    label = "Ever Emptied the Storage Tank",
                    selectedValue = uiState.everEmptied,
                    selectedKey = uiState.everEmptiedKey,
                    options = mapOf(
                        "yes" to "Yes",
                        "no" to "No"
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
                        label = { Text("Last Emptied Year") },
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
                    Text(StringResources.getString(StringResources.UPDATE, languageCode))
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