package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ProduceCategory(val marathiLabel: String) {
    ALL("सर्व"),
    VEGETABLE("ताज्या भाज्या"),
    FRUIT("रसाळ फळे")
}

data class ProduceItem(
    val id: String,
    val marathiName: String,
    val englishKeywords: String,
    val category: ProduceCategory,
    val priceRupees: Int,
    val unitMarathi: String,
    val badgeMarathi: String,
    val descriptionMarathi: String,
    @DrawableRes val imageRes: Int,
    val imageUrl: String? = null,
    val isPieceBased: Boolean = false
)

object ProduceCatalog {
    @Volatile
    var items: List<ProduceItem> = emptyList()
        private set

    private val _itemsFlow = MutableStateFlow<List<ProduceItem>>(emptyList())
    val itemsFlow: StateFlow<List<ProduceItem>> = _itemsFlow.asStateFlow()

    fun replaceFromAdmin(products: List<ProduceItem>) {
        items = products
        _itemsFlow.value = products
    }

    fun findById(id: String): ProduceItem? = items.firstOrNull { it.id == id }
}
