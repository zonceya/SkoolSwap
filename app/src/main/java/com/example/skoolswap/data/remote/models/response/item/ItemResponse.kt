package com.example.skoolswap.data.remote.models.response.item

import com.google.gson.annotations.SerializedName

data class CreateItemResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("item")
    val item: ItemDto?,

    @SerializedName("images")
    val images: List<ItemImageDto> = emptyList(),

    @SerializedName("message")
    val message: String?
)

data class ItemDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("shop_id")
    val shopId: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("price")
    val price: String,

    @SerializedName("quantity")
    val quantity: Int,

    @SerializedName("status")
    val status: String,

    @SerializedName("meta")
    val meta: ItemMetaDto?,

    @SerializedName("shop")
    val shop: ItemShopDto?,

    @SerializedName("created_at")
    val createdAt: String
)

data class ItemImageDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("url")
    val url: String,

    @SerializedName("filename")
    val filename: String?,

    @SerializedName("content_type")
    val contentType: String?,

    @SerializedName("created_at")
    val createdAt: String?
)

data class ItemMetaDto(
    @SerializedName("color")
    val color: String?,

    @SerializedName("size")
    val size: String?
)

data class ItemShopDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String
)

// Separate response for image operations
data class AddImagesResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("uploaded_count")
    val uploadedCount: Int?,

    @SerializedName("failed_count")
    val failedCount: Int?,

    @SerializedName("failed_uploads")
    val failedUploads: List<String>?,

    @SerializedName("total_images")
    val totalImages: Int?,

    @SerializedName("images")
    val images: List<ItemImageDto>?
)

data class RemoveImageResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("remaining_images")
    val remainingImages: Int?
)