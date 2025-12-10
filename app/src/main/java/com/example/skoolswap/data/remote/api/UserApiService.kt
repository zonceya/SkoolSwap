package com.example.skoolswap.data.remote.api

import com.example.skoolswap.data.remote.models.request.SignInRequest
import com.example.skoolswap.data.remote.models.response.SignInResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UserApiService {

    @POST("api/v1/users/sign_in")
    suspend fun signIn(
        @Body request: SignInRequest
    ): Response<SignInResponse>
}