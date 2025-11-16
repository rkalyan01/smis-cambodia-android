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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.innovative.smis.BuildConfig
import com.innovative.smis.R
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
    val shouldRestartActivity by viewModel.shouldRestartActivity.collectAsState()
    val context = LocalContext.current

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    
    // Restart activity when language changes
    LaunchedEffect(shouldRestartActivity) {
        if (shouldRestartActivity) {
            viewModel.activityRestarted()
            (context as? android.app.Activity)?.recreate()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
                    title = stringResource(R.string.settings_app_section)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = Languages.getLanguageByCode(uiState.selectedLanguage)?.nativeName ?: stringResource(R.string.language_english),
                    onClick = { showLanguageDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.settings_theme),
                    subtitle = when (uiState.themeMode) {
                        PreferenceHelper.ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                        PreferenceHelper.ThemeMode.DARK -> stringResource(R.string.theme_dark)
                        PreferenceHelper.ThemeMode.AUTO -> stringResource(R.string.theme_auto)
                    },
                    onClick = { showThemeDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.CalendarMonth,
                    title = stringResource(R.string.settings_date_format),
                    subtitle = uiState.dateFormat.displayName,
                    onClick = { showDateFormatDialog = true }
                )
            }

            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_offline_section)
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.CloudOff,
                    title = stringResource(R.string.settings_offline_mode),
                    subtitle = stringResource(R.string.settings_work_without_internet),
                    isChecked = uiState.isOfflineModeEnabled,
                    onCheckedChange = { enabled: Boolean -> viewModel.setOfflineMode(enabled) }
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Sync,
                    title = stringResource(R.string.settings_auto_sync),
                    subtitle = stringResource(R.string.settings_sync_when_online),
                    isChecked = uiState.isAutoSyncEnabled,
                    onCheckedChange = { enabled: Boolean -> viewModel.setAutoSync(enabled) }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.SyncProblem,
                    title = stringResource(R.string.settings_test_sync),
                    subtitle = if (uiState.isSyncing) {
                        stringResource(R.string.status_syncing)
                    } else if (!uiState.syncResult.isNullOrEmpty()) {
                        uiState.syncResult!!
                    } else {
                        stringResource(R.string.settings_tap_to_test_sync)
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
                    title = stringResource(R.string.settings_data_section)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = stringResource(R.string.settings_database_size),
                    subtitle = if (uiState.databaseSizeMB > 0) {
                        "${String.format("%.2f", uiState.databaseSizeMB)} ${stringResource(R.string.unit_megabytes)}"
                    } else {
                        stringResource(R.string.status_calculating)
                    },
                    onClick = { /* Read-only item */ }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.settings_clear_cache),
                    subtitle = if (uiState.isClearingCache) {
                        stringResource(R.string.status_clearing)
                    } else if (uiState.cacheCleared) {
                        stringResource(R.string.message_cache_cleared)
                    } else {
                        stringResource(R.string.settings_clear_cache_description)
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
                    title = stringResource(R.string.settings_about_section)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_version),
                    subtitle = "SMIS v${BuildConfig.VERSION_NAME}",
                    onClick = { }
                )
            }

        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = uiState.selectedLanguage,
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
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_select_language)) },
        text = {
            Column {
                LanguageOption(stringResource(R.string.language_english), Languages.ENGLISH, currentLanguage, onLanguageSelected)
                LanguageOption(stringResource(R.string.language_khmer), Languages.KHMER, currentLanguage, onLanguageSelected)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
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
    onThemeSelected: (PreferenceHelper.ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme)) },
        text = {
            Column {
                ThemeOption(
                    stringResource(R.string.theme_light),
                    PreferenceHelper.ThemeMode.LIGHT,
                    currentTheme,
                    onThemeSelected
                )
                ThemeOption(
                    stringResource(R.string.theme_dark),
                    PreferenceHelper.ThemeMode.DARK,
                    currentTheme,
                    onThemeSelected
                )
                ThemeOption(
                    stringResource(R.string.theme_auto),
                    PreferenceHelper.ThemeMode.AUTO,
                    currentTheme,
                    onThemeSelected
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
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
    onDateFormatSelected: (PreferenceHelper.DateFormat) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.setting_date_format)) },
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
                Text(stringResource(R.string.action_close))
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





