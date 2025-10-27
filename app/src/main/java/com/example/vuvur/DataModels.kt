package com.example.vuvur

// ✅ Import for serialization name
import com.google.gson.annotations.SerializedName

data class MediaFile(
    val id: Int,
    val path: String,
    val type: String,
    val width: Int,
    val height: Int,
    val mod_time: Double,
    val exif: Map<String, Any> = emptyMap()
)

data class PaginatedFileResponse(
    val total_items: Int,
    val page: Int,
    val total_pages: Int,
    val items: List<MediaFile>
)

// ✅ Add data class for Group Info
data class GroupInfo(
    // Map JSON key "group_tag" to Kotlin field "groupTag"
    @SerializedName("group_tag")
    val groupTag: String,
    val count: Int
)

data class ScanStatusResponse(
    val scan_complete: Boolean,
    val progress: Int,
    val total: Int
)

data class CleanupResponse(
    val message: String,
    val deleted_files: Int
)

data class DeleteResponse(
    val status: String,
    val message: String
)

sealed interface GalleryUiState {
    data class Loading(val apiUrl: String? = null) : GalleryUiState
    data class Scanning(val progress: Int, val total: Int) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
    data class Success(
        val files: List<MediaFile> = emptyList(),
        val totalPages: Int = 1,
        val currentPage: Int = 1,
        val isLoadingNextPage: Boolean = false,
        val activeApiUrl: String,
        val zoomLevel: Float = 2.5f,
        // ✅ Add groups and selected group tag to Success state
        val groups: List<GroupInfo> = emptyList(),
        val selectedGroupTag: String? = null
    ) : GalleryUiState
}