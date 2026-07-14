package com.example.vpnclient.data.model

/**
 * Способы входа в LoudVPN. EMAIL обрабатывается отдельно (см. [AuthRepository.loginWithEmail]),
 * остальные — единым OAuth2 Authorization Code + PKCE флоу через Chrome Custom Tabs
 * (см. data/auth/OAuthConfig.kt и data/auth/AuthRepository.kt).
 */
enum class AuthProvider {
    GOOGLE,
    YANDEX,
    VK,
    GOSUSLUGI,
    EMAIL
}

/** Профиль вошедшего пользователя, показывается на экране настроек. */
data class AuthUser(
    val id: String,
    val displayName: String,
    val email: String?,
    val provider: AuthProvider,
    val avatarUrl: String? = null
)
