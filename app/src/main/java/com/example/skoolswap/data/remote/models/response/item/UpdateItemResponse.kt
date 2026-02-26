package com.example.skoolswap.data.remote.models.response.item

import com.google.gson.annotations.SerializedName

data class UpdateItemResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("item")
    val item: UpdateItemDto?,

    @SerializedName("error")
    val error: String?
)

data class UpdateItemDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("price")
    val price: String,

    @SerializedName("quantity")
    val quantity: Int,

    @SerializedName("available_quantity")
    val availableQuantity: Int,

    @SerializedName("status")
    val status: String,

    @SerializedName("main_category_id")
    val mainCategoryId: Int,

    @SerializedName("main_category_name")
    val mainCategoryName: String?,

    @SerializedName("sub_category_id")
    val subCategoryId: Int,

    @SerializedName("sub_category_name")
    val subCategoryName: String?,

    @SerializedName("gender_id")
    val genderId: Int?,

    @SerializedName("gender_name")
    val genderName: String?,

    @SerializedName("school_id")
    val schoolId: Int?,

    @SerializedName("school_name")
    val schoolName: String?,

    @SerializedName("size_id")
    val sizeId: Int?,

    @SerializedName("size_name")
    val sizeName: String?,

    @SerializedName("color_id")
    val colorId: Int?,

    @SerializedName("color_name")
    val colorName: String?,

    @SerializedName("brand_id")
    val brandId: Int?,

    @SerializedName("brand_name")
    val brandName: String?,

    @SerializedName("condition_id")
    val conditionId: Int?,

    @SerializedName("condition_name")
    val conditionName: String?,

    @SerializedName("province_id")
    val provinceId: Int?,

    @SerializedName("province_name")
    val provinceName: String?,

    @SerializedName("town_id")
    val townId: Int?,

    @SerializedName("town_name")
    val townName: String?,

    @SerializedName("images")
    val images: List<ItemImageDto>?,

    @SerializedName("tags")
    val tags: List<TagDto>?,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String
)

data class TagDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)