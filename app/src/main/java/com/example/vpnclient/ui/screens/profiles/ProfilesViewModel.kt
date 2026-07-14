package com.example.vpnclient.ui.screens.profiles

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vpnclient.VpnClientApp
import com.example.vpnclient.data.model.Profile
import com.example.vpnclient.data.model.SubscriptionGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfilesUiState(
    val profiles: List<Profile> = emptyList(),
    val groups: List<SubscriptionGroup> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProfilesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as VpnClientApp).repository
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProfilesUiState> = combine(
        repository.observeProfiles(),
        repository.observeGroups(),
        _isLoading,
        _errorMessage
    ) { profiles, groups, loading, error ->
        ProfilesUiState(profiles, groups, loading, error)
    }.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        ProfilesUiState()
    )

    fun addFromLink(link: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.addFromLink(link)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось разобрать конфиг"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSubscription(name: String, url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.addSubscription(name, url)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось загрузить подписку"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshSubscription(group: SubscriptionGroup) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.refreshSubscription(group)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Не удалось обновить подписку"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
