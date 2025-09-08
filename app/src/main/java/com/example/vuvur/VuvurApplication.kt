package com.example.vuvur

import android.app.Application
import com.example.vuvur.data.SettingsRepository

class VuvurApplication : Application() {

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(applicationContext)
    }
}