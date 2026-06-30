package com.example.skoolswap.data.remote.models.response.user

data class RefreshTokenResponse(
    val success: Boolean,
    val token: String,
    val message: String? = null,
    val timestamp: String? = null
)