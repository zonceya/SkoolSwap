package com.example.skoolswap.domain.model

data class Shop(
    val id: Long,
    val name: String,
    val displayName: String = "",
    val userId: Long,
    val sellerMobile: String?,
    val sellerName: String,
    val profilePictureUrl: String,
    val createdAt: String, // Changed from LocalDateTime to String
    val itemsCount: Int = 0
)