package za.co.skoolswap.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import za.co.skoolswap.SkoolSwapApplication
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.model.homefeed.Section
import za.co.skoolswap.domain.repository.HomeRepositoryInterface
import za.co.skoolswap.domain.repository.ProductsCacheRepositoryInterface
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Syncs ALL product sections using the reliable home feed endpoint
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

            // ✅ SINGLE RELIABLE CALL - get everything from home feed
            when (val result = homeRepository.getHomeFeed(schoolId)) {
                is za.co.skoolswap.utils.Result.Success -> {
                    val feed = result.data
                    Timber.tag(TAG).d("📡 Got ${feed.sections.size} sections from home feed")

                    // Cache each section individually
                    feed.sections.forEach { section ->
                        when (section) {
                            is Section.Trending -> {
                                cacheSection("trending", schoolId, section.items)
                            }
                            is Section.Recent -> {
                                cacheSection("recent", schoolId, section.items)
                            }
                            is Section.Recommended -> {
                                cacheSection("recommended", schoolId, section.items)
                            }
                            is Section.Essentials -> {
                                // Combine all essentials sub-sections
                                val allEssentials = section.sections.uniforms +
                                        section.sections.sports +
                                        section.sections.accessories
                                cacheSection("essentials", schoolId, allEssentials)
                            }
                        }
                    }

                    Timber.tag(TAG).d("✅ Products sync completed - ${feed.sections.size} sections cached")
                }
                is za.co.skoolswap.utils.Result.Error -> {
                    Timber.tag(TAG).e("❌ Failed to sync home feed: ${result.exception.message}")
                    // Don't retry on non-network errors, but if it's network, retry
                    if (result.exception is java.io.IOException) {
                        return ListenableWorker.Result.retry()
                    }
                }
            }

            // ✅ Optional: Also sync Uniforms and Sports for their dedicated tabs
            // These are separate endpoints but we handle failures gracefully
            syncUniforms(schoolId)
            syncSports(schoolId)

            ListenableWorker.Result.success()

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Products sync worker failed")
            ListenableWorker.Result.retry()
        }
    }

    // ==================== HELPER: Cache a section ====================
    private suspend fun cacheSection(
        sectionType: String,
        schoolId: Int,
        items: List<Item>
    ) {
        if (items.isEmpty()) {
            Timber.tag(TAG).d("⚠️ No items to cache for $sectionType")
            return
        }

        try {
            productsCacheRepository.cacheProducts(
                cacheKey = "${sectionType}_${schoolId}",
                items = items,
                schoolId = schoolId,
                sectionType = sectionType
            )
            Timber.tag(TAG).d("✅ Cached ${items.size} $sectionType items")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed to cache $sectionType")
        }
    }

    // ==================== SYNC UNIFORMS (for Uniform tab) ====================
    private suspend fun syncUniforms(schoolId: Int) {
        try {
            val result = homeRepository.getUniforms(schoolId, null)
            when (result) {
                is za.co.skoolswap.utils.Result.Success -> {
                    val items = result.data.sections.flatMap { it.items }
                    if (items.isNotEmpty()) {
                        productsCacheRepository.cacheProducts(
                            cacheKey = "uniforms_${schoolId}",
                            items = items,
                            schoolId = schoolId,
                            sectionType = "uniforms"
                        )
                        Timber.tag(TAG).d("✅ Cached ${items.size} uniform items")
                    }
                }
                is za.co.skoolswap.utils.Result.Error -> {
                    // Non-critical - don't fail the whole worker for this
                    Timber.tag(TAG).w("⚠️ Failed to sync uniforms: ${result.exception.message}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed to sync uniforms")
        }
    }

    // ==================== SYNC SPORTS (for Sports tab) ====================
    private suspend fun syncSports(schoolId: Int) {
        try {
            val result = homeRepository.getSportItems(schoolId, null)
            when (result) {
                is za.co.skoolswap.utils.Result.Success -> {
                    val items = result.data.sections.flatMap { it.items }
                    if (items.isNotEmpty()) {
                        productsCacheRepository.cacheProducts(
                            cacheKey = "sports_${schoolId}",
                            items = items,
                            schoolId = schoolId,
                            sectionType = "sports"
                        )
                        Timber.tag(TAG).d("✅ Cached ${items.size} sport items")
                    }
                }
                is za.co.skoolswap.utils.Result.Error -> {
                    // Non-critical - don't fail the whole worker for this
                    Timber.tag(TAG).w("⚠️ Failed to sync sports: ${result.exception.message}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed to sync sports")
        }
    }
}