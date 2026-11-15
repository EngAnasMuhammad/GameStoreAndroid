package com.anasdev.gamestoreandroid.presentation.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.anasdev.gamestoreandroid.domain.model.GameResponse
import com.anasdev.gamestoreandroid.presentation.components.AdminSheet
import com.anasdev.gamestoreandroid.presentation.components.AuthDialog
import com.anasdev.gamestoreandroid.presentation.components.CartSheet
import com.anasdev.gamestoreandroid.presentation.components.GameDetailsSheet
import com.anasdev.gamestoreandroid.presentation.components.PurchaseReceiptDialog
import com.anasdev.gamestoreandroid.presentation.screens.OrdersScreen
import com.anasdev.gamestoreandroid.presentation.screens.SettingsScreen
import com.anasdev.gamestoreandroid.presentation.screens.StoreScreen
import com.anasdev.gamestoreandroid.presentation.state.GameStoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameStoreScreen(viewModel: GameStoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val token by viewModel.tokenFlow.collectAsState(initial = null)
    val isLoggedIn = !token.isNullOrBlank()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showCartSheet by remember { mutableStateOf(false) }
    var showAdminSheet by remember { mutableStateOf(false) }
    var selectedGameForDetails by remember { mutableStateOf<GameResponse?>(null) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    // Automatically close Cart sheet when a receipt is generated so it doesn't overlap
    LaunchedEffect(uiState.latestReceipt) {
        if (uiState.latestReceipt != null) {
            showCartSheet = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            0 -> "GameTrade"
                            1 -> "Orders"
                            else -> "Settings"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    when (selectedTab) {
                        0 -> {
                            // Cart Icon with Badge
                            IconButton(onClick = {
                                if (isLoggedIn) {
                                    viewModel.loadCart()
                                }
                                showCartSheet = true
                            }) {
                                BadgedBox(
                                    badge = {
                                        if (uiState.cartItems.isNotEmpty()) {
                                            Badge {
                                                Text("${uiState.cartItems.size}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ShoppingCart,
                                        contentDescription = "Cart"
                                    )
                                }
                            }

                            // Profile Icon
                            IconButton(onClick = {
                                if (isLoggedIn) {
                                    selectedTab = 2
                                } else {
                                    showAuthDialog = true
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = "Account"
                                )
                            }
                        }
                        1 -> {
                            // Orders Tab: Refresh action
                            IconButton(onClick = {
                                if (isLoggedIn) {
                                    viewModel.loadOrders()
                                } else {
                                    showAuthDialog = true
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                    contentDescription = "Refresh Orders"
                                )
                            }

                            // Profile Icon
                            IconButton(onClick = {
                                if (isLoggedIn) {
                                    selectedTab = 2
                                } else {
                                    showAuthDialog = true
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = "Account"
                                )
                            }
                        }
                        2 -> {
                            // Settings Tab: No extra top actions
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        viewModel.loadGames()
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = "Store"
                        )
                    },
                    label = { Text("Store") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        if (isLoggedIn) viewModel.loadOrders()
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = "Orders"
                        )
                    },
                    label = { Text("Orders") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> StoreScreen(
                    viewModel = viewModel,
                    onGameClick = { game -> selectedGameForDetails = game },
                    onRequireLogin = { showAuthDialog = true }
                )
                1 -> OrdersScreen(
                    viewModel = viewModel,
                    onRequireLogin = { showAuthDialog = true }
                )
                2 -> SettingsScreen(
                    viewModel = viewModel,
                    onRequireLogin = { showAuthDialog = true },
                    onOpenAdminDashboard = { showAdminSheet = true }
                )
            }
        }
    }

    // Modal Bottom Sheets
    if (showAdminSheet) {
        AdminSheet(
            games = uiState.games,
            isLoading = uiState.isLoading,
            isUploadingImage = uiState.isUploadingImage,
            uploadedImagePath = uiState.uploadedImagePath,
            successMessage = uiState.successMessage,
            onDismiss = {
                viewModel.clearSuccess()
                viewModel.clearUploadedImage()
                showAdminSheet = false
            },
            onCreateGame = { request, imageBytes ->
                viewModel.createGame(request, imageBytes)
            },
            onUploadKeys = { gameId, keys -> viewModel.addGameKeys(gameId, keys) },
            onUploadImage = { bytes, fileName, mimeType ->
                viewModel.uploadGameImage(bytes, fileName, mimeType)
            },
            onClearSuccess = { viewModel.clearSuccess() },
            onClearUploadedImage = { viewModel.clearUploadedImage() }
        )
    }

    if (showCartSheet) {
        CartSheet(
            cartItems = uiState.cartItems,
            isLoggedIn = isLoggedIn,
            isLoading = uiState.isLoading,
            onDismiss = { showCartSheet = false },
            onCheckout = { viewModel.checkoutCart() },
            onLoginClick = {
                showCartSheet = false
                showAuthDialog = true
            }
        )
    }

    selectedGameForDetails?.let { game ->
        GameDetailsSheet(
            game = game,
            isLoggedIn = isLoggedIn,
            onDismiss = { selectedGameForDetails = null },
            onAddToCart = { gameId ->
                if (isLoggedIn) {
                    viewModel.addToCart(gameId)
                } else {
                    selectedGameForDetails = null
                    showAuthDialog = true
                }
            }
        )
    }

    // Global Shell Dialogs
    if (showAuthDialog) {
        AuthDialog(
            onDismiss = { showAuthDialog = false },
            onGoogleLogin = { idToken ->
                viewModel.loginWithGoogle(idToken)
                showAuthDialog = false
            },
            onError = { error ->
                authErrorMessage = error
            }
        )
    }

    // Auth error popup if Credential Manager fails
    authErrorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { authErrorMessage = null },
            title = { Text("Sign-In Notice") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { authErrorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    uiState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Notice") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    uiState.latestReceipt?.let { receipt ->
        PurchaseReceiptDialog(
            receipt = receipt,
            onDismiss = { viewModel.dismissReceipt() }
        )
    }
}
