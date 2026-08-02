// MyShopItemsResponse.kt
package za.co.skoolswap.data.remote.models.response.shop

import com.google.gson.annotations.SerializedName

data class ShopItemsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("shop")
    val shop: ShopSummaryDto?,

    @SerializedName("items")
    val items: List<ShopItemDto>?,  // Different from ItemDto!

    @SerializedName("stats")
    val stats: ShopStatsDto?
)

data class ShopItemDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("price")
    val price: Double,

    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("view_count")
    val viewCount: Int = 0,
    @SerializedName("available_quantity")
    val availableQuantity: Int,

    @SerializedName("status")
    val status: String,

    @SerializedName("main_category")
    val mainCategory: String?,

    @SerializedName("main_category_id")
    val mainCategoryId: Int,

    @SerializedName("sub_category")
    val subCategory: String?,

    @SerializedName("sub_category_id")
    val subCategoryId: Int,

    @SerializedName("gender")
    val gender: String?,

    @SerializedName("school")
    val school: String?,

    @SerializedName("size")
    val size: String?,

    @SerializedName("color")
    val color: String?,

    @SerializedName("brand")
    val brand: String?,

    @SerializedName("condition")
    val condition: String?,

    @SerializedName("province")
    val province: String?,

    @SerializedName("town")
    val town: String?,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String,

    @SerializedName("images")
    val images: List<String> = emptyList(),

    @SerializedName("tags")
    val tags: List<String>

)

data class ShopItemImageDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("url")
    val url: String,

    @SerializedName("filename")
    val filename: String?,

    @SerializedName("content_type")
    val contentType: String?,

    @SerializedName("created_at")
    val createdAt: String?
)

data class ShopSummaryDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("display_name")
    val displayName: String?
)