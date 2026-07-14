package com.example.vpnclient.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vpnclient.VpnClientApp
import com.example.vpnclient.data.db.ProfileJson
import com.example.vpnclient.data.model.Profile
import com.example.vpnclient.vpn.VpnConnectionService
import com.example.vpnclient.vpn.VpnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val profiles: List<Profile> = emptyList(),
    val selectedProfileId: String? = null,
    val vpnState: VpnState = VpnState.Disconnected
) {
    val selectedProfile: Profile? get() = profiles.find { it.id == selectedProfileId } ?: profiles.firstOrNull()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as VpnClientApp).repository
    private val settingsRepository = (application as VpnClientApp).settingsRepository
    private val _selectedProfileId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeProfiles(),
        _selectedProfileId,
        VpnConnectionService.state
    ) { profiles, selectedId, vpnState ->
        HomeUiState(profiles = profiles, selectedProfileId = selectedId, vpnState = vpnState)
    }.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        HomeUiState()
    )

    fun selectProfile(id: String) {
        _selectedProfileId.value = id
    }

    /** Возвращает Intent для VpnService.prepare(), если нужно системное разрешение. Null — уже разрешено. */
    fun connect(profile: Profile): android.content.Intent? {
        val prepareIntent = android.net.VpnService.prepare(getApplication())
        if (prepareIntent != null) return prepareIntent
        startVpn(profile)
        return null
    }

    fun startVpn(profile: Profile) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val bypassRuServices = settingsRepository.bypassRussianServices.first()
            val intent = android.content.Intent(context, VpnConnectionService::class.java).apply {
                action = VpnConnectionService.ACTION_CONNECT
                putExtra(VpnConnectionService.EXTRA_PROFILE_JSON, ProfileJson.toJson(profile))
                putExtra(VpnConnectionService.EXTRA_BYPASS_RU_SERVICES, bypassRuServices)
            }
            context.startService(intent)
        }
    }

    fun disconnect() {
        val context = getApplication<Application>()
        val intent = android.content.Intent(context, VpnConnectionService::class.java).apply {
            action = VpnConnectionService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }
}
