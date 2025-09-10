package com.example.vuvur

import com.example.vuvur.data.SettingsRepository
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface VuvurApiService {
    @GET("/api/media")
    suspend fun getFiles(
        @Query("sort") sortBy: String,
        @Query("q") query: String,
        @Query("exif_q") exifQuery: String,
        @Query("page") page: Int
    ): PaginatedFileResponse

    @GET("/api/media")
    suspend fun getFilesScanning(@Query("page") page: Int): ScanStatusResponse

    @GET("/api/scan-status")
    suspend fun getScanStatus(): ScanStatusResponse

    @GET("/api/files/random")
    suspend fun getRandomFiles(@Query("count") count: Int): List<MediaFile>

    @GET("/api/random-single")
    suspend fun getRandomSingle(@Query("q") query: String): MediaFile

    @GET("/api/settings")
    suspend fun getSettings(): SettingsResponse

    @POST("/api/settings")
    suspend fun saveSettings(@Body settings: AppSettings): AppSettings

    @POST("/api/cache/cleanup")
    suspend fun cleanCache(): CleanupResponse
}

object ApiClient {
    private const val DUMMY_URL = "http://localhost/"

    fun createService(repository: SettingsRepository): VuvurApiService {

        val interceptor = Interceptor { chain ->
            var request = chain.request()
            val activeUrlFull = repository.activeApiUrl
            val activeHost = activeUrlFull.substringAfter("http://").substringBefore(":")
            val activePort = activeUrlFull.substringAfterLast(":").removeSuffix("/").toInt()

            val newUrl = request.url.newBuilder()
                .host(activeHost)
                .port(activePort)
                .scheme("http")
                .build()

            request = request.newBuilder().url(newUrl).build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(DUMMY_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(VuvurApiService::class.java)
    }
}