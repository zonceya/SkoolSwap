package com.example.skoolswap.data.remote.models.response.item

import com.google.gson.annotations.SerializedName

data class ImageUploadResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("url") val url: String?,
    @SerializedName("message") val message: String?
)