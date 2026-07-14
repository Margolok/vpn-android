package com.example.vpnclient.vpn

import android.content.pm.PackageManager
import android.net.VpnService
import android.util.Log

/**
 * Реализация настройки "Не обходить российские сервисы" (см. SettingsScreen).
 *
 * Android VpnService поддерживает split-tunneling "из коробки" через
 * [VpnService.Builder.addDisallowedApplication] — трафик перечисленных
 * приложений просто не попадает в TUN-интерфейс и идёт как обычно, напрямую.
 * Это работает независимо от того, какое VPN-ядро в итоге подключено
 * (см. CoreEngineBridge.kt), т.к. решается на уровне системного интерфейса.
 *
 * Список ниже — стартовый набор популярных российских сервисов, для которых
 * часто важно сохранить российский IP/локальную сеть (Госуслуги, банки,
 * VK, Яндекс). Список можно расширять под свою аудиторию — в идеале вынести
 * в удалённо обновляемый конфиг, чтобы не пересобирать APK ради добавления
 * ещё одного приложения.
 *
 * Примечание: начиная с Android 11 (API 30) видимость чужих пакетов через
 * PackageManager ограничена — see AndroidManifest.xml, блок <queries>,
 * где эти же пакеты явно объявлены, иначе isPackageInstalled всегда вернёт false.
 */
object RuServicesBypass {

    val PACKAGES: List<String> = listOf(
        "ru.gosuslugi.online",              // Госуслуги
        "ru.sberbankmobile",                 // СберБанк Онлайн
        "ru.vtb24.mobilebanking.android",    // ВТБ Онлайн
        "com.idamob.tinkoff.android",        // Т-Банк (Тинькофф)
        "ru.alfabank.mobile.android",        // Альфа-Банк
        "com.vkontakte.android",             // VK
        "ru.mail.mailapp",                   // Почта Mail.ru
        "ru.yandex.searchplugin",            // Яндекс с Алисой
        "ru.yandex.taxi",                    // Яндекс Go
        "ru.yandex.market",                  // Яндекс Маркет
        "ru.yandex.music",                   // Яндекс Музыка
        "ru.mts.mtsbank",                    // МТС Банк
        "com.wildberries.ru",                // Wildberries
        "ru.ozon.app.android"                // Ozon
    )

    /**
     * Добавляет в билдер только те пакеты из [PACKAGES], которые реально
     * установлены на устройстве (иначе addDisallowedApplication бросает
     * PackageManager.NameNotFoundException).
     */
    fun applyTo(builder: VpnService.Builder, packageManager: PackageManager) {
        for (packageName in PACKAGES) {
            if (!isInstalled(packageManager, packageName)) continue
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w("RuServicesBypass", "Пакет пропал между проверкой и добавлением: $packageName")
            }
        }
    }

    private fun isInstalled(packageManager: PackageManager, packageName: String): Boolean =
        try {
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
}
