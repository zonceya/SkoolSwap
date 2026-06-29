package com.example.skoolswap.data.remote.api

import com.example.skoolswap.data.remote.models.request.SignInRequest
import com.example.skoolswap.data.remote.models.request.SignUpRequest
import com.example.skoolswap.data.remote.models.request.UpdateMobileRequest
import com.example.skoolswap.data.remote.models.request.VerifyLoginRequest
import com.example.skoolswap.data.remote.models.request.VerifySignUpRequest
import com.example.skoolswap.data.remote.models.response.profile.DeleteProfileResponse
import com.example.skoolswap.data.remote.models.response.profile.ProfileResponse
import com.example.skoolswap.data.remote.models.response.school.AssignSchoolResponse
import com.example.skoolswap.data.remote.models.response.user.FirebaseAuthResponse
import com.example.skoolswap.data.remote.models.response.user.RefreshTokenResponse
import com.example.skoolswap.data.remote.models.response.user.SendOtpResponse
import com.example.skoolswap.data.remote.models.response.user.SignInResponse
import com.example.skoolswap.data.remote.models.response.user.UpdateMobileResponse
import com.example.skoolswap.data.remote.models.response.user.UserResponse
import com.example.skoolswap.data.remote.models.response.user.VerifyOtpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApiService {
    @POST("api/v1/users/firebase_auth")
    suspend fun firebaseAuth(
        @Body request: Map<String, String>
    ): Response<FirebaseAuthResponse>
    @POST("api/v1/users/sign_in")
    suspend fun signIn(
        @Body request: SignInRequest
    ): Response<SignInResponse>
    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Header("Authorization") token: String
    ): Response<RefreshTokenResponse>
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

    @GET("api/v1/users/{userId}")
    suspend fun getUserById(
        @Header("Authorization") authToken: String,
        @Path("userId") userId: Long
    ): Response<UserResponse>
    // Add to UserApiService.kt
    @POST("api/v1/users/signup")  // Change from "api/auth/signup"
    suspend fun signUp(@Body request: SignUpRequest): Response<SendOtpResponse>

    @POST("api/v1/users/send_login_otp")  // Change from "api/auth/send_login_otp"
    suspend fun sendLoginOtp(@Body body: Map<String, String>): Response<SendOtpResponse>
    @POST("api/auth/verify_signup")
    suspend fun verifySignUp(@Body request: VerifySignUpRequest): Response<VerifyOtpResponse>

    @POST("api/v1/users/verify_login_otp")  // Change from "api/auth/verify_login"
    suspend fun verifyLogin(@Body request: VerifyLoginRequest): Response<VerifyOtpResponse>
    @POST("api/auth/resend_signup_otp")
    suspend fun resendSignUpOtp(@Body body: Map<String, String>): Response<SendOtpResponse>

    @POST("api/auth/resend_login_otp")
    suspend fun resendLoginOtp(@Body body: Map<String, String>): Response<SendOtpResponse>
}