package za.co.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class ProvincesResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("provinces")
    val provinces: List<ProvinceDto>?,

    @SerializedName("error")
    val error: String?
)

data class ProvinceDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)