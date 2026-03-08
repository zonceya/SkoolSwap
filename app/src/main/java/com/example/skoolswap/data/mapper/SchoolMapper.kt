// data/mapper/SchoolMapper.kt
package com.example.skoolswap.data.mapper

import com.example.skoolswap.data.local.database.entities.SchoolEntity
import com.example.skoolswap.data.remote.models.response.school.SchoolMappingResponse
import com.example.skoolswap.data.remote.models.response.school.SchoolResponse
import com.example.skoolswap.domain.model.School

// For converting SchoolMappingResponse to SchoolEntity (for caching)
fun SchoolMappingResponse.toSchoolEntity(): SchoolEntity {
    return SchoolEntity(
        id = this.id,
        name = this.name,
        provinceId = this.province_id,  // Use province_id, not location_id
        schoolType = this.school_type
    )
}

// For converting SchoolResponse to SchoolEntity (for search results)
fun SchoolResponse.toSchoolEntity(): SchoolEntity {
    return SchoolEntity(
        id = this.id,
        name = this.name,
        provinceId = this.province_id,  // Use province_id
        schoolType = this.school_type
    )
}

// For converting SchoolEntity to domain School
fun SchoolEntity.toDomainSchool(): School {
    return School(
        id = this.id,
        name = this.name,
        provinceId = this.provinceId,
        provinceName = null, // Would need to join with province table
        locationId = null,   // Schools don't have direct location_id
        schoolType = this.schoolType
    )
}