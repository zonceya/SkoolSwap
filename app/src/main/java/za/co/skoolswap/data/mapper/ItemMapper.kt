package za.co.skoolswap.data.mapper

import za.co.skoolswap.data.local.database.entities.ItemEntity
import za.co.skoolswap.data.remote.models.response.home.RecommendationItemDto
import za.co.skoolswap.data.remote.models.response.item.*
import za.co.skoolswap.data.remote.models.response.shop.PublicShopItemDto
import za.co.skoolswap.data.remote.models.response.shop.PublicShopItemImageDto
import za.co.skoolswap.data.remote.models.response.shop.ShopItemDto
import za.co.skoolswap.data.remote.models.response.shop.ShopItemImageDto
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.model.ItemImage
import za.co.skoolswap.domain.model.ItemMeta
import za.co.skoolswap.domain.model.Shop
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber

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
        images = allImages,
        coverImage = coverPhotoUrl,
        schoolName = school?.name,
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
    val variant = variants?.firstOrNull()  // Get the first variant (contains actual price/quantity)

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

    // Add images from the response (these are the uploaded images)
    val additionalImages = images
        .map { imageDto ->
            ItemImage(
                id = imageDto.id,
                url = imageDto.url,
                filename = imageDto.filename,
                contentType = imageDto.contentType,
                createdAt = imageDto.createdAt,
                isCover = false
            )
        }

    allImages.addAll(additionalImages)
    val finalImages = allImages.take(3)

    // ✅ CRITICAL FIX: Get price and quantity from VARIANT, not from itemData
    val finalPrice = variant?.price ?: itemData?.price?.toDoubleOrNull() ?: 0.0
    val finalQuantity = variant?.quantity ?: itemData?.quantity ?: 0

    return Item(
        id = itemData?.id ?: "",
        shopId = itemData?.shopId ?: 0L,
        name = itemData?.name ?: "",
        description = itemData?.description ?: "",
        price = finalPrice,      // ← Now from variant (30.0)
        quantity = finalQuantity, // ← Now from variant (1)
        status = itemData?.status ?: "active",
        meta = itemData?.meta?.toDomain(),
        createdAt = itemData?.createdAt ?: "",
        shop = itemData?.shop?.toDomain(),
        images = finalImages,
        coverImage = coverPhotoUrl,
        brandId = itemData?.brand?.id,
        sizeId = variant?.sizeId ?: itemData?.size?.id,
        colorId = variant?.colorId ?: itemData?.color?.id,
        schoolId = itemData?.school?.id,
        itemConditionId = variant?.conditionId ?: itemData?.condition?.id,
        locationId = itemData?.town?.id,
        provinceId = itemData?.province?.id,
        genderId = itemData?.gender?.id,
        mainCategoryId = itemData?.mainCategory?.id,
        subCategoryId = itemData?.subCategory?.id,
        reserved = variant?.let {
            variant.quantity - (itemData?.availableQuantity ?: variant.quantity)
        } ?: 0,
        label = itemData?.label,
        itemTypeId = null,
        sizeName = variant?.sizeName ?: itemData?.size?.name,
        colorName = variant?.colorName ?: itemData?.color?.name,
        brandName = itemData?.brand?.name,
        conditionName = variant?.conditionName ?: itemData?.condition?.name
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
// ============ DOMAIN TO ENTITY (WITH IMAGES JSON) ============
fun Item.toEntity(): ItemEntity {
    Timber.tag("ItemMapper").d("🔄 Item.toEntity() called")
    Timber.tag("ItemMapper").d("   Item ID: $id")
    Timber.tag("ItemMapper").d("   Name: $name")
    Timber.tag("ItemMapper").d("   mainCategoryId: $mainCategoryId")
    Timber.tag("ItemMapper").d("   subCategoryId: $subCategoryId")
    Timber.tag("ItemMapper").d("   colorId: $colorId")
    Timber.tag("ItemMapper").d("   locationId: $locationId")
    Timber.tag("ItemMapper").d("   syncStatus: $syncStatus")
    Timber.tag("ItemMapper").d("   retryCount: $retryCount")

    val imagesJson = if (images.isNotEmpty()) {
        Gson().toJson(images.map { mapOf("url" to it.url, "isCover" to it.isCover) })
    } else "[]"

    val entity = ItemEntity(
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
        sizeName = sizeName,
        colorName = colorName,
        brandName = brandName,
        conditionName = conditionName,
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
        imagesJson = imagesJson,
        coverImage = coverImage,
        viewCount = viewCount,
        syncStatus = syncStatus,
        syncError = syncError,
        retryCount = retryCount,
        lastSyncAttempt = lastSyncAttempt,
        mainCategoryId = mainCategoryId,
        subCategoryId = subCategoryId,
        colorId = colorId
    )

    Timber.tag("ItemMapper").d("✅ ItemEntity created:")
    Timber.tag("ItemMapper").d("   mainCategoryId: ${entity.mainCategoryId}")
    Timber.tag("ItemMapper").d("   subCategoryId: ${entity.subCategoryId}")
    Timber.tag("ItemMapper").d("   colorId: ${entity.colorId}")
    Timber.tag("ItemMapper").d("   locationId: ${entity.locationId}")
    Timber.tag("ItemMapper").d("   syncStatus: ${entity.syncStatus}")

    return entity
}
// KEEP THIS VERSION
fun RecommendationItemDto.toDomain(): za.co.skoolswap.domain.model.Item {
    return za.co.skoolswap.domain.model.Item(
        id = id,
        shopId = shop?.id ?: 0L,
        name = name,
        description = description ?: "",
        price = price,
        quantity = availableQuantity,
        status = "active",
        createdAt = createdAt,
        images = listOfNotNull(coverPhoto ?: image).map { url ->
            za.co.skoolswap.domain.model.ItemImage(
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
        schoolName = school,
        shop = shop?.let {
            za.co.skoolswap.domain.model.Shop(
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
    Timber.tag("ItemMapper").d("🔄 ItemEntity.toDomain() called")
    Timber.tag("ItemMapper").d("   Entity ID: $id")
    Timber.tag("ItemMapper").d("   Name: $name")
    Timber.tag("ItemMapper").d("   mainCategoryId: $mainCategoryId")
    Timber.tag("ItemMapper").d("   subCategoryId: $subCategoryId")
    Timber.tag("ItemMapper").d("   colorId: $colorId")
    Timber.tag("ItemMapper").d("   locationId: $locationId")
    Timber.tag("ItemMapper").d("   syncStatus: $syncStatus")
    Timber.tag("ItemMapper").d("   retryCount: $retryCount")

    // Parse images from JSON
    val imagesList = mutableListOf<ItemImage>()
    try {
        if (imagesJson.isNotEmpty() && imagesJson != "[]") {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val imagesData: List<Map<String, Any>> = Gson().fromJson(imagesJson, type)
            imagesData.forEachIndexed { index, data ->
                val url = data["url"] as? String ?: ""
                val isCover = data["isCover"] as? Boolean ?: (index == 0)
                if (url.isNotEmpty()) {
                    imagesList.add(ItemImage(id = 0, url = url, isCover = isCover))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Fallback to coverImage if no images parsed
    if (imagesList.isEmpty() && coverImage != null && coverImage!!.isNotEmpty()) {
        imagesList.add(ItemImage(id = 0, url = coverImage!!, isCover = true))
    }

    val item = Item(
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
        conditionName = conditionName,
        syncStatus = syncStatus,
        syncError = syncError,
        retryCount = retryCount,
        lastSyncAttempt = lastSyncAttempt,
        mainCategoryId = mainCategoryId,
        subCategoryId = subCategoryId,
        colorId = colorId
    )

    Timber.tag("ItemMapper").d("✅ Item created:")
    Timber.tag("ItemMapper").d("   mainCategoryId: ${item.mainCategoryId}")
    Timber.tag("ItemMapper").d("   subCategoryId: ${item.subCategoryId}")
    Timber.tag("ItemMapper").d("   colorId: ${item.colorId}")
    Timber.tag("ItemMapper").d("   locationId: ${item.locationId}")
    Timber.tag("ItemMapper").d("   syncStatus: ${item.syncStatus}")

    return item
}