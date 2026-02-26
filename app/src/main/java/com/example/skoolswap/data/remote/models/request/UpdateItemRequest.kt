package com.example.skoolswap.data.remote.models.request

import com.google.gson.annotations.SerializedName

data class UpdateItemRequest(
    @SerializedName("item")
    val item: UpdateItemData
)

data class UpdateItemData(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double? = null,
    @SerializedName("quantity") val quantity: Int? = null,
    @SerializedName("main_category_id") val mainCategoryId: Int? = null,
    @SerializedName("sub_category_id") val subCategoryId: Int? = null,
    @SerializedName("brand_id") val brandId: Int? = null,
    @SerializedName("size_id") val sizeId: Int? = null,
    @SerializedName("color_id") val colorId: Int? = null,
    @SerializedName("item_condition_id") val itemConditionId: Int? = null,
    @SerializedName("province_id") val provinceId: Int? = null,
    @SerializedName("location_id") val locationId: Int? = null,
    @SerializedName("gender_id") val genderId: Int? = null,
    @SerializedName("school_id") val schoolId: Int? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("tag_ids") val tagIds: List<Int>? = null
)