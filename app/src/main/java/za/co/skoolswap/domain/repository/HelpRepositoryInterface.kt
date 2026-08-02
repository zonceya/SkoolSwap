package za.co.skoolswap.domain.repository

import za.co.skoolswap.data.remote.models.response.help.HelpResponse
import za.co.skoolswap.data.remote.models.response.help.HelpStatusResponse

interface HelpRepositoryInterface {
    suspend fun sendSupportRequest(
        subject: String,
        description: String
    ): Result<HelpResponse>

    suspend fun getHelpStatus(): Result<HelpStatusResponse>
}