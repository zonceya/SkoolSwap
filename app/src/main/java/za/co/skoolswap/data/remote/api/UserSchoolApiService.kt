package za.co.skoolswap.data.remote.api

import za.co.skoolswap.data.remote.models.response.school.CurrentSchoolResponse
import za.co.skoolswap.data.remote.models.response.school.AssignSchoolResponse
import retrofit2.http.*

interface UserSchoolApiService {

    @GET("api/v1/user_schools/current")
    suspend fun getCurrentSchool(
        @Header("Authorization") token: String
    ): retrofit2.Response<CurrentSchoolResponse>

    @POST("api/v1/user_schools")
    suspend fun assignSchool(
        @Header("Authorization") token: String,
        @Body request: Map<String, Int>
    ): retrofit2.Response<AssignSchoolResponse>

    @PUT("api/v1/user_schools/{mappingId}")
    suspend fun updateSchool(
        @Header("Authorization") token: String,
        @Path("mappingId") mappingId: String,
        @Body request: Map<String, Int>
    ): retrofit2.Response<AssignSchoolResponse>

    @DELETE("api/v1/user_schools/{mappingId}")
    suspend fun removeSchool(
        @Header("Authorization") token: String,
        @Path("mappingId") mappingId: String
    ): retrofit2.Response<Unit>
}