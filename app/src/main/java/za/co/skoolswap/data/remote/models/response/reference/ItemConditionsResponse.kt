package za.co.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class ItemConditionsResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("item_conditions")
    val itemConditions: List<ItemConditionDto>?,

    @SerializedName("error")
    val error: String?
)

data class ItemConditionDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String?
)