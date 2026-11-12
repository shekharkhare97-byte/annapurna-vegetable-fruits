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
    val priceRupees: Double,
    val unitMarathi: String,
    val badgeMarathi: String,
    val descriptionMarathi: String,
    @DrawableRes val imageRes: Int,
    val imageUrl: String? = null,
    val isPieceBased: Boolean = false,
    val hindiName: String = "",
    val englishName: String = "",
    val hindiDescription: String = "",
    val englishDescription: String = ""
) {
    private fun languageCode(): String = java.util.Locale.getDefault().language

    fun displayName(): String = when (languageCode()) {
        "hi" -> hindiName.ifBlank { marathiName }
        "en" -> englishName.ifBlank { englishKeywords.ifBlank { marathiName } }
        else -> marathiName
    }

    fun displayDescription(): String = when (languageCode()) {
        "hi" -> hindiDescription.ifBlank { descriptionMarathi }
        "en" -> englishDescription.ifBlank { englishName.ifBlank { descriptionMarathi } }
        else -> descriptionMarathi
    }

    fun displayUnit(): String {
        val isBunch = unitMarathi.contains("जुडी") || unitMarathi.contains("जोडी") || unitMarathi.contains("गड्डी")
        return when (languageCode()) {
            "hi" -> when { isBunch -> "1 गुच्छा"; isPieceBased -> "1 नग"; else -> "1 किलो" }
            "en" -> when { isBunch -> "1 bunch"; isPieceBased -> "1 piece"; else -> "1 kg" }
            else -> unitMarathi
        }
    }

    fun displayBadge(): String = when (languageCode()) {
        "hi" -> "ताज़ा"
        "en" -> "Fresh"
        else -> badgeMarathi
    }
}

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
