// ItemTypesResponse.kt - CORRECTED VERSION
package com.example.skoolswap.data.remote.models.response.item

import com.example.skoolswap.data.remote.models.response.ApiResponse
import com.google.gson.annotations.SerializedName

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

// Option 1: If your API returns {success, message, item_types}
data class ItemTypesResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("item_types")
    val itemTypes: List<ItemTypeDto>?
)

