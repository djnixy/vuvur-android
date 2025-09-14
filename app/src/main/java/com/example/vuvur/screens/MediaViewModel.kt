package com.example.vuvur.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vuvur.ApiClient
import com.example.vuvur.AppSettings
import com.example.vuvur.GalleryUiState
import com.example.vuvur.VuvurApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as VuvurApplication).settingsRepository
    private val apiService = ApiClient.createService(repository)

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var appSettings: AppSettings? = null
    private var pollingJob: Job? = null
    private var currentSort = "random"
    private var currentQuery = ""

    init {
        viewModelScope.launch {
            repository.refreshTrigger.collectLatest {
                refresh()
            }
        }
        loadSettingsAndFiles()
    }

    private fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (appSettings == null) {
                    appSettings = apiService.getSettings().settings
                }
            } catch (e: Exception) {
                // failed, will use default
            }
        }
    }

    fun loadPage(page: Int, isNewSearch: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (appSettings == null) { loadSettings() }

            val currentState = _uiState.value
            if (currentState is GalleryUiState.Success && currentState.isLoadingNextPage) return@launch
            if (currentState is GalleryUiState.Success && page > currentState.totalPages) return@launch

            if (isNewSearch) {
                _uiState.value = GalleryUiState.Loading
            } else if (currentState is GalleryUiState.Success) {
                _uiState.value = currentState.copy(isLoadingNextPage = true)
            }

            try {
                val activeApiUrl = repository.activeApiUrlFlow.first()
                val response = apiService.getFiles(
                    sortBy = currentSort,
                    query = currentQuery,
                    page = page
                )
                _uiState.update {
                    val currentFiles = if (isNewSearch || it !is GalleryUiState.Success) {
                        emptyList()
                    } else {
                        it.files
                    }
                    // ✅ FIX: Filter out duplicate items before adding them to the list
                    val newItems = response.items.filter { newItem -> currentFiles.none { it.id == newItem.id } }

                    GalleryUiState.Success(
                        files = currentFiles + newItems,
                        totalPages = response.total_pages,
                        currentPage = response.page,
                        isLoadingNextPage = false,
                        activeApiUrl = activeApiUrl
                    )
                }
            } catch (e: HttpException) {
                handleApiError(e)
            } catch (e: Exception) {
                _uiState.value = GalleryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun handleApiError(e: HttpException) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val scanStatus = apiService.getScanStatus()
                if (!scanStatus.scan_complete) {
                    _uiState.value = GalleryUiState.Scanning(scanStatus.progress, scanStatus.total)
                    startPollingForScanStatus()
                } else {
                    _uiState.value = GalleryUiState.Error("Error: ${e.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = GalleryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun startPollingForScanStatus() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(2000)
                try {
                    val status = apiService.getScanStatus()
                    if (status.scan_complete) {
                        refresh()
                        break
                    } else {
                        _uiState.value = GalleryUiState.Scanning(status.progress, status.total)
                    }
                } catch (e: Exception) {
                    // continue polling
                }
            }
        }
    }

    fun getZoomLevel(): Float {
        return appSettings?.zoom_level?.toFloat() ?: 2.5f
    }

    fun loadSettingsAndFiles() {
        viewModelScope.launch {
            loadSettings()
            loadPage(1, isNewSearch = true)
        }
    }

    fun refresh() {
        loadPage(1, isNewSearch = true)
    }
}