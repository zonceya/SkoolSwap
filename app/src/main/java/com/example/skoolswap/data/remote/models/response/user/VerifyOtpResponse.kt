// data/remote/models/response/VerifyOtpResponse.kt
package com.example.skoolswap.data.remote.models.response.user

import com.google.gson.annotations.SerializedName

data class VerifyOtpResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: VerifyData?
)

data class VerifyData(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserResponse?
)