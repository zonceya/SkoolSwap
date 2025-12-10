package com.example.skoolswap.data.mapper

import com.example.skoolswap.data.local.database.entities.UserEntity
import com.example.skoolswap.data.remote.models.response.SignInResponse
import com.example.skoolswap.data.remote.models.response.UserResponse
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
        updatedAt = updatedAt
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
        updatedAt = updatedAt
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
        updatedAt = updatedAt
    )
}
fun SignInResponse.toDomain(): User {
    return user.toDomain(token)
}

fun SignInResponse.toEntity(): UserEntity {
    return user.toDomain(token).toEntity()
}

