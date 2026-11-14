package com.anasdev.gamestoreandroid.domain.model

import kotlinx.serialization.Serializable


@Serializable
data class GameResponse(
    val id: String,
    val title: String,
    val description: String?,
    val imagePath: String,
    val genre: String,
    val releaseDate: String,
    val currentPrice: Double,
    val availableStock: Int,
)