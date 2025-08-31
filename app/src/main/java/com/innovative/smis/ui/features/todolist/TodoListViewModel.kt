// file: com/innovative/smis/ui/features/todolist/TodoListViewModel.kt

package com.innovative.smis.ui.features.todolist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.model.response.TodoFilter
import com.innovative.smis.data.model.response.TodoItem
import com.innovative.smis.data.repository.TodoListRepository
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for the redesigned TodoList screen.
 * Manages UI state, including all filtering logic, and serves as the bridge
 * to the offline-first TodoListRepository.
 */
class TodoListViewModel(
    private val repository: TodoListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoListUiState())
    val uiState: StateFlow<TodoListUiState> = _uiState.asStateFlow()

    init {
        // Load initial data with default filters when the ViewModel is created.
        loadTodoItems(isInitialLoad = true)
    }

    // --- PUBLIC ACTIONS FROM THE UI ---

    fun setStatusFilter(status: String) {
        _uiState.update { it.copy(selectedStatus = status) }
        loadTodoItems() // Re-fetch data with the new filter.
    }

    fun setDateFilter(startDateMillis: Long?, endDateMillis: Long?) {
        _uiState.update {
            it.copy(
                startDate = startDateMillis,
                endDate = endDateMillis,
                dateRangeText = formatDisplayDateRange(startDateMillis, endDateMillis)
            )
        }
        loadTodoItems() // Re-fetch data with the new filter.
    }

    fun clearDateFilter() {
        _uiState.update {
            it.copy(
                startDate = null,
                endDate = null,
                dateRangeText = "Filter by Date Range"
            )
        }
        loadTodoItems() // Re-fetch data with the cleared filter.
    }

    fun refreshList() {
        loadTodoItems(isRefresh = true)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // --- PRIVATE DATA HANDLING ---

    private fun loadTodoItems(isInitialLoad: Boolean = false, isRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentState = uiState.value

            val filter = TodoFilter(
                status = if (currentState.selectedStatus.equals("All", true) || currentState.selectedStatus.equals("Today", true)) {
                    null
                } else {
                    currentState.selectedStatus
                },
                dateFrom = currentState.startDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) },
                dateTo = currentState.endDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) },
                isToday = currentState.selectedStatus.equals("Today", true)
            )

            repository.getFilteredTodoItems(filter)
                .onStart {
                    if (isInitialLoad) {
                        _uiState.update { it.copy(listState = Resource.Loading()) }
                    }
                    if (isRefresh) {
                        _uiState.update { it.copy(isRefreshing = true) }
                    }
                }
                .onCompletion {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
                .collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            result.data?.let { cachedItems ->
                                _uiState.update { it.copy(todoItems = cachedItems) }
                            }
                        }
                        is Resource.Success -> {
                            val items = result.data ?: emptyList()
                            _uiState.update {
                                it.copy(
                                    listState = Resource.Success(items),
                                    todoItems = items
                                )
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update {
                                it.copy(
                                    listState = Resource.Error(result.message, result.data),
                                    todoItems = result.data ?: emptyList(),
                                    errorMessage = result.message
                                )
                            }
                        }
                        is Resource.Idle -> { /* No-op */ }
                    }
                }
        }
    }

    private fun formatDisplayDateRange(startMillis: Long?, endMillis: Long?): String {
        if (startMillis == null) return "Filter by Date Range"
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val startDate = formatter.format(Date(startMillis))
        val endDate = endMillis?.let { formatter.format(Date(it)) } ?: startDate
        return if (startDate == endDate) startDate else "$startDate - $endDate"
    }
}

/**
 * Represents the complete state of the TodoList UI.
 */
data class TodoListUiState(
    val listState: Resource<List<TodoItem>> = Resource.Idle(),
    val isRefreshing: Boolean = false,
    val todoItems: List<TodoItem> = emptyList(),
    val errorMessage: String? = null,
    // Filter State
    val selectedStatus: String = "All",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val dateRangeText: String = "Filter by Date Range"
)
