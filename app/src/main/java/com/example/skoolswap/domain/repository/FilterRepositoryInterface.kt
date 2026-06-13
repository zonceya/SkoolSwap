// domain/repository/FilterRepositoryInterface.kt
package com.example.skoolswap.domain.repository

import com.example.skoolswap.domain.model.FilterConfig
import com.example.skoolswap.utils.Result

interface FilterRepositoryInterface {
    suspend fun getFilterConfig(categoryId: Int): Result<FilterConfig>
    suspend fun getGlobalFilterConfig(): Result<FilterConfig>
}