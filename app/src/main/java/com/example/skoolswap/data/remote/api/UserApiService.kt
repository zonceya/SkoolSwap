package com.example.skoolswap.data.remote.api

import com.example.skoolswap.data.remote.models.request.SignInRequest
import com.example.skoolswap.data.remote.models.request.UpdateMobileRequest
import com.example.skoolswap.data.remote.models.response.DeleteProfileResponse
import com.example.skoolswap.data.remote.models.response.ProfileResponse
import com.example.skoolswap.data.remote.models.response.SignInResponse
import com.example.skoolswap.data.remote.models.response.UpdateMobileResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

interface UserApiService {

    @POST("api/v1/users/sign_in")
    suspend fun signIn(
        @Body request: SignInRequest
    ): Response<SignInResponse>

    @PUT("api/v1/users/update_mobile")
    suspend fun updateMobile(
        @Header("Authorization") authToken: String,
        @Body request: UpdateMobileRequest
    ): Response<UpdateMobileResponse>
    @GET("api/v1/users/profile")
    suspend fun getProfile(
        @Header("Authorization") authToken: String
    ): Response<ProfileResponse>
    @DELETE("api/v1/users/disable")
    suspend fun deleteProfile(
        @Header("Authorization") authToken: String
    ): Response<DeleteProfileResponse>
}