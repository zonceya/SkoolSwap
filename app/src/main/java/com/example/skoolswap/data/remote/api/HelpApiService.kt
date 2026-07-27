package com.example.skoolswap.data.remote.api

import com.example.skoolswap.common.constants.AppConstants
import com.example.skoolswap.data.remote.models.request.HelpRequest
import com.example.skoolswap.data.remote.models.response.help.HelpResponse
import com.example.skoolswap.data.remote.models.response.help.HelpStatusResponse
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