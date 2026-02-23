package com.example.skoolswap.data.remote.models.response.shop

import com.google.gson.annotations.SerializedName  // Add this import
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.skoolswap.domain.model.Shop

data class ShopResponse(
    val success: Boolean,
    val shop: ShopDto?,
    val error: String? = null
)

data class ShopDto(
    val id: Long,
    val name: String,
    val display_name: String = "",
    val user_id: Long,
    val seller_name: String,
    val created_at: String,
    val items_count: Int = 0
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun toDomain(profilePictureUrl: String): Shop {
        return Shop(
            id = id,
            name = name,
            displayName = display_name,
            userId = user_id,
            sellerName = seller_name,
            profilePictureUrl = profilePictureUrl,
            createdAt = created_at,
            itemsCount = items_count
        )
    }
}

data class UpdateShopResponse(
    val success: Boolean,
    val message: String? = null,
    val shop: UpdateShopDto? = null,
    val error: String? = null,
    val errors: List<String>? = null
)

data class UpdateShopDto(
    val id: Long,
    val name: String,
    val display_name: String = ""
)

data class PublicShopResponse(
    val success: Boolean,
    val shop: PublicShopDto? = null,
    val error: String? = null
)

data class PublicShopDto(
    val id: Long,
    val name: String,
    val seller: SellerDto,
    val stats: ShopStatsDto,
    val created_at: String
)

data class SellerDto(
    val id: Long,
    val name: String
)

// ✅ KEEP THIS VERSION (with all 4 fields and @SerializedName)
data class ShopStatsDto(
    @SerializedName("total_items")
    val totalItems: Int,
    @SerializedName("active_items")
    val activeItems: Int,
    @SerializedName("sold_items")
    val soldItems: Int,
    @SerializedName("inactive_items")
    val inactiveItems: Int
)