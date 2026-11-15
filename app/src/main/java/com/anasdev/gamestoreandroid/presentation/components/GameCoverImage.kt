package com.anasdev.gamestoreandroid.presentation.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale

@Composable
fun GameCoverImage(
    imagePath: String?,
    title: String,
    modifier: Modifier = Modifier,
    isDimmed: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
    context: Context = LocalContext.current.applicationContext,
) {
    if (imagePath.isNullOrBlank()) {
        ImageFallbackBox(title = title, modifier = modifier.fillMaxSize())
        return
    }

    var isError by remember(imagePath) { mutableStateOf(false) }

    if (isError) {
        ImageFallbackBox(title = title, modifier = modifier.fillMaxSize())
    } else {
        val imageRequest = remember(context, imagePath) {
            ImageRequest.Builder(context)
                .data(imagePath)
                .scale(Scale.FILL)
                .allowHardware(enable = true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .build()
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = title,
                contentScale = contentScale,
                onError = { _ -> isError = true },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (isDimmed) alpha = 0.55f
                    }
            )
        }
    }
}

@Composable
fun ImageFallbackBox(
    title: String,
    modifier: Modifier = Modifier
) {
    val highColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val lowestColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val fallbackBrush = remember(highColor, lowestColor) {
        Brush.verticalGradient(
            colors = listOf(highColor, lowestColor)
        )
    }

    Box(
        modifier = modifier.background(fallbackBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SportsEsports,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
