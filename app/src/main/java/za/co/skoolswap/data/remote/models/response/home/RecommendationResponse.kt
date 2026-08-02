package za.co.skoolswap.data.remote.models.response.home

import com.google.gson.annotations.SerializedName

data class HomeRecommendationResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("school_id") val schoolId: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("sections") val sections: List<HomeSectionDto>
)

data class HomeSectionDto(
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String,
    @SerializedName("items") val items: List<RecommendationItemDto>?,
    @SerializedName("sections") val sections: EssentialsSectionsDto? // For essentials type
)

data class EssentialsSectionsDto(
    @SerializedName("uniforms") val uniforms: List<RankedItemDto> = emptyList(),
    @SerializedName("sports") val sports: List<RankedItemDto> = emptyList(),
    @SerializedName("accessories") val accessories: List<RankedItemDto> = emptyList(),
    @SerializedName("stationery") val stationery: List<RankedItemDto>? = null
)

// ============ UNIFORM RESPONSE ============
data class UniformRecommendationResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("school_id") val schoolId: Int,
    @SerializedName("gender") val gender: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("sections") val sections: List<UniformSectionDto>
)

data class UniformSectionDto(
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String, // summer, winter, pe_kit, accessories
    @SerializedName("items") val items: List<RecommendationItemDto>
)

// ============ SPORT RESPONSE ============
data class SportRecommendationResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("school_id") val schoolId: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("sections") val sections: List<SportSectionDto>
)

data class SportSectionDto(
    @SerializedName("title") val title: String, // Rugby, Cricket, Hockey, etc.
    @SerializedName("type") val type: String, // rugby, cricket, hockey, netball, soccer
    @SerializedName("items") val items: List<RecommendationItemDto>
)

// ============ RECENT RESPONSE ============
data class RecentRecommendationResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("school_id") val schoolId: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("period") val period: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("sections") val sections: List<RecentSectionDto>?,
    @SerializedName("items") val items: List<RecommendationItemDto>? // For single period response
)

data class RecentSectionDto(
    @SerializedName("title") val title: String, // Today, Yesterday, Earlier This Week
    @SerializedName("period") val period: String, // today, yesterday, week
    @SerializedName("items") val items: List<RecommendationItemDto>
)

// ============ COMMON ITEM DTO ============
data class RecommendationItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("price") val price: Double,
    @SerializedName("image") val image: String?,
    @SerializedName("cover_photo") val coverPhoto: String?,
    @SerializedName("school_id") val schoolId: Int,
    @SerializedName("category") val category: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("size_name") val sizeName: String?,
    @SerializedName("color_name") val colorName: String?,
    @SerializedName("condition_name") val conditionName: String?,
    @SerializedName("brand_name") val brandName: String?,
    @SerializedName("available_quantity") val availableQuantity: Int,
    @SerializedName("school") val school: String?,
    @SerializedName("images") val images: List<String>?,
    @SerializedName("view_count")
    val viewCount: Int = 0,
    @SerializedName("shop") val shop: ShopInfoDto?
)

data class ShopInfoDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("seller_name") val sellerName: String?,
    @SerializedName("seller_mobile") val sellerMobile: String?
)