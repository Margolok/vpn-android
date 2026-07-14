package com.example.vpnclient.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.vpnclient.MainActivity
import com.example.vpnclient.R
import com.example.vpnclient.data.db.AppDatabase
import com.example.vpnclient.data.db.ProfileJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Системный VPN-сервис Android. Отвечает за:
 *  - создание TUN-интерфейса через VpnService.Builder (establish())
 *  - жизненный цикл foreground-уведомления (обязателен для активного VPN)
 *  - делегирование реального туннелирования в [CoreEngine] (см. CoreEngineBridge.kt)
 *
 * Состояние публикуется через [state], которое слушает UI (HomeViewModel).
 */
class VpnConnectionService : VpnService() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var engine: CoreEngine = MockCoreEngine()

    companion object {
        const val ACTION_CONNECT = "com.example.vpnclient.CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpnclient.DISCONNECT"
        const val EXTRA_PROFILE_JSON = "profile_json"
        const val EXTRA_BYPASS_RU_SERVICES = "bypass_ru_services"
        private const val CHANNEL_ID = "vpn_status"
        private const val NOTIFICATION_ID = 1

        private val _state = MutableStateFlow<VpnState>(VpnState.Disconnected)
        val state: StateFlow<VpnState> = _state.asStateFlow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val json = intent.getStringExtra(EXTRA_PROFILE_JSON)
                val bypassRuServices = intent.getBooleanExtra(EXTRA_BYPASS_RU_SERVICES, true)
                if (json != null) connect(ProfileJson.fromJson(json), bypassRuServices)
            }
            ACTION_DISCONNECT -> disconnect()
        }
        return START_NOT_STICKY
    }

    private fun connect(profile: com.example.vpnclient.data.model.Profile, bypassRuServices: Boolean) {
        _state.value = VpnState.Connecting
        startForeground(NOTIFICATION_ID, buildNotification("Подключение к ${profile.name}…"))

        scope.launch {
            try {
                // Системный TUN-интерфейс. Реальные адреса/маршруты обычно
                // выставляет само ядро (sing-box и т.п.) при интеграции —
                // здесь базовый пример для случая, когда ядро ожидает уже
                // установленный интерфейс.
                val builder = Builder()
                    .setSession(profile.name)
                    .addAddress("10.10.0.2", 32)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    .setMtu(1420)

                // Настройка "Не обходить российские сервисы": исключаем из
                // туннеля установленные приложения из RuServicesBypass.PACKAGES,
                // чтобы Госуслуги/банки/VK/Яндекс работали напрямую.
                if (bypassRuServices) {
                    RuServicesBypass.applyTo(builder, packageManager)
                }

                // NB: .establish() создаёт fd интерфейса. Пока используется
                // MockCoreEngine, интерфейс поднимается, но трафик через него
                // не обрабатывается настоящим ядром.
                builder.establish()

                engine.start(profile, this@VpnConnectionService)

                _state.value = VpnState.Connected(profile.name, System.currentTimeMillis())
                startForeground(NOTIFICATION_ID, buildNotification("Подключено: ${profile.name}"))
            } catch (e: Exception) {
                _state.value = VpnState.Error(e.message ?: "Неизвестная ошибка подключения")
                stopSelf()
            }
        }
    }

    private fun disconnect() {
        scope.launch {
            engine.stop()
            _state.value = VpnState.Disconnected
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onRevoke() {
        // Пользователь отозвал разрешение VPN в системных настройках Android.
        disconnect()
        super.onRevoke()
    }

    override fun onDestroy() {
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    private fun buildNotification(text: String): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, getString(R.string.notification_channel_vpn), NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }
}
