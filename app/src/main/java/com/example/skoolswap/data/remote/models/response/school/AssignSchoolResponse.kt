package com.example.skoolswap.data.remote.models.response.school

import com.example.skoolswap.data.remote.models.response.user.UserResponse


data class AssignSchoolResponse(
    val success: Boolean,
    val message: String?,
    val school: SchoolMappingResponse?,
    val user: UserResponse?  // Optional - if your API returns updated user
)