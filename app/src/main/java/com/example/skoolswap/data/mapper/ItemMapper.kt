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
        images = emptyList()
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
    val itemData = item
    val allImages = mutableListOf<ItemImage>()

    // 1. Get cover photo (primary image)
    val coverPhotoUrl = when {
        !itemData?.image.isNullOrEmpty() -> itemData.image
        !itemData?.cover_photo.isNullOrEmpty() -> itemData.cover_photo
        else -> null
    }

    // Add cover photo as first image if exists
    coverPhotoUrl?.let { url ->
        allImages.add(ItemImage(
            id = 0,
            url = url,
            filename = null,
            contentType = null,
            createdAt = null,
            isCover = true
        ))
    }

    // 2. Get additional images from images array
    val additionalImages = when (val imagesRaw = itemData?.imagesRaw) {
        is List<*> -> {
            imagesRaw.mapNotNull { image ->
                when (image) {
                    is String -> {
                        if (image != coverPhotoUrl) {
                            ItemImage(
                                id = 0,
                                url = image,
                                filename = null,
                                contentType = null,
                                createdAt = null,
                                isCover = false
                            )
                        } else null
                    }
                    is ItemImageDto -> {
                        if (image.url != coverPhotoUrl) {
                            image.toDomain().copy(isCover = false)
                        } else null
                    }
                    else -> null
                }
            }
        }
        else -> emptyList()
    }

    allImages.addAll(additionalImages)
    val finalImages = allImages.take(3)

    return Item(
        id = itemData?.id ?: "",
        shopId = itemData?.shopId ?: 0L,
        name = itemData?.name ?: "",
        description = itemData?.description ?: "",
        price = itemData?.price?.toDoubleOrNull() ?: 0.0,
        quantity = itemData?.quantity ?: 0,
        status = itemData?.status ?: "active",
        meta = itemData?.meta?.toDomain(),
        createdAt = itemData?.createdAt ?: "",
        shop = itemData?.shop?.toDomain(),
        images = finalImages,  // ← List<ItemImage>
        coverImage = coverPhotoUrl,
        brandId = itemData?.brand?.id,
        sizeId = itemData?.size?.id,
        colorId = itemData?.color?.id,
        schoolId = itemData?.school?.id,
        itemConditionId = itemData?.condition?.id,
        locationId = itemData?.town?.id,
        provinceId = itemData?.province?.id,
        genderId = itemData?.gender?.id,
        mainCategoryId = itemData?.mainCategory?.id,
        subCategoryId = itemData?.subCategory?.id,
        reserved = itemData?.let {
            it.quantity - (it.availableQuantity ?: it.quantity)
        } ?: 0,
        label = itemData?.label,
        itemTypeId = null
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
        images = images?.map { imageDto ->
            when (imageDto) {
                is PublicShopItemImageDto -> imageDto.toDomain()
                is String -> ItemImage(id = 0, url = imageDto)
                else -> {
                    val url = (imageDto as? Map<*, *>)?.get("url") as? String ?: ""
                    ItemImage(id = 0, url = url)
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
        images = images.map { imageDto ->
            when (imageDto) {
                is ShopItemImageDto -> imageDto.toDomain()
                is String -> ItemImage(id = 0, url = imageDto)
                else -> {
                    val url = (imageDto as? Map<*, *>)?.get("url") as? String ?: ""
                    ItemImage(id = 0, url = url)
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