package com.example.skoolswap.data.remote.models.response.home

import com.google.gson.annotations.SerializedName

data class RecommendedItemsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("title") val title: String?,
    @SerializedName("items") val items: List<RecommendationItemDto>,
    @SerializedName("pagination") val pagination: PaginationDto?
)

