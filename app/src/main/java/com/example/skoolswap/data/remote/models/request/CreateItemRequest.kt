package com.example.skoolswap.data.remote.models.request

import com.google.gson.annotations.SerializedName

data class CreateItemRequest(
    @SerializedName("item")
    val item: ItemData
)

data class ItemData(
    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String,

    // ✅ CHANGED: from item_type_id to main_category_id
    @SerializedName("main_category_id")
    val mainCategoryId: Int,

    // ✅ NEW: sub_category_id
    @SerializedName("sub_category_id")
    val subCategoryId: Int,

    @SerializedName("brand_id")
    val brandId: Int? = null,

    @SerializedName("price")
    val price: Double,

    @SerializedName("quantity")
    val quantity: Int,

    @SerializedName("item_condition_id")
    val itemConditionId: Int? = null,

    @SerializedName("province_id")
    val provinceId: Int? = null,

    @SerializedName("location_id")
    val locationId: Int? = null,

    @SerializedName("gender_id")
    val genderId: Int? = null,

    @SerializedName("school_id")
    val schoolId: Int? = null,

    @SerializedName("size_id")
    val sizeId: Int? = null,

    // ✅ CHANGED: color is now color_id
    @SerializedName("color_id")
    val colorId: Int? = null,

    @SerializedName("label")
    val label: String? = "popular",

    @SerializedName("status")
    val status: String = "active",

    @SerializedName("tag_ids")
    val tagIds: List<Int>? = emptyList()
)