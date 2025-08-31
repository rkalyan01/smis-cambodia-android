package com.innovative.smis.ui.features.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.domain.repository.TaskRepository
import com.innovative.smis.data.repository.AuthRepository
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.constants.PrefConstant
import com.innovative.smis.util.helper.PreferenceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LogoutViewModel(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _logoutState = MutableStateFlow(LogoutState())
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    private val preferenceHelper = PreferenceHelper(context)

    data class LogoutState(
        val isLoading: Boolean = false,
        val pendingSyncCount: Int = 0,
        val isSyncing: Boolean = false,
        val syncComplete: Boolean = false,
        val error: String? = null
    )

    init {
        checkPendingSyncCount()
    }

    private fun checkPendingSyncCount() {
        viewModelScope.launch {
            val count = 0
            _logoutState.value = _logoutState.value.copy(pendingSyncCount = count)
        }
    }

    fun syncAndLogout() {
        viewModelScope.launch {
            _logoutState.value = _logoutState.value.copy(isSyncing = true, error = null)

            try {
                taskRepository.syncOfflineData().collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            _logoutState.value = _logoutState.value.copy(isSyncing = true)
                        }
                        is Resource.Success -> {
                            _logoutState.value = _logoutState.value.copy(
                                isSyncing = false,
                                syncComplete = true,
                                pendingSyncCount = 0
                            )
                            performLogout()
                        }
                        is Resource.Error -> {
                            _logoutState.value = _logoutState.value.copy(
                                isSyncing = false,
                                error = resource.message ?: "Sync failed"
                            )
                        }
                        is Resource.Idle -> {
                            //
                        }
                    }
                }
            } catch (e: Exception) {
                _logoutState.value = _logoutState.value.copy(
                    isSyncing = false,
                    error = "Sync failed: ${e.message}"
                )
            }
        }
    }

    fun logoutWithoutSync() {
        performLogout()
    }

    private fun performLogout() {
        viewModelScope.launch {
            _logoutState.value = _logoutState.value.copy(isLoading = true, error = null)

            authRepository.logout().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _logoutState.value = _logoutState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _logoutState.value = _logoutState.value.copy(
                            isLoading = false,
                            syncComplete = true
                        )
                    }
                    is Resource.Error -> {
                        _logoutState.value = _logoutState.value.copy(
                            isLoading = false,
                            syncComplete = true,
                            error = resource.message
                        )
                    }
                    is Resource.Idle -> {
                        //
                    }
                }
            }
        }
    }

    fun clearError() {
        _logoutState.value = _logoutState.value.copy(error = null)
    }

    fun resetState() {
        _logoutState.value = LogoutState()
        checkPendingSyncCount()
    }
}