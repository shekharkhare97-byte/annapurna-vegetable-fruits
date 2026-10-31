package com.example.data.local
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
@Database(entities=[CartItemEntity::class,CustomerProfileEntity::class,OrderEntity::class,OwnerInventoryEntity::class,StockMovementEntity::class,ShopSettingsEntity::class],version=2,exportSchema=false)
abstract class AnnapurnaDatabase:RoomDatabase() {
 abstract fun storeDao():StoreDao
 abstract fun ownerDao():OwnerDao
 companion object {
  @Volatile private var INSTANCE:AnnapurnaDatabase?=null
  private val MIGRATION_1_2=object:Migration(1,2) {
   override fun migrate(db:SupportSQLiteDatabase) {
    db.execSQL("CREATE TABLE IF NOT EXISTS owner_inventory (produceId TEXT NOT NULL PRIMARY KEY, stockGrams INTEGER NOT NULL, purchasePricePaisePerKg INTEGER NOT NULL, sellingPricePaisePerKg INTEGER NOT NULL, updatedAtMillis INTEGER NOT NULL)")
    db.execSQL("CREATE TABLE IF NOT EXISTS stock_movements (movementId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, produceId TEXT NOT NULL, movementType TEXT NOT NULL, quantityGrams INTEGER NOT NULL, note TEXT NOT NULL, createdAtMillis INTEGER NOT NULL)")
    db.execSQL("CREATE TABLE IF NOT EXISTS shop_settings (id INTEGER NOT NULL PRIMARY KEY, ownerName TEXT NOT NULL, phoneNumber TEXT NOT NULL)")
   }
  }
  fun getInstance(context:Context):AnnapurnaDatabase {
   return INSTANCE ?: synchronized(this) {
    INSTANCE ?: Room.databaseBuilder(context.applicationContext,AnnapurnaDatabase::class.java,"annapurna_veg_fruits.db")
     .addMigrations(MIGRATION_1_2).build().also { INSTANCE=it }
   }
  }
 }
}
