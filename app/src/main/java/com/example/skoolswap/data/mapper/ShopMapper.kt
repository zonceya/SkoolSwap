package com.example.skoolswap.data.mapper


import com.example.skoolswap.data.local.database.entities.ShopEntity
import com.example.skoolswap.data.remote.models.response.shop.ShopDto
import com.example.skoolswap.domain.model.Shop

// Domain to Entity
fun Shop.toEntity(): ShopEntity {
    return ShopEntity(
        id = id,
        name = name,
        displayName = displayName,
        userId = userId,
        sellerName = sellerName,
        sellerMobile = sellerMobile,
        profilePictureUrl = profilePictureUrl,
        createdAt = createdAt,
        itemsCount = itemsCount
    )
}

// Entity to Domain
fun ShopEntity.toDomain(): Shop {
    return Shop(
        id = id,
        name = name,
        displayName = displayName,
        userId = userId,
        sellerName = sellerName,
        sellerMobile = sellerMobile,
        profilePictureUrl = profilePictureUrl,
        createdAt = createdAt,
        itemsCount = itemsCount
    )
}

// DTO to Domain
fun ShopDto.toDomain(profilePictureUrl: String): Shop {
    return Shop(
        id = id,
        name = name,
        displayName = display_name,
        userId = user_id,
        sellerName = seller_name,
        sellerMobile = seller_mobile,  // ← ADDED sellerMobile from DTO
        profilePictureUrl = profilePictureUrl,
        createdAt = created_at,
        itemsCount = items_count
    )
}

// DTO to Entity
fun ShopDto.toEntity(profilePictureUrl: String): ShopEntity {
    return ShopEntity(
        id = id,
        name = name,
        displayName = display_name,
        userId = user_id,
        sellerName = seller_name,
        sellerMobile = seller_mobile,  // ← ADDED sellerMobile from DTO
        profilePictureUrl = profilePictureUrl,
        createdAt = created_at,
        itemsCount = items_count
    )
}