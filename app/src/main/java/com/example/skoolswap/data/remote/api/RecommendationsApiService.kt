package com.example.skoolswap.data.remote.api

import com.example.skoolswap.data.remote.models.response.home.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RecommendationsApiService {

    // ================================================================
    // 🔥 NEW RANKED ENDPOINTS (USE THESE!)
    // ================================================================

    @GET("api/v1/recommendations/uniform/ranked")
    suspend fun getUniformsRanked(
        @Query("school_id") schoolId: Int,
        @Query("gender") gender: String? = null,
        @Query("sub_category_id") subCategoryId: Int? = null,
        @Query("min_price") minPrice: Float? = null,
        @Query("max_price") maxPrice: Float? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<RankedItemsResponse>

    @GET("api/v1/recommendations/sport/ranked")
    suspend fun getSportItemsRanked(
        @Query("school_id") schoolId: Int,
        @Query("sport_type") sportType: String? = null,
        @Query("sub_category_id") subCategoryId: Int? = null,
        @Query("min_price") minPrice: Float? = null,
        @Query("max_price") maxPrice: Float? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<RankedItemsResponse>

    @GET("api/v1/recommendations/recent/ranked")
    suspend fun getRecentItemsRanked(
        @Query("school_id") schoolId: Int,
        @Query("period") period: String? = null,
        @Query("min_price") minPrice: Float? = null,
        @Query("max_price") maxPrice: Float? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<RankedItemsResponse>

    @GET("api/v1/recommendations/search/ranked")
    suspend fun searchItemsRanked(
        @Query("query") query: String,
        @Query("school_id") schoolId: Int,
        @Query("category_id") categoryId: Int? = null,
        @Query("sub_category_id") subCategoryId: Int? = null,
        @Query("min_price") minPrice: Float? = null,
        @Query("max_price") maxPrice: Float? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<RankedItemsResponse>

    @GET("api/v1/recommendations/recommended/ranked")
    suspend fun getRecommendedRanked(
        @Query("school_id") schoolId: Int,
        @Query("category_id") categoryId: Int? = null,
        @Query("period") period: String? = null,
        @Query("min_price") minPrice: Float? = null,
        @Query("max_price") maxPrice: Float? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<RankedItemsResponse>

    @GET("api/v1/recommendations/trending/ranked")
    suspend fun getTrendingRanked(
        @Query("school_id") schoolId: Int,
        @Query("period") period: String = "today",
        @Query("min_price") minPrice: Float? = null,
        @Query("max_price") maxPrice: Float? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<RankedItemsResponse>

    @GET("api/v1/recommendations/essentials/ranked")
    suspend fun getEssentialsRanked(
        @Query("school_id") schoolId: Int,
        @Query("category") category: String? = null,
        @Query("sub_category_id") subCategoryId: Int? = null,
        @Query("min_price") minPrice: Float? = null,
        @Query("max_price") maxPrice: Float? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<RankedItemsResponse>

    // ================================================================
    // ⚠️ LEGACY ENDPOINTS (DEPRECATED - Remove gradually)
    // ================================================================

    @GET("api/v1/recommendations/home")
    suspend fun getHomeFeed(
        @Query("school_id") schoolId: Int
    ): Response<HomeRecommendationResponse>

    @Deprecated("Use getUniformsRanked instead")
    @GET("api/v1/recommendations/uniform")
    suspend fun getUniforms(
        @Query("school_id") schoolId: Int,
        @Query("gender") gender: String? = null
    ): Response<UniformRecommendationResponse>

    @Deprecated("Use getSportItemsRanked instead")
    @GET("api/v1/recommendations/sport")
    suspend fun getSportItems(
        @Query("school_id") schoolId: Int,
        @Query("sport_type") sportType: String? = null
    ): Response<SportRecommendationResponse>

    @Deprecated("Use getRecentItemsRanked instead")
    @GET("api/v1/recommendations/recent")
    suspend fun getRecentItems(
        @Query("school_id") schoolId: Int,
        @Query("period") period: String? = null
    ): Response<RecentRecommendationResponse>

    @Deprecated("Use getRecommendedRanked instead")
    @GET("api/v1/recommendations/recommended/all")
    suspend fun getRecommendedAll(
        @Query("school_id") schoolId: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("category_id") categoryId: Int? = null,
        @Query("condition_id") conditionId: Int? = null,
        @Query("min_price") minPrice: Float? = null,
        @Query("max_price") maxPrice: Float? = null
    ): Response<PaginatedItemsResponse>

    @Deprecated("Use getEssentialsRanked instead")
    @GET("api/v1/recommendations/essentials/all")
    suspend fun getEssentialsAll(
        @Query("school_id") schoolId: Int,
        @Query("category") category: String? = null,
        @Query("sub_category_id") subCategoryId: Int? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("condition_id") conditionId: Int? = null,
        @Query("min_price") minPrice: Float? = null,
        @Query("max_price") maxPrice: Float? = null
    ): Response<PaginatedItemsResponse>

    @Deprecated("Use getTrendingRanked instead")
    @GET("api/v1/recommendations/trending/all")
    suspend fun getTrendingAll(
        @Query("school_id") schoolId: Int,
        @Query("period") period: String = "today",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("category_id") categoryId: Int? = null,
        @Query("condition_id") conditionId: Int? = null,
        @Query("min_price") minPrice: Float? = null,
        @Query("max_price") maxPrice: Float? = null
    ): Response<PaginatedItemsResponse>

    @Deprecated("Use getRecentItemsRanked instead")
    @GET("api/v1/recommendations/recent/all")
    suspend fun getRecentAll(
        @Query("school_id") schoolId: Int,
        @Query("period") period: String = "all",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("category_id") categoryId: Int? = null,
        @Query("condition_id") conditionId: Int? = null,
        @Query("min_price") minPrice: Float? = null,
        @Query("max_price") maxPrice: Float? = null
    ): Response<PaginatedItemsResponse>

    // ================================================================
    // TRACKING ENDPOINTS (Keep as-is)
    // ================================================================

    @POST("api/v1/recommendations/track_view")
    suspend fun trackView(
        @Query("item_id") itemId: String,
        @Query("source") source: String
    ): Response<Unit>

    @POST("api/v1/recommendations/track_click")
    suspend fun trackClick(
        @Query("item_id") itemId: String,
        @Query("source") source: String,
        @Query("position") position: Int
    ): Response<Unit>

    @Deprecated("Use searchItemsRanked instead")
    @GET("api/v1/items")
    suspend fun searchItems(
        @Query("school_id") schoolId: Int,
        @Query("q") query: String,
        @Query("main_category_id") categoryId: Int? = null,
        @Query("gender_id") genderId: Int? = null,
        @Query("brand_id") brandId: Int? = null,
        @Query("size_id") sizeId: Int? = null,
        @Query("color_id") colorId: Int? = null,
        @Query("condition_id") conditionId: Int? = null,
        @Query("min_price") minPrice: Float? = null,
        @Query("max_price") maxPrice: Float? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): Response<RecommendedItemsResponse>
}