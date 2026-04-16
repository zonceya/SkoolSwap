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

// In your mapper file - COMPLETE FIX
fun CreateItemResponse.toDomain(): Item {
    val itemData = item
    val allImages = mutableListOf<ItemImage>()

    // 1. Get cover photo (primary image)
    val coverPhotoUrl = when {
        // From Active Storage via image field
        !itemData?.image.isNullOrEmpty() -> itemData.image
        // From database cover_photo column
        !itemData?.cover_photo.isNullOrEmpty() -> itemData.cover_photo
        else -> null
    }

    // Add cover photo as first image if exists
    coverPhotoUrl?.let { url ->
        allImages.add(ItemImage(
            id = 0,  // Temporary ID for CDN images
            url = url,
            filename = null,
            contentType = null,
            createdAt = null,
            isCover = true  // Mark as cover (you may need to add this field)
        ))
    }

    // 2. Get additional images from images array
    // Handle both formats: List<String> OR List<ItemImageDto>
    val additionalImages = when (val imagesRaw = itemData?.imagesRaw) {
        is List<*> -> {
            imagesRaw.mapNotNull { image ->
                when (image) {
                    is String -> {
                        // String URL from CDN
                        if (image != coverPhotoUrl) {  // Avoid duplicate cover
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
                        // Object from Active Storage
                        ItemImage(
                            id = image.id,
                            url = image.url,
                            filename = image.filename,
                            contentType = image.contentType,
                            createdAt = image.createdAt,
                            isCover = false
                        )
                    }
                    else -> null
                }
            }
        }
        else -> emptyList()
    }

    allImages.addAll(additionalImages)

    // 3. Ensure we don't exceed 3 images
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
        images = finalImages,
        coverImage = coverPhotoUrl,  // Add this field to your Item model
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