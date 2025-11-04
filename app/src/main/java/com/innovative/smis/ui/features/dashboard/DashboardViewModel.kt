package com.innovative.smis.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.model.response.TodoFilter
import com.innovative.smis.data.model.response.TodoItem
import com.innovative.smis.data.repository.TodoListRepository
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.helper.PreferenceHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(
    private val todoListRepository: TodoListRepository,
    private val preferenceHelper: PreferenceHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        android.util.Log.d("DashboardViewModel", "🚀 DashboardViewModel initialized - starting data load")
        loadTodoItems(isInitialLoad = true)
    }

    fun setStatusFilter(status: String) {
        _uiState.update { it.copy(selectedStatus = status) }
        loadTodoItems()
    }



    fun refreshApplications() {
        loadTodoItems(isRefresh = true)
    }

    fun syncOfflineData() = viewModelScope.launch { loadTodoItems() }
    fun selectApplication(application: TodoItem) { _uiState.update { it.copy(selectedApplication = application) } }
    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
    fun updateLanguage(language: String) { preferenceHelper.selectedLanguage = language }
    fun getPreferenceHelper(): PreferenceHelper = preferenceHelper

    private fun loadTodoItems(isInitialLoad: Boolean = false, isRefresh: Boolean = false) {
        android.util.Log.d("DashboardViewModel", "📋 Starting loadTodoItems - isInitialLoad: $isInitialLoad, isRefresh: $isRefresh")
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()) {
            try {
                val currentState = uiState.value
                android.util.Log.d("DashboardViewModel", "📊 Current selected status: ${currentState.selectedStatus}")

                val filter = TodoFilter(
                    status = if (currentState.selectedStatus.equals("All", true) || currentState.selectedStatus.equals("Today", true)) {
                        null
                    } else {
                        currentState.selectedStatus // Pass "Urgent" to repository
                    },
                    isToday = currentState.selectedStatus.equals("Today", true)
                )
                android.util.Log.d("DashboardViewModel", "🔍 Filter created: status=${filter.status}, isToday=${filter.isToday}")

                todoListRepository.getFilteredTodoItems(filter)
                    .onStart {
                        android.util.Log.d("DashboardViewModel", "🔄 Repository flow started")
                        if (isInitialLoad) {
                            android.util.Log.d("DashboardViewModel", "📱 Setting Loading state for initial load")
                            _uiState.update { it.copy(applicationLoadingState = Resource.Loading()) }
                        }
                        if (isRefresh) {
                            android.util.Log.d("DashboardViewModel", "🔄 Setting refresh state")
                            _uiState.update { it.copy(isRefreshing = true) }
                        }
                    }
                    .onCompletion {
                        android.util.Log.d("DashboardViewModel", "✅ Repository flow completed")
                        _uiState.update { it.copy(isRefreshing = false) }
                    }
                    .collect { result ->
                        android.util.Log.d("DashboardViewModel", "📦 Repository result: ${result::class.simpleName}")
                        when (result) {
                            is Resource.Loading -> {
                                android.util.Log.d("DashboardViewModel", "🔄 Loading state - cached data: ${result.data?.size ?: 0} items")
                                result.data?.let { cachedItems ->
                                    _uiState.update {
                                        it.copy(applications = cachedItems)
                                    }
                                }
                            }
                            is Resource.Success -> {
                                val items = result.data ?: emptyList()
                                android.util.Log.d("DashboardViewModel", "✅ Success state - ${items.size} items loaded")
                                
                                // Sort urgent items to the top (urgent first, then rest)
                                val sortedItems = items.sortedByDescending { item ->
                                    item.urgency?.equals("yes", ignoreCase = true) == true
                                }
                                
                                android.util.Log.d("DashboardViewModel", "📋 Final items: ${sortedItems.size} (filter: ${currentState.selectedStatus})")
                                
                                _uiState.update {
                                    it.copy(
                                        applicationLoadingState = Resource.Success(sortedItems),
                                        applications = sortedItems
                                    )
                                }
                            }
                            is Resource.Error -> {
                                android.util.Log.e("DashboardViewModel", "❌ Error state: ${result.message}")
                                _uiState.update {
                                    it.copy(
                                        applicationLoadingState = Resource.Error(result.message, result.data),
                                        applications = result.data ?: emptyList(), // Keep showing old data on error
                                        errorMessage = result.message
                                    )
                                }
                            }
                            is Resource.Idle -> {
                                android.util.Log.w("DashboardViewModel", "⚠️ Repository returned Idle state - this might cause white screen!")
                            }
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "💥 Exception in loadTodoItems: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        applicationLoadingState = Resource.Error("Failed to load data: ${e.message}", emptyList()),
                        errorMessage = "Failed to load data: ${e.message}"
                    )
                }
            }
        }
    }


}

data class DashboardUiState(
    val applicationLoadingState: Resource<List<TodoItem>> = Resource.Loading(), // Start with Loading to prevent white screen
    val isRefreshing: Boolean = false, // <-- NEW STATE FOR PULL-TO-REFRESH
    val applications: List<TodoItem> = emptyList(),
    val selectedApplication: TodoItem? = null,
    val errorMessage: String? = null,

    val selectedStatus: String = "All"
)
