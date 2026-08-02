package za.co.skoolswap.data.remote.models.response.shop

import com.google.gson.annotations.SerializedName

data class PublicShopItemsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("shop")
    val shop: PublicShopSummaryDto?,

    @SerializedName("items")
    val items: List<PublicShopItemDto>?  // Maybe simpler than ShopItemDto
)

data class PublicShopSummaryDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String
)

data class PublicShopItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: Double,
    @SerializedName("images") val images: List<PublicShopItemImageDto>?
    // Add only the fields you need for public viewing
)

data class PublicShopItemImageDto(
    @SerializedName("id") val id: Long,
    @SerializedName("url") val url: String
)