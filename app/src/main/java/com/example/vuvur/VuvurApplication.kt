package com.example.vuvur

import android.app.Application
import android.content.Context
import android.os.Build.VERSION.SDK_INT // Import SDK_INT
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import coil.ImageLoader // Import ImageLoader
import coil.ImageLoaderFactory // Import ImageLoaderFactory
import coil.decode.GifDecoder // Import GifDecoder
import coil.decode.ImageDecoderDecoder // Import ImageDecoderDecoder
import com.example.vuvur.data.SettingsRepository
import com.example.vuvur.data.dataStore // Import top-level dataStore definition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Define the CoroutineScope at the application level
private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

// Implement ImageLoaderFactory
class VuvurApplication : Application(), ImageLoaderFactory {
    // Initialize repository using the imported dataStore and applicationScope
    val settingsRepository by lazy {
        SettingsRepository(dataStore, applicationScope)
    }
    val apiClient by lazy {
        ApiClient(settingsRepository)
    }
    lateinit var vuvurApiService: VuvurApiService

    override fun onCreate() {
        super.onCreate()
        // Initialize ApiService asynchronously
        applicationScope.launch {
            // Fetch initial URL using the suspend function from the initialized repository
            // Make sure getActiveApiUrl exists and is a suspend function in SettingsRepository
            val activeUrl = settingsRepository.getActiveApiUrl()
            vuvurApiService = apiClient.createService(activeUrl)
        }
    }

    // Override newImageLoader to provide a custom instance
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                // Add the correct decoder based on SDK version
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory()) // Handles GIFs and more on API 28+
                } else {
                    add(GifDecoder.Factory()) // Fallback for older APIs
                }
            }
            .crossfade(true) // Optional: for smooth image loading
            .build()
    }
}