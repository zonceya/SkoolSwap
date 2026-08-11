package za.co.skoolswap.data.remote.models.response.province

import com.google.gson.annotations.SerializedName

data class ProvinceResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("code")
    val code: String? = null,  // Made nullable since API might return null

    @SerializedName("active")
    val active: Boolean = true
)