package com.example.vuvur.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vuvur.VuvurApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = false, // ✅ Simplified
    val activeApi: String = "",
    val apiList: List<String> = emptyList(),
    val message: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // ✅ Get the application instance
    private val app = application as VuvurApplication
    private val repository = app.settingsRepository
    // ✅ Get the apiService from the application instance
    private var apiService = app.vuvurApiService

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.activeApiUrlFlow,
                repository.apiListFlow
            ) { activeUrl, urlList ->
                SettingsUiState(
                    activeApi = activeUrl,
                    apiList = urlList,
                )
            }.collect {
                _uiState.value = it
            }
        }
        // ✅ Listen for API changes and update the local apiService instance
        viewModelScope.launch {
            repository.apiChanged.collectLatest { newApiUrl ->
                apiService = app.apiClient.createService(newApiUrl)
            }
        }
    }

    // ✅ Simplified save function
    fun saveSettings(newActiveApi: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveApiUrl(newActiveApi)
            _uiState.value = _uiState.value.copy(message = "Settings saved!")
        }
    }

    fun runCacheCleanup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.cleanCache()
                _uiState.value = _uiState.value.copy(message = response.message)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = "Cleanup failed")
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}