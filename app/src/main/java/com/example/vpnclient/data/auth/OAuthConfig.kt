package com.example.vpnclient.data.auth

import com.example.vpnclient.data.model.AuthProvider

/**
 * ============================================================================
 *  ТОЧКА ИНТЕГРАЦИИ РЕАЛЬНОЙ АВТОРИЗАЦИИ
 * ============================================================================
 * Здесь используется единый OAuth2 Authorization Code + PKCE флоу для всех
 * четырёх внешних провайдеров: он открывается через Chrome Custom Tabs
 * (см. [AuthRepository.startOAuthLogin]) и не требует подключения отдельного
 * SDK под каждый сервис.
 *
 * Перед релизом нужно для каждого провайдера:
 *  1) Зарегистрировать OAuth-приложение в консоли провайдера и получить
 *     clientId (и, если провайдер того требует, настроить redirect URI
 *     "loudvpn://auth-callback/<provider>" в его настройках).
 *  2) Подставить настоящий clientId вместо CLIENT_ID_PLACEHOLDER ниже.
 *  3) Обмен code → access/id token (метод [AuthRepository.completeOAuthCallback])
 *     в проде должен идти НЕ напрямую с телефона на token endpoint (это требует
 *     client_secret, который нельзя хранить в APK), а через ваш backend:
 *     клиент отправляет backend'у code+verifier, backend обменивает его на
 *     токен и возвращает клиенту уже свою сессию.
 *
 * Ссылки на регистрацию приложений:
 *  - Google: https://console.cloud.google.com/apis/credentials (OAuth client ID, тип Android или Web)
 *  - Яндекс ID: https://oauth.yandex.ru/client/new
 *  - VK ID: https://id.vk.com/business/go/docs/vkid/latest/vk-id/connection/create-application
 *  - Госуслуги (ЕСИА): требует официальной регистрации системы через личный
 *    кабинет технической поддержки ЕСИА (esia-support.gosuslugi.ru) — доступ
 *    выдаётся только юрлицам/ИП после проверки, "тестовая" интеграция обычно
 *    делается сначала с окружением esia-portal1.test.gosuslugi.ru.
 * ============================================================================
 */
data class OAuthEndpoint(
    val provider: AuthProvider,
    val authorizationUrl: String,
    val tokenUrl: String,
    val clientId: String,
    val scope: String,
    val redirectUri: String
)

object OAuthConfig {

    private const val CLIENT_ID_PLACEHOLDER = "TODO_REPLACE_WITH_REAL_CLIENT_ID"

    // Кастомная схема, зарегистрированная в AndroidManifest на AuthCallbackActivity.
    private const val REDIRECT_SCHEME = "loudvpn://auth-callback"

    val GOOGLE = OAuthEndpoint(
        provider = AuthProvider.GOOGLE,
        authorizationUrl = "https://accounts.google.com/o/oauth2/v2/auth",
        tokenUrl = "https://oauth2.googleapis.com/token",
        clientId = CLIENT_ID_PLACEHOLDER,
        scope = "openid email profile",
        redirectUri = "$REDIRECT_SCHEME/google"
    )

    val YANDEX = OAuthEndpoint(
        provider = AuthProvider.YANDEX,
        authorizationUrl = "https://oauth.yandex.ru/authorize",
        tokenUrl = "https://oauth.yandex.ru/token",
        clientId = CLIENT_ID_PLACEHOLDER,
        scope = "login:email login:info",
        redirectUri = "$REDIRECT_SCHEME/yandex"
    )

    val VK = OAuthEndpoint(
        provider = AuthProvider.VK,
        authorizationUrl = "https://id.vk.com/authorize",
        tokenUrl = "https://id.vk.com/oauth2/auth",
        clientId = CLIENT_ID_PLACEHOLDER,
        scope = "email",
        redirectUri = "$REDIRECT_SCHEME/vk"
    )

    /** Данные тестового окружения ЕСИА — заменить на боевые после регистрации. */
    val GOSUSLUGI = OAuthEndpoint(
        provider = AuthProvider.GOSUSLUGI,
        authorizationUrl = "https://esia-portal1.test.gosuslugi.ru/aas/oauth2/ac",
        tokenUrl = "https://esia-portal1.test.gosuslugi.ru/aas/oauth2/te",
        clientId = CLIENT_ID_PLACEHOLDER,
        scope = "openid fullname email",
        redirectUri = "$REDIRECT_SCHEME/gosuslugi"
    )

    fun forProvider(provider: AuthProvider): OAuthEndpoint = when (provider) {
        AuthProvider.GOOGLE -> GOOGLE
        AuthProvider.YANDEX -> YANDEX
        AuthProvider.VK -> VK
        AuthProvider.GOSUSLUGI -> GOSUSLUGI
        AuthProvider.EMAIL -> throw IllegalArgumentException("EMAIL не использует OAuth")
    }
}
