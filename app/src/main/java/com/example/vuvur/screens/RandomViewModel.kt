package com.example.vuvur.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vuvur.ApiClient
import com.example.vuvur.MediaFile
import com.example.vuvur.VuvurApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RandomViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as VuvurApplication).settingsRepository
    private val apiService = ApiClient.createService(repository)

    private val _files = MutableStateFlow<List<MediaFile>>(emptyList())
    val files = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _zoomLevel = MutableStateFlow(2.5) // Default zoom
    val zoomLevel = _zoomLevel.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        loadNextRandomImages(3) // Load first images
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Also load the app settings
                val settingsResponse = apiService.getSettings()
                _zoomLevel.value = settingsResponse.settings.zoom_level
            } catch (e: Exception) {
                e.printStackTrace() // Stick with default zoom if settings fail
            }
        }
    }

    fun loadNextRandomImages(count: Int) {
        if (_isLoading.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val newFiles = apiService.getRandomFiles(count)
                _files.update { currentFiles -> currentFiles + newFiles }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getActiveApiUrl(): String {
        return repository.activeApiUrl
    }
}