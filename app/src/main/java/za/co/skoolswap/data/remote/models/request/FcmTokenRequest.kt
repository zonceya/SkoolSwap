package za.co.skoolswap.data.remote.models.request

import com.google.gson.annotations.SerializedName

data class FcmTokenRequest(
    @SerializedName("fcm_token") val fcmToken: String
)