package com.example.vuvur.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope
) {

    private object PreferencesKeys {
        val ACTIVE_API_URL = stringPreferencesKey("active_api_url")
        val API_LIST = stringSetPreferencesKey("api_list")
    }

    // ✅ Updated the default IP addresses
    private val DEFAULT_API_LIST = listOf(
        "http://100.97.27.128:5001/",
        "http://100.97.27.128:5002/",
        "http://100.78.149.91:5001/",
        "http://100.78.149.91:5002/"
    )

    var activeApiUrl: String = DEFAULT_API_LIST.first()
        private set

    val activeApiUrlFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACTIVE_API_URL] ?: DEFAULT_API_LIST.first()
    }

    val apiListFlow: Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.API_LIST]?.toList()?.takeIf { it.isNotEmpty() } ?: DEFAULT_API_LIST
    }

    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1)
    val refreshTrigger = _refreshTrigger.asSharedFlow()

    init {
        scope.launch {
            activeApiUrlFlow.collect { url ->
                activeApiUrl = url
            }
        }
    }

    suspend fun saveApiUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_API_URL] = url
        }
        _refreshTrigger.tryEmit(Unit)
    }
}