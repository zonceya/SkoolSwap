package za.co.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class ItemColorsResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("item_colors")
    val itemColors: List<ItemColorDto>?,

    @SerializedName("error")
    val error: String?
)

data class ItemColorDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)