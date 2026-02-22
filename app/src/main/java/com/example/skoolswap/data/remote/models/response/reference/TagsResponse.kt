package com.example.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class TagsResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("tags")
    val tags: List<TagDto>?,

    @SerializedName("error")
    val error: String?
)

data class TagDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("tag_type")
    val tagType: String
)