// data/remote/api/RecommendationsApi.kt
package com.example.skoolswap.data.remote.api


import com.example.skoolswap.data.remote.models.response.home.HomeRecommendationResponse
import com.example.skoolswap.data.remote.models.response.home.RecentRecommendationResponse
import com.example.skoolswap.data.remote.models.response.home.SportRecommendationResponse
import com.example.skoolswap.data.remote.models.response.home.UniformRecommendationResponse
import retrofit2.Response
import retrofit2.http.GET
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
}