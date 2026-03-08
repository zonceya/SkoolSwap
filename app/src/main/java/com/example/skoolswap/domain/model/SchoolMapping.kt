package com.example.skoolswap.domain.model

data class SchoolMapping(
    val mappingId: String,        // UUID from user_schools table
    val schoolId: Int,
    val schoolName: String,
    val provinceId: Int?,
    val locationId: Int?,
    val schoolType: String?,
    val mappedAt: String?,
    val updatedAt: String?
)