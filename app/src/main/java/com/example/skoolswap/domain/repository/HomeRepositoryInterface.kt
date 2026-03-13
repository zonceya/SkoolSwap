package com.example.skoolswap.domain.repository

import com.example.skoolswap.domain.model.HomeFeed
import com.example.skoolswap.domain.model.RecentFeed
import com.example.skoolswap.domain.model.SportFeed
import com.example.skoolswap.domain.model.UniformFeed
import com.example.skoolswap.utils.Result
import kotlinx.coroutines.flow.StateFlow

interface HomeRepositoryInterface {
    // StateFlow for home feed data
    val homeFeed: StateFlow<HomeFeed?>

    // Get home feed
    suspend fun getHomeFeed(schoolId: Int): Result<HomeFeed>

    // Get uniforms
    suspend fun getUniforms(schoolId: Int, gender: String? = null): Result<UniformFeed>

    // Get sport items
    suspend fun getSportItems(schoolId: Int, sportType: String? = null): Result<SportFeed>

    // Get recent items
    suspend fun getRecentItems(schoolId: Int, period: String? = null): Result<RecentFeed>

    // Clear home data (on logout/school change)
    suspend fun clearHomeData()
}