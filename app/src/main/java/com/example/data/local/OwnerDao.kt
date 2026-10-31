package com.example.data.local
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
@Dao
interface OwnerDao {
 @Query("SELECT * FROM owner_inventory ORDER BY produceId") fun observeInventory(): Flow<List<OwnerInventoryEntity>>
 @Query("SELECT * FROM owner_inventory WHERE produceId = :id LIMIT 1") suspend fun getInventory(id: String): OwnerInventoryEntity?
 @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addInventoryIfMissing(item: OwnerInventoryEntity)
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveInventory(item: OwnerInventoryEntity)
 @Insert suspend fun addMovement(movement: StockMovementEntity)
 @Query("SELECT * FROM stock_movements ORDER BY createdAtMillis DESC LIMIT 100") fun observeMovements(): Flow<List<StockMovementEntity>>
 @Query("SELECT * FROM shop_settings WHERE id = 1 LIMIT 1") fun observeShopSettings(): Flow<ShopSettingsEntity?>
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveShopSettings(settings: ShopSettingsEntity)
}
