package com.example.skoolswap.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String,
    val mobile: String?,
    val username: String?,
    val profilePictureUrl: String?,
    val authMode: String,
    val role: String,
    val token: String,
    val createdAt: String,
    val updatedAt: String,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val schoolMapped: Boolean = false,
    val schoolId: Int? = null,
    val schoolName: String? = null
)