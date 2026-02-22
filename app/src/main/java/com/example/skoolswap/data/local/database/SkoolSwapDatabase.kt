package com.example.skoolswap.data.local.database

import android.content.Context
import android.os.Build
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
        com.example.skoolswap.data.local.database.entities.UserEntity::class,
        com.example.skoolswap.data.local.database.entities.ShopEntity::class,
        com.example.skoolswap.data.local.database.entities.ItemEntity::class,
        com.example.skoolswap.data.local.database.entities.ItemTypeEntity::class,
        com.example.skoolswap.data.local.database.entities.MainCategoryEntity::class,
        com.example.skoolswap.data.local.database.entities.SubCategoryEntity::class,  // Fully qualified
        com.example.skoolswap.data.local.database.entities.ColorEntity::class,
        com.example.skoolswap.data.local.database.entities.SizeEntity::class,
        com.example.skoolswap.data.local.database.entities.BrandEntity::class,
        com.example.skoolswap.data.local.database.entities.ConditionEntity::class,
        com.example.skoolswap.data.local.database.entities.ProvinceEntity::class,
        com.example.skoolswap.data.local.database.entities.TownEntity::class,
        com.example.skoolswap.data.local.database.entities.SchoolEntity::class,
        com.example.skoolswap.data.local.database.entities.GenderEntity::class,
        com.example.skoolswap.data.local.database.entities.TagEntity::class,
        com.example.skoolswap.data.local.database.entities.LocationEntity::class
    ],
    version = 7,
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
    abstract fun conditionDao(): ConditionDao
    abstract fun provinceDao(): ProvinceDao
    abstract fun townDao(): TownDao
    abstract fun schoolDao(): SchoolDao
    abstract fun genderDao(): GenderDao
    abstract fun tagDao(): TagDao
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: SkoolSwapDatabase? = null

        fun getInstance(context: Context): SkoolSwapDatabase {
            return INSTANCE ?: synchronized(this) {
                // DELETE OLD DATABASE FILES FIRST
                deleteOldDatabaseFiles(context)

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

        private fun deleteOldDatabaseFiles(context: Context) {
            try {
                // Delete main database file
                val databasePath = context.getDatabasePath("skoolswap_database")
                if (databasePath.exists()) {
                    databasePath.delete()
                }

                // Delete journal file if exists
                val journalFile = File("${databasePath.path}-journal")
                if (journalFile.exists()) {
                    journalFile.delete()
                }

                // Delete wal file if exists
                val walFile = File("${databasePath.path}-wal")
                if (walFile.exists()) {
                    walFile.delete()
                }

                // Delete shm file if exists
                val shmFile = File("${databasePath.path}-shm")
                if (shmFile.exists()) {
                    shmFile.delete()
                }

                Log.d("DatabaseHelper", "Deleted old database files")
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error deleting old database files", e)
            }
        }
    }
}