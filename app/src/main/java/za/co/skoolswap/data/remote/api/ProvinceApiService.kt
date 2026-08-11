// data/remote/api/ProvinceApiService.kt
package za.co.skoolswap.data.remote.api

import za.co.skoolswap.data.remote.models.response.province.ProvinceListResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface ProvinceApiService {
    @GET("api/v1/provinces")
    suspend fun getProvinces(
        @Header("Authorization") token: String
    ): retrofit2.Response<ProvinceListResponse>
}