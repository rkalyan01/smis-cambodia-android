package com.innovative.smis.ui.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.innovative.smis.util.constants.Languages
import com.innovative.smis.util.helper.PreferenceHelper
import com.innovative.smis.util.localization.LocalizationManager
import com.innovative.smis.util.localization.StringResources
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val languageCode = LocalizationManager.getCurrentLanguage(context)

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = StringResources.getString(StringResources.SETTINGS, languageCode)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSectionHeader(
                    title = StringResources.getString(StringResources.APP_SETTINGS, languageCode)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = StringResources.getString(StringResources.LANGUAGE, languageCode),
                    subtitle = Languages.getLanguageByCode(uiState.selectedLanguage)?.nativeName ?: "English",
                    onClick = { showLanguageDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = StringResources.getString(StringResources.THEME, languageCode),
                    subtitle = when (uiState.themeMode) {
                        PreferenceHelper.ThemeMode.LIGHT -> StringResources.getString(StringResources.LIGHT, languageCode)
                        PreferenceHelper.ThemeMode.DARK -> StringResources.getString(StringResources.DARK, languageCode)
                        PreferenceHelper.ThemeMode.AUTO -> StringResources.getString(StringResources.AUTO, languageCode)
                    },
                    onClick = { showThemeDialog = true }
                )
            }

            item {
                SettingsSectionHeader(
                    title = StringResources.getString(StringResources.OFFLINE_MODE, languageCode)
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.CloudOff,
                    title = StringResources.getString(StringResources.OFFLINE_MODE, languageCode),
                    subtitle = StringResources.getString(StringResources.WORK_WITHOUT_INTERNET, languageCode),
                    isChecked = uiState.isOfflineModeEnabled,
                    onCheckedChange = { enabled: Boolean -> viewModel.setOfflineMode(enabled) }
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Sync,
                    title = StringResources.getString(StringResources.AUTO_SYNC, languageCode),
                    subtitle = StringResources.getString(StringResources.SYNC_WHEN_ONLINE, languageCode),
                    isChecked = uiState.isAutoSyncEnabled,
                    onCheckedChange = { enabled: Boolean -> viewModel.setAutoSync(enabled) }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.SyncProblem,
                    title = if (languageCode == Languages.KHMER) "សាកល្បងសមកាលកម្ម" else "Test Sync",
                    subtitle = if (uiState.isSyncing) {
                        if (languageCode == Languages.KHMER) "កំពុងសមកាលកម្ម..." else "Syncing..."
                    } else if (!uiState.syncResult.isNullOrEmpty()) {
                        uiState.syncResult!!
                    } else {
                        if (languageCode == Languages.KHMER) "ចុចដើម្បីសាកល្បងសមកាលកម្មដោយដៃ" else "Tap to test manual sync"
                    },
                    onClick = { 
                        if (!uiState.isSyncing) {
                            viewModel.testManualSync()
                        }
                    }
                )
            }

            item {
                SettingsSectionHeader(
                    title = if (languageCode == Languages.KHMER) "ការគ្រប់គ្រងទិន្នន័យ" else "Data Management"
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = if (languageCode == Languages.KHMER) "ទំហំមូលដ្ឋានទិន្នន័យ" else "Database Size",
                    subtitle = if (uiState.databaseSizeMB > 0) {
                        "${String.format("%.2f", uiState.databaseSizeMB)} MB"
                    } else {
                        if (languageCode == Languages.KHMER) "កំពុងគណនា..." else "Calculating..."
                    },
                    onClick = { /* Read-only item */ }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = if (languageCode == Languages.KHMER) "លុបទិន្នន័យបណ្តោះអាសន្ន" else "Clear Cache",
                    subtitle = if (uiState.isClearingCache) {
                        if (languageCode == Languages.KHMER) "កំពុងលុប..." else "Clearing..."
                    } else if (uiState.cacheCleared) {
                        if (languageCode == Languages.KHMER) "លុបរួចរាល់" else "Cache cleared"
                    } else {
                        if (languageCode == Languages.KHMER) "លុបទិន្នន័យបណ្តោះអាសន្ន និងទទួលបាននូវទិន្នន័យថ្មី" else "Clear cached data and refresh with new data"
                    },
                    onClick = { 
                        if (!uiState.isClearingCache) {
                            viewModel.clearCache()
                        }
                    }
                )
            }

            item {
                SettingsSectionHeader(
                    title = StringResources.getString(StringResources.ABOUT, languageCode)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = StringResources.getString(StringResources.VERSION, languageCode),
                    subtitle = "SMIS v1.0",
                    onClick = { }
                )
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = uiState.selectedLanguage,
            languageCode = languageCode,
            onLanguageSelected = { language: String ->
                viewModel.setLanguage(language)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.themeMode,
            languageCode = languageCode,
            onThemeSelected = { theme: PreferenceHelper.ThemeMode ->
                viewModel.setThemeMode(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }


}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    languageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(StringResources.getString(StringResources.SELECT_LANGUAGE, languageCode)) },
        text = {
            Column {
                LanguageOption("English", Languages.ENGLISH, currentLanguage, onLanguageSelected)
                LanguageOption("ខ្មែរ", Languages.KHMER, currentLanguage, onLanguageSelected)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(StringResources.getString(StringResources.CLOSE, languageCode))
            }
        }
    )
}

@Composable
fun LanguageOption(
    label: String,
    code: String,
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = currentLanguage == code,
                onClick = { onLanguageSelected(code) },
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = currentLanguage == code,
            onClick = { onLanguageSelected(code) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: PreferenceHelper.ThemeMode,
    languageCode: String,
    onThemeSelected: (PreferenceHelper.ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(StringResources.getString(StringResources.THEME, languageCode)) },
        text = {
            Column {
                ThemeOption(
                    StringResources.getString(StringResources.LIGHT, languageCode),
                    PreferenceHelper.ThemeMode.LIGHT,
                    currentTheme,
                    onThemeSelected
                )
                ThemeOption(
                    StringResources.getString(StringResources.DARK, languageCode),
                    PreferenceHelper.ThemeMode.DARK,
                    currentTheme,
                    onThemeSelected
                )
                ThemeOption(
                    StringResources.getString(StringResources.AUTO, languageCode),
                    PreferenceHelper.ThemeMode.AUTO,
                    currentTheme,
                    onThemeSelected
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(StringResources.getString(StringResources.CLOSE, languageCode))
            }
        }
    )
}

@Composable
fun ThemeOption(
    label: String,
    theme: PreferenceHelper.ThemeMode,
    currentTheme: PreferenceHelper.ThemeMode,
    onThemeSelected: (PreferenceHelper.ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = currentTheme == theme,
                onClick = { onThemeSelected(theme) },
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = currentTheme == theme,
            onClick = { onThemeSelected(theme) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}





