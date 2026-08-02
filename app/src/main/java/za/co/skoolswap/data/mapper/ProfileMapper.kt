package za.co.skoolswap.data.mapper

import za.co.skoolswap.data.remote.models.response.profile.ProfileResponse
import za.co.skoolswap.domain.model.User

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