package com.innovative.smis.ui.features.login

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.innovative.smis.data.model.response.LoginResponse
import com.innovative.smis.data.repository.AuthRepository
import com.innovative.smis.util.common.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<Resource<LoginResponse>>(Resource.Idle())
    val loginState: StateFlow<Resource<LoginResponse>> get() = _loginState

    private val _emailError = mutableStateOf<String?>(null)
    val emailError: State<String?> = _emailError

    private val _passwordError = mutableStateOf<String?>(null)
    val passwordError: State<String?> = _passwordError

    private val _passwordVisible = mutableStateOf(false)
    val passwordVisible: State<Boolean> = _passwordVisible

    fun onLoginClicked(email: String, password: String) {
        _emailError.value = null
        _passwordError.value = null

        if (email.isBlank()) {
            _emailError.value = "Email cannot be empty"
            return
        }
        if (password.isBlank()) {
            _passwordError.value = "Password cannot be empty"
            return
        }

        val loginRequest = JsonObject().apply {
            addProperty("email", email)
            addProperty("password", password)
        }

        viewModelScope.launch {
            // 1. Set state to Loading immediately.
            _loginState.value = Resource.Loading()

            // 2. Collect the result from the repository's flow.
            authRepository.login(loginRequest)
                .collect { result ->
                    _loginState.value = result
                }
        }
    }

    fun togglePasswordVisibility() {
        _passwordVisible.value = !_passwordVisible.value
    }
}
