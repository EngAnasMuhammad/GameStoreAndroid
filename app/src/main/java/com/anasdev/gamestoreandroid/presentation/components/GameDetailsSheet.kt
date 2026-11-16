package com.anasdev.gamestoreandroid.presentation.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.anasdev.gamestoreandroid.util.consumeUpwardSheetOverscroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anasdev.gamestoreandroid.R
import com.anasdev.gamestoreandroid.domain.model.GameResponse
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailsSheet(
    game: GameResponse?,
    isLoggedIn: Boolean,
    onDismiss: () -> Unit,
    onAddToCart: (gameId: String, onResult: (Boolean) -> Unit) -> Unit
) {
    if (game == null) return

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isAddingToCart by rememberSaveable { mutableStateOf(false) }
    var isAddedSuccess by rememberSaveable { mutableStateOf(false) }
    var localAvailableStock by rememberSaveable(game.id) { mutableIntStateOf(game.availableStock) }

    // Keep local stock aligned if game model stock updates externally
    LaunchedEffect(game.availableStock) {
        localAvailableStock = game.availableStock
    }

    // Auto-dismiss the transient "Added to Cart" confirmation state after 2.5s
    LaunchedEffect(isAddedSuccess) {
        if (isAddedSuccess) {
            delay(2500.milliseconds)
            isAddedSuccess = false
        }
    }

    val inStock = localAvailableStock > 0

    val coverArtwork: @Composable (Modifier, Float) -> Unit = { modifier, ratio ->
        Box(
            modifier = modifier
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            GameCoverImage(
                imagePath = game.imagePath,
                title = game.title,
                isDimmed = !inStock
            )

            if (!inStock) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.store_sold_out),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    val detailsPaneContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Prominent Title
            Text(
                text = game.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Metadata Chips (Genre & Release Date)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(game.genre) },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Category,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(game.releaseDate) },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            // Transient confirmation banner when item is locked & added to cart
            AnimatedVisibility(
                visible = isAddedSuccess,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.details_item_locked_banner),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Stock, Price & Primary Add to Cart Action Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$${"%.2f".format(game.currentPrice)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (inStock) {
                                stringResource(R.string.details_available_stock, localAvailableStock)
                            } else {
                                stringResource(R.string.store_out_of_stock)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (inStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            if (!isLoggedIn) {
                                onAddToCart(game.id) { /* no-op */ }
                                return@Button
                            }
                            if (inStock && !isAddingToCart) {
                                isAddingToCart = true
                                onAddToCart(game.id) { success ->
                                    isAddingToCart = false
                                    if (success) {
                                        localAvailableStock = (localAvailableStock - 1).coerceAtLeast(0)
                                        isAddedSuccess = true
                                    }
                                }
                            }
                        },
                        enabled = (inStock || isAddedSuccess) && !isAddingToCart,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAddedSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        when {
                            isAddingToCart -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(stringResource(R.string.details_adding))
                            }
                            isAddedSuccess -> {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(stringResource(R.string.details_added_success))
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Rounded.AddShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    if (isLoggedIn) {
                                        stringResource(R.string.store_add_to_cart)
                                    } else {
                                        stringResource(R.string.store_login_to_buy)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section 5: Demo / Simulated Keys Disclaimer Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.demo_disclaimer_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Scrollable Description Section
            Text(
                text = stringResource(R.string.details_about_game),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (!game.description.isNullOrBlank()) {
                    game.description
                } else {
                    stringResource(R.string.details_no_description)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = if (isLandscape) 820.dp else BottomSheetDefaults.SheetMaxWidth
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .consumeUpwardSheetOverscroll()
        ) {
            if (isLandscape) {
                // Two-Pane Landscape Layout: Left Artwork (38%) + Right Scrollable Details (62%)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.38f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        coverArtwork(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp),
                            16f / 9f
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.62f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        detailsPaneContent()
                    }
                }
            } else {
                // Single-Column Portrait Layout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    coverArtwork(Modifier.fillMaxWidth(), 16f / 9f)
                    detailsPaneContent()
                }
            }
        }
    }
}
