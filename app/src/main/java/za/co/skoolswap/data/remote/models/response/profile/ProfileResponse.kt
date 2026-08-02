package za.co.skoolswap.data.remote.models.response.profile

import za.co.skoolswap.data.remote.models.response.user.UserResponse
import com.google.gson.annotations.SerializedName

// For GET /api/v1/users/profile
data class ProfileResponse(
    @SerializedName("user") val user: UserResponse,  // Use UserResponse
    @SerializedName("profile") val profile: UserProfile?
)

// Profile object in the response
data class UserProfile(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("mobile") val mobile: String?
)