package com.example.skoolswap.data.remote.models.response.help

data class HelpResponse(
    val success: Boolean,
    val message: String
)

data class HelpData(
    val user: String,
    val from: String,
    val subject: String,
    val timestamp: String
)