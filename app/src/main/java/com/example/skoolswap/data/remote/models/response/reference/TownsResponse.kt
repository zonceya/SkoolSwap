package com.example.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class TownsResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("towns")
    val towns: List<TownDto>?,

    @SerializedName("error")
    val error: String?
)

data class TownDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("province_id")
    val provinceId: Int
)