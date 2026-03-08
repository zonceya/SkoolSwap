// data/mapper/ProvinceMapper.kt
package com.example.skoolswap.data.mapper

import com.example.skoolswap.data.local.database.entities.ProvinceEntity
import com.example.skoolswap.domain.model.Province

fun ProvinceEntity.toDomain(): Province {
    return Province(
        id = this.id,
        name = this.name
    )
}

fun Province.toEntity(): ProvinceEntity {
    return ProvinceEntity(
        id = this.id,
        name = this.name
    )
}