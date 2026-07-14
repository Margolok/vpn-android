package com.example.vpnclient.data.auth

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.vpnclient.data.model.AuthProvider
import com.example.vpnclient.data.model.AuthUser
import com.example.vpnclient.data.settings.appDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

sealed class AuthResult {
    data class Success(val user: AuthUser) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

/**
 * Отвечает за вход через Google/Яндекс/VK/Госуслуги (единый OAuth2 + PKCE флоу,
 * см. [OAuthConfig]) и через почту/пароль, а также за хранение текущей сессии.
 *
 * ВАЖНО: обмен authorization code на токен ([completeOAuthCallback]) и вход по
 * почте ([loginWithEmail]) сейчас — рабочие заглушки, как и `MockCoreEngine`
 * в vpn/CoreEngineBridge.kt: они создают локальную сессию без реального
 * похода на сервер провайдера/ваш backend, чтобы экран логина можно было
 * полностью протестировать уже сейчас. Перед релизом их нужно заменить на
 * настоящие сетевые вызовы — см. TODO внутри каждого метода.
 */
class AuthRepository(private val context: Context) {

    private object Keys {
        val USER_ID = stringPreferencesKey("auth_user_id")
        val NAME = stringPreferencesKey("auth_name")
        val EMAIL = stringPreferencesKey("auth_email")
        val PROVIDER = stringPreferencesKey("auth_provider")
    }

    val currentUser: Flow<AuthUser?> = context.appDataStore.data.map { prefs ->
        val id = prefs[Keys.USER_ID] ?: return@map null
        AuthUser(
            id = id,
            displayName = prefs[Keys.NAME] ?: "Пользователь LoudVPN",
            email = prefs[Keys.EMAIL],
            provider = prefs[Keys.PROVIDER]?.let { runCatching { AuthProvider.valueOf(it) }.getOrNull() }
                ?: AuthProvider.EMAIL
        )
    }

    // Состояние текущего OAuth-запроса, живёт только на время открытого Custom Tab.
    private var pendingProvider: AuthProvider? = null
    private var pendingPkce: PkceUtil.Pkce? = null
    private var pendingState: String? = null

    /** Открывает страницу авторизации провайдера в Chrome Custom Tabs. */
    fun startOAuthLogin(context: Context, provider: AuthProvider) {
        val endpoint = OAuthConfig.forProvider(provider)
        val pkce = PkceUtil.generate()
        val state = PkceUtil.randomState()
        pendingProvider = provider
        pendingPkce = pkce
        pendingState = state

        val authorizationUri = Uri.parse(endpoint.authorizationUrl).buildUpon()
            .appendQueryParameter("client_id", endpoint.clientId)
            .appendQueryParameter("redirect_uri", endpoint.redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", endpoint.scope)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", pkce.challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()

        CustomTabsIntent.Builder().build().launchUrl(context, authorizationUri)
    }

    /**
     * Вызывается из [com.example.vpnclient.data.auth.AuthCallbackActivity], когда
     * браузер вернул управление приложению по redirect_uri.
     */
    suspend fun completeOAuthCallback(callbackUri: Uri): AuthResult {
        val provider = pendingProvider
        val pkce = pendingPkce
        val expectedState = pendingState
        pendingProvider = null
        pendingPkce = null
        pendingState = null

        if (provider == null || pkce == null || expectedState == null) {
            return AuthResult.Failure("Сессия входа истекла, попробуйте ещё раз")
        }

        val error = callbackUri.getQueryParameter("error")
        if (error != null) {
            return AuthResult.Failure("Провайдер отклонил вход: $error")
        }

        val returnedState = callbackUri.getQueryParameter("state")
        if (returnedState != expectedState) {
            return AuthResult.Failure("Некорректный state, вход отклонён из соображений безопасности")
        }

        val code = callbackUri.getQueryParameter("code")
            ?: return AuthResult.Failure("Провайдер не вернул код авторизации")

        // TODO: заменить на реальный вызов вашего backend'а:
        //   POST /auth/oauth/{provider} { code, code_verifier: pkce.verifier, redirect_uri }
        // backend обменивает code на токен провайдера (используя client_secret,
        // который нельзя хранить в APK) и возвращает вашу собственную сессию.
        val user = AuthUser(
            id = UUID.randomUUID().toString(),
            displayName = displayNameForProvider(provider),
            email = null,
            provider = provider
        )
        persistUser(user)
        return AuthResult.Success(user)
    }

    /** TODO: заменить на настоящий вызов backend'а (POST /auth/login). */
    suspend fun loginWithEmail(email: String, password: String): AuthResult {
        if (!isValidEmail(email)) return AuthResult.Failure("Введите корректную почту")
        if (password.length < 6) return AuthResult.Failure("Пароль должен быть не короче 6 символов")

        val user = AuthUser(
            id = UUID.randomUUID().toString(),
            displayName = email.substringBefore("@"),
            email = email,
            provider = AuthProvider.EMAIL
        )
        persistUser(user)
        return AuthResult.Success(user)
    }

    /** TODO: заменить на настоящий вызов backend'а (POST /auth/register). */
    suspend fun registerWithEmail(email: String, password: String): AuthResult =
        loginWithEmail(email, password)

    suspend fun logout() {
        context.appDataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.NAME)
            prefs.remove(Keys.EMAIL)
            prefs.remove(Keys.PROVIDER)
        }
    }

    private suspend fun persistUser(user: AuthUser) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.USER_ID] = user.id
            prefs[Keys.NAME] = user.displayName
            user.email?.let { prefs[Keys.EMAIL] = it }
            prefs[Keys.PROVIDER] = user.provider.name
        }
    }

    private fun displayNameForProvider(provider: AuthProvider) = when (provider) {
        AuthProvider.GOOGLE -> "Пользователь Google"
        AuthProvider.YANDEX -> "Пользователь Яндекс ID"
        AuthProvider.VK -> "Пользователь VK ID"
        AuthProvider.GOSUSLUGI -> "Пользователь Госуслуг"
        AuthProvider.EMAIL -> "Пользователь LoudVPN"
    }

    private fun isValidEmail(email: String) =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
