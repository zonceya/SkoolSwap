package za.co.skoolswap.data.remote.api

import za.co.skoolswap.common.constants.AppConstants
import za.co.skoolswap.data.remote.models.request.HelpRequest
import za.co.skoolswap.data.remote.models.response.help.HelpResponse
import za.co.skoolswap.data.remote.models.response.help.HelpStatusResponse
import retrofit2.Response
import retrofit2.http.*

interface HelpApiService {
    @POST("api/${AppConstants.API_VERSION}/help/send")
    suspend fun sendSupportRequest(
        @Header("Authorization") authToken: String,
        @Body request: HelpRequest
    ): Response<HelpResponse>

    @GET("api/${AppConstants.API_VERSION}/help/status")
    suspend fun getHelpStatus(): Response<HelpStatusResponse>
}