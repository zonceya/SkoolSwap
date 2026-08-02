package za.co.skoolswap.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import za.co.skoolswap.SkoolSwapApplication
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.model.homefeed.Section
import za.co.skoolswap.domain.repository.HomeRepositoryInterface
import za.co.skoolswap.domain.repository.ItemRepositoryInterface
import za.co.skoolswap.domain.repository.ProductsCacheRepositoryInterface
import kotlinx.coroutines.flow.first
import timber.log.Timber

class ItemCacheWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val itemRepository: ItemRepositoryInterface by lazy {
        (applicationContext as SkoolSwapApplication).itemRepository
    }

    private val homeRepository: HomeRepositoryInterface by lazy {
        (applicationContext as SkoolSwapApplication).homeRepository
    }

    private val productsCacheRepository: ProductsCacheRepositoryInterface by lazy {
        (applicationContext as SkoolSwapApplication).productsCacheRepository
    }

    companion object {
        private const val TAG = "ItemCacheWorker"

        fun createOneTimeRequest(itemId: String): OneTimeWorkRequest {
            return OneTimeWorkRequest.Builder(ItemCacheWorker::class.java)
                .setInputData(
                    androidx.work.Data.Builder()
                        .putString("itemId", itemId)
                        .build()
                )
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
        val itemId = inputData.getString("itemId") ?: return ListenableWorker.Result.failure()

        return try {
            Timber.tag(TAG).d("🔄 Caching item: $itemId")

            val result = itemRepository.getItem(itemId)

            // ✅ Use getOrNull to avoid Result.Success conflict
            val item = result.getOrNull()
            if (item != null) {
                // 1. Cache item details
                productsCacheRepository.cacheItemDetail(itemId, item)
                Timber.tag(TAG).d("✅ Cached item: ${item.name}")

                // 2. Get and cache similar items
                try {
                    val similarItems = getSimilarItems(item)
                    if (similarItems.isNotEmpty()) {
                        productsCacheRepository.cacheSimilarItems(itemId, similarItems)
                        Timber.tag(TAG).d("✅ Cached ${similarItems.size} similar items")
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "❌ Failed to cache similar items")
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Timber.tag(TAG).e("❌ Failed to cache item: $error")
            }

            ListenableWorker.Result.success()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Item cache worker failed")
            ListenableWorker.Result.retry()
        }
    }

    private suspend fun getSimilarItems(item: Item): List<Item> {
        try {
            // Get schoolId from preferences
            val schoolId = (applicationContext as SkoolSwapApplication).appPreferences.schoolId.first()
            if (schoolId == null || schoolId <= 0) {
                Timber.tag(TAG).d("No school ID available for similar items")
                return emptyList()
            }

            // Get home feed
            val feedResult = homeRepository.getHomeFeed(schoolId)

            // ✅ Explicitly check using when with fully qualified names
            return when (feedResult) {
                is za.co.skoolswap.utils.Result.Success -> {
                    val feed = feedResult.data
                    val allItems = mutableListOf<Item>()

                    // ✅ Explicit type for section
                    feed.sections.forEach { section: Section ->
                        when (section) {
                            is Section.Recommended -> allItems.addAll(section.items)
                            is Section.Trending -> allItems.addAll(section.items)
                            is Section.Recent -> allItems.addAll(section.items)
                            is Section.Essentials -> {
                                allItems.addAll(section.sections.uniforms)
                                allItems.addAll(section.sections.sports)
                                allItems.addAll(section.sections.accessories)
                            }
                        }
                    }

                    // Filter out the current item and take first 6
                    val similar = allItems
                        .filter { it.id != item.id }
                        .distinctBy { it.id }
                        .take(6)

                    Timber.tag(TAG).d("Found ${similar.size} similar items for ${item.name}")
                    similar
                }
                is za.co.skoolswap.utils.Result.Error -> {
                    Timber.tag(TAG).e("Failed to get home feed: ${feedResult.exception.message}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to get similar items")
            return emptyList()
        }
    }
}