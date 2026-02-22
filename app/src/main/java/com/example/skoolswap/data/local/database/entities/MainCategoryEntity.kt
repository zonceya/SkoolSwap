package com.example.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "main_categories")
data class MainCategoryEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val description: String?,
    val iconName: String?,
    val displayOrder: Int,
    val isActive: Boolean
)