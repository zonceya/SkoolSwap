package com.example.skoolswap.domain.model

data class SportItem(
    val id: Int,
    val name: String,
    val imageResId: Int
)

data class GearItem(
    val id: Int,
    val name: String,
    val type: String
)