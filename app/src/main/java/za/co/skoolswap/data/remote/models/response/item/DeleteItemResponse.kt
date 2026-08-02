package za.co.skoolswap.data.remote.models.response.item

import com.google.gson.annotations.SerializedName

data class DeleteItemResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("error")
    val error: String?
)

data class MarkAsSoldResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("item")
    val item: ItemDto?,

    @SerializedName("error")
    val error: String?
)