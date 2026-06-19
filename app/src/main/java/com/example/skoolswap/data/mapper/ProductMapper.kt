package com.example.skoolswap.data.mapper

import com.example.skoolswap.data.remote.models.response.home.ProductItemDto
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.ItemImage

fun ProductItemDto.toDomain(): Item {
    val allImages = mutableListOf<ItemImage>()

    val coverUrl = image
    coverUrl?.let { url ->
        allImages.add(ItemImage(id = 0L, url = url, filename = null, contentType = null, createdAt = null, isCover = true))
    }
    images?.forEach { url ->
        if (url != coverUrl && url.isNotBlank()) {
            allImages.add(ItemImage(id = 0L, url = url, filename = null, contentType = null, createdAt = null, isCover = false))
        }
    }

    return Item(
        id = id,
        shopId = 0L,
        name = name,
        description = description ?: "",
        price = price,
        quantity = availableQuantity ?: 1,
        status = status ?: "active",
        createdAt = createdAt,
        schoolId = schoolId,
        images = allImages,
        coverImage = coverUrl,
        brandId = null,
        sizeId = null,
        itemConditionId = null,
        locationId = null,
        provinceId = null,
        genderId = genderId,
        mainCategoryId = mainCategoryId,
        subCategoryId = subCategoryId,
        label = null,
        reserved = 0,
        meta = null,
        shop = null,
        itemTypeId = null,
        sizeName = sizeName,
        colorName = colorName,
        conditionName = conditionName,
        brandName = brandName,
        gender = gender
    )
}