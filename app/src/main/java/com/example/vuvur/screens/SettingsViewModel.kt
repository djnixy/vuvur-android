package com.example.vuvur.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vuvur.ApiClient
import com.example.vuvur.AppSettings
import com.example.vuvur.VuvurApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings? = null,
    val lockedKeys: List<String> = emptyList(),
    val message: String? = null,
    val apiList: List<String> = emptyList(),
    val activeApi: String = ""
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as VuvurApplication).settingsRepository
    private val api = ApiClient.createService(repository)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val response = api.getSettings()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        settings = response.settings,
                        lockedKeys = response.locked_keys,
                        apiList = repository.savedApiUrls.toList(),
                        activeApi = repository.activeApiUrl
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Failed to load settings from API. Using local settings.",
                        apiList = repository.savedApiUrls.toList(),
                        activeApi = repository.activeApiUrl
                    )
                }
            }
        }
    }

    fun saveSettings(newSettings: AppSettings, newActiveApi: String) {
        viewModelScope.launch {
            val oldApi = repository.activeApiUrl
            repository.activeApiUrl = newActiveApi
            repository.addApiUrl(newActiveApi)

            try {
                val savedSettings = api.saveSettings(newSettings)
                _uiState.update {
                    it.copy(
                        settings = savedSettings,
                        activeApi = newActiveApi,
                        apiList = repository.savedApiUrls.toList(),
                        message = "Settings saved!"
                    )
                }
                if (oldApi != newActiveApi) {
                    repository.triggerRefresh()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Failed to save remote settings") }
            }
        }
    }

    fun runCacheCleanup() {
        viewModelScope.launch {
            _uiState.update { it.copy(message = "Cleaning cache...") }
            try {
                val response = api.cleanCache()
                _uiState.update { it.copy(message = response.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Cache cleanup failed") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}