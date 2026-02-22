// di/ItemModule.kt
package com.example.skoolswap.di

import com.example.skoolswap.data.local.database.SkoolSwapDatabase
import com.example.skoolswap.data.local.database.dao.BrandDao
import com.example.skoolswap.data.local.database.dao.ColorDao
import com.example.skoolswap.data.local.database.dao.ConditionDao
import com.example.skoolswap.data.local.database.dao.GenderDao
import com.example.skoolswap.data.local.database.dao.ItemDao
import com.example.skoolswap.data.local.database.dao.ItemTypeDao
import com.example.skoolswap.data.local.database.dao.LocationDao
import com.example.skoolswap.data.local.database.dao.MainCategoryDao
import com.example.skoolswap.data.local.database.dao.ProvinceDao
import com.example.skoolswap.data.local.database.dao.SchoolDao
import com.example.skoolswap.data.local.database.dao.ShopDao
import com.example.skoolswap.data.local.database.dao.SizeDao
import com.example.skoolswap.data.local.database.dao.SubCategoryDao
import com.example.skoolswap.data.local.database.dao.TagDao
import com.example.skoolswap.data.local.database.dao.TownDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ItemModule {

    // Item DAO
    @Provides
    @Singleton
    fun provideItemDao(database: SkoolSwapDatabase): ItemDao = database.itemDao()

    // Item-related reference DAOs
    @Provides
    @Singleton
    fun provideMainCategoryDao(database: SkoolSwapDatabase): MainCategoryDao = database.mainCategoryDao()

    @Provides
    @Singleton
    fun provideSubCategoryDao(database: SkoolSwapDatabase): SubCategoryDao = database.subCategoryDao()

    @Provides
    @Singleton
    fun provideColorDao(database: SkoolSwapDatabase): ColorDao = database.colorDao()

    @Provides
    @Singleton
    fun provideSizeDao(database: SkoolSwapDatabase): SizeDao = database.sizeDao()

    @Provides
    @Singleton
    fun provideBrandDao(database: SkoolSwapDatabase): BrandDao = database.brandDao()

    @Provides
    @Singleton
    fun provideConditionDao(database: SkoolSwapDatabase): ConditionDao = database.conditionDao()

    @Provides
    @Singleton
    fun provideProvinceDao(database: SkoolSwapDatabase): ProvinceDao = database.provinceDao()

    @Provides
    @Singleton
    fun provideTownDao(database: SkoolSwapDatabase): TownDao = database.townDao()

    @Provides
    @Singleton
    fun provideSchoolDao(database: SkoolSwapDatabase): SchoolDao = database.schoolDao()

    @Provides
    @Singleton
    fun provideGenderDao(database: SkoolSwapDatabase): GenderDao = database.genderDao()

    @Provides
    @Singleton
    fun provideTagDao(database: SkoolSwapDatabase): TagDao = database.tagDao()

    @Provides
    @Singleton
    fun provideLocationDao(database: SkoolSwapDatabase): LocationDao = database.locationDao()

    @Provides
    @Singleton
    fun provideShopDao(database: SkoolSwapDatabase): ShopDao = database.shopDao()

    @Provides
    @Singleton
    fun provideItemTypeDao(database: SkoolSwapDatabase): ItemTypeDao = database.itemTypeDao()
}