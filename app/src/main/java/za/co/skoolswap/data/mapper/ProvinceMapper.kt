// data/mapper/ProvinceMapper.kt
package za.co.skoolswap.data.mapper

import za.co.skoolswap.data.local.database.entities.ProvinceEntity
import za.co.skoolswap.domain.model.Province

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