package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CartItemEntity::class,
        CustomerProfileEntity::class,
        OrderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AnnapurnaDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao

    companion object {
        @Volatile
        private var INSTANCE: AnnapurnaDatabase? = null

        fun getInstance(context: Context): AnnapurnaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AnnapurnaDatabase::class.java,
                    "annapurna_veg_fruits.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
