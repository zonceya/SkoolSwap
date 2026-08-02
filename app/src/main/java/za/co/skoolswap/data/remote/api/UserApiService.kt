package za.co.skoolswap.data.remote.api

import za.co.skoolswap.common.constants.NetworkConstants.Endpoints
import za.co.skoolswap.data.remote.models.request.AssignSchoolRequest
import za.co.skoolswap.data.remote.models.request.SignInRequest
import za.co.skoolswap.data.remote.models.request.SignUpRequest
import za.co.skoolswap.data.remote.models.request.UpdateMobileRequest
import za.co.skoolswap.data.remote.models.request.VerifyLoginRequest
import za.co.skoolswap.data.remote.models.request.VerifySignUpRequest
import za.co.skoolswap.data.remote.models.response.profile.DeleteProfileResponse
import za.co.skoolswap.data.remote.models.response.profile.ProfileResponse
import za.co.skoolswap.data.remote.models.response.school.AssignSchoolResponse
import za.co.skoolswap.data.remote.models.response.user.FirebaseAuthResponse
import za.co.skoolswap.data.remote.models.response.user.RefreshTokenResponse
import za.co.skoolswap.data.remote.models.response.user.SendOtpResponse
import za.co.skoolswap.data.remote.models.response.user.SignInResponse
import za.co.skoolswap.data.remote.models.response.user.UpdateMobileResponse
import za.co.skoolswap.data.remote.models.response.user.UserResponse
import za.co.skoolswap.data.remote.models.response.user.VerifyOtpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApiService {

    // ============ AUTHENTICATION ============
    @POST(Endpoints.User.FIREBASE_AUTH)
    suspend fun firebaseAuth(
        @Body request: Map<String, String>
    ): Response<FirebaseAuthResponse>

    @POST(Endpoints.User.SIGN_IN)
    suspend fun signIn(
        @Body request: SignInRequest
    ): Response<SignInResponse>

    @POST(Endpoints.User.SIGN_UP)
    suspend fun signUp(
        @Body request: SignUpRequest
    ): Response<SendOtpResponse>

    // ============ OTP ============
    @POST(Endpoints.User.SEND_LOGIN_OTP)
    suspend fun sendLoginOtp(
        @Body body: Map<String, String>
    ): Response<SendOtpResponse>

    @POST(Endpoints.User.VERIFY_SIGNUP)
    suspend fun verifySignUp(
        @Body request: VerifySignUpRequest
    ): Response<VerifyOtpResponse>

    @POST(Endpoints.User.VERIFY_LOGIN)
    suspend fun verifyLogin(
        @Body request: VerifyLoginRequest
    ): Response<VerifyOtpResponse>

    @POST(Endpoints.User.RESEND_SIGNUP_OTP)
    suspend fun resendSignUpOtp(
        @Body body: Map<String, String>
    ): Response<SendOtpResponse>

    @POST(Endpoints.User.RESEND_LOGIN_OTP)
    suspend fun resendLoginOtp(
        @Body body: Map<String, String>
    ): Response<SendOtpResponse>

    // ============ TOKEN ============
    @POST(Endpoints.Auth.REFRESH_TOKEN)
    suspend fun refreshToken(
        @Header("Authorization") token: String
    ): Response<RefreshTokenResponse>

    // ============ PROFILE ============
    @GET(Endpoints.User.PROFILE)
    suspend fun getProfile(
        @Header("Authorization") authToken: String
    ): Response<ProfileResponse>

    @PUT(Endpoints.User.UPDATE_MOBILE)
    suspend fun updateMobile(
        @Header("Authorization") authToken: String,
        @Body request: UpdateMobileRequest
    ): Response<UpdateMobileResponse>

    // ============ ACCOUNT MANAGEMENT ============
    @DELETE(Endpoints.User.DISABLE_USER)
    suspend fun deleteProfile(
        @Header("Authorization") authToken: String
    ): Response<DeleteProfileResponse>

    // ============ USER DATA ============
    @GET(Endpoints.User.GET_USER)
    suspend fun getUserById(
        @Header("Authorization") authToken: String,
        @Path("userId") userId: Long
    ): Response<UserResponse>

    // ============ SCHOOL ============
    @POST(Endpoints.User.ASSIGN_SCHOOL)
    suspend fun assignSchool(
        @Header("Authorization") authToken: String,
        @Body request: AssignSchoolRequest
    ): Response<AssignSchoolResponse>
}