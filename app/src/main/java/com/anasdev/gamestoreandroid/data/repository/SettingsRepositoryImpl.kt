package com.anasdev.gamestoreandroid.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anasdev.gamestoreandroid.domain.model.AppLanguage
import com.anasdev.gamestoreandroid.domain.model.AppTheme
import com.anasdev.gamestoreandroid.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class SettingsRepositoryImpl(
    private val context: Context,
) : SettingsRepository {

    private companion object {
        val KEY_THEME = stringPreferencesKey("key_app_theme")
        val KEY_LANGUAGE = stringPreferencesKey("key_app_language")
        const val PREFS_BACKUP = "app_settings_shared_prefs"
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_BACKUP, Context.MODE_PRIVATE)

    override val appThemeFlow: Flow<AppTheme> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME] ?: sharedPrefs.getString(KEY_THEME.name, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        runCatching { AppTheme.valueOf(raw) }.getOrDefault(AppTheme.SYSTEM)
    }

    override val appLanguageFlow: Flow<AppLanguage> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_LANGUAGE] ?: sharedPrefs.getString(KEY_LANGUAGE.name, AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name
        runCatching { AppLanguage.valueOf(raw) }.getOrDefault(AppLanguage.SYSTEM)
    }

    override fun getAppTheme(): AppTheme {
        val raw = sharedPrefs.getString(KEY_THEME.name, null)
        if (raw != null) {
            return runCatching { AppTheme.valueOf(raw) }.getOrDefault(AppTheme.SYSTEM)
        }
        return runBlocking {
            val fromDataStore = context.settingsDataStore.data.map { it[KEY_THEME] }.firstOrNull()
            if (fromDataStore != null) {
                runCatching { AppTheme.valueOf(fromDataStore) }.getOrDefault(AppTheme.SYSTEM)
            } else {
                AppTheme.SYSTEM
            }
        }
    }

    override fun setAppTheme(theme: AppTheme) {
        sharedPrefs.edit().putString(KEY_THEME.name, theme.name).apply()
        CoroutineScope(Dispatchers.IO).launch {
            context.settingsDataStore.edit { prefs ->
                prefs[KEY_THEME] = theme.name
            }
        }
    }

    override fun getAppLanguage(): AppLanguage {
        val raw = sharedPrefs.getString(KEY_LANGUAGE.name, null)
        if (raw != null) {
            return runCatching { AppLanguage.valueOf(raw) }.getOrDefault(AppLanguage.SYSTEM)
        }
        return runBlocking {
            val fromDataStore = context.settingsDataStore.data.map { it[KEY_LANGUAGE] }.firstOrNull()
            if (fromDataStore != null) {
                runCatching { AppLanguage.valueOf(fromDataStore) }.getOrDefault(AppLanguage.SYSTEM)
            } else {
                AppLanguage.SYSTEM
            }
        }
    }

    override fun setAppLanguage(language: AppLanguage) {
        sharedPrefs.edit().putString(KEY_LANGUAGE.name, language.name).apply()
        CoroutineScope(Dispatchers.IO).launch {
            context.settingsDataStore.edit { prefs ->
                prefs[KEY_LANGUAGE] = language.name
            }
        }
    }
}
