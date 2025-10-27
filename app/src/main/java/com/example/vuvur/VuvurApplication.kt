package com.example.vuvur

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder
import com.example.vuvur.data.SettingsRepository
import com.example.vuvur.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
            val activeUrl = settingsRepository.getActiveApiUrl()
            vuvurApiService = apiClient.createService(activeUrl)
        }
    }

    // Override newImageLoader to provide a custom instance
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                // ✅ Remove the SDK_INT check, always use ImageDecoderDecoder
                // Since minSdk is 33, SDK_INT will always be >= 28.
                add(ImageDecoderDecoder.Factory())
            }
            .crossfade(true) // Optional: for smooth image loading
            .build()
    }
}