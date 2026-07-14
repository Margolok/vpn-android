package com.example.vpnclient.data.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** Единое хранилище настроек LoudVPN (сессия авторизации + флаги вроде "не обходить РФ-сервисы"). */
val Context.appDataStore by preferencesDataStore(name = "loudvpn_prefs")
