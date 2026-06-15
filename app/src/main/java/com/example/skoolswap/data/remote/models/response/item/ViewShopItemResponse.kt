package com.example.skoolswap.data.remote.models.response.item


import com.google.gson.annotations.SerializedName

data class ViewShopItemResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("item")
    val item: ViewShopItemDto?,
    @SerializedName("message")  // Add this optional field
    val message: String?
)

data class ViewShopItemDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("shop_id")
    val shopId: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String,

    @SerializedName("school_id")
    val schoolId: Int?,

    @SerializedName("brand_id")
    val brandId: Int?,

    @SerializedName("label")
    val label: String?,

    @SerializedName("price")
    val price: String?,

    @SerializedName("location_id")
    val locationId: Int?,

    @SerializedName("province_id")
    val provinceId: Int?,


    @SerializedName("gender_id")
    val genderId: Int?,

    @SerializedName("main_category_id")
    val mainCategoryId: Int?,

    @SerializedName("sub_category_id")
    val subCategoryId: Int?,
    @SerializedName("size")
    val size: SizeDto?,  // ← ADD THIS

    @SerializedName("color")
    val color: ColorDto?,  // ← ADD THIS
    @SerializedName("total_quantity")
    val totalQuantity: Int,

    @SerializedName("total_reserved")
    val totalReserved: Int,

    @SerializedName("item_condition_id")
    val itemConditionId: Int?,

    @SerializedName("shop")
    val shop: ViewShopDto?,

    @SerializedName("main_category")
    val mainCategory: ViewCategoryDto?,

    @SerializedName("sub_category")
    val subCategory: ViewCategoryDto?,

    @SerializedName("gender")
    val gender: ViewGenderDto?,

    @SerializedName("school")
    val school: ViewSchoolDto?,

    @SerializedName("province")
    val province: ViewProvinceDto?,

    @SerializedName("brand")
    val brand: ViewBrandDto?,
    @SerializedName("town")
    val town: ViewTownDto?,
    @SerializedName("tags")
    val tags: List<ViewTagDto>?,


    @SerializedName("images")
    val images: List<String>?,  // ← Change from List<ItemImageDto> to List<String>

    // ✅ Add variants to get price/quantity
    @SerializedName("variants")
    val variants: List<ViewVariantDto>?,// Reuse your existing ItemImageDto
)
data class ViewVariantDto(
    @SerializedName("id") val id: String,
    @SerializedName("price") val price: Double,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("size_id") val sizeId: Int?,
    @SerializedName("size_name") val sizeName: String?,
    @SerializedName("color_id") val colorId: Int?,
    @SerializedName("color_name") val colorName: String?,
    @SerializedName("condition_id") val conditionId: Int?,
    @SerializedName("condition_name") val conditionName: String?
)
data class ViewShopDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,
    @SerializedName("seller_name")
    val sellerName: String?,

    @SerializedName("seller_mobile")
    val sellerMobile: String?
)
data class ViewSizeDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class ViewColorDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class ViewConditionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)
data class ViewCategoryDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)

data class ViewGenderDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)

data class ViewSchoolDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)

data class ViewProvinceDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)

data class ViewBrandDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)
data class ViewTownDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)
data class ViewTagDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)