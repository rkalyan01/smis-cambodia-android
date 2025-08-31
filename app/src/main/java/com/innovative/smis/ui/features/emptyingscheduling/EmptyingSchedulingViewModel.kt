package com.innovative.smis.ui.features.emptyingscheduling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.model.response.TodoFilter
import com.innovative.smis.data.model.response.TodoItem
import com.innovative.smis.data.repository.EmptyingSchedulingRepository
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EmptyingSchedulingViewModel(
    private val repository: EmptyingSchedulingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmptyingSchedulingUiState())
    val uiState: StateFlow<EmptyingSchedulingUiState> = _uiState.asStateFlow()

    init {
        setStatusFilter("Initiated")
    }

    fun setStatusFilter(status: String) {
        _uiState.update { it.copy(selectedStatus = status) }
        loadItems()
    }

    fun setDateFilter(startDateMillis: Long?, endDateMillis: Long?) {
        _uiState.update { currentState ->
            currentState.copy(
                startDate = startDateMillis,
                endDate = endDateMillis,
                dateRangeText = formatDisplayDateRange(startDateMillis, endDateMillis)
            )
        }
        loadItems()
    }

    fun clearDateFilter() {
        _uiState.update { currentState ->
            currentState.copy(
                startDate = null,
                endDate = null,
                dateRangeText = "Filter by Date Range"
            )
        }
        loadItems()
    }

    fun refreshList() {
        loadItems(isRefresh = true)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadItems(isInitialLoad: Boolean = false, isRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentState = uiState.value

            repository.getApplicationsByStatus(currentState.selectedStatus)
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
                                // Apply status filtering to cached items too
                                val filteredCachedItems = cachedItems.filter { item ->
                                    item.status.equals(currentState.selectedStatus, ignoreCase = true)
                                }
                                _uiState.update { it.copy(applications = filteredCachedItems) }
                            }
                        }
                        is Resource.Success -> {
                            val allItems = result.data ?: emptyList()
                            // Filter items by selected status
                            val filteredItems = allItems.filter { item ->
                                item.status.equals(currentState.selectedStatus, ignoreCase = true)
                            }
                            _uiState.update {
                                it.copy(
                                    listState = Resource.Success(filteredItems),
                                    applications = filteredItems
                                )
                            }
                        }
                        is Resource.Error -> {
                            // Apply status filtering to error fallback data too
                            val filteredErrorData = result.data?.filter { item ->
                                item.status.equals(currentState.selectedStatus, ignoreCase = true)
                            } ?: emptyList()
                            _uiState.update {
                                it.copy(
                                    listState = Resource.Error(result.message, filteredErrorData),
                                    applications = filteredErrorData,
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

data class EmptyingSchedulingUiState(
    val listState: Resource<List<TodoItem>> = Resource.Idle(),
    val isRefreshing: Boolean = false,
    val applications: List<TodoItem> = emptyList(),
    val errorMessage: String? = null,

    val selectedStatus: String = "Scheduled",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val dateRangeText: String = "Filter by Date Range"
)
