package com.example.vpnclient.vpn

sealed class VpnState {
    data object Disconnected : VpnState()
    data object Connecting : VpnState()
    data class Connected(val profileName: String, val connectedAtMs: Long) : VpnState()
    data class Error(val message: String) : VpnState()
}
