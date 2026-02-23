package com.example.skoolswap.data.mapper

import com.example.skoolswap.data.local.database.entities.ItemEntity
import com.example.skoolswap.data.remote.models.response.item.*
import com.example.skoolswap.data.remote.models.response.shop.PublicShopItemDto
import com.example.skoolswap.data.remote.models.response.shop.PublicShopItemImageDto
import com.example.skoolswap.data.remote.models.response.shop.ShopItemDto
import com.example.skoolswap.data.remote.models.response.shop.ShopItemImageDto
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.ItemImage
import com.example.skoolswap.domain.model.ItemMeta
import com.example.skoolswap.domain.model.Shop

// ============ ITEM DTO TO DOMAIN ============
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

// ============ ITEM IMAGE DTO TO DOMAIN ============
fun ItemImageDto.toDomain(): ItemImage {
    return ItemImage(
        id = id,
        url = url,
        filename = filename,
        contentType = contentType,
        createdAt = createdAt
    )
}

// ============ SHOP ITEM IMAGE DTO TO DOMAIN ============
fun ShopItemImageDto.toDomain(): ItemImage {
    return ItemImage(
        id = id,
        url = url,
        filename = filename,
        contentType = contentType,
        createdAt = createdAt
    )
}

// ============ PUBLIC SHOP ITEM IMAGE DTO TO DOMAIN ============
fun PublicShopItemImageDto.toDomain(): ItemImage {
    return ItemImage(
        id = id,
        url = url
    )
}

// ============ ITEM META DTO TO DOMAIN ============
fun ItemMetaDto.toDomain(): ItemMeta {
    return ItemMeta(
        color = color,
        size = size
    )
}

// ============ ITEM SHOP DTO TO DOMAIN ============
fun ItemShopDto.toDomain(): Shop {
    return Shop(
        id = id,
        name = name,
        displayName = "",
        userId = 0L,
        sellerName = "",
        profilePictureUrl = "",
        createdAt = "",
        itemsCount = 0
    )
}

// ============ CREATE ITEM RESPONSE TO DOMAIN ============
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

// ============ PUBLIC SHOP ITEM DTO TO DOMAIN ============
fun PublicShopItemDto.toDomain(shopId: Long): Item {
    return Item(
        id = id,
        shopId = shopId,
        name = name,
        description = "",
        price = price,
        quantity = 1,
        status = "active",
        createdAt = "",
        // ✅ FIXED: Explicitly handle PublicShopItemImageDto
        images = images?.map { imageDto ->
            when (imageDto) {
                is PublicShopItemImageDto -> imageDto.toDomain()
                else -> {
                    // Fallback for any other type
                    ItemImage(
                        id = (imageDto as? Map<*, *>)?.get("id") as? Long ?: 0L,
                        url = (imageDto as? Map<*, *>)?.get("url") as? String ?: "",
                        filename = null,
                        contentType = null,
                        createdAt = null
                    )
                }
            }
        } ?: emptyList(),
        brandId = null,
        sizeId = null,
        schoolId = null,
        itemConditionId = null,
        locationId = null,
        provinceId = null,
        genderId = null,
        label = null,
        reserved = 0,
        meta = null,
        shop = null
    )
}

// ============ SHOP ITEM DTO TO DOMAIN ============
fun ShopItemDto.toDomain(shopId: Long): Item {
    return Item(
        id = id,
        shopId = shopId,
        name = name,
        description = description,
        price = price,
        quantity = quantity,
        status = status,
        createdAt = createdAt,
        // ✅ FIXED: Explicitly handle ShopItemImageDto
        images = images.map { imageDto ->
            when (imageDto) {
                is ShopItemImageDto -> imageDto.toDomain()
                else -> {
                    // Fallback for any other type
                    ItemImage(
                        id = (imageDto as? Map<*, *>)?.get("id") as? Long ?: 0L,
                        url = (imageDto as? Map<*, *>)?.get("url") as? String ?: "",
                        filename = null,
                        contentType = null,
                        createdAt = null
                    )
                }
            }
        },
        brandId = null,
        sizeId = null,
        schoolId = null,
        itemConditionId = null,
        locationId = null,
        provinceId = null,
        genderId = null,
        label = null,
        reserved = quantity - availableQuantity,
        meta = null,
        shop = null
    )
}

// ============ DOMAIN TO ENTITY ============
fun Item.toEntity(): ItemEntity {
    return ItemEntity(
        id = id,
        shopId = shopId,
        name = name,
        description = description,
        price = price,
        quantity = quantity,
        status = status,
        itemTypeId = null,
        brandId = null,
        sizeId = null,
        schoolId = null,
        itemConditionId = null,
        locationId = null,
        provinceId = null,
        genderId = null,
        metaColor = meta?.color,
        metaSize = meta?.size,
        label = null,
        reserved = 0,
        createdAt = createdAt,
        imageCount = images.size
    )
}

// ============ ENTITY TO DOMAIN ============
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
        shop = null,
        images = emptyList()
    )
}