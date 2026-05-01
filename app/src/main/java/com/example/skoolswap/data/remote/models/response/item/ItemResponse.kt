package com.example.skoolswap.data.remote.models.response.item

import com.example.skoolswap.data.remote.models.response.reference.BrandDto
import com.example.skoolswap.data.remote.models.response.reference.GenderDto
import com.example.skoolswap.data.remote.models.response.reference.MainCategoryDto
import com.example.skoolswap.data.remote.models.response.reference.ProvinceDto
import com.example.skoolswap.data.remote.models.response.reference.SchoolDto
import com.example.skoolswap.data.remote.models.response.reference.SubCategoryDto
import com.example.skoolswap.data.remote.models.response.reference.TownDto
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
    @SerializedName("images")
    val imagesRaw: Any? = null,

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
    @SerializedName("images")
    val images: List<String>? = emptyList(),
    @SerializedName("image")
    val image: String?,  // ← Single image URL from home feed

    @SerializedName("cover_photo")
    val cover_photo: String?,  // ← Cover photo URL

    @SerializedName("shop")
    val shop: ItemShopDto?,

    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String?,
    // ===== ADD ALL THESE MISSING FIELDS =====
    @SerializedName("brand")
    val brand: BrandDto?,

    @SerializedName("size")
    val size: SizeDto?,

    @SerializedName("color")
    val color: ColorDto?,

    @SerializedName("school")
    val school: SchoolDto?,

    @SerializedName("condition")
    val condition: ConditionDto?,

    @SerializedName("town")
    val town: TownDto?,

    @SerializedName("province")
    val province: ProvinceDto?,

    @SerializedName("gender")
    val gender: GenderDto?,

    @SerializedName("main_category")
    val mainCategory: MainCategoryDto?,  // ← Note: main_category maps to mainCategory

    @SerializedName("sub_category")
    val subCategory: SubCategoryDto?,    // ← Note: sub_category maps to subCategory

    @SerializedName("available_quantity")
    val availableQuantity: Int?,

    @SerializedName("label")
    val label: String?
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
data class ConditionDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)
data class ColorDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)
data class MainCategoryDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)

data class SubCategoryDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)
data class SizeDto(
    @SerializedName("id")
    val id: Int,
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