package com.example.vpnclient.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vpnclient.data.model.AuthProvider
import com.example.vpnclient.ui.components.BrandHeader
import com.example.vpnclient.ui.components.ProviderButton
import com.example.vpnclient.ui.theme.GosuslugiButton
import com.example.vpnclient.ui.theme.VkButton
import com.example.vpnclient.ui.theme.YandexButton

@Composable
fun LoginScreen() {
    val viewModel: LoginViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandHeader(tagline = "Быстрый и надёжный VPN — без компромиссов")

        Spacer(Modifier.height(36.dp))

        Text(
            "Войдите, чтобы синхронизировать профили и настройки",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        ProviderButton(
            text = "Продолжить с Google",
            containerColor = Color.White,
            contentColor = Color(0xFF1F1F1F),
            outlined = true,
            onClick = { viewModel.onProviderClick(context, AuthProvider.GOOGLE) }
        )
        Spacer(Modifier.height(12.dp))
        ProviderButton(
            text = "Продолжить с Яндекс ID",
            containerColor = YandexButton,
            contentColor = Color.White,
            onClick = { viewModel.onProviderClick(context, AuthProvider.YANDEX) }
        )
        Spacer(Modifier.height(12.dp))
        ProviderButton(
            text = "Продолжить с VK ID",
            containerColor = VkButton,
            contentColor = Color.White,
            onClick = { viewModel.onProviderClick(context, AuthProvider.VK) }
        )
        Spacer(Modifier.height(12.dp))
        ProviderButton(
            text = "Продолжить с Госуслугами",
            containerColor = GosuslugiButton,
            contentColor = Color.White,
            onClick = { viewModel.onProviderClick(context, AuthProvider.GOSUSLUGI) }
        )

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                "  или по почте  ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Электронная почта") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        uiState.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = viewModel::submitEmailForm,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Color.White)
            } else {
                Text(if (uiState.isRegisterMode) "Зарегистрироваться" else "Войти")
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = viewModel::toggleMode) {
            Text(
                if (uiState.isRegisterMode) "Уже есть аккаунт? Войти"
                else "Нет аккаунта? Зарегистрироваться"
            )
        }
    }
}
