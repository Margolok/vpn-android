package com.example.vpnclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.vpnclient.ui.theme.ConnectedGreen
import com.example.vpnclient.ui.theme.ErrorRed
import com.example.vpnclient.vpn.VpnState

@Composable
fun ConnectButton(
    state: VpnState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = when (state) {
        is VpnState.Connected -> ConnectedGreen
        is VpnState.Connecting -> MaterialTheme.colorScheme.primary
        is VpnState.Error -> ErrorRed
        VpnState.Disconnected -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .size(160.dp)
            .background(color.copy(alpha = 0.15f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is VpnState.Connecting -> CircularProgressIndicator(color = Color.White)
                is VpnState.Connected -> Icon(Icons.Default.Close, contentDescription = "Отключить", tint = Color.White, modifier = Modifier.size(48.dp))
                else -> Icon(Icons.Default.PowerSettingsNew, contentDescription = "Подключить", tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }
    }
}
