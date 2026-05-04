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

    @SerializedName("tags")
    val tags: List<ViewTagDto>?,

    // ⚠️ IMPORTANT: Add this field for images!
    @SerializedName("images")
    val images: List<ItemImageDto>?  // Reuse your existing ItemImageDto
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

data class ViewTagDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)