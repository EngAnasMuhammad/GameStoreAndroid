package com.anasdev.gamestoreandroid.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.RemoveShoppingCart
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anasdev.gamestoreandroid.R
import com.anasdev.gamestoreandroid.domain.model.LockedCartItemResponse
import com.anasdev.gamestoreandroid.util.BiometricHelper
import com.anasdev.gamestoreandroid.util.consumeUpwardSheetOverscroll
import com.anasdev.gamestoreandroid.util.findFragmentActivity
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

private sealed interface ExpiryState {
    data class Minutes(val count: Long) : ExpiryState
    data object ExpiringSoon : ExpiryState
    data object Expired : ExpiryState
    data class Fallback(val time: String) : ExpiryState
}

/**
 * Safely formats the ISO-8601 `lockedUntil` timestamp into localized countdown text.
 */
@Composable
private fun formatLockedUntil(lockedUntilStr: String): String {
    val state = remember(lockedUntilStr) {
        try {
            val targetInstant = try {
                OffsetDateTime.parse(lockedUntilStr).toInstant()
            } catch (_: Exception) {
                Instant.parse(lockedUntilStr)
            }
            val now = Instant.now()
            val minutesLeft = ChronoUnit.MINUTES.between(now, targetInstant)

            when {
                minutesLeft > 1 -> ExpiryState.Minutes(minutesLeft)
                minutesLeft in 0..1 -> ExpiryState.ExpiringSoon
                else -> ExpiryState.Expired
            }
        } catch (_: Exception) {
            val timeDisplay = if (lockedUntilStr.contains("T")) {
                lockedUntilStr.substringAfter("T").take(5)
            } else {
                lockedUntilStr
            }
            ExpiryState.Fallback(timeDisplay)
        }
    }

    return when (state) {
        is ExpiryState.Minutes -> stringResource(R.string.cart_expires_in_minutes, state.count)
        is ExpiryState.ExpiringSoon -> stringResource(R.string.cart_expiring_soon)
        is ExpiryState.Expired -> stringResource(R.string.cart_expired)
        is ExpiryState.Fallback -> stringResource(R.string.cart_locked_until, state.time)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartSheet(
    cartItems: List<LockedCartItemResponse>,
    isLoggedIn: Boolean,
    isLoading: Boolean,
    removingCartItemIds: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onCheckout: () -> Unit,
    onRemoveItem: (String) -> Unit = {},
    onLoginClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val totalAmount = cartItems.sumOf { it.price }

    val confirmPurchaseTitle = stringResource(R.string.biometric_confirm_purchase_title)
    val confirmPurchaseSubtitle = stringResource(R.string.biometric_confirm_purchase_subtitle)
    val cancelText = stringResource(R.string.action_cancel)

    val handleCheckoutWithBiometrics: () -> Unit = {
        val activity = context.findFragmentActivity()
        if (activity != null && BiometricHelper.isBiometricAvailable(activity)) {
            BiometricHelper.authenticate(
                activity = activity,
                title = confirmPurchaseTitle,
                subtitle = confirmPurchaseSubtitle,
                negativeButtonText = cancelText,
                onSuccess = { onCheckout() },
            )
        } else {
            onCheckout()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        sheetMaxWidth = if (isLandscape) 820.dp else BottomSheetDefaults.SheetMaxWidth
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .consumeUpwardSheetOverscroll()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = stringResource(R.string.cart_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (isLoggedIn && cartItems.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (cartItems.size == 1) {
                                    stringResource(R.string.cart_game_count_single)
                                } else {
                                    stringResource(R.string.cart_game_count_plural, cartItems.size)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()

            when {
                !isLoggedIn -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Login,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.cart_sign_in_required_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.cart_sign_in_required_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onLoginClick,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.action_sign_in_or_register))
                            }
                        }
                    }
                }

                cartItems.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RemoveShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = stringResource(R.string.cart_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.cart_empty_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                isLandscape -> {
                    // Landscape 2-Pane Layout: Left List (~55%) + Right Summary & Checkout (~45%)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left List of Items
                        LazyColumn(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight(0.75f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(cartItems, key = { it.keyId }) { item ->
                                val countForGame = cartItems.count { it.gameId == item.gameId }
                                val indexForGame = if (countForGame > 1) {
                                    cartItems.filter { it.gameId == item.gameId }.indexOf(item) + 1
                                } else null

                                CartItemRow(
                                    item = item,
                                    isRemoving = item.keyId in removingCartItemIds,
                                    onRemove = { onRemoveItem(item.keyId) },
                                    keyIndex = indexForGame
                                )
                            }
                        }

                        // Right Summary & Checkout Action
                        Column(
                            modifier = Modifier
                                .weight(0.9f)
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Reservation Info Note
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.cart_reservation_notice),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.cart_total_amount),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$${"%.2f".format(totalAmount)}",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Button(
                                        onClick = handleCheckoutWithBiometrics,
                                        enabled = !isLoading,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.cart_processing))
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.cart_checkout_button), fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Simulated checkout caption
                                    Text(
                                        text = stringResource(R.string.cart_simulated_checkout_caption),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Portrait Layout: Reservation Notice + Vertical List + Sticky Bottom Footer
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.cart_reservation_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(cartItems, key = { it.keyId }) { item ->
                            val countForGame = cartItems.count { it.gameId == item.gameId }
                            val indexForGame = if (countForGame > 1) {
                                cartItems.filter { it.gameId == item.gameId }.indexOf(item) + 1
                            } else null

                            CartItemRow(
                                item = item,
                                isRemoving = item.keyId in removingCartItemIds,
                                onRemove = { onRemoveItem(item.keyId) },
                                keyIndex = indexForGame
                            )
                        }
                    }

                    // Sticky Bottom Footer
                    Surface(
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.cart_total_amount),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$${"%.2f".format(totalAmount)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Button(
                                    onClick = handleCheckoutWithBiometrics,
                                    enabled = !isLoading,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.cart_processing))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.cart_checkout_button), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = stringResource(R.string.cart_simulated_checkout_caption),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun CartItemRow(
    item: LockedCartItemResponse,
    isRemoving: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    keyIndex: Int? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.gameTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (keyIndex != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.cart_key_locked_badge, keyIndex),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$${"%.2f".format(item.price)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LockClock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatLockedUntil(item.lockedUntil),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = onRemove,
                enabled = !isRemoving
            ) {
                if (isRemoving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.admin_remove_image),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
