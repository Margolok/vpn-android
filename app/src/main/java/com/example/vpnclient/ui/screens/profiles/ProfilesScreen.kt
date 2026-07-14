@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.vpnclient.ui.screens.profiles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vpnclient.ui.components.ProfileCard

@Composable
fun ProfilesScreen(onBack: () -> Unit) {
    val viewModel: ProfilesViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профили") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить профиль")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(error, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { viewModel.clearError() }) { Text("Ок") }
                    }
                }
            }

            if (uiState.profiles.isEmpty() && !uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нажмите + чтобы добавить первый профиль")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.profiles, key = { it.id }) { profile ->
                        ProfileCard(
                            profile = profile,
                            isSelected = false,
                            onClick = { /* редактирование профиля — TODO */ },
                            onDelete = { viewModel.deleteProfile(profile.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddProfileDialog(
            onDismiss = { showAddDialog = false },
            onAddLink = { link -> viewModel.addFromLink(link); showAddDialog = false },
            onAddSubscription = { name, url -> viewModel.addSubscription(name, url); showAddDialog = false }
        )
    }
}

@Composable
private fun AddProfileDialog(
    onDismiss: () -> Unit,
    onAddLink: (String) -> Unit,
    onAddSubscription: (String, String) -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var linkText by remember { mutableStateOf("") }
    var subName by remember { mutableStateOf("") }
    var subUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить профиль") },
        text = {
            Column {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Ссылка / WireGuard") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Подписка") })
                }
                Spacer(Modifier.height(16.dp))

                if (tab == 0) {
                    Text(
                        "Вставьте vmess://, vless://, trojan://, ss:// или содержимое WireGuard .conf",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = linkText,
                        onValueChange = { linkText = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        placeholder = { Text("vless://...") }
                    )
                } else {
                    OutlinedTextField(
                        value = subName,
                        onValueChange = { subName = it },
                        label = { Text("Название подписки") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subUrl,
                        onValueChange = { subUrl = it },
                        label = { Text("URL подписки") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (tab == 0) onAddLink(linkText) else onAddSubscription(subName.ifBlank { "Подписка" }, subUrl)
            }) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
