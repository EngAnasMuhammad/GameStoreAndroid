package com.anasdev.gamestoreandroid.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Renders a circular user avatar using Coil [SubcomposeAsyncImage] with a subtle border in dark/light mode.
 * Gracefully falls back to [fallbackIcon] when loading, on error, or when [profilePictureUrl] is null/blank.
 */
@Composable
fun UserAvatar(
    profilePictureUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    fallbackIcon: ImageVector = Icons.Rounded.AccountCircle,
    fallbackTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentDescription: String? = "Profile Picture",
) {
    if (!profilePictureUrl.isNullOrBlank()) {
        val context = LocalContext.current
        val imageRequest = remember(profilePictureUrl) {
            ImageRequest.Builder(context)
                .data(profilePictureUrl)
                .crossfade(enable = true)
                .build()
        }

        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                ),
            loading = {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = contentDescription,
                        tint = fallbackTint,
                        modifier = Modifier.size(size)
                    )
                }
            },
            error = {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = contentDescription,
                        tint = fallbackTint,
                        modifier = Modifier.size(size)
                    )
                }
            }
        )
    } else {
        Icon(
            imageVector = fallbackIcon,
            contentDescription = contentDescription,
            tint = fallbackTint,
            modifier = modifier.size(size)
        )
    }
}
