package com.example.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class GendersResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("genders")
    val genders: List<GenderDto>?,

    @SerializedName("error")
    val error: String?
)

data class GenderDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("display_name") val displayName: String?  // Add this
)