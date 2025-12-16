package com.example.skoolswap.data.remote.models.response.user

import com.google.gson.annotations.SerializedName

data class UpdateMobileResponse(
    @SerializedName("message") val message: String,
    @SerializedName("user") val user: UserResponse? = null,
    @SerializedName("profile") val profileData: ProfileData? = null, // Renamed to avoid conflict
    @SerializedName("cache_updated") val cacheUpdated: Boolean = false
)

// For update_mobile response only
data class ProfileData(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("mobile") val mobile: String?
)