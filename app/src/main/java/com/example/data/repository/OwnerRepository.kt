package com.example.data.repository
import com.example.data.local.OwnerDao
import com.example.data.local.OwnerInventoryEntity
import com.example.data.local.ShopSettingsEntity
import com.example.data.local.StockMovementEntity
import kotlinx.coroutines.flow.Flow
class OwnerRepository(private val dao: OwnerDao) {
 val inventory: Flow<List<OwnerInventoryEntity>> = dao.observeInventory()
 val movements: Flow<List<StockMovementEntity>> = dao.observeMovements()
 val settings: Flow<ShopSettingsEntity?> = dao.observeShopSettings()
 suspend fun ensureProducts(products: List<Pair<String, Int>>) {
  products.forEach { (id, price) -> dao.addInventoryIfMissing(OwnerInventoryEntity(produceId=id, sellingPricePaisePerKg=price*100)) }
 }
 suspend fun updateDailyPrice(id: String, rupeesPerKg: Int) {
  val current=dao.getInventory(id) ?: OwnerInventoryEntity(id)
  dao.saveInventory(current.copy(sellingPricePaisePerKg=rupeesPerKg.coerceAtLeast(0)*100, updatedAtMillis=System.currentTimeMillis()))
 }
 suspend fun recordStock(id: String, grams: Int, type: String) {
  if (grams<=0) return
  val current=dao.getInventory(id) ?: OwnerInventoryEntity(id)
  val stock=if(type=="PURCHASE") current.stockGrams+grams else (current.stockGrams-grams).coerceAtLeast(0)
  dao.saveInventory(current.copy(stockGrams=stock, updatedAtMillis=System.currentTimeMillis()))
  dao.addMovement(StockMovementEntity(produceId=id,movementType=type,quantityGrams=grams))
 }
 suspend fun saveSettings(name: String, phone: String) { dao.saveShopSettings(ShopSettingsEntity(ownerName=name.trim(),phoneNumber=phone.trim())) }
}
