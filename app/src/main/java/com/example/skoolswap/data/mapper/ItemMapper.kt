package com.example.skoolswap.data.mapper

import com.example.skoolswap.data.local.database.entities.ItemEntity
import com.example.skoolswap.data.remote.models.response.home.RecommendationItemDto
import com.example.skoolswap.data.remote.models.response.item.*
import com.example.skoolswap.data.remote.models.response.shop.PublicShopItemDto
import com.example.skoolswap.data.remote.models.response.shop.PublicShopItemImageDto
import com.example.skoolswap.data.remote.models.response.shop.ShopItemDto
import com.example.skoolswap.data.remote.models.response.shop.ShopItemImageDto
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.domain.model.ItemImage
import com.example.skoolswap.domain.model.ItemMeta
import com.example.skoolswap.domain.model.Shop
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ============ ITEM DTO TO DOMAIN ============
fun ItemDto.toDomain(): Item {
    // Parse all images from the API response
    val allImages = mutableListOf<ItemImage>()

    // 1. Add cover photo if exists
    val coverPhotoUrl = coverPhoto ?: image
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

    // 2. Add all images from the images array (THIS WAS THE PROBLEM!)
    this.images?.forEach { imageUrl ->
        if (imageUrl != coverPhotoUrl && imageUrl.isNotBlank()) {
            allImages.add(ItemImage(
                id = 0,
                url = imageUrl,
                filename = null,
                contentType = null,
                createdAt = null,
                isCover = false
            ))
        }
    }

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
        updatedAt = updatedAt,
        shop = shop?.toDomain(),
        images = allImages,  // ✅ Now has all images!
        coverImage = coverPhotoUrl,
        brandId = brand?.id,
        sizeId = size?.id,
        colorId = color?.id,
        schoolId = school?.id,
        itemConditionId = condition?.id,
        locationId = town?.id,
        provinceId = province?.id,
        genderId = gender?.id,
        mainCategoryId = mainCategory?.id,
        subCategoryId = subCategory?.id,
        sizeName = size?.name,
        colorName = color?.name,
        brandName = brand?.name,
        conditionName = condition?.name,
        reserved = (quantity - (availableQuantity ?: quantity)),
        label = label
    )
}

// ============ ITEM IMAGE DTO TO DOMAIN ============
fun ItemImageDto.toDomain(): ItemImage {
    return ItemImage(
        id = id,
        url = url,
        filename = filename,
        contentType = contentType,
        createdAt = createdAt,
        isCover = false
    )
}

// ============ SHOP ITEM IMAGE DTO TO DOMAIN ============
fun ShopItemImageDto.toDomain(): ItemImage {
    return ItemImage(
        id = id,
        url = url,
        filename = filename,
        contentType = contentType,
        createdAt = createdAt,
        isCover = false
    )
}

