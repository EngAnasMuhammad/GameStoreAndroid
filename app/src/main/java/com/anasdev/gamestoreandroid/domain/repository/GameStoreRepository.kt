package com.anasdev.gamestoreandroid.domain.repository

import com.anasdev.gamestoreandroid.domain.model.AuthRequest
import com.anasdev.gamestoreandroid.domain.model.AuthResponse
import com.anasdev.gamestoreandroid.domain.model.CreateGameRequest
import com.anasdev.gamestoreandroid.domain.model.GameResponse
import com.anasdev.gamestoreandroid.domain.model.LockedCartItemResponse
import com.anasdev.gamestoreandroid.domain.model.OrderReceiptResponse
import kotlinx.coroutines.flow.Flow

interface GameStoreRepository {
    val tokenFlow: Flow<String?>
    val roleFlow: Flow<String?>
    val profilePictureUrlFlow: Flow<String?>
    val userNameFlow: Flow<String?>

    suspend fun getToken(): String?
    suspend fun clearAuthData()

    suspend fun getAllGames(): Result<List<GameResponse>>
    suspend fun login(request: AuthRequest): Result<AuthResponse>
    suspend fun register(request: AuthRequest): Result<AuthResponse>
    suspend fun loginWithGoogle(
        idToken: String,
        profilePictureUrl: String? = null,
        displayName: String? = null
    ): Result<AuthResponse>

    suspend fun lockKeyForCart(gameId: String): Result<LockedCartItemResponse>
    suspend fun removeFromCart(keyId: String): Result<Unit>
    suspend fun getCartItems(): Result<List<LockedCartItemResponse>>
    suspend fun checkout(): Result<OrderReceiptResponse>
    suspend fun getOrderHistory(): Result<List<OrderReceiptResponse>>

    suspend fun createGame(request: CreateGameRequest): Result<String>
    suspend fun addGameKeys(gameId: String, keys: List<String>): Result<Int>
    suspend fun uploadGameImage(bytes: ByteArray, fileName: String, mimeType: String): Result<String>
}
