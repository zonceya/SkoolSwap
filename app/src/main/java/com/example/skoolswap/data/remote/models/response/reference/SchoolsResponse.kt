package com.example.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class SchoolsResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("schools")
    val schools: List<SchoolDto>?,

    @SerializedName("error")
    val error: String?
)

data class SchoolDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("school_type")
    val schoolType: String?,

    @SerializedName("province") val province: ProvinceDto?
)