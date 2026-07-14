package com.example.vpnclient.data.parser

import android.util.Base64
import com.example.vpnclient.data.model.Profile
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Скачивает содержимое ссылки-подписки и разбирает её на список профилей.
 * Подписки обычно отдают либо построчный список ссылок в открытом виде,
 * либо всё это же, закодированное целиком в base64 — поддерживаем оба варианта.
 */
object SubscriptionFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    class SubscriptionFetchException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun fetchAndParse(url: String): List<Profile> {
        val body = try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw SubscriptionFetchException("Сервер вернул код ${response.code}")
                }
                response.body?.string() ?: throw SubscriptionFetchException("Пустой ответ от сервера подписки")
            }
        } catch (e: SubscriptionFetchException) {
            throw e
        } catch (e: Exception) {
            throw SubscriptionFetchException("Не удалось загрузить подписку: ${e.message}", e)
        }

        val decoded = tryBase64Decode(body) ?: body

        val links = decoded.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && (it.startsWith("vmess://") || it.startsWith("vless://") ||
                    it.startsWith("trojan://") || it.startsWith("ss://")) }

        if (links.isEmpty()) {
            throw SubscriptionFetchException("В подписке не найдено ни одного распознаваемого конфига")
        }

        return links.mapNotNull { link ->
            runCatching { ConfigLinkParser.parse(link) }.getOrNull()
        }
    }

    private fun tryBase64Decode(text: String): String? = runCatching {
        val cleaned = text.trim().replace("\n", "").replace("\r", "")
        String(Base64.decode(cleaned, Base64.DEFAULT))
    }.getOrNull()?.takeIf { it.contains("://") }
}
