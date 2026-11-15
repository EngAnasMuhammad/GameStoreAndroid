package com.anasdev.gamestoreandroid.domain.repository

import com.anasdev.gamestoreandroid.domain.model.AppLanguage
import com.anasdev.gamestoreandroid.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val appThemeFlow: Flow<AppTheme>
    val appLanguageFlow: Flow<AppLanguage>
    fun getAppTheme(): AppTheme
    fun setAppTheme(theme: AppTheme)
    fun getAppLanguage(): AppLanguage
    fun setAppLanguage(language: AppLanguage)
}
