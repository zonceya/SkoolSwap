package com.example.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "item_images")
data class ItemImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: String,
    val url: String,
    val isCover: Boolean = false,
    val position: Int = 0
)