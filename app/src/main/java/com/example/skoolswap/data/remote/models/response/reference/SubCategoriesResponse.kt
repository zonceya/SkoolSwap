package com.example.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class SubCategoriesResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("sub_categories")
    val subCategories: List<SubCategoryDto>?,

    @SerializedName("main_category")
    val mainCategory: MainCategoryInfo?,

    @SerializedName("error")
    val error: String?
)

data class MainCategoryInfo(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)
