package com.example.vuvur

import android.app.Application
import com.example.vuvur.data.SettingsRepository
import com.example.vuvur.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class VuvurApplication : Application() {
    // ✅ Create a CoroutineScope that lives as long as the application
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ✅ Pass the new scope into the repository's constructor
    val settingsRepository by lazy {
        SettingsRepository(dataStore, applicationScope)
    }

    // ✅ Create the ApiClient
    val apiClient by lazy {
        ApiClient(settingsRepository)
    }

    // ✅ Create a VuvurApiService instance
    lateinit var vuvurApiService: VuvurApiService

    override fun onCreate() {
        super.onCreate()
        // ✅ Initialize the vuvurApiService
        applicationScope.launch {
            val activeUrl = settingsRepository.activeApiUrlFlow.first()
            vuvurApiService = apiClient.createService(activeUrl)
        }
    }
}