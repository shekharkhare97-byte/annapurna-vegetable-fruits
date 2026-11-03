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
 suspend fun ensureProducts(products: List<Triple<String, Int, Boolean>>) {
  products.forEach { (id, price, countBased) ->
   dao.addInventoryIfMissing(OwnerInventoryEntity(produceId=id, sellingPricePaisePerKg=if(countBased) 0 else price*100, sellingPricePaisePerUnit=if(countBased) price*100 else 0))
  }
 }
 suspend fun updateDailyPrice(id: String, rupees: Int, countBased: Boolean) {
  val current=dao.getInventory(id) ?: OwnerInventoryEntity(id)
  val price=rupees.coerceAtLeast(0)*100
  dao.saveInventory(if(countBased) current.copy(sellingPricePaisePerUnit=price,updatedAtMillis=System.currentTimeMillis()) else current.copy(sellingPricePaisePerKg=price,updatedAtMillis=System.currentTimeMillis()))
 }
 suspend fun recordStock(id: String, quantity: Int, type: String, countBased: Boolean) {
  if(quantity<=0) return
  val current=dao.getInventory(id) ?: OwnerInventoryEntity(id)
  if(countBased) {
   val stock=if(type=="PURCHASE") current.stockUnits+quantity else (current.stockUnits-quantity).coerceAtLeast(0)
   dao.saveInventory(current.copy(stockUnits=stock,updatedAtMillis=System.currentTimeMillis()))
   dao.addMovement(StockMovementEntity(produceId=id,movementType=type,quantityGrams=quantity,note="UNIT"))
  } else {
   val stock=if(type=="PURCHASE") current.stockGrams+quantity else (current.stockGrams-quantity).coerceAtLeast(0)
   dao.saveInventory(current.copy(stockGrams=stock,updatedAtMillis=System.currentTimeMillis()))
   dao.addMovement(StockMovementEntity(produceId=id,movementType=type,quantityGrams=quantity))
  }
 }
 suspend fun saveSettings(name: String, phone: String) { dao.saveShopSettings(ShopSettingsEntity(ownerName=name.trim(),phoneNumber=phone.trim())) }
}