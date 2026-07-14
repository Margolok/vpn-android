@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.vpnclient.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vpnclient.ui.components.BrandHeaderCompact
import com.example.vpnclient.ui.components.ConnectButton
import com.example.vpnclient.vpn.VpnState

@Composable
fun HomeScreen(onNavigateToProfiles: () -> Unit) {
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            uiState.selectedProfile?.let { viewModel.startVpn(it) }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { BrandHeaderCompact() }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            StatusLabel(uiState.vpnState)

            Spacer(Modifier.height(32.dp))

            ConnectButton(
                state = uiState.vpnState,
                onClick = {
                    val profile = uiState.selectedProfile
                    when (uiState.vpnState) {
                        is VpnState.Connected -> viewModel.disconnect()
                        is VpnState.Connecting -> { /* игнорируем повторные тапы во время подключения */ }
                        else -> profile?.let {
                            val prepareIntent = viewModel.connect(it)
                            if (prepareIntent != null) vpnPermissionLauncher.launch(prepareIntent)
                        }
                    }
                }
            )

            Spacer(Modifier.height(40.dp))

            Text("Активный профиль", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))

            if (uiState.profiles.isEmpty()) {
                EmptyProfilesHint(onNavigateToProfiles)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(uiState.profiles) { profile ->
                        ListItem(
                            headlineContent = { Text(profile.name) },
                            supportingContent = { Text("${profile.server}:${profile.port}") },
                            trailingContent = {
                                RadioButton(
                                    selected = profile.id == uiState.selectedProfile?.id,
                                    onClick = { viewModel.selectProfile(profile.id) }
                                )
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(onClick = onNavigateToProfiles, modifier = Modifier.fillMaxWidth()) {
                Text("Управление профилями")
            }
        }
    }
}

@Composable
private fun StatusLabel(state: VpnState) {
    val text = when (state) {
        VpnState.Disconnected -> "Отключено"
        VpnState.Connecting -> "Подключение…"
        is VpnState.Connected -> "Подключено к ${state.profileName}"
        is VpnState.Error -> "Ошибка: ${state.message}"
    }
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun EmptyProfilesHint(onNavigateToProfiles: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Профилей пока нет. Добавьте конфиг, чтобы подключиться.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onNavigateToProfiles) { Text("Добавить профиль") }
    }
}
