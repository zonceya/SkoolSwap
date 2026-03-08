package com.example.skoolswap.data.remote.models.response.school

data class SchoolMappingResponse(
    val id: Int,                    // school_id
    val name: String,                // school_name
    val province_id: Int?,
    val location_id: Int?,
    val school_type: String?,
    val mapping_id: String,          // user_schools UUID
    val mapped_at: String?,
    val updated_at: String?
)