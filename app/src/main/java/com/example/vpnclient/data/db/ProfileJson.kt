package com.example.vpnclient.data.db

import com.example.vpnclient.data.model.Profile
import com.example.vpnclient.data.model.ProtocolConfig
import com.example.vpnclient.data.model.ProtocolType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Ручная (де)сериализация вместо kotlinx.serialization/gson —
 * чтобы не тащить лишнюю зависимость ради нескольких полей.
 */
object ProfileJson {

    fun toJson(profile: Profile): String {
        val root = JSONObject()
        root.put("id", profile.id)
        root.put("name", profile.name)
        root.put("server", profile.server)
        root.put("port", profile.port)
        root.put("protocol", profile.protocol.name)
        root.put("groupId", profile.groupId)
        root.put("order", profile.order)
        root.put("lastPingMs", profile.lastPingMs)
        root.put("config", configToJson(profile.config))
        return root.toString()
    }

    fun fromJson(jsonStr: String): Profile {
        val root = JSONObject(jsonStr)
        val protocol = ProtocolType.valueOf(root.getString("protocol"))
        return Profile(
            id = root.getString("id"),
            name = root.getString("name"),
            server = root.getString("server"),
            port = root.getInt("port"),
            protocol = protocol,
            groupId = root.optString("groupId").takeIf { it.isNotBlank() && it != "null" },
            order = root.optInt("order", 0),
            lastPingMs = if (root.isNull("lastPingMs")) null else root.optInt("lastPingMs"),
            config = configFromJson(protocol, root.getJSONObject("config"))
        )
    }

    private fun configToJson(config: ProtocolConfig): JSONObject {
        val obj = JSONObject()
        when (config) {
            is ProtocolConfig.VMess -> {
                obj.put("uuid", config.uuid); obj.put("alterId", config.alterId)
                obj.put("security", config.security); obj.put("network", config.network)
                obj.put("path", config.path); obj.put("host", config.host)
                obj.put("tls", config.tls); obj.put("sni", config.sni)
            }
            is ProtocolConfig.VLess -> {
                obj.put("uuid", config.uuid); obj.put("flow", config.flow)
                obj.put("encryption", config.encryption); obj.put("network", config.network)
                obj.put("path", config.path); obj.put("host", config.host)
                obj.put("tls", config.tls); obj.put("sni", config.sni)
                obj.put("fingerprint", config.fingerprint)
                obj.put("publicKey", config.publicKey); obj.put("shortId", config.shortId)
            }
            is ProtocolConfig.Trojan -> {
                obj.put("password", config.password); obj.put("network", config.network)
                obj.put("path", config.path); obj.put("host", config.host)
                obj.put("sni", config.sni); obj.put("allowInsecure", config.allowInsecure)
            }
            is ProtocolConfig.Shadowsocks -> {
                obj.put("method", config.method); obj.put("password", config.password)
                obj.put("plugin", config.plugin)
            }
            is ProtocolConfig.WireGuard -> {
                obj.put("privateKey", config.privateKey); obj.put("publicKey", config.publicKey)
                obj.put("presharedKey", config.presharedKey)
                obj.put("localAddress", JSONArray(config.localAddress))
                obj.put("dns", JSONArray(config.dns))
                obj.put("mtu", config.mtu)
                obj.put("allowedIps", JSONArray(config.allowedIps))
            }
        }
        return obj
    }

    private fun configFromJson(protocol: ProtocolType, obj: JSONObject): ProtocolConfig = when (protocol) {
        ProtocolType.VMESS -> ProtocolConfig.VMess(
            uuid = obj.getString("uuid"),
            alterId = obj.optInt("alterId", 0),
            security = obj.optString("security", "auto"),
            network = obj.optString("network", "tcp"),
            path = obj.optNullableString("path"),
            host = obj.optNullableString("host"),
            tls = obj.optBoolean("tls", false),
            sni = obj.optNullableString("sni")
        )
        ProtocolType.VLESS -> ProtocolConfig.VLess(
            uuid = obj.getString("uuid"),
            flow = obj.optNullableString("flow"),
            encryption = obj.optString("encryption", "none"),
            network = obj.optString("network", "tcp"),
            path = obj.optNullableString("path"),
            host = obj.optNullableString("host"),
            tls = obj.optBoolean("tls", false),
            sni = obj.optNullableString("sni"),
            fingerprint = obj.optNullableString("fingerprint"),
            publicKey = obj.optNullableString("publicKey"),
            shortId = obj.optNullableString("shortId")
        )
        ProtocolType.TROJAN -> ProtocolConfig.Trojan(
            password = obj.getString("password"),
            network = obj.optString("network", "tcp"),
            path = obj.optNullableString("path"),
            host = obj.optNullableString("host"),
            sni = obj.optNullableString("sni"),
            allowInsecure = obj.optBoolean("allowInsecure", false)
        )
        ProtocolType.SHADOWSOCKS -> ProtocolConfig.Shadowsocks(
            method = obj.getString("method"),
            password = obj.getString("password"),
            plugin = obj.optNullableString("plugin")
        )
        ProtocolType.WIREGUARD -> ProtocolConfig.WireGuard(
            privateKey = obj.getString("privateKey"),
            publicKey = obj.getString("publicKey"),
            presharedKey = obj.optNullableString("presharedKey"),
            localAddress = obj.optJSONArray("localAddress").toStringList(),
            dns = obj.optJSONArray("dns").toStringList(),
            mtu = obj.optInt("mtu", 1420),
            allowedIps = obj.optJSONArray("allowedIps").toStringList()
        )
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key) || !has(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { getString(it) }
    }
}
