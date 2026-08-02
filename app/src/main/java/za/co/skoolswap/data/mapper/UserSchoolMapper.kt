package za.co.skoolswap.data.mapper

import za.co.skoolswap.data.local.database.entities.UserSchoolEntity
import za.co.skoolswap.data.remote.models.response.school.SchoolMappingResponse
import za.co.skoolswap.domain.model.SchoolMapping

fun SchoolMappingResponse.toEntity(userId: Int): UserSchoolEntity {
    return UserSchoolEntity(
        id = this.mapping_id,
        userId = userId,
        schoolId = this.id,
        schoolName = this.name,
        mappedAt = this.mapped_at,
        updatedAt = this.updated_at
    )
}

fun UserSchoolEntity.toDomain(): SchoolMapping {
    return SchoolMapping(
        mappingId = this.id,
        schoolId = this.schoolId,
        schoolName = this.schoolName,
        provinceId = null,  // These would need to be joined from schools table
        locationId = null,   // or fetched separately
        schoolType = null,
        mappedAt = this.mappedAt,
        updatedAt = this.updatedAt
    )
}

fun UserSchoolEntity.toDomainWithSchool(school: za.co.skoolswap.domain.model.School): SchoolMapping {
    return SchoolMapping(
        mappingId = this.id,
        schoolId = this.schoolId,
        schoolName = this.schoolName,
        provinceId = school.provinceId,
        locationId = school.locationId,
        schoolType = school.schoolType,
        mappedAt = this.mappedAt,
        updatedAt = this.updatedAt
    )
}