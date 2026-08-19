package za.co.skoolswap.data.remote.models.response.home

import com.google.gson.annotations.SerializedName

// ================================================================
// 🔥 NEW RANKED RESPONSE MODELS
// ================================================================

data class RankedItemsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("school_id") val schoolId: Int,
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("items") val items: List<RankedItemDto>,
    @SerializedName("pagination") val pagination: RankedPaginationDto? = null
)

data class RankedItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: Double,
    @SerializedName("description") val description: String?,
    @SerializedName("main_category") val mainCategory: String?,
    @SerializedName("main_category_id") val mainCategoryId: Int?,
    @SerializedName("sub_category") val subCategory: String?,
    @SerializedName("sub_category_id") val subCategoryId: Int?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("gender_id") val genderId: Int?,
    @SerializedName("condition") val condition: String?,
    @SerializedName("brand") val brand: String?,
    @SerializedName("school_name") val schoolName: String?,
    @SerializedName("school_logo_url") val schoolLogoUrl: String?,
    @SerializedName("school_id") val schoolId: Int,
    @SerializedName("relevance") val relevance: String,
    @SerializedName("images") val images: List<String>?,
    @SerializedName("cover_photo") val coverPhoto: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("view_count") val viewCount: Int = 0
)

data class RankedPaginationDto(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("total_pages") val totalPages: Int
)