package com.example.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class MainCategoriesResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("categories")
    val categories: List<MainCategoryDto>?,

    @SerializedName("error")
    val error: String?
)

data class MainCategoryDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("icon_name")
    val iconName: String?,

    @SerializedName("display_order")
    val displayOrder: Int,

    @SerializedName("is_active")
    val isActive: Boolean
)