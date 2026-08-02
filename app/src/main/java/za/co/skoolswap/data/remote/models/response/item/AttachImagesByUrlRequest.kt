package za.co.skoolswap.data.remote.models.response.item

import com.google.gson.annotations.SerializedName

data class AttachImagesByUrlRequest(
    @SerializedName("image_urls") val imageUrls: List<String>
)