@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.vpnclient.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vpnclient.data.model.AuthProvider

@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Настройки") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            ListItem(
                headlineContent = { Text(uiState.user?.displayName ?: "Гость") },
                supportingContent = {
                    Text(
                        uiState.user?.email
                            ?: uiState.user?.provider?.let { providerLabel(it) }
                            ?: "Не авторизован"
                    )
                }
            )
            if (uiState.user != null) {
                TextButton(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) {
                    Text("Выйти из аккаунта")
                }
            }

            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Не обходить российские сервисы") },
                supportingContent = {
                    Text("Госуслуги, банки, VK и Яндекс будут работать напрямую, минуя VPN-туннель")
                },
                trailingContent = {
                    Switch(
                        checked = uiState.bypassRussianServices,
                        onCheckedChange = viewModel::setBypassRussianServices
                    )
                }
            )

            HorizontalDivider()

            ListItem(headlineContent = { Text("Ядро VPN") }, supportingContent = { Text("Не подключено (заглушка)") })
            ListItem(headlineContent = { Text("Версия") }, supportingContent = { Text("0.1.0") })
        }
    }
}

private fun providerLabel(provider: AuthProvider): String = when (provider) {
    AuthProvider.GOOGLE -> "Google"
    AuthProvider.YANDEX -> "Яндекс ID"
    AuthProvider.VK -> "VK ID"
    AuthProvider.GOSUSLUGI -> "Госуслуги"
    AuthProvider.EMAIL -> "Почта"
}
