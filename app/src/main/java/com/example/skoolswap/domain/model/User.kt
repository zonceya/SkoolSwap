package com.example.skoolswap.domain.model

data class User(
    val id: Int,
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
    // Add these school fields
    val schoolId: Int? = null,
    val schoolName: String? = null,
    val schoolMapped: Boolean = false
)