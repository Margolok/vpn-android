package com.example.vpnclient.data.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.vpnclient.MainActivity
import com.example.vpnclient.VpnClientApp
import kotlinx.coroutines.launch

/**
 * Ловит redirect_uri OAuth-провайдера (`loudvpn://auth-callback/<provider>?code=...&state=...`),
 * прозрачно передаёт результат в [AuthRepository] и возвращает пользователя на [MainActivity].
 * Зарегистрирована в AndroidManifest.xml как android:exported с intent-filter на схему loudvpn.
 */
class AuthCallbackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        val authRepository = (application as VpnClientApp).authRepository

        if (uri != null) {
            lifecycleScope.launch {
                authRepository.completeOAuthCallback(uri)
                returnToApp()
            }
        } else {
            returnToApp()
        }
    }

    private fun returnToApp() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }
}
