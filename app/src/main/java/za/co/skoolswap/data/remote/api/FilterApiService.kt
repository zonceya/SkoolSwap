// data/remote/api/FilterApiService.kt
package za.co.skoolswap.data.remote.api

import za.co.skoolswap.data.remote.models.response.home.FilterConfigResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface FilterApiService {

    @GET("api/v1/categories/{categoryId}/filter_config")
    suspend fun getFilterConfig(
        @Path("categoryId") categoryId: Int
    ): Response<FilterConfigResponse>
    @GET("api/v1/filters/global_config")
    suspend fun getGlobalFilterConfig(): Response<FilterConfigResponse>
}