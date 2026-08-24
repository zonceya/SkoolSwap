package za.co.skoolswap.data.remote.api

import za.co.skoolswap.data.remote.models.response.reference.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface ReferenceDataApiService {

    @GET("api/v1/main_categories")
    suspend fun getMainCategories(
        @Header("Authorization") authToken: String? = null
    ): Response<MainCategoriesResponse>

    @GET("api/v1/main_categories/{mainCategoryId}/sub_categories")
    suspend fun getSubCategories(
        @Path("mainCategoryId") mainCategoryId: Int,
        @Header("Authorization") authToken: String? = null
    ): Response<SubCategoriesResponse>
    @GET("api/v1/item_conditions")
    suspend fun getItemConditions(
        @Header("Authorization") authToken: String? = null
    ): Response<List<ItemConditionDto>>

    @GET("api/v1/item_sizes")
    suspend fun getItemSizes(
        @Header("Authorization") authToken: String? = null
    ): Response<List<ItemSizeDto>>
    @GET("api/v1/provinces")
    suspend fun getProvinces(
        @Header("Authorization") authToken: String? = null
    ): Response<List<ProvinceDto>>
    @GET("api/v1/brands")
    suspend fun getBrands(
        @Header("Authorization") authToken: String? = null
    ): Response<List<BrandDto>>
    @GET("api/v1/towns")
    suspend fun getTowns(
        @Query("province_id") provinceId: Int,
        @Header("Authorization") authToken: String? = null
    ): Response<List<TownDto>>
    @GET("api/v1/schools")
    suspend fun getSchools(
        @Header("Authorization") authToken: String? = null
    ): Response<List<SchoolDto>>
    @GET("api/v1/item_colors")
    suspend fun getItemColors(
        @Header("Authorization") authToken: String? = null
    ): Response<List<ItemColorDto>>

    @GET("api/v1/genders")
    suspend fun getGenders(
        @Header("Authorization") authToken: String? = null
    ): Response<List<GenderDto>>

    @GET("api/v1/tags")
    suspend fun getTags(
        @Header("Authorization") authToken: String? = null
    ): Response<List<TagDto>>

    @GET("api/v1/locations")
    suspend fun getLocations(
        @Header("Authorization") authToken: String? = null
    ): Response<LocationsResponse>
    @GET("api/v1/all_reference_data")
    suspend fun getAllReferenceData(
        @Header("Authorization") authToken: String? = null
    ): Response<AllReferenceDataResponse>

}