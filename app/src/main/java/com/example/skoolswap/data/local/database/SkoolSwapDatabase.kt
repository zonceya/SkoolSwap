package com.example.skoolswap.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.skoolswap.data.local.database.dao.*
import com.example.skoolswap.data.local.database.entities.*
import java.io.File

@Database(
    entities = [
        UserEntity::class,
        ShopEntity::class,
        ItemEntity::class,
        ItemTypeEntity::class,
        MainCategoryEntity::class,
        SubCategoryEntity::class,
        ColorEntity::class,
        SizeEntity::class,
        BrandEntity::class,
        ConditionEntity::class,
        ProvinceEntity::class,
        TownEntity::class,
        SchoolEntity::class,
        GenderEntity::class,
        TagEntity::class,
        LocationEntity::class,
        ItemImageEntity::class,
        FavoriteEntity::class,
        UserSchoolEntity::class,
        HomeFeedEntity::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SkoolSwapDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun shopDao(): ShopDao
    abstract fun itemDao(): ItemDao
    abstract fun itemTypeDao(): ItemTypeDao
    abstract fun mainCategoryDao(): MainCategoryDao
    abstract fun subCategoryDao(): SubCategoryDao
    abstract fun colorDao(): ColorDao
    abstract fun sizeDao(): SizeDao
    abstract fun brandDao(): BrandDao
    abstract fun itemImageDao(): ItemImageDao  // This should now resolve
    abstract fun conditionDao(): ConditionDao
    abstract fun provinceDao(): ProvinceDao
    abstract fun townDao(): TownDao
    abstract fun schoolDao(): SchoolDao
    abstract fun genderDao(): GenderDao
    abstract fun tagDao(): TagDao
    abstract fun locationDao(): LocationDao
    abstract fun userSchoolDao(): UserSchoolDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun homeFeedDao(): HomeFeedDao
    companion object {
        @Volatile
        private var INSTANCE: SkoolSwapDatabase? = null

        fun getInstance(context: Context): SkoolSwapDatabase {
            return INSTANCE ?: synchronized(this) {
                // Force delete old database to avoid migration issues
              //  context.getDatabasePath("skoolswap_database").delete()

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SkoolSwapDatabase::class.java,
                    "skoolswap_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}