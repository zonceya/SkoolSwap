package za.co.skoolswap.data.remote.models.response.school

import za.co.skoolswap.data.remote.models.response.province.ProvinceResponse

data class SchoolResponse(
    val id: Int,
    val name: String,
    val province_id: Int,
    val province: ProvinceResponse?,
    val location_id: Int?,
    val school_type: String?
)