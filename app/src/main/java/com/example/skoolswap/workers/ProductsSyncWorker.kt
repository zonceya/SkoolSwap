package com.example.skoolswap.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import com.example.skoolswap.SkoolSwapApplication
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.domain.model.homefeed.Section
import com.example.skoolswap.domain.repository.HomeRepositoryInterface
import com.example.skoolswap.domain.repository.ProductsCacheRepositoryInterface
import com.example.skoolswap.utils.Result
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Syncs ALL product sections: Trending, Recent, Uniforms, Sports
 * Runs on app open/login
 */
class ProductsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val homeRepository: HomeRepositoryInterface by lazy {
        (applicationContext as SkoolSwapApplication).homeRepository
    }

    private val appPreferences: AppPreferences by lazy {
        (applicationContext as SkoolSwapApplication).appPreferences
    }

    private val productsCacheRepository: ProductsCacheRepositoryInterface by lazy {
        (applicationContext as SkoolSwapApplication).productsCacheRepository
    }

    companion object {
        private const val TAG = "ProductsSyncWorker"

        fun createOneTimeRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequest.Builder(ProductsSyncWorker::class.java)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .addTag(TAG)
                .build()
        }
    }

    override suspend fun doWork(): ListenableWorker.Result {
        return try {
            Timber.tag(TAG).d("🔄 Products sync started")

            val schoolId = appPreferences.schoolId.first()
            if (schoolId == null || schoolId <= 0) {
                Timber.tag(TAG).d("⏭️ No school selected, skipping sync")
                return ListenableWorker.Result.success()
            }

            // 1. Sync TRENDING
            syncTrending(schoolId)

            // 2. Sync RECENT
            syncRecent(schoolId)

            // 3. Sync UNIFORMS
            syncUniforms(schoolId)

            // 4. Sync SPORTS
            syncSports(schoolId)

            Timber.tag(TAG).d("✅ Products sync completed")
            ListenableWorker.Result.success()

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Products sync worker failed")
            ListenableWorker.Result.retry()
        }
    }

    // ==================== SYNC TRENDING ====================
    private suspend fun syncTrending(schoolId: Int) {
        try {
            val result = homeRepository.getHomeFeed(schoolId)

            // ✅ Use fully qualified names
            when (result) {
                is com.example.skoolswap.utils.Result.Success -> {
                    val feed = result.data
                    val trendingSection = feed.sections.find { it is Section.Trending }
                    if (trendingSection is Section.Trending) {
                        productsCacheRepository.cacheProducts(
                            cacheKey = "trending_${schoolId}",
                            items = trendingSection.items,
                            schoolId = schoolId,
                            sectionType = "trending"
                        )
                        val count = trendingSection.items.size
                        Timber.tag(TAG).d("✅ Cached $count trending items")
                    }
                }
                is com.example.skoolswap.utils.Result.Error -> {
                    Timber.tag(TAG).e("❌ Failed to sync trending: ${result.exception.message}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed to sync trending")
        }
    }

    // ==================== SYNC RECENT ====================
    private suspend fun syncRecent(schoolId: Int) {
        try {
            val result = homeRepository.getRecentItems(schoolId, "week")

            when (result) {
                is com.example.skoolswap.utils.Result.Success -> {
                    val feed = result.data
                    val items = feed.sections.flatMap { it.items }
                    productsCacheRepository.cacheProducts(
                        cacheKey = "recent_${schoolId}",
                        items = items,
                        schoolId = schoolId,
                        sectionType = "recent",
                        period = "week"
                    )
                    val count = items.size
                    Timber.tag(TAG).d("✅ Cached $count recent items")
                }
                is com.example.skoolswap.utils.Result.Error -> {
                    Timber.tag(TAG).e("❌ Failed to sync recent: ${result.exception.message}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed to sync recent")
        }
    }

    // ==================== SYNC UNIFORMS ====================
    private suspend fun syncUniforms(schoolId: Int) {
        try {
            val result = homeRepository.getUniforms(schoolId, null)

            when (result) {
                is com.example.skoolswap.utils.Result.Success -> {
                    val feed = result.data
                    val items = feed.sections.flatMap { it.items }
                    productsCacheRepository.cacheProducts(
                        cacheKey = "uniforms_${schoolId}",
                        items = items,
                        schoolId = schoolId,
                        sectionType = "uniforms"
                    )
                    val count = items.size
                    Timber.tag(TAG).d("✅ Cached $count uniform items")
                }
                is com.example.skoolswap.utils.Result.Error -> {
                    Timber.tag(TAG).e("❌ Failed to sync uniforms: ${result.exception.message}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed to sync uniforms")
        }
    }

    // ==================== SYNC SPORTS ====================
    private suspend fun syncSports(schoolId: Int) {
        try {
            val result = homeRepository.getSportItems(schoolId, null)

            when (result) {
                is com.example.skoolswap.utils.Result.Success -> {
                    val feed = result.data
                    val items = feed.sections.flatMap { it.items }
                    productsCacheRepository.cacheProducts(
                        cacheKey = "sports_${schoolId}",
                        items = items,
                        schoolId = schoolId,
                        sectionType = "sports"
                    )
                    val count = items.size
                    Timber.tag(TAG).d("✅ Cached $count sport items")
                }
                is com.example.skoolswap.utils.Result.Error -> {
                    Timber.tag(TAG).e("❌ Failed to sync sports: ${result.exception.message}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed to sync sports")
        }
    }
}