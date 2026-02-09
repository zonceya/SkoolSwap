// di/ItemModule.kt
package com.example.skoolswap.di

import com.example.skoolswap.data.local.database.SkoolSwapDatabase
import com.example.skoolswap.data.local.database.dao.ItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ItemModule {

    @Provides
    @Singleton
    fun provideItemDao(database: SkoolSwapDatabase): ItemDao {
        return database.itemDao()
    }

    // ✅ REMOVED: provideItemTypeDao() - it's in AppModule.kt
    // Keep ItemDao only here
}