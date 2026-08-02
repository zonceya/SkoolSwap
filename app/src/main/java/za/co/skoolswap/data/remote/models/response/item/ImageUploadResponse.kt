package za.co.skoolswap.data.remote.models.response.item

import com.google.gson.annotations.SerializedName


data class ImageUploadResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("urls") val urls: List<String>? = null
)