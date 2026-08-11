// data/mapper/FilterMapper.kt
package za.co.skoolswap.data.mapper

import za.co.skoolswap.data.remote.models.response.home.FilterConfigResponse
import za.co.skoolswap.data.remote.models.response.home.FilterGroupResponse
import za.co.skoolswap.data.remote.models.response.home.FilterOptionResponse
import za.co.skoolswap.domain.model.FilterConfig
import za.co.skoolswap.domain.model.FilterGroup
import za.co.skoolswap.domain.model.FilterOption

fun FilterConfigResponse.toDomain(): FilterConfig {
    return FilterConfig(
        categoryId = this.categoryId,  // Will be null for global filter
        categoryName = this.categoryName,
        mainCategoryId = this.mainCategoryId,// Will be null for global filter
        filterGroups = this.filterGroups.map { it.toDomain() }
    )
}

fun FilterGroupResponse.toDomain(): FilterGroup {
    return FilterGroup(
        id = this.id,
        name = this.name,
        filterType = this.filterType,
        options = this.options?.map { it.toDomain() } ?: emptyList(),
        min = this.min,
        max = this.max,
        step = this.step
    )
}

fun FilterOptionResponse.toDomain(): FilterOption {
    return FilterOption(
        id = this.id,
        name = this.name
    )
}