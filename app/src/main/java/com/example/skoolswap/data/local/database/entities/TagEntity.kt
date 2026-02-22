package com.example.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val tagType: String
)