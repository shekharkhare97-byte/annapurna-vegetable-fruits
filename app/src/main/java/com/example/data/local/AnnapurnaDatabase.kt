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
    version = 3,
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cart_items RENAME TO cart_items_old")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS cart_items (cartLineId TEXT NOT NULL, produceId TEXT NOT NULL, quantity INTEGER NOT NULL, weightGrams INTEGER NOT NULL, addedAtMillis INTEGER NOT NULL, PRIMARY KEY(cartLineId))"
                )
                database.execSQL(
                    "INSERT INTO cart_items (cartLineId, produceId, quantity, weightGrams, addedAtMillis) SELECT produceId || '-' || weightGrams, produceId, quantity, weightGrams, addedAtMillis FROM cart_items_old"
                )
                database.execSQL("DROP TABLE cart_items_old")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
