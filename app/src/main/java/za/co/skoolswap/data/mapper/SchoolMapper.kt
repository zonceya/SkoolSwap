package za.co.skoolswap.data.mapper

import za.co.skoolswap.data.local.database.entities.SchoolEntity
import za.co.skoolswap.data.remote.models.response.school.SchoolMappingResponse
import za.co.skoolswap.data.remote.models.response.school.SchoolResponse
import za.co.skoolswap.domain.model.School

// For converting SchoolMappingResponse to SchoolEntity (for caching)
fun SchoolMappingResponse.toSchoolEntity(): SchoolEntity {
    return SchoolEntity(
        id = this.id,
        name = this.name,
        provinceId = this.province_id,
        locationId = null,  // ✅ Add missing field
        schoolType = this.school_type,
        logoUrl = null       // ✅ Add missing field
    )
}

// For converting SchoolResponse to SchoolEntity (for search results)
fun SchoolResponse.toSchoolEntity(): SchoolEntity {
    return SchoolEntity(
        id = this.id,
        name = this.name,
        provinceId = this.province_id,
        locationId = this.location_id,  // ✅ Add missing field
        schoolType = this.school_type,
        logoUrl = this.logo_url          // ✅ Add missing field
    )
}

// For converting SchoolEntity to domain School
fun SchoolEntity.toDomainSchool(): School {
    return School(
        id = this.id,
        name = this.name,
        provinceId = this.provinceId,
        provinceName = null,
        locationId = this.locationId,
        schoolType = this.schoolType,
        logoUrl = this.logoUrl
    )
}