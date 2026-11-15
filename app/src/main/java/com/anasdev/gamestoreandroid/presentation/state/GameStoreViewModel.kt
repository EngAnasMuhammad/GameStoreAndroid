package com.anasdev.gamestoreandroid.presentation.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewModelScope
import com.anasdev.gamestoreandroid.domain.model.AppLanguage
import com.anasdev.gamestoreandroid.domain.model.AppTheme
import com.anasdev.gamestoreandroid.domain.model.AuthRequest
import com.anasdev.gamestoreandroid.domain.model.CreateGameRequest
import com.anasdev.gamestoreandroid.domain.model.GameResponse
import com.anasdev.gamestoreandroid.domain.model.LockedCartItemResponse
import com.anasdev.gamestoreandroid.domain.model.OrderReceiptResponse
import com.anasdev.gamestoreandroid.domain.repository.GameStoreRepository
import com.anasdev.gamestoreandroid.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameStoreUiState(
    val isLoading: Boolean = false,
    val games: List<GameResponse> = emptyList(),
    val cartItems: List<LockedCartItemResponse> = emptyList(),
    val removingCartItemIds: Set<String> = emptySet(),
    val orders: List<OrderReceiptResponse> = emptyList(),
    val latestReceipt: OrderReceiptResponse? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isUploadingImage: Boolean = false,
    val uploadedImagePath: String? = null
)