// ============ PUBLIC SHOP ITEM IMAGE DTO TO DOMAIN ============
fun PublicShopItemImageDto.toDomain(): ItemImage {
    return ItemImage(
        id = id,
        url = url,
        isCover = false
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
        sellerMobile = "",
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
        !itemData?.coverPhoto.isNullOrEmpty() -> itemData.coverPhoto
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


    val additionalImages = itemData?.images
        ?.filter { it != coverPhotoUrl && it.isNotBlank() }
        ?.map { imageUrl ->
            ItemImage(
                id = 0,
                url = imageUrl,
                filename = null,
                contentType = null,
                createdAt = null,
                isCover = false
            )
        } ?: emptyList()

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
        images = finalImages,
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
        itemTypeId = null,
        sizeName = itemData?.size?.name,
        colorName = itemData?.color?.name,
        brandName = itemData?.brand?.name,
        conditionName = itemData?.condition?.name
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
        viewCount = viewCount,
        createdAt = createdAt,
        images = images.mapIndexed { index, url ->
            ItemImage(
                id = 0,
                url = url,
                filename = null,
                contentType = null,
                createdAt = null,
                isCover = index == 0
            )
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
        shop = null,
        itemTypeId = null,
        mainCategoryId = mainCategoryId,
        subCategoryId = subCategoryId
    )
}

// ============ DOMAIN TO ENTITY (WITH IMAGES JSON) ============
fun Item.toEntity(): ItemEntity {
    // Convert images to JSON string
    val imagesJson = if (images.isNotEmpty()) {
        val imagesData = images.map { image ->
            mapOf(
                "url" to image.url,
                "isCover" to image.isCover
            )
        }
        Gson().toJson(imagesData)
    } else {
        "[]"
    }

    // Get cover image URL
    val coverImageUrl = coverImage ?: images.firstOrNull { it.isCover }?.url ?: images.firstOrNull()?.url

    return ItemEntity(
        id = id,
        shopId = shopId,
        name = name,
        description = description,
        price = price,
        quantity = quantity,
        status = status,
        itemTypeId = itemTypeId,
        brandId = brandId,
        sizeId = sizeId,
        schoolId = schoolId,
        itemConditionId = itemConditionId,
        locationId = locationId,
        provinceId = provinceId,
        genderId = genderId,
        metaColor = meta?.color,
        metaSize = meta?.size,
        label = label,
        reserved = reserved,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = false,
        imageCount = images.size,
        lastCacheTime = System.currentTimeMillis(),
        sizeName = sizeName,
        colorName = colorName,
        brandName = brandName,
        conditionName = conditionName,
        imagesJson = imagesJson,
        coverImage = coverImageUrl
    )
}
// KEEP THIS VERSION
fun RecommendationItemDto.toDomain(): com.example.skoolswap.domain.model.Item {
    return com.example.skoolswap.domain.model.Item(
        id = id,
        shopId = shop?.id ?: 0L,
        name = name,
        description = description ?: "",
        price = price,
        quantity = availableQuantity,
        status = "active",
        createdAt = createdAt,
        images = listOfNotNull(coverPhoto ?: image).map { url ->
            com.example.skoolswap.domain.model.ItemImage(
                id = 0,
                url = url,
                isCover = true
            )
        },
        coverImage = coverPhoto ?: image,
        sizeName = sizeName,
        colorName = colorName,
        conditionName = conditionName,
        brandName = brandName,
        gender = gender,
        viewCount = viewCount,
        shop = shop?.let {
            com.example.skoolswap.domain.model.Shop(
                id = it.id,
                name = it.name,
                displayName = "",
                userId = 0L,
                sellerName = it.sellerName ?: "",
                sellerMobile = it.sellerMobile,
                profilePictureUrl = "",
                createdAt = "",
                itemsCount = 0
            )
        }
    )
}
// ============ ENTITY TO DOMAIN (WITH IMAGES FROM JSON) ============
fun ItemEntity.toDomain(): Item {
    // Parse images from JSON
    val imagesList = mutableListOf<ItemImage>()

    try {
        if (imagesJson.isNotEmpty() && imagesJson != "[]") {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val imagesData: List<Map<String, Any>> = Gson().fromJson(imagesJson, type)

            imagesData.forEachIndexed { index, imageData ->
                val url = imageData["url"] as? String ?: ""
                val isCover = imageData["isCover"] as? Boolean ?: (index == 0)

                if (url.isNotEmpty()) {
                    imagesList.add(
                        ItemImage(
                            id = 0,
                            url = url,
                            filename = null,
                            contentType = null,
                            createdAt = null,
                            isCover = isCover
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Fallback: If no images parsed and there's a coverImage field, use it
    if (imagesList.isEmpty() && coverImage != null && coverImage!!.isNotEmpty()) {
        imagesList.add(
            ItemImage(
                id = 0,
                url = coverImage!!,
                filename = null,
                contentType = null,
                createdAt = null,
                isCover = true
            )
        )
    }

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
        updatedAt = updatedAt,
        shop = null,
        images = imagesList,
        coverImage = coverImage ?: imagesList.firstOrNull()?.url,
        brandId = brandId,
        sizeId = sizeId,
        schoolId = schoolId,
        itemConditionId = itemConditionId,
        locationId = locationId,
        provinceId = provinceId,
        genderId = genderId,
        label = label,
        reserved = reserved,
        itemTypeId = itemTypeId,
        sizeName = sizeName,
        colorName = colorName,
        brandName = brandName,
        conditionName = conditionName
    )
}