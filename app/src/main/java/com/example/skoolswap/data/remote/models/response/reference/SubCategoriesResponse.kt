package com.example.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class SubCategoriesResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("sub_categories")
    val subCategories: List<SubCategoryDto>?,

    @SerializedName("main_category")
    val mainCategory: MainCategoryInfo? ,

    @SerializedName("error")
        val error: String?
)
data class MainCategoryInfo(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class SubCategoryDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("display_order")
    val displayOrder: Int

    // Remove mainCategoryId - it's not in the response
)