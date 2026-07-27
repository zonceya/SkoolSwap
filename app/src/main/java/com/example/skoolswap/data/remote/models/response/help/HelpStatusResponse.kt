package com.example.skoolswap.data.remote.models.response.help

data class HelpStatusResponse(
    val success: Boolean,
    val status: String,
    val support_email: String,
    val response_time: String,
    val phone: String,
    val availability: HelpAvailability
)

data class HelpAvailability(
    val weekdays: String,
    val public_holidays: String,
    val exceptions: String
)