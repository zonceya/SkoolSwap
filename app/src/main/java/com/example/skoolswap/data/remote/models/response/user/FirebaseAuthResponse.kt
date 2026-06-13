package com.example.skoolswap.data.remote.models.response.user

data class FirebaseAuthResponse(
    val success: Boolean,
    val message: String,
    val is_new_user: Boolean,
    val user: UserResponse,
    val token: String,
    val timestamp: String
)