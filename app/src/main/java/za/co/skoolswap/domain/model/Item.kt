package za.co.skoolswap.domain.model

data class Item(
    val id: String,
    val shopId: Long,
    val name: String,
    val description: String,
    val price: Double,
    val quantity: Int,
    val status: Any? = null,
    val itemTypeId: Int? = null,
    val brandId: Int? = null,
    val sizeId: Int? = null,
    val colorId: Int? = null,
    val mainCategoryId: Int? = null,
    val subCategoryId: Int? = null,
    val schoolId: Int? = null,
    val schoolLogoUrl: String? = null,
    val schoolName: String? = null,
    val itemConditionId: Int? = null,
    val locationId: Int? = null,
    val locationName: String? = null,
    val provinceId: Int? = null,
    val genderId: Int? = null,
    val meta: ItemMeta? = null,
    val label: String? = null,
    val reserved: Int = 0,
    val updatedAt: String? = null,
    val gender: String? = null,
    val createdAt: String,
    val shop: Shop? = null,
    val images: List<ItemImage> = emptyList(),
    val coverImage: String? = null,
    val image: String? = null,
    val additionalPhoto: String? = null,
    val sizeName: String? = null,
    val colorName: String? = null,
    val brandName: String? = null,
    val conditionName: String? = null,
    val viewCount: Int = 0,
    val syncStatus: String = "ACTIVE",
    val syncError: String? = null,
    val retryCount: Int = 0,
    val relevance: String? = null,
    val lastSyncAttempt: Long? = null
) {
    val availableQuantity: Int
        get() = quantity - reserved

    fun resolveImageUrl(): String? {
        return coverImage?.takeIf { it.isNotBlank() }
            ?: image?.takeIf { it.isNotBlank() }
            ?: additionalPhoto?.takeIf { it.isNotBlank() }
            ?: images.firstOrNull { !it.url.isNullOrBlank() }?.url
    }

    fun resolveAllImageUrls(): List<String> {
        val result = mutableListOf<String>()

        coverImage?.takeIf { it.isNotBlank() }?.let {
            if (!result.contains(it)) result.add(it)
        }

        image?.takeIf { it.isNotBlank() }?.let {
            if (!result.contains(it)) result.add(it)
        }

        additionalPhoto?.takeIf { it.isNotBlank() }?.let {
            if (!result.contains(it)) result.add(it)
        }

        images.forEach { img ->
            val url = img.url
            if (!url.isNullOrBlank() && !result.contains(url)) {
                result.add(url)
            }
        }

        return result
    }

    val isSold: Boolean
        get() = when (status) {
            is String -> status.lowercase() == "sold"
            is Int -> status == 2
            else -> false
        }

    val isActive: Boolean
        get() = when (status) {
            is String -> status.lowercase() == "active"
            is Int -> status == 1
            else -> false
        }
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
    val isCover: Boolean = false
)