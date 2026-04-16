// di/AppModule.kt
package com.example.skoolswap.di

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CredentialManager
import com.example.skoolswap.data.local.database.SkoolSwapDatabase
import com.example.skoolswap.data.local.database.dao.ItemDao
import com.example.skoolswap.data.local.database.dao.ProvinceDao
import com.example.skoolswap.data.local.database.dao.SchoolDao
import com.example.skoolswap.data.local.database.dao.ShopDao
import com.example.skoolswap.data.local.database.dao.UserDao
import com.example.skoolswap.data.local.database.dao.UserSchoolDao
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.remote.api.FilterApiService
import com.example.skoolswap.data.remote.api.ItemApiService
import com.example.skoolswap.data.remote.api.ProvinceApiService
import com.example.skoolswap.data.remote.api.RecommendationsApiService
import com.example.skoolswap.data.remote.api.ReferenceDataApiService
import com.example.skoolswap.data.remote.api.RetrofitClient
import com.example.skoolswap.data.remote.api.SchoolApiService
import com.example.skoolswap.data.remote.api.ShopApiService
import com.example.skoolswap.data.remote.api.UserApiService
import com.example.skoolswap.data.remote.api.UserSchoolApiService
import com.example.skoolswap.data.repository.AuthRepository
import com.example.skoolswap.data.repository.FilterRepository
import com.example.skoolswap.data.repository.HomeRepository
import com.example.skoolswap.data.repository.ItemRepository
import com.example.skoolswap.data.repository.SchoolRepository
import com.example.skoolswap.data.repository.ShopRepository
import com.example.skoolswap.data.repository.UserSchoolRepository
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.FilterRepositoryInterface
import com.example.skoolswap.domain.repository.HomeRepositoryInterface
import com.example.skoolswap.domain.repository.ItemRepositoryInterface
import com.example.skoolswap.domain.repository.ShopRepositoryInterface
import com.example.skoolswap.domain.repository.UserSchoolRepositoryInterface
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SkoolSwapDatabase {
        return SkoolSwapDatabase.getInstance(context)
    }

    // ========== DAO PROVIDERS ==========
    @Provides
    @Singleton
    fun provideUserDao(database: SkoolSwapDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideSchoolDao(database: SkoolSwapDatabase): SchoolDao {
        return database.schoolDao()
    }

    @Provides
    @Singleton
    fun provideUserSchoolDao(database: SkoolSwapDatabase): UserSchoolDao {
        return database.userSchoolDao()
    }

    @Provides
    @Singleton
    fun provideProvinceDao(database: SkoolSwapDatabase): ProvinceDao {
        return database.provinceDao()
    }

    @Provides
    @Singleton
    fun provideShopDao(database: SkoolSwapDatabase): ShopDao {
        return database.shopDao()
    }

    // ========== API SERVICES ==========
    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }
    @Provides
    @Singleton
    fun provideShopApiService(retrofit: Retrofit): ShopApiService {
        return retrofit.create(ShopApiService::class.java)
    }
    @Provides
    @Singleton
    fun provideItemApiService(retrofit: Retrofit): ItemApiService {  // ← Inject Retrofit
        return retrofit.create(ItemApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideProvinceApiService(): ProvinceApiService = RetrofitClient.instance.create(ProvinceApiService::class.java)

    @Provides
    @Singleton
    fun provideSchoolApiService(): SchoolApiService = RetrofitClient.instance.create(SchoolApiService::class.java)

    @Provides
    @Singleton
    fun provideUserSchoolApiService(): UserSchoolApiService = RetrofitClient.instance.create(UserSchoolApiService::class.java)

    @Provides
    @Singleton
    fun provideReferenceDataApiService(): ReferenceDataApiService {
        return RetrofitClient.instance.create(ReferenceDataApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRecommendationsApiService(retrofit: Retrofit): RecommendationsApiService {
        return retrofit.create(RecommendationsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideFilterApiService(retrofit: Retrofit): FilterApiService {
        return retrofit.create(FilterApiService::class.java)
    }

    // ========== OTHER DEPENDENCIES ==========
    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager =
        CredentialManager.create(context)

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences =
        AppPreferences(context)

    // REMOVED: Gson provider (already in NetworkModule)

    // ========== REPOSITORY PROVIDERS ==========
    @Provides
    @Singleton
    fun provideSchoolRepository(
        schoolApiService: SchoolApiService,
        provinceApiService: ProvinceApiService,
        provinceDao: ProvinceDao,
        appPreferences: AppPreferences
    ): SchoolRepository {
        return SchoolRepository(
            schoolApiService = schoolApiService,
            provinceApiService = provinceApiService,
            appPreferences = appPreferences,
            provinceDao = provinceDao
        )
    }

    @Provides
    @Singleton
    fun provideUserSchoolRepository(
        userSchoolApiService: UserSchoolApiService,
        userSchoolDao: UserSchoolDao,
        schoolDao: SchoolDao,
        provinceDao: ProvinceDao,
        appPreferences: AppPreferences
    ): UserSchoolRepository {
        return UserSchoolRepository(
            userSchoolApiService = userSchoolApiService,
            userSchoolDao = userSchoolDao,
            schoolDao = schoolDao,
            provinceDao = provinceDao,
            appPreferences = appPreferences
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Provides
    @Singleton
    fun provideShopRepository(
        shopApiService: ShopApiService,
        shopDao: ShopDao,
        authRepository: AuthRepositoryInterface,
        itemRepository: ItemRepositoryInterface
    ): ShopRepositoryInterface {
        return ShopRepository(
            shopApiService = shopApiService,
            shopDao = shopDao,
            authRepository = authRepository as AuthRepository,
            itemRepository = itemRepository
        )
    }

    @Provides
    @Singleton
    fun provideItemRepository(
        itemApiService: ItemApiService,
        itemDao: ItemDao,
        authRepository: AuthRepositoryInterface
    ): ItemRepositoryInterface {
        return ItemRepository(
            itemApiService = itemApiService,
            itemDao = itemDao,
            authRepository = authRepository
        )
    }

    @Provides
    @Singleton
    fun provideHomeRepository(
        recommendationsApiService: RecommendationsApiService,
        authRepository: AuthRepositoryInterface
    ): HomeRepositoryInterface {
        return HomeRepository(
            recommendationsApiService = recommendationsApiService,
            authRepository = authRepository
        )
    }

    @Provides
    @Singleton
    fun provideFilterRepository(
        filterApiService: FilterApiService,
        authRepository: AuthRepositoryInterface,
        appPreferences: AppPreferences,
        gson: Gson  // This Gson comes from NetworkModule
    ): FilterRepositoryInterface {
        return FilterRepository(
            api = filterApiService,
            authRepository = authRepository,
            appPreferences = appPreferences

        )
    }
}