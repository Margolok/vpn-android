package com.example.vpnclient.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vpnclient.VpnClientApp
import com.example.vpnclient.data.model.AuthUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val user: AuthUser? = null,
    val bypassRussianServices: Boolean = true
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = (application as VpnClientApp).authRepository
    private val settingsRepository = application.settingsRepository

    val uiState: StateFlow<SettingsUiState> = combine(
        authRepository.currentUser,
        settingsRepository.bypassRussianServices
    ) { user, bypass ->
        SettingsUiState(user = user, bypassRussianServices = bypass)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setBypassRussianServices(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBypassRussianServices(enabled) }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
