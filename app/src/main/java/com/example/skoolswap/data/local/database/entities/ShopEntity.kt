package com.example.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val displayName: String = "",
    val userId: Long,
    val sellerName: String,
    val profilePictureUrl: String,
    val createdAt: String,
    val itemsCount: Int = 0,
    val updatedAt: Date = Date()
)