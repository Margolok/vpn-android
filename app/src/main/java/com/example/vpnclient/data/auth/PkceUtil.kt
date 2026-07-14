package com.example.vpnclient.data.auth

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/** Генерация code_verifier/code_challenge для OAuth2 Authorization Code + PKCE (RFC 7636). */
object PkceUtil {

    data class Pkce(val verifier: String, val challenge: String)

    fun generate(): Pkce {
        val verifier = randomUrlSafeString(64)
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        return Pkce(verifier, challenge)
    }

    fun randomState(): String = randomUrlSafeString(24)

    private fun randomUrlSafeString(byteLength: Int): String {
        val bytes = ByteArray(byteLength)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
