package za.co.skoolswap.data.remote.api

import za.co.skoolswap.domain.model.Province
import retrofit2.http.GET
import retrofit2.http.Header

interface ProvinceApiService {
    @GET("api/v1/provinces")
    suspend fun getProvinces(
        @Header("Authorization") token: String
    ): retrofit2.Response<List<Province>>
}