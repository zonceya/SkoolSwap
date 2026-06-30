package com.example.skoolswap.data.mapper

import com.example.skoolswap.data.local.database.entities.UserEntity
import com.example.skoolswap.data.remote.models.response.user.SignInResponse
import com.example.skoolswap.data.remote.models.response.user.UserResponse
import com.example.skoolswap.domain.model.User

fun UserResponse.toDomain(token: String): User {
    return User(
        id = id,
        name = name,
        email = email,
        mobile = mobile,
        username = username,
        profilePictureUrl = profilePictureUrl,
        authMode = authMode,
        role = role,
        token = token,
        createdAt = createdAt,
        updatedAt = updatedAt,
        schoolMapped = schoolMapped ?: false,
        schoolId = schoolId,
        schoolName = schoolName
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        name = name,
        email = email,
        mobile = mobile,
        username = username,
        profilePictureUrl = profilePictureUrl,
        authMode = authMode,
        role = role,
        token = token,
        createdAt = createdAt,
        updatedAt = updatedAt,
        schoolMapped = schoolMapped,  // ✅ ADD THIS
        schoolId = schoolId,          // ✅ ADD THIS
        schoolName = schoolName       // ✅ ADD THIS
    )
}

fun UserEntity.toDomain(): User {
    return User(
        id = id,
        name = name,
        email = email,
        mobile = mobile,
        username = username,
        profilePictureUrl = profilePictureUrl,
        authMode = authMode,
        role = role,
        token = token,
        createdAt = createdAt,
        updatedAt = updatedAt,
        schoolMapped = schoolMapped,  // ✅ FIXED - use actual value
        schoolId = schoolId,          // ✅ FIXED - use actual value
        schoolName = schoolName       // ✅ FIXED - use actual value
    )
}
fun SignInResponse.toDomain(): User {
    return user.toDomain(token)
}

fun SignInResponse.toEntity(): UserEntity {
    return user.toDomain(token).toEntity()
}

