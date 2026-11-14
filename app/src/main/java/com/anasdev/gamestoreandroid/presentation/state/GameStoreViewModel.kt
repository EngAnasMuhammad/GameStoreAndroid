package com.anasdev.gamestoreandroid.presentation.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasdev.gamestoreandroid.data.local.TokenManager
import com.anasdev.gamestoreandroid.data.remote.GameStoreApi
import com.anasdev.gamestoreandroid.domain.model.AuthRequest
import com.anasdev.gamestoreandroid.domain.model.GameResponse
import com.anasdev.gamestoreandroid.domain.model.LockedCartItemResponse
import com.anasdev.gamestoreandroid.domain.model.OrderReceiptResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider.Factory

data class GameStoreUiState(
    val isLoading: Boolean = false,
    val games: List<GameResponse> = emptyList(),
    val cartItems: List<LockedCartItemResponse> = emptyList(),
    val orders: List<OrderReceiptResponse> = emptyList(),
    val latestReceipt: OrderReceiptResponse? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isUploadingImage: Boolean = false,
    val uploadedImagePath: String? = null
)
class GameStoreViewModel(
    private val api: GameStoreApi,
    private val tokenManager: TokenManager
): ViewModel() {
    private val _uiState = MutableStateFlow(GameStoreUiState())
    val uiState = _uiState.asStateFlow()

    val tokenFlow = tokenManager.tokenFlow
    val roleFlow = tokenManager.roleFlow

    init {
        loadGames()
        viewModelScope.launch {
            if (tokenManager.getToken() != null) {
                loadCart()
                loadOrders()
            }
        }
    }

    fun loadGames(){
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            api.getAllGames()
                .onSuccess { gameList ->
                    _uiState.update { it.copy(isLoading = false, games = gameList) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    fun login(email: String, password: String){
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            api.login(AuthRequest(email,password))
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    loadCart()
                    loadOrders()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            api.register(AuthRequest(email, password))
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    loadCart()
                    loadOrders()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            api.loginWithGoogle(idToken)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    loadCart()
                    loadOrders()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    fun logout(){
        viewModelScope.launch {
            tokenManager.clearAuthData()

            _uiState.update { it.copy(cartItems = emptyList(), orders = emptyList(), latestReceipt = null) }
        }
    }

    fun clearError(){
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadCart(){
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            api.getCartItems()
                .onSuccess { cartItems ->
                    _uiState.update { it.copy(isLoading = false,cartItems = cartItems) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false,errorMessage = error.message) }
                }
        }
    }

    fun addToCart(gameId: String){
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            api.lockKeyForCart(gameId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    loadCart()
                    loadGames()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false,errorMessage = error.message) }
                }
        }
    }

    fun checkoutCart(){
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            api.checkout()
                .onSuccess { receipt ->
                    _uiState.update { it.copy(isLoading = false,latestReceipt = receipt, cartItems = emptyList()) }
                    loadOrders()
                    loadGames()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false,errorMessage = error.message) }
                }
        }
    }
    fun loadOrders(){
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            api.getOrderHistory()
                .onSuccess { ordersList ->
                    _uiState.update { it.copy(isLoading = false,orders = ordersList) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false,errorMessage = error.message) }
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
        request: com.anasdev.gamestoreandroid.domain.model.CreateGameRequest,
        imageBytes: ByteArray? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            var finalImagePath = request.imagePath
            if (imageBytes != null && imageBytes.isNotEmpty()) {
                val uploadResult = api.uploadGameImage(imageBytes, "cover.jpg", "image/jpeg")
                if (uploadResult.isFailure) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Image upload failed: ${uploadResult.exceptionOrNull()?.message}"
                        )
                    }
                    return@launch
                }
                finalImagePath = uploadResult.getOrNull() ?: ""
            }

            val finalRequest = request.copy(imagePath = finalImagePath)
            api.createGame(finalRequest)
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
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    fun addGameKeys(gameId: String, keys: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            api.addGameKeys(gameId, keys)
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
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    fun uploadGameImage(bytes: ByteArray, fileName: String, mimeType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true, errorMessage = null) }
            api.uploadGameImage(bytes, fileName, mimeType)
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
                            errorMessage = error.message
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
            api: GameStoreApi,
            tokenManager: TokenManager
        ): Factory =
            object : Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameStoreViewModel(api, tokenManager) as T
                }
            }
    }
}
