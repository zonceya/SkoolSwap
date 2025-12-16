package com.example.skoolswap.data.remote.models.response.user

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("mobile") val mobile: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("profile_picture_url") val profilePictureUrl: String,
    @SerializedName("auth_mode") val authMode: String,
    @SerializedName("role") val role: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)