class GameStoreViewModel(
    private val repository: GameStoreRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameStoreUiState())
    val uiState = _uiState.asStateFlow()

    val tokenFlow = repository.tokenFlow
    val roleFlow = repository.roleFlow
    val profilePictureUrlFlow = repository.profilePictureUrlFlow
    val userNameFlow = repository.userNameFlow

    private val _appTheme = MutableStateFlow(settingsRepository.getAppTheme())
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _appLanguage = MutableStateFlow(settingsRepository.getAppLanguage())
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    fun setAppTheme(theme: AppTheme) {
        settingsRepository.setAppTheme(theme)
        _appTheme.value = theme
    }

    fun setAppLanguage(language: AppLanguage) {
        settingsRepository.setAppLanguage(language)
        _appLanguage.value = language
    }

    private fun sanitizeErrorMessage(throwable: Throwable, fallback: String): String {
        val raw = throwable.message.orEmpty()
        return when {
            raw.contains("resolve host", ignoreCase = true) ||
            raw.contains("connect", ignoreCase = true) ||
            raw.contains("timeout", ignoreCase = true) ||
            raw.contains("onrender.com", ignoreCase = true) ||
            raw.contains("http", ignoreCase = true) ||
            raw.isBlank() -> fallback
            else -> raw
        }
    }

    init {
        loadGames()
        viewModelScope.launch {
            if (repository.getToken() != null) {
                loadCart()
                loadOrders()
            }
        }
    }

    fun loadGames() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            repository.getAllGames()
                .onSuccess { gameList ->
                    _uiState.update { it.copy(isLoading = false, games = gameList) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = sanitizeErrorMessage(error, "Unable to load games. Please check your network connection.")
                        )
                    }
                }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            repository.login(AuthRequest(email, password))
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    loadCart()
                    loadOrders()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = sanitizeErrorMessage(error, "Sign-in failed. Please verify your credentials.")
                        )
                    }
                }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            repository.register(AuthRequest(email, password))
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    loadCart()
                    loadOrders()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = sanitizeErrorMessage(error, "Registration failed. Please check your information and try again.")
                        )
                    }
                }
        }
    }

    fun loginWithGoogle(
        idToken: String,
        profilePictureUrl: String? = null,
        displayName: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            repository.loginWithGoogle(idToken, profilePictureUrl, displayName)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    loadCart()
                    loadOrders()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = sanitizeErrorMessage(error, "Google authentication failed. Please try again.")
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearAuthData()

            _uiState.update {
                it.copy(
                    cartItems = emptyList(),
                    removingCartItemIds = emptySet(),
                    orders = emptyList(),
                    latestReceipt = null
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadCart() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getCartItems()
                .onSuccess { cartItems ->
                    _uiState.update { it.copy(isLoading = false, cartItems = cartItems) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = sanitizeErrorMessage(error, "Unable to load cart. Please try again.")
                        )
                    }
                }
        }
    }

    fun addToCart(gameId: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            repository.lockKeyForCart(gameId)
                .onSuccess { lockedItem ->
                    _uiState.update { current ->
                        current.copy(
                            cartItems = current.cartItems + lockedItem,
                            games = current.games.map { game ->
                                if (game.id == gameId) {
                                    game.copy(availableStock = (game.availableStock - 1).coerceAtLeast(0))
                                } else {
                                    game
                                }
                            }
                        )
                    }
                    onResult?.invoke(true)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = sanitizeErrorMessage(error, "Unable to reserve game key. Item may be out of stock.")
                        )
                    }
                    onResult?.invoke(false)
                }
        }
    }

    fun removeFromCart(keyId: String) {
        val removedItem = _uiState.value.cartItems.find { it.keyId == keyId }
        val gameId = removedItem?.gameId

        viewModelScope.launch {
            _uiState.update { it.copy(removingCartItemIds = it.removingCartItemIds + keyId, errorMessage = null) }
            repository.removeFromCart(keyId)
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            removingCartItemIds = current.removingCartItemIds - keyId,
                            cartItems = current.cartItems.filterNot { it.keyId == keyId },
                            games = if (gameId != null) {
                                current.games.map { game ->
                                    if (game.id == gameId) {
                                        game.copy(availableStock = game.availableStock + 1)
                                    } else {
                                        game
                                    }
                                }
                            } else {
                                current.games
                            }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            removingCartItemIds = it.removingCartItemIds - keyId,
                            errorMessage = sanitizeErrorMessage(error, "Failed to remove item from cart. Please try again.")
                        )
                    }
                }
        }
    }

    fun checkoutCart() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.checkout()
                .onSuccess { receipt ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            latestReceipt = receipt,
                            cartItems = emptyList()
                        )
                    }
                    loadOrders()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = sanitizeErrorMessage(error, "Checkout failed. Please review your cart and try again.")
                        )
                    }
                }
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getOrderHistory()
                .onSuccess { ordersList ->
                    _uiState.update { it.copy(isLoading = false, orders = ordersList) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = sanitizeErrorMessage(error, "Unable to load order history. Please try again.")
                        )
                    }
                }
        }
    }

    fun dismissReceipt() {
        _uiState.update { it.copy(latestReceipt = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun createGame(
        request: CreateGameRequest,
        imageBytes: ByteArray? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            var finalImagePath = request.imagePath
            if (imageBytes != null && imageBytes.isNotEmpty()) {
                val uploadResult = repository.uploadGameImage(imageBytes, "cover.jpg", "image/jpeg")
                if (uploadResult.isFailure) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Image upload failed. Please try a different image or try again."
                        )
                    }
                    return@launch
                }
                finalImagePath = uploadResult.getOrNull() ?: ""
            }

            val finalRequest = request.copy(imagePath = finalImagePath)
            repository.createGame(finalRequest)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            uploadedImagePath = null,
                            successMessage = "Game \"${finalRequest.title}\" published successfully!"
                        )
                    }
                    loadGames()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = sanitizeErrorMessage(error, "Failed to create game. Please check your input and try again.")
                        )
                    }
                }
        }
    }

    fun addGameKeys(gameId: String, keys: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            repository.addGameKeys(gameId, keys)
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "$count key(s) uploaded successfully!"
                        )
                    }
                    loadGames()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = sanitizeErrorMessage(error, "Failed to upload keys. Please try again.")
                        )
                    }
                }
        }
    }

    fun uploadGameImage(bytes: ByteArray, fileName: String, mimeType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true, errorMessage = null) }
            repository.uploadGameImage(bytes, fileName, mimeType)
                .onSuccess { path ->
                    _uiState.update {
                        it.copy(
                            isUploadingImage = false,
                            uploadedImagePath = path
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isUploadingImage = false,
                            errorMessage = sanitizeErrorMessage(error, "Image upload failed. Please try again.")
                        )
                    }
                }
        }
    }

    fun clearUploadedImage() {
        _uiState.update { it.copy(uploadedImagePath = null, isUploadingImage = false) }
    }

    companion object {
        fun provideFactory(
            repository: GameStoreRepository,
            settingsRepository: SettingsRepository
        ): Factory =
            object : Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameStoreViewModel(repository, settingsRepository) as T
                }
            }
    }
}
