package com.example.skoolswap.data.remote.models.response.shop

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.skoolswap.domain.model.Shop
import java.time.LocalDateTime

data class ShopResponse(
    val success: Boolean,
    val shop: ShopDto?,
    val error: String? = null
)
// TEMPORARY FIX - Change in ShopDto.kt
data class ShopDto(
    val id: Long,
    val name: String,
    val display_name: String = "",
    val user_id: Long,
    val seller_name: String,
    val created_at: String, // TEMPORARY: Change to String
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
            createdAt = created_at, // Parse String to LocalDateTime
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
    val id: Long,  // Changed from Int to Long
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

data class ShopStatsDto(
    val total_items: Int,
    val active_items: Int
)