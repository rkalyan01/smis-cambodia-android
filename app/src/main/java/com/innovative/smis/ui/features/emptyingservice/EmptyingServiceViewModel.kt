package com.innovative.smis.ui.features.emptyingservice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.model.response.TodoItem
import com.innovative.smis.data.model.response.TodoFilter
import com.innovative.smis.data.model.response.ApplicationListResponse
import com.innovative.smis.data.repository.WorkflowRepository
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EmptyingServiceViewModel(
    private val repository: WorkflowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmptyingServiceUiState())
    val uiState: StateFlow<EmptyingServiceUiState> = _uiState.asStateFlow()

    init {
        // EmptyingService shows applications ready for emptying
        // Backend handles complex filtering: (site_visit_required=no & status=Scheduled) OR (site_visit_required=yes & status=Site-Preparation)
        loadItems(isInitialLoad = true)
    }

    fun setStatusFilter(status: String) {
        _uiState.update { it.copy(selectedStatus = status) }
        loadItems()
    }

    fun setDateFilter(startDateMillis: Long?, endDateMillis: Long?) {
        _uiState.update {
            it.copy(
                startDate = startDateMillis,
                endDate = endDateMillis,
                dateRangeText = formatDisplayDateRange(startDateMillis, endDateMillis)
            )
        }
        loadItems()
    }

    fun clearDateFilter() {
        _uiState.update {
            it.copy(
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

            // EmptyingService handles complex OR filtering via WorkflowRepository
            // Backend handles: (site_visit_required=no & status=Scheduled) OR (site_visit_required=yes & status=Site-Preparation)
            repository.getEmptyingServiceApplications()
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
                            // Handle cached data if available
                        }
                        is Resource.Success -> {
                            val applicationResponse = result.data
                            val applications = applicationResponse?.data?.map { app ->
                                TodoItem(
                                    applicationId = app.id.toIntOrNull() ?: 0,
                                    applicationDatetime = null,
                                    applicantName = app.applicant_name,
                                    applicantContact = null,
                                    proposedEmptyingDate = null,
                                    status = app.status
                                )
                            } ?: emptyList()
                            
                            _uiState.update {
                                it.copy(
                                    listState = Resource.Success(applications),
                                    applications = applications
                                )
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update {
                                it.copy(
                                    listState = Resource.Error(result.message, emptyList()),
                                    applications = emptyList(),
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

data class EmptyingServiceUiState(
    val listState: Resource<List<TodoItem>> = Resource.Idle(),
    val isRefreshing: Boolean = false,
    val applications: List<TodoItem> = emptyList(),
    val errorMessage: String? = null,
    val selectedStatus: String = "All", // EmptyingService shows all ready-to-empty applications
    val startDate: Long? = null,
    val endDate: Long? = null,
    val dateRangeText: String = "Filter by Date Range"
)
