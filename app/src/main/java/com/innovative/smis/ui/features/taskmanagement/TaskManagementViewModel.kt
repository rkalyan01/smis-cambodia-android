package com.innovative.smis.ui.features.taskmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.repository.TaskManagementRepository
import com.innovative.smis.data.model.response.TodoItem
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskManagementViewModel(
    private val repository: TaskManagementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskManagementUiState())
    val uiState: StateFlow<TaskManagementUiState> = _uiState.asStateFlow()

    // Predefined status filters for Task Management
    private val availableStatuses = listOf("All", "Today", "Urgent", "On-Demand", "Initiated", "Scheduled", "Rescheduled", "Site-Preparation", "Emptied", "Completed")

    init {
        _uiState.update {
            it.copy(
                availableStatuses = availableStatuses,
                selectedStatus = "All" // Default to "All"
            )
        }
        // Load all tasks initially
        setStatusFilter("All")
    }

    fun loadTasks() {
        // Load all tasks initially (empty status means all)
        loadTasksWithStatus("")
    }

    private fun loadTasksWithStatus(status: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Determine if this is an "Urgent" or "On-Demand" filter request
            val currentStatus = _uiState.value.selectedStatus
            val urgency = if (currentStatus.equals("Urgent", true)) "yes" else null
            val applicationType = when {
                currentStatus.equals("Urgent", true) -> "On-Demand"
                currentStatus.equals("On-Demand", true) -> "On-Demand"
                else -> null
            }

            repository.getTaskManagementApplications(
                status = status,
                urgency = urgency,
                applicationType = applicationType
            ).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val tasks = result.data ?: emptyList()
                        
                        // For "Today" filter, apply client-side date filtering with status-aware logic
                        // For "Urgent" and "On-Demand" filters, data is already filtered by API
                        val currentStatus = _uiState.value.selectedStatus
                        val filteredTasks = if (currentStatus.equals("Today", true)) {
                            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                            tasks.filter { task ->
                                // For "Initiated" status, use application_datetime; for others, use proposed_emptying_date
                                if (task.status?.equals("Initiated", ignoreCase = true) == true) {
                                    task.applicationDatetime?.substring(0, 10) == todayStr
                                } else {
                                    task.proposedEmptyingDate == todayStr
                                }
                            }
                        } else {
                            tasks // API already handles Urgent and On-Demand filters
                        }
                        
                        // Sort urgent items to the top (urgent first, then rest)
                        val sortedTasks = filteredTasks.sortedByDescending { item ->
                            item.urgency?.equals("yes", ignoreCase = true) == true
                        }
                        
                        _uiState.update {
                            it.copy(
                                tasks = sortedTasks,
                                isLoading = false
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message
                            )
                        }
                    }
                    is Resource.Loading -> {
                        // Loading state already set
                    }
                    else -> {}
                }
            }
        }
    }

    fun setStatusFilter(status: String) {
        _uiState.update { it.copy(selectedStatus = status) }
        // Load tasks with the specific status filter
        // Handle special filters: All, Today (client-side), Urgent, On-Demand (API-side)
        val filterStatus = when (status) {
            "All", "Today" -> "" // These are handled client-side
            "Urgent", "On-Demand" -> "" // API-side filter via urgency & applicationType params
            else -> status
        }
        loadTasksWithStatus(filterStatus)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class TaskManagementUiState(
    val isLoading: Boolean = false,
    val tasks: List<TodoItem> = emptyList(),
    val availableStatuses: List<String> = emptyList(),
    val selectedStatus: String = "Rescheduled", // Default to first filter
    val errorMessage: String? = null
)