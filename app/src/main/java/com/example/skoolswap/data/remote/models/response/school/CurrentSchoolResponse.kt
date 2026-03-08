package com.example.skoolswap.data.remote.models.response.school

data class CurrentSchoolResponse(
    val school_mapped: Boolean,
    val school: SchoolMappingResponse?
)