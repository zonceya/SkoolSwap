package com.example.skoolswap.data.remote.models.response.user

import com.google.gson.annotations.SerializedName

data class UserResponse(
    val id: Int,
    val name: String,
    val email: String,
    val mobile: String?,
    val username: String?,
    @SerializedName("profile_picture_url")
    val profilePictureUrl: String,
    @SerializedName("auth_mode")
    val authMode: String,
    val role: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    // 🔥 ADD THESE THREE FIELDS
    @SerializedName("school_mapped")
    val schoolMapped: Boolean? = false,
    @SerializedName("school_id")
    val schoolId: Int? = null,
    @SerializedName("school_name")
    val schoolName: String? = null
)