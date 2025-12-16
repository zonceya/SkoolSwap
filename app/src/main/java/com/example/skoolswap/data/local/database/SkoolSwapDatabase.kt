package com.example.skoolswap.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.skoolswap.data.local.database.dao.ShopDao
import com.example.skoolswap.data.local.database.dao.UserDao
import com.example.skoolswap.data.local.database.entities.ShopEntity
import com.example.skoolswap.data.local.database.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ShopEntity::class  // ADD THIS
    ],
    version = 3,  // INCREMENT VERSION
    exportSchema = false
)
@TypeConverters(Converters::class)  // ADD THIS
abstract class SkoolSwapDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun shopDao(): ShopDao  // KEEP THIS

    companion object {
        @Volatile
        private var INSTANCE: SkoolSwapDatabase? = null

        fun getInstance(context: Context): SkoolSwapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SkoolSwapDatabase::class.java,
                    "skoolswap_database"
                )
                    .fallbackToDestructiveMigration()  // This will delete old data when version changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}