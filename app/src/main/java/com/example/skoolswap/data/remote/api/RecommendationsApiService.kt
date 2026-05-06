package com.example.skoolswap.data.remote.api

import com.example.skoolswap.data.remote.models.response.home.HomeRecommendationResponse
import com.example.skoolswap.data.remote.models.response.home.RecentRecommendationResponse
import com.example.skoolswap.data.remote.models.response.home.SportRecommendationResponse
import com.example.skoolswap.data.remote.models.response.home.UniformRecommendationResponse
import com.example.skoolswap.data.remote.models.response.home.PaginatedItemsResponse
import com.example.skoolswap.data.remote.models.response.home.RecommendedItemsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RecommendationsApiService {

    @GET("api/v1/recommendations/home")
    suspend fun getHomeFeed(
        @Query("school_id") schoolId: Int
    ): Response<HomeRecommendationResponse>

    @GET("api/v1/recommendations/uniform")
    suspend fun getUniforms(
        @Query("school_id") schoolId: Int,
        @Query("gender") gender: String? = null
    ): Response<UniformRecommendationResponse>

    @GET("api/v1/recommendations/sport")
    suspend fun getSportItems(
        @Query("school_id") schoolId: Int,
        @Query("sport_type") sportType: String? = null
    ): Response<SportRecommendationResponse>

    @GET("api/v1/recommendations/recent")
    suspend fun getRecentItems(
        @Query("school_id") schoolId: Int,
        @Query("period") period: String? = null
    ): Response<RecentRecommendationResponse>

    // ============ "VIEW ALL" PAGINATED ENDPOINTS ============

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

    @GET("api/v1/recommendations/essentials/all")
    suspend fun getEssentialsAll(
        @Query("school_id") schoolId: Int,
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("condition_id") conditionId: Int? = null,
        @Query("min_price") minPrice: Float? = null,
        @Query("max_price") maxPrice: Float? = null
    ): Response<PaginatedItemsResponse>

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

    // ============ TRACKING ENDPOINTS ============

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
    @GET("api/v1/items")
    suspend fun searchItems(
        @Query("school_id") schoolId: Int,
        @Query("q") query: String,
        @Query("main_category_id") categoryId: Int? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<RecommendedItemsResponse>
}