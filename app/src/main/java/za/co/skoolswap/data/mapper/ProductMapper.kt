package za.co.skoolswap.data.mapper

import za.co.skoolswap.data.remote.models.response.home.ProductItemDto
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.model.ItemImage

// ItemMapper.kt - update toDomain() extension
fun ProductItemDto.toDomain(): Item {
    return Item(
        id = id,
        shopId = 0L, // or appropriate default
        name = name,
        description = description ?: "",
        price = price,
        quantity = availableQuantity ?: 0,
        status = status ?: "ACTIVE",
        mainCategoryId = mainCategoryId,
        subCategoryId = subCategoryId,
        schoolId = schoolId,
        schoolName = schoolName,
        genderId = genderId,
        gender = gender,
        sizeName = sizeName,
        colorName = colorName,
        brandName = brandName,
        conditionName = conditionName,
        viewCount = viewCount,
        relevance = relevance,
        createdAt = createdAt,
        // ✅ Map cover_photo to coverImage
        coverImage = coverPhoto?.takeIf { it.isNotBlank() } ?: image,
        image = image,
        // Convert string list to ItemImage objects
        images = images?.map { imageUrl ->
            ItemImage(
                id = 0,
                url = imageUrl,
                filename = null,
                contentType = null,
                createdAt = null,
                isCover = false
            )
        } ?: emptyList()
    )
}