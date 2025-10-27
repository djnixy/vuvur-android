package com.example.vuvur.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vuvur.GalleryUiState
import com.example.vuvur.GroupInfo // ✅ Import GroupInfo
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

    private val app = application as VuvurApplication
    private val repository = app.settingsRepository
    private var apiService = app.vuvurApiService

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading())
    val uiState = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var currentSort = "random"
    private var currentQuery = ""
    // ✅ Add state for current group tag
    private var currentGroupTag: String? = null

    init {
        viewModelScope.launch {
            repository.refreshTrigger.collectLatest {
                refresh()
            }
        }
        viewModelScope.launch {
            repository.apiChanged.collectLatest { newApiUrl ->
                apiService = app.apiClient.createService(newApiUrl)
                refresh()
            }
        }
        viewModelScope.launch {
            repository.zoomChanged.collect { newZoomLevel ->
                _uiState.update { currentState ->
                    if (currentState is GalleryUiState.Success) {
                        currentState.copy(zoomLevel = newZoomLevel)
                    } else {
                        currentState
                    }
                }
            }
        }
        loadPage(1, isNewSearch = true)
    }

    fun applySearch(query: String) {
        currentQuery = query
        refresh()
    }

    fun applySort(sortBy: String) {
        currentSort = sortBy
        refresh()
    }

    // ✅ Add function to apply group filter
    fun applyGroupFilter(groupTag: String?) {
        currentGroupTag = groupTag
        refresh()
    }

    fun deleteMediaItem(mediaId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiService.deleteMediaItem(mediaId)
                _uiState.update {
                    if (it is GalleryUiState.Success) {
                        it.copy(files = it.files.filterNot { file -> file.id == mediaId })
                    } else {
                        it
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadPage(page: Int, isNewSearch: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = _uiState.value
            // Prevent loading next page if already loading or no more pages
            if (currentState is GalleryUiState.Success && currentState.isLoadingNextPage) return@launch
            if (currentState is GalleryUiState.Success && !isNewSearch && page > currentState.totalPages) return@launch

            val activeApiUrl = repository.activeApiUrlFlow.first()
            val zoomLevel = repository.zoomLevelFlow.first()
            var groups: List<GroupInfo> = (currentState as? GalleryUiState.Success)?.groups ?: emptyList()

            if (isNewSearch) {
                _uiState.value = GalleryUiState.Loading(activeApiUrl)
                // ✅ Fetch groups only on a new search/refresh
                try {
                    groups = apiService.getGroups()
                } catch (e: Exception) {
                    // Ignore group fetching errors for now, gallery might still work
                    println("Failed to fetch groups: ${e.message}")
                    groups = emptyList() // Ensure groups is empty on error
                }
            } else if (currentState is GalleryUiState.Success) {
                _uiState.value = currentState.copy(isLoadingNextPage = true)
            }

            try {
                val response = apiService.getFiles(
                    sortBy = currentSort,
                    query = currentQuery,
                    page = page,
                    // ✅ Pass the current group tag
                    group = currentGroupTag
                )
                _uiState.update {
                    val currentFiles = if (isNewSearch || it !is GalleryUiState.Success) {
                        emptyList()
                    } else {
                        it.files
                    }
                    // Prevent duplicates when loading next page
                    val newItems = if (isNewSearch) response.items else response.items.filter { newItem -> currentFiles.none { it.id == newItem.id } }

                    GalleryUiState.Success(
                        files = currentFiles + newItems,
                        totalPages = response.total_pages,
                        currentPage = response.page,
                        isLoadingNextPage = false,
                        activeApiUrl = activeApiUrl,
                        zoomLevel = zoomLevel,
                        // ✅ Update state with groups and selected tag
                        groups = groups,
                        selectedGroupTag = currentGroupTag
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
                        refresh() // Refresh gallery after scan completes
                        break
                    } else {
                        _uiState.value = GalleryUiState.Scanning(status.progress, status.total)
                    }
                } catch (e: Exception) {
                    // continue polling on error
                }
            }
        }
    }

    fun refresh() {
        pollingJob?.cancel() // Stop polling if a manual refresh is triggered
        loadPage(1, isNewSearch = true)
    }
}