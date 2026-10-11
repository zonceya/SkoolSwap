package za.co.skoolswap.data.remote.models.response.item

import com.google.gson.annotations.SerializedName

/**
 * Matches the JSON shape rendered by Api::V1::ItemsController#index:
 *
 *   { success:, items: [...], total_count:, filters_applied: { ... } }
 *
 * `filters_applied` is intentionally omitted — its values are mixed types
 * (params come in as strings, .compact drops nils), and nothing in the app
 * consumes it. Gson would otherwise choke on type inference.
 */
data class ItemsIndexResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("items") val items: List<ItemsIndexItemDto> = emptyList(),
    @SerializedName("total_count") val totalCount: Int
)

data class ItemsIndexItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("quantity") val quantity: Int = 0,
    @SerializedName("available_quantity") val availableQuantity: Int = 0,
    @SerializedName("status") val status: String = "active",

    @SerializedName("main_category") val mainCategory: String? = null,
    @SerializedName("main_category_id") val mainCategoryId: Int? = null,
    @SerializedName("sub_category") val subCategory: String? = null,
    @SerializedName("sub_category_id") val subCategoryId: Int? = null,

    @SerializedName("gender") val gender: String? = null,
    @SerializedName("gender_id") val genderId: Int? = null,
    @SerializedName("school") val school: String? = null,
    @SerializedName("school_id") val schoolId: Int? = null,

    @SerializedName("size") val size: String? = null,
    @SerializedName("size_id") val sizeId: Int? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("color_id") val colorId: Int? = null,

    @SerializedName("image") val image: String? = null,
    @SerializedName("images") val images: List<String>? = emptyList(),

    @SerializedName("brand") val brand: String? = null,
    @SerializedName("brand_id") val brandId: Int? = null,
    @SerializedName("condition") val condition: String? = null,
    @SerializedName("condition_id") val conditionId: Int? = null,

    @SerializedName("province") val province: String? = null,
    @SerializedName("province_id") val provinceId: Int? = null,
    @SerializedName("town") val town: String? = null,
    @SerializedName("town_id") val townId: Int? = null,

    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("updated_at") val updatedAt: String? = null,

    @SerializedName("shop") val shop: ItemsIndexShopDto? = null,
    @SerializedName("tags") val tags: List<String>? = emptyList()
)

data class ItemsIndexShopDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("seller_name") val sellerName: String? = null,
    @SerializedName("seller_mobile") val sellerMobile: String? = null
)