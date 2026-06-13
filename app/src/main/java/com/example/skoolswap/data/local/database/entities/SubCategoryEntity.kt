package com.example.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "sub_categories",
    indices = [Index(value = ["mainCategoryId"])]
)
data class SubCategoryEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val mainCategoryId: Int? = null,
    val description: String?,
    val displayOrder: Int = 0,
    val isActive: Boolean? = true
)