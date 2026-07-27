package com.example.skoolswap.domain.repository

import com.example.skoolswap.data.remote.models.response.help.HelpResponse
import com.example.skoolswap.data.remote.models.response.help.HelpStatusResponse

interface HelpRepositoryInterface {
    suspend fun sendSupportRequest(
        subject: String,
        description: String
    ): Result<HelpResponse>

    suspend fun getHelpStatus(): Result<HelpStatusResponse>
}