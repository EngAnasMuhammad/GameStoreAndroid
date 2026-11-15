package com.anasdev.gamestoreandroid

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anasdev.gamestoreandroid.data.local.TokenManager
import com.anasdev.gamestoreandroid.data.remote.GameStoreApi
import com.anasdev.gamestoreandroid.data.remote.KtorClient
import com.anasdev.gamestoreandroid.data.repository.GameStoreRepositoryImpl
import com.anasdev.gamestoreandroid.data.repository.SettingsRepositoryImpl
import com.anasdev.gamestoreandroid.domain.model.AppTheme
import com.anasdev.gamestoreandroid.presentation.state.GameStoreViewModel
import com.anasdev.gamestoreandroid.presentation.ui.screens.GameStoreScreen
import com.anasdev.gamestoreandroid.presentation.ui.theme.GameStoreAndroidTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenManager = TokenManager(applicationContext)
        val api = GameStoreApi(KtorClient.httpClient)
        val repository = GameStoreRepositoryImpl(api, tokenManager)
        val settingsRepository = SettingsRepositoryImpl(applicationContext)

        setContent {
            val viewModel: GameStoreViewModel = viewModel(
                factory = GameStoreViewModel.provideFactory(repository, settingsRepository)
            )

            val appTheme by viewModel.appTheme.collectAsState()
            val appLanguage by viewModel.appLanguage.collectAsState()

            // Dynamic per-app locale switching
            LaunchedEffect(appLanguage) {
                val targetLocales = if (appLanguage.tag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(appLanguage.tag)
                }
                if (AppCompatDelegate.getApplicationLocales() != targetLocales) {
                    AppCompatDelegate.setApplicationLocales(targetLocales)
                }
            }

            // Dynamic dark theme calculation
            val darkTheme = when (appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            GameStoreAndroidTheme(darkTheme = darkTheme) {
                GameStoreScreen(viewModel = viewModel)
            }
        }
    }
}
