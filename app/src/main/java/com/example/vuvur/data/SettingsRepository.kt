package com.example.vuvur.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vuvur_prefs", Context.MODE_PRIVATE)

    var activeApiUrl: String
        get() = prefs.getString(KEY_ACTIVE_URL, DEFAULT_URLS.first()) ?: DEFAULT_URLS.first()
        set(value) = prefs.edit().putString(KEY_ACTIVE_URL, value).apply()

    var savedApiUrls: Set<String>
        get() = prefs.getStringSet(KEY_URL_LIST, DEFAULT_URLS.toSet()) ?: DEFAULT_URLS.toSet()
        set(value) = prefs.edit().putStringSet(KEY_URL_LIST, value).apply()

    fun addApiUrl(url: String) {
        val current = savedApiUrls.toMutableSet()
        current.add(url)
        savedApiUrls = current
    }

    companion object {
        private const val KEY_ACTIVE_URL = "active_api_url"
        private const val KEY_URL_LIST = "api_url_list"

        // Your saved default URLs
        val DEFAULT_URLS = listOf(
            "http://100.78.149.91:5001",
            "http://100.78.149.91:5002"
        )
    }
}