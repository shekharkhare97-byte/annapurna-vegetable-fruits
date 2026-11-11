package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CartItemEntity::class,
        CustomerProfileEntity::class,
        OrderEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AnnapurnaDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE orders ADD COLUMN totalAmountRupeesDecimal REAL NOT NULL DEFAULT 0")
                database.execSQL("UPDATE orders SET totalAmountRupeesDecimal = totalAmountRupees")
            }
        }

        @Volatile
        private var INSTANCE: AnnapurnaDatabase? = null

        fun getInstance(context: Context): AnnapurnaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AnnapurnaDatabase::class.java,
                    "annapurna_veg_fruits.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
