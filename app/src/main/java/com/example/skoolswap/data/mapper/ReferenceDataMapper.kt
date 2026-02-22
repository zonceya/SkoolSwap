package com.example.skoolswap.data.mapper


import com.example.skoolswap.data.local.database.entities.*
import com.example.skoolswap.data.remote.models.response.reference.*
import com.example.skoolswap.domain.model.reference.*

object ReferenceDataMapper {

    // ============ MAIN CATEGORY MAPPERS ============
    fun toEntity(dto: MainCategoryDto): MainCategoryEntity {
        return MainCategoryEntity(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            iconName = dto.iconName,
            displayOrder = dto.displayOrder,
            isActive = dto.isActive
        )
    }

    fun toDomain(entity: MainCategoryEntity): MainCategory {
        return MainCategory(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            iconName = entity.iconName,
            displayOrder = entity.displayOrder,
            isActive = entity.isActive
        )
    }

    // ============ SUB CATEGORY MAPPERS ============
    fun toEntity(dto: SubCategoryDto, mainCategoryId: Int? = null): SubCategoryEntity {
        return SubCategoryEntity(
            id = dto.id,
            name = dto.name,
            mainCategoryId = mainCategoryId,  // Use the passed parameter
            description = dto.description,
            displayOrder = dto.displayOrder,
            isActive = true  // Default to true since it's not in DTO
        )
    }

    fun toDomain(entity: SubCategoryEntity): SubCategory {
        return SubCategory(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            displayOrder = entity.displayOrder
            // mainCategoryId is intentionally omitted - not needed in domain
        )
    }

    // ============ COLOR MAPPERS ============
    fun toEntity(dto: ItemColorDto): ColorEntity {
        return ColorEntity(
            id = dto.id,
            name = dto.name
        )
    }

    fun toDomain(entity: ColorEntity): Color {
        return Color(
            id = entity.id,
            name = entity.name
        )
    }

    // ============ SIZE MAPPERS ============
    fun toEntity(dto: ItemSizeDto): SizeEntity {
        return SizeEntity(
            id = dto.id,
            name = dto.name
        )
    }

    fun toDomain(entity: SizeEntity): Size {
        return Size(
            id = entity.id,
            name = entity.name
        )
    }

    // ============ BRAND MAPPERS ============
    fun toEntity(dto: BrandDto): BrandEntity {
        return BrandEntity(
            id = dto.id,
            name = dto.name
        )
    }

    fun toDomain(entity: BrandEntity): Brand {
        return Brand(
            id = entity.id,
            name = entity.name
        )
    }

    // ============ CONDITION MAPPERS ============
    fun toEntity(dto: ItemConditionDto): ConditionEntity {
        return ConditionEntity(
            id = dto.id,
            name = dto.name,
            description = dto.description
        )
    }

    fun toDomain(entity: ConditionEntity): Condition {
        return Condition(
            id = entity.id,
            name = entity.name,
            description = entity.description
        )
    }

    // ============ PROVINCE MAPPERS ============
    fun toEntity(dto: ProvinceDto): ProvinceEntity {
        return ProvinceEntity(
            id = dto.id,
            name = dto.name
        )
    }

    fun toDomain(entity: ProvinceEntity): Province {
        return Province(
            id = entity.id,
            name = entity.name
        )
    }

    // ============ TOWN MAPPERS ============
    fun toEntity(dto: TownDto): TownEntity {
        return TownEntity(
            id = dto.id,
            name = dto.name,
            provinceId = dto.provinceId
        )
    }

    fun toDomain(entity: TownEntity): Town {
        return Town(
            id = entity.id,
            name = entity.name,
            provinceId = entity.provinceId
        )
    }

    // ============ SCHOOL MAPPERS ============
    fun toEntity(dto: SchoolDto): SchoolEntity {
        return SchoolEntity(
            id = dto.id,
            name = dto.name,
            schoolType = dto.schoolType,
            provinceId = dto.province?.id
        )
    }

    fun toDomain(entity: SchoolEntity): School {
        return School(
            id = entity.id,
            name = entity.name,
            schoolType = entity.schoolType,
            provinceId = entity.provinceId
        )
    }

    // ============ GENDER MAPPERS ============
    fun toEntity(dto: GenderDto): GenderEntity {
        return GenderEntity(
            id = dto.id,
            name = dto.name
        )
    }

    fun toDomain(entity: GenderEntity): Gender {
        return Gender(
            id = entity.id,
            name = entity.name
        )
    }

    // ============ TAG MAPPERS ============
    fun toEntity(dto: TagDto): TagEntity {
        return TagEntity(
            id = dto.id,
            name = dto.name,
            tagType = dto.tagType
        )
    }

    fun toDomain(entity: TagEntity): Tag {
        return Tag(
            id = entity.id,
            name = entity.name,
            tagType = entity.tagType
        )
    }

    // ============ LOCATION MAPPERS ============
    fun toEntity(dto: LocationDto): LocationEntity {
        return LocationEntity(
            id = dto.id,
            province = dto.province,
            stateOrRegion = dto.stateOrRegion,
            country = dto.country,
            townId = dto.townId
        )
    }

    fun toDomain(entity: LocationEntity): Location {
        return Location(
            id = entity.id,
            province = entity.province,
            stateOrRegion = entity.stateOrRegion,
            country = entity.country,
            townId = entity.townId
        )
    }
}