package com.example.vpnclient.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vpnclient.VpnClientApp
import com.example.vpnclient.data.auth.AuthResult
import com.example.vpnclient.data.model.AuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = (application as VpnClientApp).authRepository

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun toggleMode() {
        _uiState.value = _uiState.value.copy(isRegisterMode = !_uiState.value.isRegisterMode, errorMessage = null)
    }

    fun onProviderClick(context: android.content.Context, provider: AuthProvider) {
        authRepository.startOAuthLogin(context, provider)
    }

    fun submitEmailForm() {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = if (state.isRegisterMode) {
                authRepository.registerWithEmail(state.email, state.password)
            } else {
                authRepository.loginWithEmail(state.email, state.password)
            }
            _uiState.value = when (result) {
                is AuthResult.Success -> _uiState.value.copy(isLoading = false, errorMessage = null)
                is AuthResult.Failure -> _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }
}
