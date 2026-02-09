package com.example.skoolswap.data.remote.models.request

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody

data class CreateItemRequest(
    @SerializedName("item")
    val item: ItemData
)

data class ItemData(
    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("item_type_id")
    val itemTypeId: Int,

    @SerializedName("brand_id")
    val brandId: Int,

    @SerializedName("price")
    val price: Double,

    @SerializedName("quantity")
    val quantity: Int,

    @SerializedName("item_condition_id")
    val itemConditionId: Int,

    @SerializedName("province_id")
    val provinceId: Int,

    @SerializedName("location_id")
    val locationId: Int,

    @SerializedName("gender_id")
    val genderId: Int,

    @SerializedName("school_id")
    val schoolId: Int,

    @SerializedName("size_id")
    val sizeId: Int,

    @SerializedName("label")
    val label: String? = "popular",

    @SerializedName("status")
    val status: String = "active",

    @SerializedName("meta")
    val meta: ItemMeta? = null,

    @SerializedName("tag_ids")
    val tagIds: List<Int>? = emptyList()
)

data class ItemMeta(
    @SerializedName("color")
    val color: String? = null,

    @SerializedName("size")
    val size: String? = null
)

// Separate request for image upload
data class ItemImageUploadRequest(
    val images: List<MultipartBody.Part> = emptyList()
)