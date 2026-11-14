package com.anasdev.gamestoreandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anasdev.gamestoreandroid.data.local.TokenManager
import com.anasdev.gamestoreandroid.data.remote.GameStoreApi
import com.anasdev.gamestoreandroid.data.remote.KtorClient
import com.anasdev.gamestoreandroid.presentation.state.GameStoreViewModel
import com.anasdev.gamestoreandroid.presentation.ui.screens.GameStoreScreen
import com.anasdev.gamestoreandroid.presentation.ui.theme.GameStoreAndroidTheme
import io.ktor.client.HttpClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenManager = TokenManager(applicationContext)
        val api = GameStoreApi(KtorClient.httpClient, tokenManager)

        setContent {
            GameStoreAndroidTheme {
                val viewModel: GameStoreViewModel = viewModel(
                    factory = GameStoreViewModel.provideFactory(api, tokenManager)
                )
                GameStoreScreen(viewModel = viewModel)
            }
        }
    }
}