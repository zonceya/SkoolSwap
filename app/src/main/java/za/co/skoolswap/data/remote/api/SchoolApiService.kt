package za.co.skoolswap.data.remote.api

import retrofit2.Response
import za.co.skoolswap.data.remote.models.response.school.SchoolSearchResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SchoolApiService {

    // ✅ Search schools by province + query (school name only)
    @GET("api/v1/schools")
    suspend fun searchSchools(
        @Header("Authorization") authHeader: String,
        @Query("province_id") provinceId: Int,
        @Query("query") query: String
    ): retrofit2.Response<SchoolSearchResponse>

    // ✅ NEW: Search schools by province + town name + school name
// In SchoolApiService.kt - verify the parameter name
    @GET("api/v1/schools/search_by_town")
    suspend fun searchSchoolsByTown(
        @Header("Authorization") authHeader: String,
        @Query("province_id") provinceId: Int,
        @Query("town_name") townName: String,  // ← Should this be town_id instead?
        @Query("school_query") schoolQuery: String?
    ): Response<SchoolSearchResponse>
}