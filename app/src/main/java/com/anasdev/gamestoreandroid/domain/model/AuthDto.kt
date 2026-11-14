package com.anasdev.gamestoreandroid.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val role: String
)

@Serializable
data class GoogleAuthRequest(
    val idToken: String
)
