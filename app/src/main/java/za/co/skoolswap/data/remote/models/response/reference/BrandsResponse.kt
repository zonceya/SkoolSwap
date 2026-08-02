package za.co.skoolswap.data.remote.models.response.reference

import com.google.gson.annotations.SerializedName

data class BrandsResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("brands")
    val brands: List<BrandDto>?,

    @SerializedName("error")
    val error: String?
)

data class BrandDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)
