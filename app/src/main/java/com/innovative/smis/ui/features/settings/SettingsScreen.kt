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
fun SettingsScreen(navController: NavController, onMenuClick: (() -> Unit)? = null) {
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val languageCode = LocalizationManager.getCurrentLanguage(context)

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = StringResources.getString(StringResources.SETTINGS, languageCode)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = StringResources.getString(StringResources.BACK, languageCode))
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
                    subtitle = Languages.getLanguageByCode(uiState.selectedLanguage)?.nativeName ?: StringResources.getString(StringResources.ENGLISH, languageCode),
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
                SettingsItem(
                    icon = Icons.Default.CalendarMonth,
                    title = "Date Format",
                    subtitle = uiState.dateFormat.displayName,
                    onClick = { showDateFormatDialog = true }
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
                    title = StringResources.getString(StringResources.TEST_SYNC, languageCode),
                    subtitle = if (uiState.isSyncing) {
                        StringResources.getString(StringResources.SYNCING, languageCode)
                    } else if (!uiState.syncResult.isNullOrEmpty()) {
                        uiState.syncResult!!
                    } else {
                        StringResources.getString(StringResources.TAP_TO_TEST_MANUAL_SYNC, languageCode)
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
                    title = StringResources.getString(StringResources.DATA_MANAGEMENT, languageCode)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = StringResources.getString(StringResources.DATABASE_SIZE, languageCode),
                    subtitle = if (uiState.databaseSizeMB > 0) {
                        "${String.format("%.2f", uiState.databaseSizeMB)} ${StringResources.getString(StringResources.MEGABYTES, languageCode)}"
                    } else {
                        StringResources.getString(StringResources.CALCULATING, languageCode)
                    },
                    onClick = { /* Read-only item */ }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = StringResources.getString(StringResources.CLEAR_CACHE, languageCode),
                    subtitle = if (uiState.isClearingCache) {
                        StringResources.getString(StringResources.CLEARING, languageCode)
                    } else if (uiState.cacheCleared) {
                        StringResources.getString(StringResources.CACHE_CLEARED, languageCode)
                    } else {
                        StringResources.getString(StringResources.CLEAR_CACHED_DATA_DESCRIPTION, languageCode)
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

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Logout,
                    title = StringResources.getString(StringResources.LOGOUT, languageCode),
                    subtitle = if (languageCode == "km") "ចាកចេញពីគណនី" else "Sign out of your account",
                    onClick = { viewModel.logout() },
                    textColor = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Handle logout navigation
    LaunchedEffect(uiState.shouldLogout) {
        if (uiState.shouldLogout) {
            // Navigate to login screen and clear back stack
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
            // Reset flag to prevent re-navigation on configuration changes
            viewModel.resetLogoutFlag()
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

    if (showDateFormatDialog) {
        DateFormatSelectionDialog(
            currentDateFormat = uiState.dateFormat,
            languageCode = languageCode,
            onDateFormatSelected = { format: PreferenceHelper.DateFormat ->
                viewModel.setDateFormat(format)
                showDateFormatDialog = false
            },
            onDismiss = { showDateFormatDialog = false }
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
                LanguageOption(StringResources.getString(StringResources.ENGLISH, languageCode), Languages.ENGLISH, currentLanguage, onLanguageSelected)
                LanguageOption(StringResources.getString(StringResources.KHMER, languageCode), Languages.KHMER, currentLanguage, onLanguageSelected)
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
fun DateFormatSelectionDialog(
    currentDateFormat: PreferenceHelper.DateFormat,
    languageCode: String,
    onDateFormatSelected: (PreferenceHelper.DateFormat) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Date Format") },
        text = {
            Column {
                DateFormatOption(
                    PreferenceHelper.DateFormat.DD_MM_YYYY,
                    currentDateFormat,
                    onDateFormatSelected
                )
                DateFormatOption(
                    PreferenceHelper.DateFormat.MM_DD_YYYY,
                    currentDateFormat,
                    onDateFormatSelected
                )
                DateFormatOption(
                    PreferenceHelper.DateFormat.YYYY_MM_DD,
                    currentDateFormat,
                    onDateFormatSelected
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
fun DateFormatOption(
    format: PreferenceHelper.DateFormat,
    currentDateFormat: PreferenceHelper.DateFormat,
    onDateFormatSelected: (PreferenceHelper.DateFormat) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = currentDateFormat == format,
                onClick = { onDateFormatSelected(format) },
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = currentDateFormat == format,
            onClick = { onDateFormatSelected(format) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = format.displayName)
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





