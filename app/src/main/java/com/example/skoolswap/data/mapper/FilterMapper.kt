// data/mapper/FilterMapper.kt
package com.example.skoolswap.data.mapper

import com.example.skoolswap.data.remote.models.response.home.FilterConfigResponse
import com.example.skoolswap.data.remote.models.response.home.FilterGroupResponse
import com.example.skoolswap.data.remote.models.response.home.FilterOptionResponse
import com.example.skoolswap.domain.model.FilterConfig
import com.example.skoolswap.domain.model.FilterGroup
import com.example.skoolswap.domain.model.FilterOption

fun FilterConfigResponse.toDomain(): FilterConfig {
    return FilterConfig(
        categoryId = this.categoryId,  // Will be null for global filter
        categoryName = this.categoryName,  // Will be null for global filter
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