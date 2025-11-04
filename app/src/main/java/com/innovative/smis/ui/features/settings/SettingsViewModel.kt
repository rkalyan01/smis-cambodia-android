package com.innovative.smis.ui.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.innovative.smis.util.constants.Languages
import com.innovative.smis.util.helper.LocalizationHelper
import com.innovative.smis.util.helper.PreferenceHelper
import com.innovative.smis.data.repository.EmptyingSchedulingRepository
import com.innovative.smis.data.local.offline.OfflineMapManager
import com.innovative.smis.data.local.cache.OfflineCacheManager
import com.innovative.smis.data.local.database.SMISDatabase
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import java.io.File

class SettingsViewModel(
    application: Application,
    private val preferenceHelper: PreferenceHelper,
    private val emptyingSchedulingRepository: EmptyingSchedulingRepository,
    private val offlineMapManager: OfflineMapManager,
    private val offlineCacheManager: OfflineCacheManager,
    private val database: SMISDatabase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    data class SettingsUiState(
        val selectedLanguage: String = Languages.KHMER,
        val themeMode: PreferenceHelper.ThemeMode = PreferenceHelper.ThemeMode.AUTO,
        val dateFormat: PreferenceHelper.DateFormat = PreferenceHelper.DateFormat.DD_MM_YYYY,
        val isOfflineModeEnabled: Boolean = true,
        val isAutoSyncEnabled: Boolean = true,
        val isLoading: Boolean = false,
        val message: String? = null,
        val isSyncing: Boolean = false,
        val syncResult: String? = null,
        val databaseSizeMB: Double = 0.0,
        val isClearingCache: Boolean = false,
        val cacheCleared: Boolean = false,
        val shouldLogout: Boolean = false
    )

    init {
        loadSettings()
        calculateDatabaseSize()
    }

    private fun loadSettings() {
        _uiState.value = _uiState.value.copy(
            selectedLanguage = preferenceHelper.selectedLanguage,
            themeMode = preferenceHelper.themeMode,
            dateFormat = preferenceHelper.dateFormat,
            isOfflineModeEnabled = preferenceHelper.isOfflineModeEnabled,
            isAutoSyncEnabled = preferenceHelper.isAutoSyncEnabled
        )
    }

    private val _shouldRestartActivity = MutableStateFlow(false)
    val shouldRestartActivity: StateFlow<Boolean> = _shouldRestartActivity.asStateFlow()
    
    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            try {
                preferenceHelper.selectedLanguage = languageCode

                _uiState.value = _uiState.value.copy(
                    selectedLanguage = languageCode,
                    message = if (languageCode == Languages.KHMER) "ភាសាត្រូវបានផ្លាស់ប្តូរ" else "Language changed successfully"
                )

                // Signal to restart the activity for language change
                kotlinx.coroutines.delay(500)
                _shouldRestartActivity.value = true

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = if (_uiState.value.selectedLanguage == Languages.KHMER)
                        "កំហុសក្នុងការផ្លាស់ប្តូរភាសា" else "Failed to change language"
                )
            }
        }
    }
    
    fun activityRestarted() {
        _shouldRestartActivity.value = false
    }

    fun setThemeMode(themeMode: PreferenceHelper.ThemeMode) {
        try {
            preferenceHelper.themeMode = themeMode
            _uiState.value = _uiState.value.copy(
                themeMode = themeMode,
                message = if (_uiState.value.selectedLanguage == Languages.KHMER)
                    "រូបរាងត្រូវបានផ្លាស់ប្តូរ" else "Theme changed successfully"
            )

            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(message = null)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                message = if (_uiState.value.selectedLanguage == Languages.KHMER)
                    "កំហុសក្នុងការផ្លាស់ប្តូររូបរាង" else "Failed to change theme"
            )
        }
    }

    fun setDateFormat(dateFormat: PreferenceHelper.DateFormat) {
        try {
            preferenceHelper.dateFormat = dateFormat
            _uiState.value = _uiState.value.copy(
                dateFormat = dateFormat,
                message = if (_uiState.value.selectedLanguage == Languages.KHMER)
                    "ទម្រង់កាលបរិច្ឆេទត្រូវបានផ្លាស់ប្តូរ" else "Date format changed successfully"
            )

            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(message = null)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                message = if (_uiState.value.selectedLanguage == Languages.KHMER)
                    "កំហុសក្នុងការផ្លាស់ប្តូរទម្រង់កាលបរិច្ឆេទ" else "Failed to change date format"
            )
        }
    }

    fun setOfflineMode(enabled: Boolean) {
        try {
            preferenceHelper.isOfflineModeEnabled = enabled
            _uiState.value = _uiState.value.copy(
                isOfflineModeEnabled = enabled,
                message = if (_uiState.value.selectedLanguage == Languages.KHMER) {
                    if (enabled) "រមៀបបណ្តាញត្រូវបានបើក" else "របៀបបណ្តាញត្រូវបានបិទ"
                } else {
                    if (enabled) "Offline mode enabled" else "Offline mode disabled"
                }
            )

            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(message = null)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                message = if (_uiState.value.selectedLanguage == Languages.KHMER)
                    "កំហុសក្នុងការកំណត់របៀបបណ្តាញ" else "Failed to update offline mode"
            )
        }
    }

    fun setAutoSync(enabled: Boolean) {
        try {
            preferenceHelper.isAutoSyncEnabled = enabled
            _uiState.value = _uiState.value.copy(
                isAutoSyncEnabled = enabled,
                message = if (_uiState.value.selectedLanguage == Languages.KHMER) {
                    if (enabled) "ធ្វើសមកាលកម្មស្វ័យប្រវត្តិត្រូវបានបើក" else "ធ្វើសមកាលកម្មស្វ័យប្រវត្តិត្រូវបានបិទ"
                } else {
                    if (enabled) "Auto sync enabled" else "Auto sync disabled"
                }
            )

            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                _uiState.value = _uiState.value.copy(message = null)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                message = if (_uiState.value.selectedLanguage == Languages.KHMER)
                    "កំហុសក្នុងការកំណត់ធ្វើសមកាលកម្មស្វ័យប្រវត្តិ" else "Failed to update auto sync"
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun testManualSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncResult = null)
            
            try {
                println("DEBUG: Manual sync test started")
                
                // Check sync queue status
                val pendingSyncs = emptyingSchedulingRepository.getPendingSyncs()
                println("DEBUG: Found ${pendingSyncs.size} pending syncs in queue")
                
                if (pendingSyncs.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncResult = if (_uiState.value.selectedLanguage == Languages.KHMER) {
                            "មិនមានទិន្នន័យនៅក្នុងជួរស្តង់ដារ។ សូមបញ្ចូលទម្រង់ម្តងទៀត។"
                        } else {
                            "No data in sync queue. Please save a form first."
                        }
                    )
                    return@launch
                }
                
                // Show pending sync details
                pendingSyncs.forEach { sync ->
                    println("DEBUG: Pending sync - Type: ${sync.entityType}, ID: ${sync.entityId}, Retries: ${sync.retryCount}")
                }
                
                // Also check for failed forms
                val failedForms = emptyingSchedulingRepository.getFailedForms()
                println("DEBUG: Found ${failedForms.size} failed forms that won't be retried")
                failedForms.forEach { form ->
                    println("DEBUG: Failed form - ID: ${form.applicationId}, App ID: ${form.applicationId}, Error: ${form.errorMessage}")
                }
                
                // Attempt sync
                val result = emptyingSchedulingRepository.syncPendingForms()
                println("DEBUG: Sync completed with result: $result")
                
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncResult = if (_uiState.value.selectedLanguage == Languages.KHMER) {
                        "ស្ថានភាពសមកាលកម្ម: $result"
                    } else {
                        "Sync Status: $result"
                    }
                )
                
                // Clear result after 5 seconds
                kotlinx.coroutines.delay(5000)
                _uiState.value = _uiState.value.copy(syncResult = null)
                
            } catch (e: Exception) {
                println("DEBUG: Sync failed with error: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncResult = if (_uiState.value.selectedLanguage == Languages.KHMER) {
                        "កំហុសសមកាលកម្ម: ${e.message}"
                    } else {
                        "Sync Error: ${e.message}"
                    }
                )
                
                // Clear result after 5 seconds
                kotlinx.coroutines.delay(5000)
                _uiState.value = _uiState.value.copy(syncResult = null)
            }
        }
    }

    private fun calculateDatabaseSize() {
        viewModelScope.launch {
            try {
                // Calculate database file size directly - more reliable approach
                val context = getApplication<Application>().applicationContext
                val dbPath = context.getDatabasePath("smis_database")
                val sizeInBytes = if (dbPath.exists()) dbPath.length() else 0L
                val sizeInMB = sizeInBytes / (1024.0 * 1024.0)
                
                Log.d("SettingsViewModel", "Database file size: ${sizeInMB}MB at path: ${dbPath.absolutePath}")
                _uiState.value = _uiState.value.copy(databaseSizeMB = sizeInMB)
                
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to calculate database size", e)
                _uiState.value = _uiState.value.copy(databaseSizeMB = 0.0)
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isClearingCache = true, cacheCleared = false)
                
                Log.d("SettingsViewModel", "Starting cache clearing process...")
                
                // Clear all database tables comprehensively
                try {
                    // Clear all offline map data
                    database.offlineMapTileDao().clearAllTiles()
                    database.offlineBuildingPolygonDao().clearAllBuildings()
                    database.offlineMapAreaDao().clearAllAreas()
                    database.offlinePOIDao().clearAllPOIs()
                    
                    // Clear form data tables
                    database.emptyingSchedulingFormDao().clearAll()
                    database.sitePreparationFormDao().clearAll()
                    database.emptyingServiceFormDao().clearAll()
                    
                    // Clear ALL todo items/applications (the complete applications list)
                    database.todoItemDao().clearAll()
                    
                    Log.d("SettingsViewModel", "Database tables cleared successfully")
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Failed to clear database tables", e)
                }
                
                // Clear application cache directories
                val context = getApplication<Application>().applicationContext
                try {
                    // Clear app cache directory
                    context.cacheDir.listFiles()?.forEach { file ->
                        try {
                            if (file.isFile) {
                                file.delete()
                            } else if (file.isDirectory) {
                                file.deleteRecursively()
                            }
                        } catch (e: Exception) {
                            Log.w("SettingsViewModel", "Failed to delete cache file: ${file.name}", e)
                        }
                    }
                    
                    // Clear external cache if available
                    context.externalCacheDir?.listFiles()?.forEach { file ->
                        try {
                            if (file.isFile) {
                                file.delete()
                            } else if (file.isDirectory) {
                                file.deleteRecursively()
                            }
                        } catch (e: Exception) {
                            Log.w("SettingsViewModel", "Failed to delete external cache file: ${file.name}", e)
                        }
                    }
                    
                    Log.d("SettingsViewModel", "Cache directories cleared successfully")
                } catch (e: Exception) {
                    Log.w("SettingsViewModel", "Failed to clear app cache directories", e)
                }
                
                // Force a small delay to ensure all operations complete
                kotlinx.coroutines.delay(500)
                
                // Recalculate database size after clearing
                calculateDatabaseSize()
                
                val successMessage = if (_uiState.value.selectedLanguage == Languages.KHMER) 
                    "លុបទិន្នន័យបណ្តោះអាសន្នជោគជ័យ" else "Cache cleared successfully"
                    
                _uiState.value = _uiState.value.copy(
                    isClearingCache = false,
                    cacheCleared = true,
                    message = successMessage
                )
                
                Log.d("SettingsViewModel", "Cache clearing completed successfully")
                
                // Auto-hide message after 3 seconds
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(message = null, cacheCleared = false)
                
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to clear cache", e)
                val errorMessage = if (_uiState.value.selectedLanguage == Languages.KHMER)
                    "កំហុសក្នុងការលុបទិន្នន័យបណ្តោះអាសន្ន" else "Failed to clear cache"
                    
                _uiState.value = _uiState.value.copy(
                    isClearingCache = false,
                    message = errorMessage
                )
                
                // Auto-hide error message after 3 seconds
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(message = null)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                Log.d("SettingsViewModel", "Logging out user...")
                
                // Clear all user preferences and auth data
                preferenceHelper.clearAll()
                
                // Clear ALL database tables to ensure no residual user data persists
                database.clearAllTables()
                
                Log.d("SettingsViewModel", "All user data and database cleared successfully")
                
                // Signal to navigate to login
                _uiState.value = _uiState.value.copy(shouldLogout = true)
                
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to logout", e)
                _uiState.value = _uiState.value.copy(
                    message = if (_uiState.value.selectedLanguage == Languages.KHMER)
                        "កំហុសក្នុងការចាកចេញ" else "Failed to logout"
                )
            }
        }
    }

    fun resetLogoutFlag() {
        _uiState.value = _uiState.value.copy(shouldLogout = false)
    }
}