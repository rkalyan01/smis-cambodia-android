package com.innovative.smis.ui.features.login

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovative.smis.data.model.request.LoginRequest
import com.innovative.smis.data.model.response.LoginResponse
import com.innovative.smis.data.repository.AuthRepository
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Modern LoginViewModel with mock authentication for role-based navigation testing
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<Resource<LoginResponse>>(Resource.Idle())
    val loginState: StateFlow<Resource<LoginResponse>> get() = _loginState

    private val _emailError = mutableStateOf<String?>(null)
    val emailError: State<String?> = _emailError

    private val _passwordError = mutableStateOf<String?>(null)
    val passwordError: State<String?> = _passwordError

    private val _passwordVisible = mutableStateOf(false)
    val passwordVisible: State<Boolean> = _passwordVisible

    // Real API authentication - no more mock service

    fun onLoginClicked(email: String, password: String) {
        _emailError.value = null
        _passwordError.value = null

        // Enhanced validation
        var hasError = false

        if (email.isBlank()) {
            _emailError.value = "Email is required"
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.value = "Please enter a valid email address"
            hasError = true
        }

        if (password.isBlank()) {
            _passwordError.value = "Password is required"
            hasError = true
        } else if (password.length < 3) {
            _passwordError.value = "Password must be at least 3 characters"
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _loginState.value = Resource.Loading()

            // Use real API for authentication
            authRepository.login(LoginRequest(email, password)).collect { result ->
                _loginState.value = result
            }
        }
    }

    fun togglePasswordVisibility() {
        _passwordVisible.value = !_passwordVisible.value
    }
}
