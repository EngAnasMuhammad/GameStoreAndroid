package com.anasdev.gamestoreandroid.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth-pref")

class TokenManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey(name = "jwt-token")
        private val ROLE_KEY = stringPreferencesKey(name = "user-role")
        private val PROFILE_PICTURE_URL_KEY = stringPreferencesKey(name = "profile-picture-url")
        private val USER_NAME_KEY = stringPreferencesKey(name = "user-name")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[TOKEN_KEY]
    }

    val roleFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ROLE_KEY]
    }

    val profilePictureUrlFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PROFILE_PICTURE_URL_KEY]
    }

    val userNameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY]
    }

    suspend fun getToken(): String? {
        return tokenFlow.firstOrNull()
    }

    suspend fun saveAuthData(token: String, role: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[ROLE_KEY] = role
        }
    }

    suspend fun saveProfilePictureUrl(url: String?) {
        context.dataStore.edit { prefs ->
            if (!url.isNullOrBlank()) {
                prefs[PROFILE_PICTURE_URL_KEY] = url
            } else {
                prefs.remove(PROFILE_PICTURE_URL_KEY)
            }
        }
    }

    suspend fun saveUserName(name: String?) {
        context.dataStore.edit { prefs ->
            if (!name.isNullOrBlank()) {
                prefs[USER_NAME_KEY] = name
            } else {
                prefs.remove(USER_NAME_KEY)
            }
        }
    }

    suspend fun clearAuthData() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
