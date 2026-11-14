package com.anasdev.gamestoreandroid.domain.model


import kotlinx.serialization.Serializable

@Serializable
data class LockKeyRequest(
    val gameId: String,
)

@Serializable
data class LockedCartItemResponse(
    val keyId: String,
    val gameId: String,
    val gameTitle: String,
    val price: Double,
    val lockedUntil: String,
)

@Serializable
data class OrderItemReceiptDto(
    val gameId: String,
    val gameTitle: String,
    val priceAtPurchase: Double,
    val keyCode: String,
)

@Serializable
data class OrderReceiptResponse(
    val orderId: String,
    val totalAmount: Double,
    val createdAt: String,
    val items: List<OrderItemReceiptDto>
)