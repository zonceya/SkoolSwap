package com.example.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

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
    val imageCount: Int = 0 // Track number of images
)