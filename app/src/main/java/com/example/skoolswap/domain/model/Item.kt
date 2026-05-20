package com.example.skoolswap.domain.model

data class Item(
    val id: String,
    val shopId: Long,
    val name: String,
    val description: String,
    val price: Double,
    val quantity: Int,
    val status: String,
    val itemTypeId: Int? = null,
    val brandId: Int? = null,
    val sizeId: Int? = null,
    val colorId: Int? = null,
    val mainCategoryId: Int? = null,
    val subCategoryId: Int? = null,
    val schoolId: Int? = null,
    val schoolName: String? = null,
    val itemConditionId: Int? = null,
    val locationId: Int? = null,
    val provinceId: Int? = null,
    val genderId: Int? = null,
    val meta: ItemMeta? = null,
    val label: String? = null,
    val reserved: Int = 0,
    val updatedAt: String? = null,
    val gender: String? = null,
    val createdAt: String,
    val shop: Shop? = null,
    val images: List<ItemImage> = emptyList(), // For backward compatibility
    val coverImage: String? = null,
    val sizeName: String? = null,
    val colorName: String? = null,
    val brandName: String? = null,
    val conditionName: String? = null
) {
    val availableQuantity: Int
        get() = quantity - reserved
}

data class ItemMeta(
    val color: String?,
    val size: String?
)

data class ItemImage(
    val id: Long,
    val url: String,
    val filename: String? = null,
    val contentType: String? = null,
    val createdAt: String? = null,
    val isCover: Boolean = false  // ← Add this to identify cover image
)