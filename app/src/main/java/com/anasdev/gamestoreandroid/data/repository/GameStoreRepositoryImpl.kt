package com.anasdev.gamestoreandroid.data.repository

import com.anasdev.gamestoreandroid.data.local.TokenManager
import com.anasdev.gamestoreandroid.data.remote.GameStoreApi
import com.anasdev.gamestoreandroid.data.remote.KtorClient
import com.anasdev.gamestoreandroid.domain.model.AuthRequest
import com.anasdev.gamestoreandroid.domain.model.AuthResponse
import com.anasdev.gamestoreandroid.domain.model.CreateGameRequest
import com.anasdev.gamestoreandroid.domain.model.GameResponse
import com.anasdev.gamestoreandroid.domain.model.LockedCartItemResponse
import com.anasdev.gamestoreandroid.domain.model.OrderReceiptResponse
import com.anasdev.gamestoreandroid.domain.repository.GameStoreRepository
import kotlinx.coroutines.flow.Flow

class GameStoreRepositoryImpl(
    private val api: GameStoreApi,
    private val tokenManager: TokenManager
) : GameStoreRepository {

    override val tokenFlow: Flow<String?> = tokenManager.tokenFlow
    override val roleFlow: Flow<String?> = tokenManager.roleFlow
    override val profilePictureUrlFlow: Flow<String?> = tokenManager.profilePictureUrlFlow
    override val userNameFlow: Flow<String?> = tokenManager.userNameFlow

    override suspend fun getToken(): String? = tokenManager.getToken()

    override suspend fun clearAuthData() {
        tokenManager.clearAuthData()
    }

    private fun resolveImageUrl(path: String?): String {
        if (path.isNullOrBlank()) return ""
        val trimmed = path.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            val base = KtorClient.BASE_URL.trimEnd('/')
            val relative = trimmed.trimStart('/')
            "$base/$relative"
        }
    }

    private fun mapGame(game: GameResponse): GameResponse {
        return game.copy(imagePath = resolveImageUrl(game.imagePath))
    }

    override suspend fun getAllGames(): Result<List<GameResponse>> {
        return api.getAllGames().map { games ->
            games.map { mapGame(it) }
        }
    }

    override suspend fun login(request: AuthRequest): Result<AuthResponse> {
        return api.login(request).onSuccess { auth ->
            tokenManager.saveAuthData(auth.token, auth.role)
        }
    }

    override suspend fun register(request: AuthRequest): Result<AuthResponse> {
        return api.register(request).onSuccess { auth ->
            tokenManager.saveAuthData(auth.token, auth.role)
        }
    }

    override suspend fun loginWithGoogle(
        idToken: String,
        profilePictureUrl: String?,
        displayName: String?
    ): Result<AuthResponse> {
        return api.loginWithGoogle(idToken).onSuccess { auth ->
            tokenManager.saveAuthData(auth.token, auth.role)
            if (!profilePictureUrl.isNullOrBlank()) {
                tokenManager.saveProfilePictureUrl(profilePictureUrl)
            }
            if (!displayName.isNullOrBlank()) {
                tokenManager.saveUserName(displayName)
            }
        }
    }

    private suspend fun requireToken(): String {
        return tokenManager.getToken() ?: throw IllegalStateException("Unauthenticated user")
    }

    override suspend fun lockKeyForCart(gameId: String): Result<LockedCartItemResponse> {
        return try {
            val token = requireToken()
            api.lockKeyForCart(gameId, token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFromCart(keyId: String): Result<Unit> {
        return try {
            val token = requireToken()
            api.removeFromCart(keyId, token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCartItems(): Result<List<LockedCartItemResponse>> {
        return try {
            val token = requireToken()
            api.getCartItems(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkout(): Result<OrderReceiptResponse> {
        return try {
            val token = requireToken()
            api.checkout(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOrderHistory(): Result<List<OrderReceiptResponse>> {
        return try {
            val token = requireToken()
            api.getOrderHistory(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createGame(request: CreateGameRequest): Result<String> {
        return try {
            val token = requireToken()
            api.createGame(request, token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addGameKeys(gameId: String, keys: List<String>): Result<Int> {
        return try {
            val token = requireToken()
            api.addGameKeys(gameId, keys, token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadGameImage(
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<String> {
        return try {
            val token = requireToken()
            api.uploadGameImage(bytes, fileName, mimeType, token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
