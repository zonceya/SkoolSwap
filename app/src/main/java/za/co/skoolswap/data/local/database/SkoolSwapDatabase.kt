package za.co.skoolswap.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import za.co.skoolswap.data.local.database.entities.*
import za.co.skoolswap.data.local.database.dao.BrandDao
import za.co.skoolswap.data.local.database.dao.ColorDao
import za.co.skoolswap.data.local.database.dao.ConditionDao
import za.co.skoolswap.data.local.database.dao.FavoriteDao
import za.co.skoolswap.data.local.database.dao.GenderDao
import za.co.skoolswap.data.local.database.dao.HomeFeedDao
import za.co.skoolswap.data.local.database.dao.ItemDao
import za.co.skoolswap.data.local.database.dao.ItemImageDao
import za.co.skoolswap.data.local.database.dao.ItemTypeDao
import za.co.skoolswap.data.local.database.dao.LocationDao
import za.co.skoolswap.data.local.database.dao.MainCategoryDao
import za.co.skoolswap.data.local.database.dao.PendingActionDao
import za.co.skoolswap.data.local.database.dao.ProductsCacheDao
import za.co.skoolswap.data.local.database.dao.ProvinceDao
import za.co.skoolswap.data.local.database.dao.SchoolDao
import za.co.skoolswap.data.local.database.dao.ShopDao
import za.co.skoolswap.data.local.database.dao.SizeDao
import za.co.skoolswap.data.local.database.dao.SubCategoryDao
import za.co.skoolswap.data.local.database.dao.TagDao
import za.co.skoolswap.data.local.database.dao.TownDao
import za.co.skoolswap.data.local.database.dao.UserDao
import za.co.skoolswap.data.local.database.dao.UserSchoolDao
import za.co.skoolswap.data.local.database.entities.BrandEntity
import za.co.skoolswap.data.local.database.entities.ConditionEntity
import za.co.skoolswap.data.local.database.entities.FavoriteEntity
import za.co.skoolswap.data.local.database.entities.GenderEntity
import za.co.skoolswap.data.local.database.entities.HomeFeedEntity
import za.co.skoolswap.data.local.database.entities.ItemEntity
import za.co.skoolswap.data.local.database.entities.ItemImageEntity
import za.co.skoolswap.data.local.database.entities.LocationEntity
import za.co.skoolswap.data.local.database.entities.MainCategoryEntity
import za.co.skoolswap.data.local.database.entities.PendingActionEntity
import za.co.skoolswap.data.local.database.entities.ProductsCacheEntity
import za.co.skoolswap.data.local.database.entities.ProvinceEntity

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
        HomeFeedEntity::class,
        ProductsCacheEntity::class,
        PendingActionEntity::class
    ],
    version = 23,
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
    abstract fun itemImageDao(): ItemImageDao
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
    abstract fun pendingActionDao(): PendingActionDao
    abstract fun productsCacheDao(): ProductsCacheDao

    companion object {
        @Volatile
        private var INSTANCE: SkoolSwapDatabase? = null

        // ✅ Migration 14 → 15 (Add school fields to UserEntity)
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE users ADD COLUMN schoolMapped INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) { /* Column might already exist */ }

                try {
                    database.execSQL("ALTER TABLE users ADD COLUMN schoolId INTEGER DEFAULT NULL")
                } catch (e: Exception) { /* Column might already exist */ }

                try {
                    database.execSQL("ALTER TABLE users ADD COLUMN schoolName TEXT DEFAULT NULL")
                } catch (e: Exception) { /* Column might already exist */ }
            }
        }

        // ✅ Migration 15 → 16 (Add PendingActionEntity table + createdAt to item_images)
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pending_actions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `actionType` TEXT NOT NULL,
                        `itemId` TEXT,
                        `userId` INTEGER,
                        `data` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `retryCount` INTEGER NOT NULL DEFAULT 0,
                        `maxRetries` INTEGER NOT NULL DEFAULT 3,
                        `status` TEXT NOT NULL DEFAULT 'pending'
                    )
                """)

                try {
                    database.execSQL("ALTER TABLE item_images ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) { /* Column might already exist */ }
            }
        }

        // ✅ Migration 16 → 17 (Add fileSize + localPath to item_images)
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE item_images ADD COLUMN fileSize INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) { /* Column might already exist */ }

                try {
                    database.execSQL("ALTER TABLE item_images ADD COLUMN localPath TEXT DEFAULT NULL")
                } catch (e: Exception) { /* Column might already exist */ }
            }
        }

        // ✅ Migration 17 → 18 (Add isCover column to item_images)
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE item_images ADD COLUMN isCover INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // Column might already exist - ignore
                }
            }
        }

        // ✅ Migration 18 → 19 (Add sync fields to items table)
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE items ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'ACTIVE'")
                } catch (e: Exception) { /* Column might already exist */ }

                try {
                    database.execSQL("ALTER TABLE items ADD COLUMN syncError TEXT DEFAULT NULL")
                } catch (e: Exception) { /* Column might already exist */ }

                try {
                    database.execSQL("ALTER TABLE items ADD COLUMN retryCount INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) { /* Column might already exist */ }

                try {
                    database.execSQL("ALTER TABLE items ADD COLUMN lastSyncAttempt INTEGER DEFAULT NULL")
                } catch (e: Exception) { /* Column might already exist */ }

                try {
                    database.execSQL("ALTER TABLE items ADD COLUMN mainCategoryId INTEGER DEFAULT NULL")
                } catch (e: Exception) { /* Column might already exist */ }

                try {
                    database.execSQL("ALTER TABLE items ADD COLUMN subCategoryId INTEGER DEFAULT NULL")
                } catch (e: Exception) { /* Column might already exist */ }

                try {
                    database.execSQL("ALTER TABLE items ADD COLUMN colorId INTEGER DEFAULT NULL")
                } catch (e: Exception) { /* Column might already exist */ }
            }
        }

        // ✅ Migration 19 → 20 (Add products_cache table)
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `products_cache` (
                        `cacheKey` TEXT NOT NULL PRIMARY KEY,
                        `itemsJson` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        `schoolId` INTEGER,
                        `sectionType` TEXT,
                        `period` TEXT,
                        `categoryId` INTEGER
                    )
                """)
            }
        }

        // ✅ NEW: Migration 20 → 21 (Add any new columns or tables here)
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add any new schema changes for version 21 here
                // Example: Add a new column to items table
                // try {
                //     database.execSQL("ALTER TABLE items ADD COLUMN newColumn TEXT DEFAULT NULL")
                // } catch (e: Exception) { /* Column might already exist */ }

                // If no changes needed, this migration can be empty
                // It just tells Room the schema changed
            }
        }

        fun getInstance(context: Context): SkoolSwapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SkoolSwapDatabase::class.java,
                    "skoolswap_database"
                )
                    .addMigrations(
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21  // ✅ ADD THIS
                    )
                    .fallbackToDestructiveMigration()  // ⚠️ For development only
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}