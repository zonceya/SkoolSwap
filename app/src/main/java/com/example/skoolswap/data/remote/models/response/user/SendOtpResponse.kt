// data/remote/models/response/OtpResponse.kt
package com.example.skoolswap.data.remote.models.response.user

import com.google.gson.annotations.SerializedName

data class SendOtpResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: OtpData?
)

data class OtpData(
    @SerializedName("otp_token") val authToken: String,
    @SerializedName("otp") val otp: String?
)