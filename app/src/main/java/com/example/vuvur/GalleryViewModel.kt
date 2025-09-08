package com.example.vuvur

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data class Scanning(val progress: Int, val total: Int) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
    data class Success(
        val files: List<MediaFile> = emptyList(),
        val totalPages: Int = 1,
        val currentPage: Int = 1,
        val isLoadingNextPage: Boolean = false
    ) : GalleryUiState
}

class GalleryViewModel : ViewModel() {
    private val api = ApiClient.service

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState = _uiState.asStateFlow()

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
                // Call the API
                val response = api.getFiles(
                    sortBy = "random",
                    query = "",
                    exifQuery = "",
                    page = page
                )
                // We got data, update state to Success
                _uiState.update {
                    val currentFiles = (it as? GalleryUiState.Success)?.files ?: emptyList()
                    GalleryUiState.Success(
                        files = if (isFirstPage) response.items else currentFiles + response.items,
                        totalPages = response.total_pages,
                        currentPage = response.page,
                        isLoadingNextPage = false
                    )
                }
            } catch (e: HttpException) {
                // API call failed, check if it's because the backend is scanning
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
                // Any other error (like network timeout)
                _uiState.value = GalleryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun startPollingForScanStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(2000) // Poll every 2 seconds
                try {
                    val status = api.getScanStatus()
                    if (status.status == "complete") {
                        loadPage(1) // Scan is done! Load the gallery.
                        break // Stop polling
                    } else {
                        _uiState.value = GalleryUiState.Scanning(status.progress, status.total)
                    }
                } catch (e: Exception) {
                    // Do nothing, just retry polling
                }
            }
        }
    }
}