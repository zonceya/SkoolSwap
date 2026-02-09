// data/local/database/entities/ItemTypeEntity.kt
package com.example.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "item_types")
data class ItemTypeEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val group_id: Int,
    val description: String?,
    val last_sync: Long = System.currentTimeMillis()
)