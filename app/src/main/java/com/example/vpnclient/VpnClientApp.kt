package com.example.vpnclient

import android.app.Application
import com.example.vpnclient.data.auth.AuthRepository
import com.example.vpnclient.data.db.AppDatabase
import com.example.vpnclient.data.repository.ProfileRepository
import com.example.vpnclient.data.settings.SettingsRepository

class VpnClientApp : Application() {

    lateinit var repository: ProfileRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        repository = ProfileRepository(db.profileDao())
        authRepository = AuthRepository(this)
        settingsRepository = SettingsRepository(this)
    }
}
