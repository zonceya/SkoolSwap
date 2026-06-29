package com.example.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "item_images")
data class ItemImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: String,
    val url: String,  // ✅ Changed from imageUrl to url
    val localPath: String? = null,
    val position: Int = 0,
    val isCover: Boolean = false,  // ✅ ADD THIS
    val createdAt: Long = System.currentTimeMillis(),
    val fileSize: Long = 0
)