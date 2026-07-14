package com.example.vpnclient.data.parser

import android.util.Base64
import com.example.vpnclient.data.model.Profile
import com.example.vpnclient.data.model.ProtocolConfig
import com.example.vpnclient.data.model.ProtocolType
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder

/**
 * Разбирает ссылки-конфиги в единый формат [Profile].
 * Поддерживает форматы, которые реально встречаются "в дикой природе":
 *  - vmess://<base64 json>
 *  - vless://uuid@host:port?params#name
 *  - trojan://password@host:port?params#name
 *  - ss://base64(method:password)@host:port#name   и   ss://base64(method:password@host:port)#name
 *  - WireGuard .conf в виде обычного INI-текста (не ссылка, а вставленный текст)
 *
 * Если формат не распознан — бросает [ConfigParseException] с понятным сообщением,
 * чтобы показать пользователю, что именно не так.
 */
object ConfigLinkParser {

    class ConfigParseException(message: String) : Exception(message)

    fun parse(rawInput: String): Profile {
        val input = rawInput.trim()
        return when {
            input.startsWith("vmess://") -> parseVMess(input)
            input.startsWith("vless://") -> parseVLess(input)
            input.startsWith("trojan://") -> parseTrojan(input)
            input.startsWith("ss://") -> parseShadowsocks(input)
            input.contains("[Interface]") -> parseWireGuard(input)
            else -> throw ConfigParseException("Неизвестный формат конфига. Поддерживаются: vmess://, vless://, trojan://, ss://, WireGuard .conf")
        }
    }

    // ---------- VMess ----------
    private fun parseVMess(link: String): Profile {
        val b64 = link.removePrefix("vmess://")
        val jsonStr = try {
            String(Base64.decode(b64, Base64.DEFAULT))
        } catch (e: Exception) {
            throw ConfigParseException("Не удалось декодировать vmess base64: ${e.message}")
        }
        val json = try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            throw ConfigParseException("vmess: содержимое не JSON")
        }

        val server = json.optString("add").ifBlank { throw ConfigParseException("vmess: отсутствует поле 'add' (сервер)") }
        val port = json.optString("port").toIntOrNull() ?: throw ConfigParseException("vmess: некорректный порт")
        val name = json.optString("ps").ifBlank { server }

        val config = ProtocolConfig.VMess(
            uuid = json.optString("id"),
            alterId = json.optString("aid").toIntOrNull() ?: 0,
            security = json.optString("scy", "auto"),
            network = json.optString("net", "tcp"),
            path = json.optString("path").takeIf { it.isNotBlank() },
            host = json.optString("host").takeIf { it.isNotBlank() },
            tls = json.optString("tls") == "tls",
            sni = json.optString("sni").takeIf { it.isNotBlank() }
        )

