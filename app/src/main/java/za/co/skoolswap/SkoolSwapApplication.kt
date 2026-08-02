package za.co.skoolswap

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import za.co.skoolswap.data.local.database.SkoolSwapDatabase
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.data.repository.OfflineQueueRepository
import za.co.skoolswap.domain.repository.AuthRepositoryInterface
import za.co.skoolswap.domain.repository.FilterRepositoryInterface
import za.co.skoolswap.domain.repository.HomeRepositoryInterface
import za.co.skoolswap.domain.repository.ItemRepositoryInterface
import za.co.skoolswap.domain.repository.ProductsCacheRepositoryInterface
import za.co.skoolswap.domain.repository.ShopRepositoryInterface
import za.co.skoolswap.workers.WorkerManager
import com.google.firebase.FirebaseApp
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class SkoolSwapApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var authRepository: AuthRepositoryInterface

    @Inject
    lateinit var homeRepository: HomeRepositoryInterface

    @Inject
    lateinit var offlineQueueRepository: OfflineQueueRepository

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    @Inject
    lateinit var itemRepository: ItemRepositoryInterface

    @Inject
    lateinit var productsCacheRepository: ProductsCacheRepositoryInterface

    @Inject
    lateinit var filterRepository: FilterRepositoryInterface  // ✅ Add this

    @Inject
    lateinit var shopRepository: ShopRepositoryInterface  // ✅ Add this

    @Inject
    lateinit var workerManagerLazy: Lazy<WorkerManager>

    lateinit var database: SkoolSwapDatabase
        private set

    // ✅ Application-wide coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // ✅ Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // ✅ Initialize Timber for debugging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        applicationScope.launch {
            filterRepository.warmUpCache()
        }
        // ✅ Initialize Firebase
        FirebaseApp.initializeApp(this)

        // ✅ Initialize Database
        database = SkoolSwapDatabase.getInstance(this)

        // ✅ Schedule ALL workers on app start
        workerManagerLazy.get().scheduleAllWorkers()

        // ✅ Monitor app lifecycle for sync
        setupAppLifecycleObserver()

        // ✅ PRELOAD ALL DATA immediately (with small delay to let UI settle)
        applicationScope.launch {
            delay(500) // Small delay to let UI settle
            preloadAllData()
        }

        Timber.d("✅ SkoolSwapApplication initialized")
    }

    /**
     * ✅ PRELOAD EVERYTHING in parallel
     * This runs when the app starts and caches all critical data
     */
    private suspend fun preloadAllData() {
        Timber.d("🔄 Starting preload of ALL data...")
        val startTime = System.currentTimeMillis()

        try {
            // ✅ Run all preloads in parallel
            coroutineScope {
                // 1. Preload category filters (Uniforms, Sports, Accessories, Books)
                launch {
                    try {
                        Timber.d("  📂 Preloading category filters...")
                        filterRepository.preloadCategoryFilters(listOf(1, 2, 3, 4))
                        Timber.d("  ✅ Category filters preloaded")
                    } catch (e: Exception) {
                        Timber.e(e, "  ❌ Failed to preload filters")
                    }
                }

                // 2. Preload shop data
                launch {
                    try {
                        Timber.d("  🏪 Preloading shop data...")
                        val shopResult = shopRepository.getMyShop()
                        if (shopResult.isSuccess) {
                            Timber.d("  ✅ Shop data preloaded")
                        } else {
                            Timber.d("  ⚠️ Shop not available yet (user may not have shop)")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "  ❌ Failed to preload shop")
                    }
                }

                // 3. Preload shop items
                launch {
                    try {
                        Timber.d("  📦 Preloading shop items...")
                        val itemsResult = shopRepository.getMyShopItems()
                        if (itemsResult.isSuccess) {
                            Timber.d("  ✅ Shop items preloaded")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "  ❌ Failed to preload shop items")
                    }
                }

                // 4. Preload products cache
                launch {
                    try {
                        Timber.d("  🗂️ Preloading products cache...")
                        productsCacheRepository.preload()
                        Timber.d("  ✅ Products cache preloaded")
                    } catch (e: Exception) {
                        Timber.e(e, "  ❌ Failed to preload products cache")
                    }
                }

                // 5. Preload home feed (if school is selected)
                launch {
                    try {
                        val schoolId = appPreferences.schoolId.first()
                        if (schoolId != null && schoolId > 0) {
                            Timber.d("  🏠 Preloading home feed for school: $schoolId...")
                            homeRepository.getHomeFeed(schoolId)
                            Timber.d("  ✅ Home feed preloaded")
                        } else {
                            Timber.d("  ⏭️ No school selected, skipping home feed preload")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "  ❌ Failed to preload home feed")
                    }
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            Timber.d("✅ All data preloaded in ${elapsed}ms")

        } catch (e: Exception) {
            Timber.e(e, "❌ Error during preload")
        }
    }

    /**
     * ✅ Monitor app foreground/background for sync
     */
    private fun setupAppLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            private var lastSyncTime = 0L
            private val MIN_SYNC_INTERVAL_MS = 30000L // 30 seconds

            override fun onStart(owner: LifecycleOwner) {
                Timber.d("🔄 App came to FOREGROUND")

                val now = System.currentTimeMillis()
                if (now - lastSyncTime > MIN_SYNC_INTERVAL_MS) {
                    lastSyncTime = now

                    Timber.d("📡 Syncing data (app foreground)")
                    val workerManager = workerManagerLazy.get()
                    workerManager.refreshTokenNow()
                    workerManager.syncHomeFeedNow()
                    workerManager.scheduleProductsSync()
                    workerManager.cleanupImagesNow()
                } else {
                    Timber.d("⏭️ Skipping sync - too soon (${now - lastSyncTime}ms since last sync)")
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                Timber.d("📴 App went to BACKGROUND")
            }
        })
    }
}