// data/remote/models/request/VerifyLoginRequest.kt
package za.co.skoolswap.data.remote.models.request

import com.google.gson.annotations.SerializedName

data class VerifyLoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp_token") val otpToken: String,
    @SerializedName("otp_code") val otpCode: String
)