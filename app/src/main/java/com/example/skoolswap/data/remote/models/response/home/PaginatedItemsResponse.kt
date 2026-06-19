package com.example.skoolswap.data.remote.models.response.home

import com.google.gson.annotations.SerializedName

data class PaginatedItemsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("title") val title: String?,
    @SerializedName("items") val items: List<ProductItemDto>,
    @SerializedName("pagination") val pagination: PaginationDto
)

data class ProductItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("price") val price: Double,
    @SerializedName("image") val image: String?,
    @SerializedName("school_id") val schoolId: Int,
    @SerializedName("category") val category: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("size_name") val sizeName: String? = null,
    @SerializedName("color_name") val colorName: String? = null,
    @SerializedName("condition_name") val conditionName: String? = null,
    @SerializedName("brand_name") val brandName: String? = null,
    @SerializedName("main_category_id") val mainCategoryId: Int? = null,
    @SerializedName("sub_category_id") val subCategoryId: Int? = null,
    @SerializedName("gender_id") val genderId: Int? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("available_quantity") val availableQuantity: Int? = null,
    @SerializedName("images") val images: List<String>? = null
)

data class PaginationDto(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("per_page") val perPage: Int
)