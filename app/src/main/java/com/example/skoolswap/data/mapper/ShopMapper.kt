package com.example.skoolswap.data.mapper

import com.example.skoolswap.data.local.database.entities.ShopEntity
import com.example.skoolswap.data.remote.models.response.shop.ShopDto
import com.example.skoolswap.domain.model.Shop

// Domain to Entity - SIMPLE!
fun Shop.toEntity(): ShopEntity {
    return ShopEntity(
        id = id,
        name = name,
        displayName = displayName,
        userId = userId,
        sellerName = sellerName,
        profilePictureUrl = profilePictureUrl,
        createdAt = createdAt, // Just copy the String
        itemsCount = itemsCount
    )
}

// Entity to Domain - SIMPLE!
fun ShopEntity.toDomain(): Shop {
    return Shop(
        id = id,
        name = name,
        displayName = displayName,
        userId = userId,
        sellerName = sellerName,
        profilePictureUrl = profilePictureUrl,
        createdAt = createdAt, // Just copy the String
        itemsCount = itemsCount
    )
}

// DTO to Domain - SIMPLE!
fun ShopDto.toDomain(profilePictureUrl: String): Shop {
    return Shop(
        id = id,
        name = name,
        displayName = display_name,
        userId = user_id,
        sellerName = seller_name,
        profilePictureUrl = profilePictureUrl,
        createdAt = created_at, // Just copy the String
        itemsCount = items_count
    )
}

// DTO to Entity - SIMPLE!
fun ShopDto.toEntity(profilePictureUrl: String): ShopEntity {
    return ShopEntity(
        id = id,
        name = name,
        displayName = display_name,
        userId = user_id,
        sellerName = seller_name,
        profilePictureUrl = profilePictureUrl,
        createdAt = created_at, // Just copy the String
        itemsCount = items_count
    )
}