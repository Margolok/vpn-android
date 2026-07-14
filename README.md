# LoudVPN (Android) — черновой проект

Нативное Android-приложение (Kotlin + Jetpack Compose), архитектурно
повторяющее логику Hiddify: добавление профилей по ссылке/подписке,
хранение локально, экран подключения, вход по нескольким провайдерам и
переключатель для российских сервисов. **VPN-ядро пока не подключено** —
см. раздел "Как подключить реальный туннель" ниже.

## Что уже работает

- Вход через Google, Яндекс ID, VK ID, Госуслуги (единый OAuth2 +
  PKCE флоу через Chrome Custom Tabs) или по почте/паролю →
  `data/auth/`, `ui/screens/auth/LoginScreen.kt`
- Переключатель "Не обходить российские сервисы" в настройках — включает
  split-tunneling для установленных Госуслуг/банков/VK/Яндекс через
  `VpnService.Builder.addDisallowedApplication` → `vpn/RuServicesBypass.kt`
- Парсинг ссылок `vmess://`, `vless://`, `trojan://`, `ss://` и вставленного
  текста WireGuard `.conf` → `data/parser/ConfigLinkParser.kt`
- Загрузка и разбор ссылок-подписок (списком или base64) →
  `data/parser/SubscriptionFetcher.kt`
- Локальное хранение профилей и групп-подписок на Room →
  `data/db/`, `data/repository/ProfileRepository.kt`
- Экраны Compose: вход, главный (кнопка подключения, выбор активного
  профиля), профили (список, добавление по ссылке/подписке, удаление),
  настройки
- `VpnConnectionService` — системный Android `VpnService` с foreground-
  уведомлением, поднимает TUN-интерфейс через `Builder().establish()`
- Открытие приложения по внешним ссылкам `vmess://`, `vless://`, `trojan://`,
  `ss://`, `sub://` (intent-filter в манифесте)

## Авторизация: что нужно донастроить перед релизом

Экран входа и `data/auth/AuthRepository.kt` уже полностью работают как UI —
как и `MockCoreEngine`, вход сейчас создаёт локальную сессию без реального
похода к провайдеру/backend'у, чтобы можно было проверить весь флоу уже
сейчас. Перед реальным релизом нужно:

1. Зарегистрировать OAuth-приложения у каждого провайдера и подставить
   настоящие `clientId` в `data/auth/OAuthConfig.kt` (сейчас там плейсхолдер
   `TODO_REPLACE_WITH_REAL_CLIENT_ID`):
   - Google: [console.cloud.google.com/apis/credentials](https://console.cloud.google.com/apis/credentials)
   - Яндекс ID: [oauth.yandex.ru/client/new](https://oauth.yandex.ru/client/new)
   - VK ID: документация [id.vk.com/business](https://id.vk.com/business/go/docs/vkid/latest/vk-id/connection/create-application)
   - Госуслуги (ЕСИА): официальная регистрация системы через кабинет
     техподдержки ЕСИА (доступно юрлицам/ИП после проверки); для разработки
     есть тестовое окружение `esia-portal1.test.gosuslugi.ru`
2. Поднять backend-эндпоинт обмена `authorization code` → токен
   (`AuthRepository.completeOAuthCallback` и `loginWithEmail`/
   `registerWithEmail` помечены `TODO`) — `client_secret` нельзя хранить в
   APK, поэтому обмен должен идти через ваш сервер.
3. При необходимости расширить/сузить список приложений в
   `vpn/RuServicesBypass.kt` (и синхронно — блок `<queries>` в
   `AndroidManifest.xml`, иначе на Android 11+ они не будут видны через
   `PackageManager`).

## Чего нет (сделано намеренно как заглушка)

`vpn/CoreEngineBridge.kt` содержит `MockCoreEngine` — интерфейс поднимает
TUN, но реальный трафик через него **не идёт**. Это позволяет полностью
протестировать UI и логику профилей уже сейчас, до интеграции ядра.

## Как открыть и собрать

1. Android Studio (Koala/2024.1 или новее).
2. `File → Open` → выбрать папку `vpn-client-android`.
3. При первом открытии Android Studio предложит сгенерировать Gradle
   wrapper — согласитесь (в архиве специально не было бинарника
   `gradle-wrapper.jar`, его проще получить через саму студию).
4. Дождаться синхронизации Gradle (потребуются AGP 8.5.2, Kotlin 1.9.24 —
   указаны в `build.gradle.kts`, подтянутся автоматически).
5. Run на эмуляторе/устройстве с Android 8.0+ (minSdk 26).

## Как подключить реальный туннель (следующий шаг)

Рекомендуемый путь — **sing-box** (он один поддерживает VMess/VLESS/Trojan/
Shadowsocks/WireGuard/Reality, то есть закрывает весь список протоколов
из ТЗ одним ядром):

1. Взять `libbox-*.aar` из релизов проекта `SagerNet/sing-box` (или собрать
   самому через `gomobile bind`, если нужен нестандартный набор фич).
2. Подключить `.aar` как обычную зависимость модуля `app`.
3. В `CoreEngineBridge.kt` реализовать `SingBoxCoreEngine : CoreEngine`:
   по `Profile` собрать JSON-конфиг sing-box (`outbounds: [...]` с нужным
   протоколом) и передать в `BoxService` из библиотеки.
4. В `VpnConnectionService` передать `ParcelFileDescriptor` из
   `builder.establish()` в это ядро — дальше весь трафик из TUN обрабатывает
   уже sing-box.
5. Заменить `MockCoreEngine()` на `SingBoxCoreEngine()` в
   `VpnConnectionService`.

Альтернатива для WireGuard в отдельности — официальная библиотека
`com.wireguard.android` (WireGuard for Android), если остальные протоколы
не нужны.

## Структура проекта

```
app/src/main/java/com/example/vpnclient/
├── MainActivity.kt
├── VpnClientApp.kt
├── data/
│   ├── model/Profile.kt           # модели профилей всех протоколов
│   ├── parser/                    # разбор ссылок и подписок
│   ├── db/                        # Room: хранение + JSON (де)сериализация
│   └── repository/ProfileRepository.kt
├── vpn/
│   ├── VpnConnectionService.kt    # системный VpnService
│   ├── VpnState.kt
│   └── CoreEngineBridge.kt        # точка интеграции реального ядра
└── ui/
    ├── screens/home/              # экран подключения
    ├── screens/profiles/          # управление профилями
    ├── screens/settings/
    ├── components/                # ConnectButton, ProfileCard
    ├── navigation/
    └── theme/
```

## Известные ограничения черновика

- Нет экрана ручного редактирования профиля (только добавление по ссылке
  и через подписку) — легко добавить по образцу `AddProfileDialog`.
- Нет сканирования QR (зависимость `zxing-android-embedded` уже добавлена
  в `build.gradle.kts`, но экран сканера ещё не написан).
- Нет автообновления подписок по расписанию (можно добавить через
  `WorkManager`).
- Иконки — временные векторные заглушки, замените на свои в
  `res/drawable/ic_launcher_*.xml`.
