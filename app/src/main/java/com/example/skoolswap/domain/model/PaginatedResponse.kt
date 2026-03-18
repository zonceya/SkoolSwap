package com.example.skoolswap.domain.model

import com.example.skoolswap.data.remote.models.response.home.PaginationDto

data class PaginatedResponse<T>(
    val items: List<T>,
    val pagination: PaginationDto
)

data class Pagination(
    val currentPage: Int,
    val totalPages: Int,
    val totalCount: Int,
    val perPage: Int
)