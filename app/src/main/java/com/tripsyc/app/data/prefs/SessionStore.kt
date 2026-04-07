package com.tripsyc.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tripsyc_session")

class SessionStore(private val context: Context) {

    companion object {
        private val COOKIES_KEY = stringPreferencesKey("session_cookies")
    }

    val cookies: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[COOKIES_KEY]
    }

    suspend fun saveCookies(cookieString: String) {
        context.dataStore.edit { prefs ->
            prefs[COOKIES_KEY] = cookieString
        }
    }

    suspend fun clearCookies() {
        context.dataStore.edit { prefs ->
            prefs.remove(COOKIES_KEY)
        }
    }
}
