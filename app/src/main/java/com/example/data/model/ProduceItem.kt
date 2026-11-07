package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R

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

    fun replaceFromAdmin(products: List<ProduceItem>) {
        items = products
    }

    fun findById(id: String): ProduceItem? = items.firstOrNull { it.id == id }
}
