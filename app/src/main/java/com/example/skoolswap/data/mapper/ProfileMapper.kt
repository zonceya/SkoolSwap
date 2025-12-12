package com.example.skoolswap.data.mapper

import com.example.skoolswap.data.remote.models.response.ProfileResponse
import com.example.skoolswap.domain.model.User

fun ProfileResponse.toDomain(token: String): User {
    return User(
        id = user.id,
        name = user.name,
        email = user.email,
        mobile = user.mobile ?: profile?.mobile, // Prefer user.mobile, fallback to profile.mobile
        username = user.username,
        profilePictureUrl = user.profilePictureUrl,
        authMode = user.authMode,
        role = user.role,
        token = token,
        createdAt = user.createdAt,
        updatedAt = user.updatedAt
    )
}