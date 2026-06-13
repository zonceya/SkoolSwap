package com.example.skoolswap.data.remote.models.response.item

import com.example.skoolswap.data.remote.models.response.reference.BrandDto
import com.example.skoolswap.data.remote.models.response.reference.GenderDto
import com.example.skoolswap.data.remote.models.response.reference.MainCategoryDto
import com.example.skoolswap.data.remote.models.response.reference.ProvinceDto
import com.example.skoolswap.data.remote.models.response.reference.SchoolDto
import com.example.skoolswap.data.remote.models.response.reference.SubCategoryDto
import com.example.skoolswap.data.remote.models.response.reference.TownDto
import com.google.gson.annotations.SerializedName

data class ItemDetailResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("item")
    val item: ItemDetailDto?,

    @SerializedName("message")
    val message: String?
)

data class ItemDetailDto(
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

    @SerializedName("available_quantity")
    val availableQuantity: Int,
    @SerializedName("size")
    val size: SizeDto?,

    @SerializedName("color")
    val color: ColorDto?,
    @SerializedName("status")
    val status: String,

    @SerializedName("image")
    val image: String?,  // Single image URL

    @SerializedName("images")
    val images: List<String>? = emptyList(), // Array of image URLs (as strings)

    @SerializedName("cover_photo")
    val coverPhoto: String?,  // Cover photo URL

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String,

    @SerializedName("shop")
    val shop: ShopDetailDto?,

    @SerializedName("view_count")
    val viewCount: Int = 0,

    @SerializedName("main_category")
    val mainCategory: MainCategoryDto?,

    @SerializedName("sub_category")
    val subCategory: SubCategoryDto?,

    @SerializedName("gender")
    val gender: GenderDto?,

    @SerializedName("school")
    val school: SchoolDto?,

    @SerializedName("brand")
    val brand: BrandDto?,

    @SerializedName("condition")
    val condition: ConditionDto?,

    @SerializedName("province")
    val province: ProvinceDto?,

    @SerializedName("town")
    val town: TownDto?,

    @SerializedName("tags")
    val tags: List<TagDto>?
)

data class ShopDetailDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("display_name")
    val displayName: String?,

    @SerializedName("seller_name")
    val sellerName: String?,

    @SerializedName("seller_mobile")
    val sellerMobile: String?
)

