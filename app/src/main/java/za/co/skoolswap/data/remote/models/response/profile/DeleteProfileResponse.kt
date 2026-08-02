package za.co.skoolswap.data.remote.models.response.profile

import com.google.gson.annotations.SerializedName

data class DeleteProfileResponse(
    @SerializedName("message") val message: String,
    @SerializedName("error") val error: String? = null
)