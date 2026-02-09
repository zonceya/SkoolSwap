package com.example.skoolswap.data.mapper

import com.example.skoolswap.data.local.database.entities.ItemTypeEntity
import com.example.skoolswap.data.remote.models.response.item.ItemTypeDto
import com.example.skoolswap.domain.model.ItemType

object ItemTypeMapper {

    // DTO → Entity
    fun dtoToEntity(dto: ItemTypeDto, lastSync: Long = System.currentTimeMillis()): ItemTypeEntity {
        return ItemTypeEntity(
            id = dto.id,
            name = dto.name,
            group_id = dto.groupId,
            description = dto.description,
            last_sync = lastSync
        )
    }

    // Entity → Domain
    fun entityToDomain(entity: ItemTypeEntity): ItemType {
        return ItemType(
            id = entity.id,
            name = entity.name,
            groupId = entity.group_id,
            description = entity.description
        )
    }

    // DTO → Domain
    fun dtoToDomain(dto: ItemTypeDto): ItemType {
        return ItemType(
            id = dto.id,
            name = dto.name,
            groupId = dto.groupId,
            description = dto.description
        )
    }

    // Domain → Entity
    fun domainToEntity(domain: ItemType): ItemTypeEntity {
        return ItemTypeEntity(
            id = domain.id,
            name = domain.name,
            group_id = domain.groupId,
            description = domain.description,
            last_sync = System.currentTimeMillis()
        )
    }
}