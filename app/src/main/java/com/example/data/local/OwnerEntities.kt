package com.example.data.local
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "owner_inventory")
data class OwnerInventoryEntity(@PrimaryKey val produceId: String, val stockGrams: Int = 0, val purchasePricePaisePerKg: Int = 0, val sellingPricePaisePerKg: Int = 0, val updatedAtMillis: Long = System.currentTimeMillis())
@Entity(tableName = "stock_movements")
data class StockMovementEntity(@PrimaryKey(autoGenerate = true) val movementId: Long = 0, val produceId: String, val movementType: String, val quantityGrams: Int, val note: String = "", val createdAtMillis: Long = System.currentTimeMillis())
@Entity(tableName = "shop_settings")
data class ShopSettingsEntity(@PrimaryKey val id: Int = 1, val ownerName: String = "", val phoneNumber: String = "")
