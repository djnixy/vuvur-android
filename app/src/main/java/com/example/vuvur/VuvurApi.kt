package com.example.vuvur

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Data models to match the JSON response from our API.
 */
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

/**
 * The Retrofit interface defining all our API endpoints.
 */
interface VuvurApiService {
    @GET("/api/files")
    suspend fun getFiles(
        @Query("sort") sortBy: String,
        @Query("q") query: String,
        @Query("exif_q") exifQuery: String,
        @Query("page") page: Int
    ): PaginatedFileResponse

    @GET("/api/files")
    suspend fun getFilesScanning(@Query("page") page: Int): ScanStatusResponse

    @GET("/api/scan-status")
    suspend fun getScanStatus(): ScanStatusResponse
}

/**
 * A singleton object to create and hold our Retrofit client.
 */
object ApiClient {
    // CHANGE THIS LINE: Replace the emulator IP with your PC's Wi-Fi IP
    const val API_BASE_URL = "http://100.78.149.91:5001/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: VuvurApiService = retrofit.create(VuvurApiService::class.java)
}