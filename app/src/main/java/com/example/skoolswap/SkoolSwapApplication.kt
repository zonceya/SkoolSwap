package com.example.skoolswap

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.skoolswap.BuildConfig  // ✅ FIXED import
import com.example.skoolswap.data.local.database.SkoolSwapDatabase
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.repository.OfflineQueueRepository
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.HomeRepositoryInterface
import com.example.skoolswap.workers.WorkerManager
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject
import com.example.skoolswap.domain.repository.ItemRepositoryInterface  // ← ADD THIS
import com.example.skoolswap.domain.repository.ProductsCacheRepositoryInterface

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
    lateinit var workerManagerLazy: dagger.Lazy<WorkerManager>

    lateinit var database: SkoolSwapDatabase
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()

    override fun onCreate() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate()
             // Timber Debug Tree
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        appPreferences = AppPreferences(this)
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Database
        database = SkoolSwapDatabase.getInstance(this)

        // ✅ Schedule ALL workers on app start
        workerManagerLazy.get().scheduleAllWorkers()

        Timber.d("✅ SkoolSwapApplication initialized")
    }
}