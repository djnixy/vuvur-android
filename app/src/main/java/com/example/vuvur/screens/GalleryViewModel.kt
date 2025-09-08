package com.example.vuvur.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vuvur.ApiClient
import com.example.vuvur.GalleryUiState
import com.example.vuvur.ScanStatusResponse
import com.example.vuvur.VuvurApplication
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as VuvurApplication).settingsRepository
    private val apiService = ApiClient.createService(repository)

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    private var currentSort = "random"
    private var currentQuery = ""
    private var currentExifQuery = ""

    init {
        loadPage(1)
    }

    fun loadPage(page: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFirstPage = page == 1
            if (isFirstPage) {
                _uiState.value = GalleryUiState.Loading
            } else {
                (_uiState.value as? GalleryUiState.Success)?.let {
                    _uiState.value = it.copy(isLoadingNextPage = true)
                }
            }

            try {
                val response = apiService.getFiles(
                    sortBy = currentSort,
                    query = currentQuery,
                    exifQuery = currentExifQuery,
                    page = page
                )
                _uiState.update {
                    val currentFiles = (it as? GalleryUiState.Success)?.files ?: emptyList()
                    GalleryUiState.Success(
                        files = if (isFirstPage) response.items else currentFiles + response.items,
                        totalPages = response.total_pages,
                        currentPage = response.page,
                        isLoadingNextPage = false,
                        activeApiUrl = repository.activeApiUrl
                    )
                }
            } catch (e: HttpException) {
                try {
                    val errorBody = e.response()?.errorBody()?.string() ?: ""
                    val scanStatus = Gson().fromJson(errorBody, ScanStatusResponse::class.java)
                    if (scanStatus.status == "scanning") {
                        _uiState.value = GalleryUiState.Scanning(scanStatus.progress, scanStatus.total)
                        startPollingForScanStatus()
                    } else {
                        _uiState.value = GalleryUiState.Error(e.message())
                    }
                } catch (jsonError: Exception) {
                    _uiState.value = GalleryUiState.Error(e.message())
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
                    if (status.status == "complete") {
                        loadPage(1)
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
}