        return Profile(name = name, server = server, port = port, protocol = ProtocolType.VMESS, config = config)
    }

    // ---------- VLESS ----------
    private fun parseVLess(link: String): Profile {
        val uri = safeUri(link, "vless")
        val uuid = uri.userInfo ?: throw ConfigParseException("vless: отсутствует uuid")
        val server = uri.host ?: throw ConfigParseException("vless: отсутствует сервер")
        val port = if (uri.port != -1) uri.port else throw ConfigParseException("vless: отсутствует порт")
        val params = parseQuery(uri.rawQuery)
        val name = decodeFragment(uri.rawFragment) ?: server

        val config = ProtocolConfig.VLess(
            uuid = uuid,
            flow = params["flow"],
            encryption = params["encryption"] ?: "none",
            network = params["type"] ?: "tcp",
            path = params["path"],
            host = params["host"],
            tls = params["security"] == "tls" || params["security"] == "reality",
            sni = params["sni"],
            fingerprint = params["fp"],
            publicKey = params["pbk"],
            shortId = params["sid"]
        )

        return Profile(name = name, server = server, port = port, protocol = ProtocolType.VLESS, config = config)
    }

    // ---------- Trojan ----------
    private fun parseTrojan(link: String): Profile {
        val uri = safeUri(link, "trojan")
        val password = uri.userInfo ?: throw ConfigParseException("trojan: отсутствует пароль")
        val server = uri.host ?: throw ConfigParseException("trojan: отсутствует сервер")
        val port = if (uri.port != -1) uri.port else 443
        val params = parseQuery(uri.rawQuery)
        val name = decodeFragment(uri.rawFragment) ?: server

        val config = ProtocolConfig.Trojan(
            password = password,
            network = params["type"] ?: "tcp",
            path = params["path"],
            host = params["host"],
            sni = params["sni"] ?: server,
            allowInsecure = params["allowInsecure"] == "1"
        )

        return Profile(name = name, server = server, port = port, protocol = ProtocolType.TROJAN, config = config)
    }

    // ---------- Shadowsocks ----------
    private fun parseShadowsocks(link: String): Profile {
        val withoutScheme = link.removePrefix("ss://")
        val hashIndex = withoutScheme.indexOf('#')
        val name = if (hashIndex != -1) decodeFragment(withoutScheme.substring(hashIndex + 1)) else null
        val body = if (hashIndex != -1) withoutScheme.substring(0, hashIndex) else withoutScheme

        // Вариант 1: ss://base64(method:password@host:port)
        // Вариант 2: ss://base64(method:password)@host:port
        val atIndex = body.lastIndexOf('@')

        val method: String
        val password: String
        val server: String
        val port: Int

        if (atIndex == -1) {
            val decoded = decodeB64(body) ?: throw ConfigParseException("ss: не удалось декодировать base64")
            val parts = decoded.split("@")
            if (parts.size != 2) throw ConfigParseException("ss: неверный формат (ожидался method:password@host:port)")
            val methodPass = parts[0].split(":", limit = 2)
            val hostPort = parts[1].split(":")
            if (methodPass.size != 2 || hostPort.size != 2) throw ConfigParseException("ss: неверный формат")
            method = methodPass[0]; password = methodPass[1]
            server = hostPort[0]; port = hostPort[1].toIntOrNull() ?: throw ConfigParseException("ss: некорректный порт")
        } else {
            val userInfoB64 = body.substring(0, atIndex)
            val hostPortPart = body.substring(atIndex + 1)
            val decodedUserInfo = decodeB64(userInfoB64) ?: userInfoB64 // некоторые генераторы не кодируют
            val methodPass = decodedUserInfo.split(":", limit = 2)
            if (methodPass.size != 2) throw ConfigParseException("ss: неверный формат method:password")
            method = methodPass[0]; password = methodPass[1]

            val hostPort = hostPortPart.split("?")[0].split(":")
            if (hostPort.size != 2) throw ConfigParseException("ss: неверный формат host:port")
            server = hostPort[0]; port = hostPort[1].toIntOrNull() ?: throw ConfigParseException("ss: некорректный порт")
        }

        val config = ProtocolConfig.Shadowsocks(method = method, password = password)
        return Profile(name = name ?: server, server = server, port = port, protocol = ProtocolType.SHADOWSOCKS, config = config)
    }

    // ---------- WireGuard (вставленный текст .conf, не ссылка) ----------
    private fun parseWireGuard(confText: String): Profile {
        val lines = confText.lines().map { it.trim() }
        val interfaceMap = mutableMapOf<String, String>()
        val peerMap = mutableMapOf<String, String>()
        var section = ""

        for (line in lines) {
            when {
                line.equals("[Interface]", ignoreCase = true) -> section = "iface"
                line.equals("[Peer]", ignoreCase = true) -> section = "peer"
                line.contains("=") -> {
                    val (k, v) = line.split("=", limit = 2).map { it.trim() }
                    if (section == "iface") interfaceMap[k.lowercase()] = v
                    if (section == "peer") peerMap[k.lowercase()] = v
                }
            }
        }

        val privateKey = interfaceMap["privatekey"] ?: throw ConfigParseException("WireGuard: отсутствует PrivateKey")
        val publicKey = peerMap["publickey"] ?: throw ConfigParseException("WireGuard: отсутствует PublicKey пира")
        val endpoint = peerMap["endpoint"] ?: throw ConfigParseException("WireGuard: отсутствует Endpoint")
        val endpointParts = endpoint.split(":")
        val server = endpointParts.getOrNull(0) ?: throw ConfigParseException("WireGuard: некорректный Endpoint")
        val port = endpointParts.getOrNull(1)?.toIntOrNull() ?: throw ConfigParseException("WireGuard: некорректный порт в Endpoint")

        val config = ProtocolConfig.WireGuard(
            privateKey = privateKey,
            publicKey = publicKey,
            presharedKey = peerMap["presharedkey"],
            localAddress = interfaceMap["address"]?.split(",")?.map { it.trim() } ?: emptyList(),
            dns = interfaceMap["dns"]?.split(",")?.map { it.trim() } ?: emptyList(),
            mtu = interfaceMap["mtu"]?.toIntOrNull() ?: 1420,
            allowedIps = peerMap["allowedips"]?.split(",")?.map { it.trim() } ?: listOf("0.0.0.0/0", "::/0")
        )

        return Profile(name = "WireGuard $server", server = server, port = port, protocol = ProtocolType.WIREGUARD, config = config)
    }

    // ---------- Вспомогательное ----------
    private fun safeUri(link: String, scheme: String): URI = try {
        URI(link)
    } catch (e: Exception) {
        throw ConfigParseException("$scheme: некорректная ссылка (${e.message})")
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split("&").mapNotNull {
            val idx = it.indexOf("=")
            if (idx == -1) null else {
                val key = it.substring(0, idx)
                val value = URLDecoder.decode(it.substring(idx + 1), "UTF-8")
                key to value
            }
        }.toMap()
    }

    private fun decodeFragment(fragment: String?): String? =
        fragment?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }

    private fun decodeB64(s: String): String? = runCatching {
        val padded = s.padEnd((s.length + 3) / 4 * 4, '=')
        String(Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP))
    }.recoverCatching {
        String(Base64.decode(s, Base64.DEFAULT))
    }.getOrNull()
}
