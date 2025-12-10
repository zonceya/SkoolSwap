package com.example.skoolswap.data.remote.models.response

import com.example.skoolswap.domain.model.User
import com.google.gson.annotations.SerializedName

data class SignInResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("user") val user: UserResponse,
    @SerializedName("token") val token: String,
    @SerializedName("timestamp") val timestamp: String
)