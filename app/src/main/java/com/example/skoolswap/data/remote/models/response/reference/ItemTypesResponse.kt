package com.example.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class ItemTypesResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("item_types")
    val itemTypes: List<ItemTypeDto>?
)

data class ItemTypeDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("group_id")
    val groupId: Int,

    @SerializedName("description")
    val description: String?
)