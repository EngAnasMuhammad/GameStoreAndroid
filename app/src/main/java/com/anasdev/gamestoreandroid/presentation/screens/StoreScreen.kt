package com.anasdev.gamestoreandroid.presentation.screens

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anasdev.gamestoreandroid.R
import com.anasdev.gamestoreandroid.domain.model.GameResponse
import com.anasdev.gamestoreandroid.presentation.components.GameCoverImage
import com.anasdev.gamestoreandroid.presentation.state.GameStoreViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun StoreScreen(
    viewModel: GameStoreViewModel,
    onGameClick: (GameResponse) -> Unit,
    onRequireLogin: () -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    searchFocusTrigger: Int = 0,
) {
    val uiState by viewModel.uiState.collectAsState()
    val token by viewModel.tokenFlow.collectAsState(initial = null)
    val isLoggedIn = !token.isNullOrBlank()
    val games = uiState.filteredGames
    val featuredGames = uiState.featuredGames
    val searchQuery = uiState.searchQuery
    val isSearching = searchQuery.isNotBlank()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchFocusRequester = remember { FocusRequester() }
    var pendingSearchFocus by remember { mutableStateOf(false) }

    // Crash-proof Tap-to-Search: animate scroll to top, then request focus once item is attached
    LaunchedEffect(searchFocusTrigger) {
        if (searchFocusTrigger > 0) {
            pendingSearchFocus = true
            gridState.animateScrollToItem(0)
        }
    }

    // Dismiss keyboard only when the user manually drags the grid (avoids clearing focus on auto-scroll/IME resize)
    LaunchedEffect(gridState.interactionSource) {
        gridState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                focusManager.clearFocus()
            }
        }
    }

    // Propagate application context once instead of querying LocalContext per lazy card
    val context = LocalContext.current.applicationContext

    // Hoist common surface-based gradient brush so every card reuses the same instance
    val surfaceColor = MaterialTheme.colorScheme.surface
    val cardGradientBrush = remember(surfaceColor) {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.70f),
                Color.Black.copy(alpha = 0.95f),
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
            // Entire catalog empty
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
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 4.dp,
                    bottom = 12.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Item 0: Inline Search Bar inside Grid for 1:1 native scroll
                item(
                    key = "store_search_bar",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "search_bar"
                ) {
                    LaunchedEffect(pendingSearchFocus) {
                        if (pendingSearchFocus) {
                            runCatching { searchFocusRequester.requestFocus() }
                            keyboardController?.show()
                            pendingSearchFocus = false
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.store_search_placeholder),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = stringResource(R.string.store_search_content_description),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.store_clear_search),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 4.dp)
                            .focusRequester(searchFocusRequester)
                    )
                }

                // Empty search result state (kept inside grid to preserve focus and avoid unmounting)
                if (isSearching && games.isEmpty()) {
                    item(
                        key = "store_empty_search",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = "empty_search"
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.store_empty_search_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.store_empty_search_desc, searchQuery),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = null)
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(stringResource(R.string.store_action_clear_search))
                                }
                            }
                        }
                    }
                }

                // Item 1: Featured Hero Carousel (Shown only when not searching and at least 2 games)
                if (!isSearching && featuredGames.size >= 2) {
                    item(
                        key = "featured_hero_carousel",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = "featured_carousel"
                    ) {
                        FeaturedHeroCarousel(
                            featuredGames = featuredGames,
                            isLandscape = isLandscape,
                            onGameClick = onGameClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        )
                    }
                }

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
fun FeaturedHeroCarousel(
    featuredGames: List<GameResponse>,
    isLandscape: Boolean,
    onGameClick: (GameResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { featuredGames.size })

    // Auto-scroll every 5 seconds, pausing on touch
    LaunchedEffect(featuredGames.size) {
        if (featuredGames.size < 2) return@LaunchedEffect
        while (true) {
            delay(5_000L.milliseconds)
            if (!pagerState.isScrollInProgress) {
                val nextPage = (pagerState.currentPage + 1) % featuredGames.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val game = featuredGames[page]
            if (isLandscape) {
                FeaturedHeroCardLandscape(
                    game = game,
                    onClick = { onGameClick(game) }
                )
            } else {
                FeaturedHeroCardPortrait(
                    game = game,
                    onClick = { onGameClick(game) }
                )
            }
        }

        // Animated page indicator dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            repeat(featuredGames.size) { index ->
                val isSelected = pagerState.currentPage == index
                val dotWidth by animateDpAsState(
                    targetValue = if (isSelected) 18.dp else 6.dp,
                    animationSpec = tween(durationMillis = 300),
                    label = "dot_width"
                )
                val dotColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = tween(durationMillis = 300),
                    label = "dot_color"
                )
                Box(
                    modifier = Modifier
                        .size(width = dotWidth, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(dotColor)
                )
            }
        }
    }
}

@Composable
fun FeaturedHeroCardPortrait(
    game: GameResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(195.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GameCoverImage(
                imagePath = game.imagePath,
                title = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Scrim overlay for legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // Featured tonal pill badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.store_featured_badge),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Bottom title, genre, and price
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = game.genre,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", game.currentPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun FeaturedHeroCardLandscape(
    game: GameResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start pane: Cover image (aspect ratio 16:9 bounded to card height)
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(16f / 9f)
            ) {
                GameCoverImage(
                    imagePath = game.imagePath,
                    title = game.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // End pane: Badge, Title, Genre, Description Preview, Price
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.store_featured_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = game.genre,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = game.description.orEmpty().ifBlank {
                            stringResource(R.string.details_no_description)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", game.currentPrice)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FilledTonalButton(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.store_view_details),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )

                Text(
                    text = game.genre,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
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
