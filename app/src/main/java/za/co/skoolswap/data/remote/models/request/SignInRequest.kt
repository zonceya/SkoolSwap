package za.co.skoolswap.data.remote.models.request

import com.google.gson.annotations.SerializedName

data class SignInRequest(
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String,
    @SerializedName("profile_picture_url") val profilePictureUrl: String,
    @SerializedName("auth_mode") val authMode: String = "google"
)