package com.example.skoolswap.data.mapper

import com.example.skoolswap.data.remote.models.response.home.ProductItemDto
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.ItemImage

fun ProductItemDto.toDomain(): Item {
    return Item(
        id = id,
        shopId = 0L, // Not provided in products response
        name = name,
        description = description ?: "",
        price = price,
        quantity = 1, // Default
        status = "active",
        createdAt = createdAt,
        schoolId = schoolId,
        images = if (!image.isNullOrEmpty()) {
            listOf(
                ItemImage(
                    id = 0L,
                    url = image,
                    filename = null,
                    contentType = null,
                    createdAt = null
                )
            )
        } else emptyList(),
        brandId = null,
        sizeId = null,
        itemConditionId = null,
        locationId = null,
        provinceId = null,
        genderId = null,
        mainCategoryId = null,
        subCategoryId = null,
        label = null,
        reserved = 0,
        meta = null,
        shop = null,
        itemTypeId = null
    )
}