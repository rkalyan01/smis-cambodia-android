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
    private val availableStatuses = listOf("Rescheduled", "Emptied", "Completed", "Pending", "Cancelled", "Reassigned")

    init {
        _uiState.update {
            it.copy(
                availableStatuses = availableStatuses,
                selectedStatus = availableStatuses.first() // Set to first status by default
            )
        }
    }

    fun loadTasks() {
        // Load all tasks initially (empty status means all)
        loadTasksWithStatus("")
    }

    private fun loadTasksWithStatus(status: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.getTaskManagementApplications(status).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val tasks = result.data ?: emptyList()
                        _uiState.update {
                            it.copy(
                                tasks = tasks,
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
        loadTasksWithStatus(status)
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