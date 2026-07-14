package com.example.vpnclient.vpn

import android.net.VpnService
import com.example.vpnclient.data.model.Profile

/**
 * ============================================================================
 *  ТОЧКА ИНТЕГРАЦИИ РЕАЛЬНОГО VPN-ЯДРА
 * ============================================================================
 *
 * Сейчас этот интерфейс реализован MockCoreEngine — он ничего не туннелирует,
 * а только имитирует подключение, чтобы можно было полностью проверить UI/UX
 * (список профилей, кнопку подключения, состояния, уведомления) без готового
 * VPN-ядра.
 *
 * Когда будете готовы подключать настоящий туннель, у вас по сути два пути,
 * оба используются в реальных приложениях такого рода (Hiddify, v2rayNG,
 * NekoBox for Android построены по такому же принципу):
 *
 * 1) sing-box (рекомендуется, поддерживает всё сразу: VMess/VLESS/Trojan/SS/
 *    WireGuard/Reality/Hysteria и т.д.)
 *    - Библиотека на Go, собирается под Android через gomobile в .aar
 *    - Готовый AAR можно взять из проекта SagerNet/sing-box (раздел Releases,
 *      файлы libbox-*.aar) или собрать самому
 *    - AAR подключается как обычная Android-зависимость, у него есть свой
 *      Kotlin/Java API для запуска "BoxService" с JSON-конфигурацией
 *    - Ваша задача в этом файле — по [Profile] сгенерировать JSON-конфиг
 *      sing-box (outbound с нужным протоколом) и передать его в libbox
 *
 * 2) Xray-core (aka v2ray) — аналогично, есть Android-биндинги (AndroidLibXrayLib
 *    в проекте v2rayNG), тоже собирается в .aar через gomobile
 *
 * Для WireGuard в частности можно не поднимать целое sing-box-ядро, а
 * использовать официальный com.wireguard.android.tunnel (WireGuard for
 * Android как библиотеку) — он даёт Tunnel/Backend API специально под это.
 *
 * В любом случае системный TUN-интерфейс создаётся через [VpnService.Builder]
 * в [VpnConnectionService] — а дальше весь трафик из этого интерфейса нужно
 * передать в файловый дескриптор ядра (sing-box/Xray/wireguard-go), что они
 * обычно делают сами, получив fd интерфейса.
 * ============================================================================
 */
interface CoreEngine {
    /**
     * Запускает туннель для профиля. [vpnService] нужен, чтобы ядро (или сам
     * этот метод) могло вызвать VpnService.Builder().establish() и получить fd.
     * Бросает исключение при ошибке — её текст уйдёт в VpnState.Error.
     */
    suspend fun start(profile: Profile, vpnService: VpnService)

    suspend fun stop()

    /** Текущая статистика для UI (байты вверх/вниз), null пока не реализовано. */
    fun currentStats(): TrafficStats?
}

data class TrafficStats(val uploadBytes: Long, val downloadBytes: Long)

/**
 * Временная заглушка. Имитирует подключение с задержкой ~1.5с и без реального
 * туннелирования — трафик пользователя НЕ идёт через VPN, пока не подключено
 * настоящее ядро. Оставлена, чтобы UI и логика профилей были полностью
 * тестируемы уже сейчас.
 */
class MockCoreEngine : CoreEngine {

    private var running = false

    override suspend fun start(profile: Profile, vpnService: VpnService) {
        kotlinx.coroutines.delay(1500)
        // TODO: заменить на реальный запуск ядра, см. комментарий класса CoreEngine выше.
        running = true
    }

    override suspend fun stop() {
        running = false
    }

    override fun currentStats(): TrafficStats? =
        if (running) TrafficStats(uploadBytes = 0, downloadBytes = 0) else null
}
