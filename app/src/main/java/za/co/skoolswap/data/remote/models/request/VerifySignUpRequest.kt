// data/remote/models/request/VerifySignUpRequest.kt
package za.co.skoolswap.data.remote.models.request

import com.google.gson.annotations.SerializedName

data class VerifySignUpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp_token") val otpToken: String,
    @SerializedName("otp_code") val otpCode: String,
    @SerializedName("name") val name: String?
)