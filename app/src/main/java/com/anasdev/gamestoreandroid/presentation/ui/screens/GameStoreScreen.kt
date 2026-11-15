package com.anasdev.gamestoreandroid.presentation.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anasdev.gamestoreandroid.R
import com.anasdev.gamestoreandroid.domain.model.GameResponse
import com.anasdev.gamestoreandroid.presentation.components.AdminSheet
import com.anasdev.gamestoreandroid.presentation.components.AuthDialog
import com.anasdev.gamestoreandroid.presentation.components.CartSheet
import com.anasdev.gamestoreandroid.presentation.components.GameDetailsSheet
import com.anasdev.gamestoreandroid.presentation.components.PurchaseReceiptDialog
import com.anasdev.gamestoreandroid.presentation.components.UserAvatar
import com.anasdev.gamestoreandroid.presentation.screens.OrdersScreen
import com.anasdev.gamestoreandroid.presentation.screens.SettingsScreen
import com.anasdev.gamestoreandroid.presentation.screens.StoreScreen
import com.anasdev.gamestoreandroid.presentation.state.GameStoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameStoreScreen(viewModel: GameStoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val token by viewModel.tokenFlow.collectAsState(initial = null)
    val profilePictureUrl by viewModel.profilePictureUrlFlow.collectAsState(initial = null)
    val isLoggedIn = !token.isNullOrBlank()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollBehavior = if (isLandscape) {
        TopAppBarDefaults.enterAlwaysScrollBehavior()
    } else {
        TopAppBarDefaults.pinnedScrollBehavior()
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAuthDialog by rememberSaveable { mutableStateOf(false) }
    var showCartSheet by rememberSaveable { mutableStateOf(false) }
    var showAdminSheet by rememberSaveable { mutableStateOf(false) }
    var selectedGameIdForDetails by rememberSaveable { mutableStateOf<String?>(null) }
    var authErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedGameForDetails = selectedGameIdForDetails?.let { id ->
        uiState.games.find { it.id == id }
    }

    // Automatically close Cart sheet when a receipt is generated so it doesn't overlap
    LaunchedEffect(uiState.latestReceipt) {
        if (uiState.latestReceipt != null) {
            showCartSheet = false
        }
    }

    val topBarContent: @Composable () -> Unit = {
        TopAppBar(
            scrollBehavior = scrollBehavior,
            title = {
                Text(
                    text = when (selectedTab) {
                        0 -> stringResource(R.string.app_name)
                        1 -> stringResource(R.string.nav_orders)
                        else -> stringResource(R.string.nav_settings)
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
                                    contentDescription = stringResource(R.string.cart_title)
                                )
                            }
                        }

                        // Profile Icon / User Avatar
                        IconButton(onClick = {
                            if (isLoggedIn) {
                                selectedTab = 2
                            } else {
                                showAuthDialog = true
                            }
                        }) {
                            UserAvatar(
                                profilePictureUrl = if (isLoggedIn) profilePictureUrl else null,
                                size = 30.dp,
                                contentDescription = stringResource(R.string.cd_account)
                            )
                        }
                    }
                    1 -> {
                        // Profile Icon / User Avatar
                        IconButton(onClick = {
                            if (isLoggedIn) {
                                selectedTab = 2
                            } else {
                                showAuthDialog = true
                            }
                        }) {
                            UserAvatar(
                                profilePictureUrl = if (isLoggedIn) profilePictureUrl else null,
                                size = 30.dp,
                                contentDescription = stringResource(R.string.cd_account)
                            )
                        }
                    }
                    2 -> {
                        // Settings Tab: No extra top actions
                    }
                }
            }
        )
    }

    val screenBody: @Composable (PaddingValues) -> Unit = { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> StoreScreen(
                    viewModel = viewModel,
                    onGameClick = { game ->
                        selectedGameIdForDetails = game.id
                    },
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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isLandscape) {
                    Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                } else {
                    Modifier
                }
            )
    ) {
        if (isLandscape) {
            NavigationRail(
                header = {
                    Spacer(modifier = Modifier.height(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.SportsEsports,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                modifier = Modifier.fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.weight(1f))
                NavigationRailItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        viewModel.loadGames()
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = stringResource(R.string.nav_store)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_store)) }
                )
                NavigationRailItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        if (isLoggedIn) viewModel.loadOrders()
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = stringResource(R.string.nav_orders)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_orders)) }
                )
                NavigationRailItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_settings)) }
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Scaffold(
            modifier = Modifier
                .weight(1f)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = topBarContent,
            bottomBar = {
                if (!isLandscape) {
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
                                    contentDescription = stringResource(R.string.nav_store)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_store)) }
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
                                    contentDescription = stringResource(R.string.nav_orders)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_orders)) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.nav_settings)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_settings)) }
                        )
                    }
                }
            },
            contentWindowInsets = if (isLandscape) {
                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
            } else {
                ScaffoldDefaults.contentWindowInsets
            }
        ) { innerPadding ->
            screenBody(innerPadding)
        }
    }

    // Cart Bottom Sheet
    if (showCartSheet) {
        CartSheet(
            cartItems = uiState.cartItems,
            isLoggedIn = isLoggedIn,
            isLoading = uiState.isLoading,
            removingCartItemIds = uiState.removingCartItemIds,
            onDismiss = { showCartSheet = false },
            onCheckout = { viewModel.checkoutCart() },
            onRemoveItem = { keyId -> viewModel.removeFromCart(keyId) },
            onLoginClick = {
                showCartSheet = false
                showAuthDialog = true
            }
        )
    }

    // Purchase Receipt Dialog
    uiState.latestReceipt?.let { receipt ->
        PurchaseReceiptDialog(
            receipt = receipt,
            onDismiss = { viewModel.dismissReceipt() }
        )
    }

    // Admin Sheet
    if (showAdminSheet) {
        AdminSheet(
            games = uiState.games,
            isLoading = uiState.isLoading,
            isUploadingImage = uiState.isUploadingImage,
            uploadedImagePath = uiState.uploadedImagePath,
            successMessage = uiState.successMessage,
            onDismiss = {
                showAdminSheet = false
                viewModel.clearSuccess()
                viewModel.clearUploadedImage()
            },
            onCreateGame = { request, imageBytes ->
                viewModel.createGame(request, imageBytes)
            },
            onUploadKeys = { gameId, keys ->
                viewModel.addGameKeys(gameId, keys)
            },
            onUploadImage = { bytes, fileName, mimeType ->
                viewModel.uploadGameImage(bytes, fileName, mimeType)
            },
            onClearSuccess = { viewModel.clearSuccess() },
            onClearUploadedImage = { viewModel.clearUploadedImage() }
        )
    }

    // Game Details Sheet
    selectedGameForDetails?.let { game ->
        val liveGame = uiState.games.find { it.id == game.id } ?: game
        GameDetailsSheet(
            game = liveGame,
            isLoggedIn = isLoggedIn,
            onDismiss = {
                selectedGameIdForDetails = null
            },
            onAddToCart = { gameId, onResult ->
                if (isLoggedIn) {
                    viewModel.addToCart(gameId, onResult)
                } else {
                    selectedGameIdForDetails = null
                    showAuthDialog = true
                }
            }
        )
    }

    // Global Shell Dialogs
    if (showAuthDialog) {
        AuthDialog(
            onDismiss = { showAuthDialog = false },
            onGoogleLogin = { idToken, profilePicUrl, displayName ->
                viewModel.loginWithGoogle(idToken, profilePicUrl, displayName)
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
            title = { Text(stringResource(R.string.notice_sign_in)) },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { authErrorMessage = null }) {
                    Text(stringResource(R.string.action_done))
                }
            }
        )
    }
}
