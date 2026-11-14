package com.anasdev.gamestoreandroid.data.local

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth-pref")

class TokenManager (private val context: Context){

    companion object{
        private val TOKEN_KEY = stringPreferencesKey(name = "jwt-token")
        private val ROLE_KEY = stringPreferencesKey(name = "user-role")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs->
        prefs[TOKEN_KEY]
    }

    val roleFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ROLE_KEY]
    }

    suspend fun getToken(): String? {
        return tokenFlow.firstOrNull()
    }

    suspend fun saveAuthData(token: String, role: String){
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[ROLE_KEY] = role
        }
    }

    suspend fun clearAuthData(){
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}