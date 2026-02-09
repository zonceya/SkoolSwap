package com.example.skoolswap.data.mapper

import com.example.skoolswap.data.local.database.entities.ItemEntity
import com.example.skoolswap.data.remote.models.response.item.*
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.ItemImage
import com.example.skoolswap.domain.model.ItemMeta
import com.example.skoolswap.domain.model.Shop

// DTO to Domain
fun ItemDto.toDomain(): Item {
    return Item(
        id = id,
        shopId = shopId,
        name = name,
        description = description,
        price = price.toDoubleOrNull() ?: 0.0,
        quantity = quantity,
        status = status,
        meta = meta?.toDomain(),
        createdAt = createdAt,
        shop = shop?.toDomain(),
        images = emptyList() // Images are separate in response
    )
}

fun ItemImageDto.toDomain(): ItemImage {
    return ItemImage(
        id = id,
        url = url,
        filename = filename,
        contentType = contentType,
        createdAt = createdAt
    )
}

fun ItemMetaDto.toDomain(): ItemMeta {
    return ItemMeta(
        color = color,
        size = size
    )
}

fun ItemShopDto.toDomain(): Shop {
    return Shop(
        id = id,
        name = name,
        displayName = "", // Not available in item response
        userId = 0L, // Not available in item response
        sellerName = "", // Not available in item response
        profilePictureUrl = "", // Not available in item response
        createdAt = "", // Not available in item response
        itemsCount = 0 // Not available in item response
    )
}

// Response to Domain
fun CreateItemResponse.toDomain(): Item {
    return Item(
        id = item?.id ?: "",
        shopId = item?.shopId ?: 0L,
        name = item?.name ?: "",
        description = item?.description ?: "",
        price = item?.price?.toDoubleOrNull() ?: 0.0,
        quantity = item?.quantity ?: 0,
        status = item?.status ?: "active",
        meta = item?.meta?.toDomain(),
        createdAt = item?.createdAt ?: "",
        shop = item?.shop?.toDomain(),
        images = images.map { it.toDomain() }
    )
}

// Domain to Entity
fun Item.toEntity(): ItemEntity {
    return ItemEntity(
        id = id,
        shopId = shopId,
        name = name,
        description = description,
        price = price,
        quantity = quantity,
        status = status,
        itemTypeId = null, // Not stored in DTO
        brandId = null, // Not stored in DTO
        sizeId = null, // Not stored in DTO
        schoolId = null, // Not stored in DTO
        itemConditionId = null, // Not stored in DTO
        locationId = null, // Not stored in DTO
        provinceId = null, // Not stored in DTO
        genderId = null, // Not stored in DTO
        metaColor = meta?.color,
        metaSize = meta?.size,
        label = null, // Not stored in DTO
        reserved = 0, // Not stored in DTO
        createdAt = createdAt,
        imageCount = images.size
    )
}

// Entity to Domain
fun ItemEntity.toDomain(): Item {
    return Item(
        id = id,
        shopId = shopId,
        name = name,
        description = description,
        price = price,
        quantity = quantity,
        status = status,
        meta = if (metaColor != null || metaSize != null) {
            ItemMeta(color = metaColor, size = metaSize)
        } else null,
        createdAt = createdAt,
        shop = null, // Not stored in entity
        images = emptyList() // Not stored in entity
    )
}