package com.example.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class ItemSizesResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("item_sizes")
    val itemSizes: List<ItemSizeDto>?,

    @SerializedName("error")
    val error: String?
)

data class ItemSizeDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)