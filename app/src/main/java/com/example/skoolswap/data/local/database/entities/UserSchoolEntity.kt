// data/local/database/entities/UserSchoolEntity.kt
package com.example.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_schools")
data class UserSchoolEntity(
    @PrimaryKey
    val id: String,  // UUID from server
    val userId: Int,
    val schoolId: Int,
    val schoolName: String,  // Denormalized for quick access
    val mappedAt: String?,
    val updatedAt: String?
)