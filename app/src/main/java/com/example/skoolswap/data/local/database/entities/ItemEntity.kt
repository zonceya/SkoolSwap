package com.example.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.skoolswap.domain.model.ImageData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey
    val id: String, // UUID
    val shopId: Long,
    val name: String,
    val description: String,
    val price: Double,
    val quantity: Int,
    val status: String,
    val itemTypeId: Int?,
    val brandId: Int?,
    val sizeId: Int?,
    val schoolId: Int?,
    val sizeName: String?,
    val colorName: String?,
    val brandName: String?,
    val conditionName: String?,
    val itemConditionId: Int?,
    val locationId: Int?,
    val provinceId: Int?,
    val genderId: Int?,
    val metaColor: String?,
    val metaSize: String?,
    val label: String?,
    val reserved: Int,
    val createdAt: String,
    val updatedAt: String? = null,
    val deleted: Boolean = false,
    val imageCount: Int = 0,
    val lastCacheTime: Long = System.currentTimeMillis(),
    val imagesJson: String = "[]",
    val coverImage: String? = null,
    val viewCount: Int = 0 // ADD THIS FIELD
)

// Extension functions (outside the data class)

// Helper function to parse images from JSON
fun ItemEntity.getImages(): List<ItemImageEntity> {
    if (imagesJson.isEmpty() || imagesJson == "[]") {
        return emptyList()
    }

    try {
        val type = object : TypeToken<List<ImageData>>() {}.type
        val imagesData: List<ImageData> = Gson().fromJson(imagesJson, type)
        return imagesData.mapIndexed { index, data ->
            ItemImageEntity(
                itemId = id,
                url = data.url,
                isCover = data.isCover,
                position = index
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return emptyList()
    }
}

// Helper function to create JSON from images
fun ItemEntity.setImages(images: List<ItemImageEntity>): String {
    val imagesData = images.map { ImageData(it.url, it.isCover) }
    return Gson().toJson(imagesData)
}


