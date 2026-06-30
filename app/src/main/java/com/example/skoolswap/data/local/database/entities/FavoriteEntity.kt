package com.example.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites", primaryKeys = ["userId", "itemId"])
data class FavoriteEntity(
    val userId: Int,
    val itemId: String,
    val addedAt: Long = System.currentTimeMillis()
)