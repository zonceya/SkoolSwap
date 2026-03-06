package com.example.skoolswap.data.remote.models.response.school

import com.example.skoolswap.data.remote.models.response.province.ProvinceResponse

data class SchoolSearchResponse(
    val schools: List<SchoolResponse>,
    val total_count: Int,
    val province: ProvinceResponse?,
    val query: String?
)