package com.example.vuvur

data class MediaFile(
    val path: String,
    val type: String,
    val width: Int,
    val height: Int,
    val mod_time: Double,
    val exif: Map<String, Any>
)

data class PaginatedFileResponse(
    val total_items: Int,
    val page: Int,
    val total_pages: Int,
    val items: List<MediaFile>
)

data class ScanStatusResponse(
    val status: String,
    val progress: Int,
    val total: Int
)

data class AppSettings(
    val scan_interval: Int,
    val batch_size: Int,
    val preload_count: Int,
    val zoom_level: Double
)

data class SettingsResponse(
    val settings: AppSettings,
    val locked_keys: List<String>
)

data class CleanupResponse(
    val message: String,
    val deleted_files: Int
)

sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data class Scanning(val progress: Int, val total: Int) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
    data class Success(
        val files: List<MediaFile> = emptyList(),
        val totalPages: Int = 1,
        val currentPage: Int = 1,
        val isLoadingNextPage: Boolean = false,
        val activeApiUrl: String
    ) : GalleryUiState
}