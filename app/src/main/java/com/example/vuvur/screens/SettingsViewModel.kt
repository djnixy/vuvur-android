package com.example.vuvur.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vuvur.ApiClient
import com.example.vuvur.AppSettings
import com.example.vuvur.VuvurApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings? = null,
    val lockedKeys: List<String> = emptyList(),
    val activeApi: String = "",
    val apiList: List<String> = emptyList(),
    val message: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as VuvurApplication).settingsRepository
    private val apiService = ApiClient.createService(repository)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.activeApiUrlFlow,
                repository.apiListFlow
            ) { activeUrl, urlList ->
                SettingsUiState(
                    isLoading = _uiState.value.isLoading,
                    settings = _uiState.value.settings,
                    lockedKeys = _uiState.value.lockedKeys,
                    activeApi = activeUrl,
                    apiList = urlList,
                    message = _uiState.value.message
                )
            }.collect {
                _uiState.value = it
            }
        }
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getSettings()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    settings = response.settings,
                    lockedKeys = response.locked_keys
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, message = "Failed to load settings")
            }
        }
    }

    fun saveSettings(newSettings: AppSettings, newActiveApi: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiService.saveSettings(newSettings)
                repository.saveApiUrl(newActiveApi)
                _uiState.value = _uiState.value.copy(message = "Settings saved!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = "Failed to save settings")
            }
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