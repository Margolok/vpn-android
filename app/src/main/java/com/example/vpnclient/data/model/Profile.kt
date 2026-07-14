package com.example.vpnclient.data.model

import java.util.UUID

/**
 * Тип протокола профиля. Определяет, какой транспорт/ядро будет
 * использовано при подключении (ядро подключается отдельно, см. vpn/CoreEngineBridge.kt).
 */
enum class ProtocolType {
    VMESS,
    VLESS,
    TROJAN,
    SHADOWSOCKS,
    WIREGUARD
}

/**
 * Общая "витрина" профиля для UI (список, карточки, сортировка).
 * Технические детали протокола лежат в поле [config].
 */
data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val server: String,
    val port: Int,
    val protocol: ProtocolType,
    val groupId: String? = null,       // id подписки, если профиль пришёл из неё
    val order: Int = 0,
    val lastPingMs: Int? = null,       // результат последнего теста задержки
    val config: ProtocolConfig
)

/**
 * Протокол-специфичные поля. Каждый вариант хранит только то,
 * что реально нужно соответствующему ядру для подключения.
 */
sealed class ProtocolConfig {

    data class VMess(
        val uuid: String,
        val alterId: Int = 0,
        val security: String = "auto",     // auto/aes-128-gcm/chacha20-poly1305/none
        val network: String = "tcp",       // tcp/ws/grpc/h2
        val path: String? = null,
        val host: String? = null,
        val tls: Boolean = false,
        val sni: String? = null
    ) : ProtocolConfig()

    data class VLess(
        val uuid: String,
        val flow: String? = null,          // xtls-rprx-vision и т.п.
        val encryption: String = "none",
        val network: String = "tcp",
        val path: String? = null,
        val host: String? = null,
        val tls: Boolean = false,
        val sni: String? = null,
        val fingerprint: String? = null,   // utls fingerprint
        val publicKey: String? = null,     // reality
        val shortId: String? = null        // reality
    ) : ProtocolConfig()

    data class Trojan(
        val password: String,
        val network: String = "tcp",
        val path: String? = null,
        val host: String? = null,
        val sni: String? = null,
        val allowInsecure: Boolean = false
    ) : ProtocolConfig()

    data class Shadowsocks(
        val method: String,                // напр. chacha20-ietf-poly1305
        val password: String,
        val plugin: String? = null
    ) : ProtocolConfig()

    data class WireGuard(
        val privateKey: String,
        val publicKey: String,             // peer public key
        val presharedKey: String? = null,
        val localAddress: List<String>,    // адреса интерфейса, напр. 10.0.0.2/32
        val dns: List<String> = emptyList(),
        val mtu: Int = 1420,
        val allowedIps: List<String> = listOf("0.0.0.0/0", "::/0")
    ) : ProtocolConfig()
}

/**
 * Группа профилей, полученная из ссылки-подписки (как в Hiddify).
 */
data class SubscriptionGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val lastUpdated: Long? = null,
    val autoUpdateHours: Int = 24
)
