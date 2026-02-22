package com.example.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class LocationsResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("locations")
    val locations: List<LocationDto>?,

    @SerializedName("error")
    val error: String?
)

data class LocationDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("province")
    val province: String,

    @SerializedName("state_or_region")
    val stateOrRegion: String,

    @SerializedName("country")
    val country: String,

    @SerializedName("town_id")
    val townId: Int?
)