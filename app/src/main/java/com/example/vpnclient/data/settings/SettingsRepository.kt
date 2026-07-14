package com.example.vpnclient.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Настройки уровня приложения, не связанные с профилями VPN.
 *
 * [bypassRussianServices] — включает split-tunneling: приложения из
 * [com.example.vpnclient.vpn.RuServicesBypass.PACKAGES] (Госуслуги, банки,
 * VK, Яндекс и т.п.) не будут заворачиваться в VPN-туннель, чтобы у
 * пользователя не отваливались отечественные сервисы, требующие российский IP
 * или проходящие через СМЭВ/антифрод-проверки, завязанные на локацию.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val BYPASS_RU_SERVICES = booleanPreferencesKey("bypass_ru_services")
    }

    val bypassRussianServices: Flow<Boolean> =
        context.appDataStore.data.map { prefs -> prefs[Keys.BYPASS_RU_SERVICES] ?: true }

    suspend fun setBypassRussianServices(enabled: Boolean) {
        context.appDataStore.edit { prefs -> prefs[Keys.BYPASS_RU_SERVICES] = enabled }
    }
}
