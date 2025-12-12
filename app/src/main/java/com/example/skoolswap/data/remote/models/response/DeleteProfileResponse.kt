package com.example.skoolswap.data.remote.models.response

import com.google.gson.annotations.SerializedName

data class DeleteProfileResponse(
    @SerializedName("message") val message: String,
    @SerializedName("error") val error: String? = null
)