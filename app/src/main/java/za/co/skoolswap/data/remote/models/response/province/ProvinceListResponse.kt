package za.co.skoolswap.data.remote.models.response.province

import com.google.gson.annotations.SerializedName

data class ProvinceListResponse(
    @SerializedName("provinces")
    val provinces: List<ProvinceResponse>?
)
