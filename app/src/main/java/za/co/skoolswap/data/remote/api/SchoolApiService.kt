package za.co.skoolswap.data.remote.api

import za.co.skoolswap.data.remote.models.response.school.SchoolSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Header

interface SchoolApiService {
    @GET("api/v1/schools")
    suspend fun searchSchools(
        @Header("Authorization") token: String,
        @Query("province_id") provinceId: Int,
        @Query("query") query: String
    ): retrofit2.Response<SchoolSearchResponse>
}





