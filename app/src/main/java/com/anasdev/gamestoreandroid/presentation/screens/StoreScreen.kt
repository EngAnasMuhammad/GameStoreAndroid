package com.anasdev.gamestoreandroid.presentation.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anasdev.gamestoreandroid.R
import com.anasdev.gamestoreandroid.domain.model.GameResponse
import com.anasdev.gamestoreandroid.presentation.components.GameCoverImage
import com.anasdev.gamestoreandroid.presentation.state.GameStoreViewModel

@Composable
fun StoreScreen(
    viewModel: GameStoreViewModel,
    onGameClick: (GameResponse) -> Unit,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val token by viewModel.tokenFlow.collectAsState(initial = null)
    val isLoggedIn = !token.isNullOrBlank()
    val games = uiState.games
    val gridState = rememberLazyGridState()

    // Propagate application context once instead of querying LocalContext per lazy card
    val context = LocalContext.current.applicationContext

    // Hoist common surface-based gradient brush so every card reuses the same instance
    val surfaceColor = MaterialTheme.colorScheme.surface
    val cardGradientBrush = remember(surfaceColor) {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                surfaceColor.copy(alpha = 0.70f),
                surfaceColor.copy(alpha = 0.95f)
            )
        )
    }

    // Hoist stable callback to avoid lambda re-allocation per item on recomposition
    val onAddToCart: (String) -> Unit = remember(viewModel) {
        { gameId -> viewModel.addToCart(gameId) }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.loadGames() },
        modifier = modifier.fillMaxSize()
    ) {
        if (!uiState.isLoading && uiState.games.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SportsEsports,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.store_no_games),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { viewModel.loadGames() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.store_reload))
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                state = gridState,
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = games,
                    key = { it.id },
                    contentType = { "game_card" }
                ) { game ->
                    GameGridCard(
                        game = game,
                        isLoggedIn = isLoggedIn,
                        gradientBrush = cardGradientBrush,
                        context = context,
                        onGameClick = onGameClick,
                        onAddToCart = onAddToCart,
                        onRequireLogin = onRequireLogin
                    )
                }
            }
        }
    }
}

@Composable
fun GameGridCard(
    game: GameResponse,
    isLoggedIn: Boolean,
    gradientBrush: Brush,
    context: Context,
    onGameClick: (GameResponse) -> Unit,
    onAddToCart: (String) -> Unit,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inStock = game.availableStock > 0
    val formattedPrice = remember(game.currentPrice) {
        "$${"%.2f".format(game.currentPrice)}"
    }

    Card(
        onClick = { onGameClick(game) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        // Flat 0.dp elevation eliminates dynamic GPU shadow overdraw during rapid scrolling
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
        ) {
            // Cover Image occupying full poster background
            GameCoverImage(
                imagePath = game.imagePath,
                title = game.title,
                isDimmed = !inStock,
                context = context,
                modifier = Modifier.fillMaxSize()
            )

            // Sold Out Overlay Pill
            if (!inStock) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.store_sold_out),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Lower Content with Translucent Opacity Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(gradientBrush)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = game.genre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = formattedPrice,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (inStock) stringResource(R.string.store_stock_count, game.availableStock) else stringResource(R.string.store_out_of_stock),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (inStock) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            if (isLoggedIn) {
                                onAddToCart(game.id)
                            } else {
                                onRequireLogin()
                            }
                        },
                        enabled = inStock,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddShoppingCart,
                            contentDescription = if (isLoggedIn) stringResource(R.string.store_add_to_cart) else stringResource(R.string.store_login_to_buy),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
