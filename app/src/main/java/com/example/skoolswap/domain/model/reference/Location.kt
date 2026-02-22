package com.example.skoolswap.domain.model.reference

data class Location(
    val id: Int,
    val province: String,
    val stateOrRegion: String,
    val country: String,
    val townId: Int?
)