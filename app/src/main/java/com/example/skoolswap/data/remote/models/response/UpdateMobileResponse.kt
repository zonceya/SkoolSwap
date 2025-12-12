package com.example.skoolswap.data.remote.models.response

import com.google.gson.annotations.SerializedName

data class UpdateMobileResponse(
    @SerializedName("message") val message: String,
    @SerializedName("user") val user: UserResponse? = null,
    @SerializedName("profile") val profile: ProfileResponse? = null,
    @SerializedName("cache_updated") val cacheUpdated: Boolean = false
)

data class ProfileResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("mobile") val mobile: String?
)