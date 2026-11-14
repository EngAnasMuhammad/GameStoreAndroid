package com.anasdev.gamestoreandroid.data.remote

import com.anasdev.gamestoreandroid.data.local.TokenManager
import com.anasdev.gamestoreandroid.domain.model.AuthRequest
import com.anasdev.gamestoreandroid.domain.model.AuthResponse
import com.anasdev.gamestoreandroid.domain.model.GameResponse
import com.anasdev.gamestoreandroid.domain.model.GoogleAuthRequest
import com.anasdev.gamestoreandroid.domain.model.LockKeyRequest
import com.anasdev.gamestoreandroid.domain.model.LockedCartItemResponse
import com.anasdev.gamestoreandroid.domain.model.OrderReceiptResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

class GameStoreApi(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) {
    suspend fun getAllGames(): Result<List<GameResponse>> = runCatching {
        val response = client.get("games")
        if (response.status.isSuccess()) {
            response.body<List<GameResponse>>()
        } else {
            throw Exception("Failed to load games: ${response.status}")
        }
    }

    suspend fun login(request: AuthRequest): Result<AuthResponse> = runCatching {
        val response = client.post("auth/login"){
            setBody(request)
        }
        if (response.status.isSuccess()){
            val authResponse = response.body<AuthResponse>()
            tokenManager.saveAuthData(authResponse.token, authResponse.role)
            authResponse
        }
        else{
            throw Exception("Invalid Email or Password (${response.status.value})")
        }
    }

    suspend fun register(request: AuthRequest): Result<AuthResponse> = runCatching {
        val response = client.post("auth/register") {
            setBody(request)
        }
        if (response.status.isSuccess()){
            val authResponse = response.body<AuthResponse>()

            tokenManager.saveAuthData(authResponse.token, authResponse.role)
            authResponse
        }
        else {
            throw Exception("Registration failed (${response.status.value})")
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<AuthResponse> = runCatching {
        val response = client.post("auth/google") {
            setBody(GoogleAuthRequest(idToken))
        }
        if (response.status.isSuccess()) {
            val authResponse = response.body<AuthResponse>()
            tokenManager.saveAuthData(authResponse.token, authResponse.role)
            authResponse
        } else {
            throw Exception("Google authentication failed (${response.status.value})")
        }
    }

    suspend fun lockKeyForCart(gameId: String): Result<LockedCartItemResponse> = runCatching {
        val token =
            tokenManager.getToken() ?: throw Exception("Unauthenticated user cannot use cart")

        val response = client.post("cart/lock") {
            header(HttpHeaders.Authorization,"Bearer $token")
            setBody(LockKeyRequest(gameId))
        }

        if(response.status.isSuccess()){
            response.body<LockedCartItemResponse>()
        }
        else if (response.status == HttpStatusCode.Conflict) {
            throw Exception("Out of stock")
        }
        else {
            throw Exception("Failed :(${response.status})")
        }
    }

    suspend fun getCartItems(): Result<List<LockedCartItemResponse>> = runCatching {
        val token = tokenManager.getToken() ?: throw Exception("Unauthenticated user cannot use cart")

        val response = client.get("cart"){
            header(HttpHeaders.Authorization,"Bearer $token")
        }

        if (response.status.isSuccess()){
            response.body<List<LockedCartItemResponse>>()
        }
        else {
            throw Exception("Failed: ${response.status}")
        }

    }

    suspend fun checkout(): Result<OrderReceiptResponse> = runCatching {
        val token = tokenManager.getToken() ?: throw Exception("Unauthenticated user cannot use cart")

        val response = client.post("cart/checkout"){
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (response.status.isSuccess()){
            response.body<OrderReceiptResponse>()
        }
        else{
            throw Exception("Failed: ${response.status}")
        }
    }

    suspend fun getOrderHistory(): Result<List<OrderReceiptResponse>> = runCatching {
        val token = tokenManager.getToken() ?: throw Exception("Unauthenticated user cannot use cart")

        val response = client.get("orders"){
            header(HttpHeaders.Authorization,"Bearer $token")
        }

        if (response.status.isSuccess()) response.body<List<OrderReceiptResponse>>()
        else throw Exception("Failed: ${response.status}")
    }

    suspend fun createGame(request: com.anasdev.gamestoreandroid.domain.model.CreateGameRequest): Result<String> = runCatching {
        val token = tokenManager.getToken() ?: throw Exception("Admin authentication required")

        val response = client.post("admin/games") {
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(request)
        }

        when (response.status) {
            HttpStatusCode.Created, HttpStatusCode.OK -> {
                response.body<String>()
            }
            HttpStatusCode.Forbidden -> {
                throw Exception("Admin privileges required (403 Forbidden)")
            }
            HttpStatusCode.Conflict -> {
                throw Exception("A game with this title already exists (409 Conflict)")
            }
            else -> {
                throw Exception("Failed to create game: ${response.status.value}")
            }
        }
    }

    suspend fun addGameKeys(gameId: String, keys: List<String>): Result<Int> = runCatching {
        val token = tokenManager.getToken() ?: throw Exception("Admin authentication required")

        val response = client.post("admin/games/$gameId/keys") {
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(com.anasdev.gamestoreandroid.domain.model.AddKeyRequest(keys))
        }

        when (response.status) {
            HttpStatusCode.Created, HttpStatusCode.OK -> {
                try {
                    response.body<Int>()
                } catch (_: Exception) {
                    keys.size
                }
            }
            HttpStatusCode.Forbidden -> {
                throw Exception("Admin privileges required (403 Forbidden)")
            }
            HttpStatusCode.Conflict -> {
                throw Exception("One or more keys are duplicate or already exist (409 Conflict)")
            }
            else -> {
                throw Exception("Failed to upload keys: ${response.status.value}")
            }
        }
    }

    suspend fun uploadGameImage(bytes: ByteArray, fileName: String, mimeType: String): Result<String> = runCatching {
        val token = tokenManager.getToken() ?: throw Exception("Admin authentication required")

        val response = client.submitFormWithBinaryData(
            url = "admin/upload-image",
            formData = formData {
                append(
                    key = "image",
                    value = bytes,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    }
                )
            }
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        when (response.status) {
            HttpStatusCode.Created, HttpStatusCode.OK -> {
                val uploadResponse = response.body<com.anasdev.gamestoreandroid.domain.model.ImageUploadResponse>()
                uploadResponse.imagePath
            }
            HttpStatusCode.Forbidden -> {
                throw Exception("Admin privileges required (403 Forbidden)")
            }
            HttpStatusCode.PayloadTooLarge -> {
                throw Exception("Image size is too large (413 Payload Too Large)")
            }
            HttpStatusCode.UnsupportedMediaType -> {
                throw Exception("Unsupported image format (415 Unsupported Media Type)")
            }
            else -> {
                throw Exception("Failed to upload image: ${response.status.value}")
            }
        }
    }
}
