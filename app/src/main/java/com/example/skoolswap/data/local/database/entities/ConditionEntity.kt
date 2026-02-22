package com.example.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conditions")
data class ConditionEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val description: String?
)