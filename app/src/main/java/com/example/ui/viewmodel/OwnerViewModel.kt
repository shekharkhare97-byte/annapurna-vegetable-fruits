package com.example.ui.viewmodel
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.AnnapurnaDatabase
import com.example.data.local.OwnerInventoryEntity
import com.example.data.local.ShopSettingsEntity
import com.example.data.local.StockMovementEntity
import com.example.data.model.ProduceCatalog
import com.example.data.repository.OwnerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
class OwnerViewModel(private val repository: OwnerRepository) : ViewModel() {
 val inventory: StateFlow<List<OwnerInventoryEntity>> = repository.inventory.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val movements: StateFlow<List<StockMovementEntity>> = repository.movements.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val settings: StateFlow<ShopSettingsEntity?> = repository.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
 init { viewModelScope.launch { repository.ensureProducts(ProduceCatalog.items.map { Triple(it.id,it.priceRupees,it.unitMarathi.contains("जुडी") || it.unitMarathi.contains("नग")) }) } }
 fun isCountBased(id:String):Boolean = ProduceCatalog.findById(id)?.let { it.unitMarathi.contains("जुडी") || it.unitMarathi.contains("नग") } ?: false
 fun updatePrice(id:String,price:Int)=viewModelScope.launch { repository.updateDailyPrice(id,price,isCountBased(id)) }
 fun recordPurchase(id:String,quantity:Int)=viewModelScope.launch { repository.recordStock(id,quantity,"PURCHASE",isCountBased(id)) }
 fun recordWastage(id:String,quantity:Int)=viewModelScope.launch { repository.recordStock(id,quantity,"WASTAGE",isCountBased(id)) }
 fun saveSettings(name:String,phone:String)=viewModelScope.launch { repository.saveSettings(name,phone) }
 companion object {
  fun provideFactory(context:Context):ViewModelProvider.Factory=viewModelFactory { initializer { val db=AnnapurnaDatabase.getInstance(context); OwnerViewModel(OwnerRepository(db.ownerDao())) } }
 }
}