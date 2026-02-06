// di/AppModule.kt
package com.example.skoolswap.di

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CredentialManager
import com.example.skoolswap.data.local.database.SkoolSwapDatabase
import com.example.skoolswap.data.local.database.dao.ItemTypeDao
import com.example.skoolswap.data.local.database.dao.ShopDao
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.data.remote.api.ItemApiService
import com.example.skoolswap.data.remote.api.RetrofitClient
import com.example.skoolswap.data.remote.api.ShopApiService
import com.example.skoolswap.data.remote.api.UserApiService
import com.example.skoolswap.data.repository.AuthRepository
import com.example.skoolswap.data.repository.ItemRepository
import com.example.skoolswap.data.repository.ShopRepository
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.domain.repository.ItemRepositoryInterface
import com.example.skoolswap.domain.repository.ShopRepositoryInterface
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SkoolSwapDatabase {
        return SkoolSwapDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideUserApiService(): UserApiService {
        return RetrofitClient.instance.create(UserApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideShopApiService(): ShopApiService {
        return RetrofitClient.instance.create(ShopApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideItemApiService(): ItemApiService {
        return RetrofitClient.instance.create(ItemApiService::class.java)
    }

    // DAOs - Keep only non-item DAOs here
    @Provides
    @Singleton
    fun provideShopDao(database: SkoolSwapDatabase): ShopDao {
        return database.shopDao()
    }

    // ✅ REMOVED: provideItemDao() - it's in ItemModule.kt

    @Provides
    @Singleton
    fun provideItemTypeDao(database: SkoolSwapDatabase): ItemTypeDao {
        return database.itemTypeDao()
    }

    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager {
        return CredentialManager.create(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        return AppPreferences(context)
    }

    // REPOSITORIES
    @RequiresApi(Build.VERSION_CODES.O)
    @Provides
    @Singleton
    fun provideShopRepository(
        shopApiService: ShopApiService,
        shopDao: ShopDao,
        authRepository: AuthRepositoryInterface
    ): ShopRepositoryInterface {
        return ShopRepository(
            shopApiService = shopApiService,
            shopDao = shopDao,
            authRepository = authRepository as AuthRepository
        )
    }

    @Provides
    @Singleton
    fun provideItemRepository(
        itemApiService: ItemApiService,
        // ItemDao will be injected from ItemModule.kt
        itemDao: com.example.skoolswap.data.local.database.dao.ItemDao,
        authRepository: AuthRepositoryInterface
    ): ItemRepositoryInterface {
        return ItemRepository(
            itemApiService = itemApiService,
            itemDao = itemDao,
            authRepository = authRepository
        )
    }
}