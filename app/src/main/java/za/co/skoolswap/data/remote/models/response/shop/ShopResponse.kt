package za.co.skoolswap.data.remote.models.response.shop

import com.google.gson.annotations.SerializedName
import android.os.Build
import androidx.annotation.RequiresApi
import za.co.skoolswap.domain.model.Shop

// Your existing ShopDto
data class ShopDto(
    val id: Long,
    val name: String,
    val display_name: String = "",
    val user_id: Long,
    val seller_name: String,
    val seller_mobile: String?,
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
            sellerMobile = seller_mobile,
            profilePictureUrl = profilePictureUrl,
            createdAt = created_at,
            itemsCount = items_count
        )
    }
}

// ✅ ADD MISSING UpdateShopResponse
data class UpdateShopResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("shop")
    val shop: UpdateShopDto? = null,

    @SerializedName("error")
    val error: String? = null,

    @SerializedName("errors")
    val errors: List<String>? = null
)

// ✅ ADD MISSING UpdateShopDto
data class UpdateShopDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("display_name")
    val displayName: String = ""
)

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

data class ShopResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("shop")
    val shop: ShopDto?,

    @SerializedName("error")
    val error: String? = null
)


data class PublicShopResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("shop")
    val shop: PublicShopDto? = null,

    @SerializedName("error")
    val error: String? = null
)


data class PublicShopDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("seller")
    val seller: SellerDto,

    @SerializedName("stats")
    val stats: ShopStatsDto,

    @SerializedName("created_at")
    val createdAt: String
)


data class SellerDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String
)