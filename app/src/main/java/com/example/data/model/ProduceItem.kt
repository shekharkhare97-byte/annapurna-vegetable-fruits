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
    val englishDescription: String = "",
    val stockQuantity: Double = 0.0
) {
    val isOutOfStock: Boolean get() = stockQuantity <= 0.0
    private fun languageCode(): String = java.util.Locale.getDefault().language

    fun displayName(): String {
        val translations = ProduceNameTranslations.find(marathiName, englishKeywords, englishName, hindiName)
        return when (languageCode()) {
            "hi" -> hindiName.ifBlank { translations?.second ?: "अनुवाद उपलब्ध नहीं" }
            "en" -> englishName.ifBlank { translations?.third ?: "Translation unavailable" }
            else -> marathiName
        }
    }

    fun displayDescription(): String = when (languageCode()) {
        "hi" -> hindiDescription.ifBlank { "विवरण उपलब्ध नहीं है" }
        "en" -> englishDescription.ifBlank { "Description unavailable" }
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


/** Fallback translations for existing catalog entries that predate multilingual Admin fields. */
private object ProduceNameTranslations {
    private val rows = listOf(
        listOf("कांदा", "प्याज", "Onion", "kanda"), listOf("बटाटा", "आलू", "Potato", "batata"),
        listOf("टोमॅटो", "टमाटर", "Tomato"), listOf("लसूण", "लहसुन", "Garlic"), listOf("आले", "अदरक", "Ginger"),
        listOf("हिरवी मिरची", "हरी मिर्च", "Green chilli"), listOf("कोथिंबीर", "हरा धनिया", "Coriander", "धनिया"),
        listOf("पालक", "पालक", "Spinach"), listOf("वांगी", "बैंगन", "Brinjal", "Eggplant"),
        listOf("फुलकोबी", "फूलगोभी", "Cauliflower"), listOf("कोबी", "पत्ता गोभी", "Cabbage"),
        listOf("गाजर", "गाजर", "Carrot"), listOf("काकडी", "खीरा", "Cucumber"), listOf("लिंबू", "नींबू", "Lemon"),
        listOf("भेंडी", "भिंडी", "Okra", "Lady finger"), listOf("दोडका", "तुरई", "Ridge gourd"),
        listOf("कारले", "करेला", "Bitter gourd"), listOf("मेथी", "मेथी", "Fenugreek"),
        listOf("शेवगा", "सहजन", "Drumstick"), listOf("भोपळा", "कद्दू", "Pumpkin"),
        listOf("दुधी भोपळा", "लौकी", "Bottle gourd"), listOf("मटार", "मटर", "Green peas"),
        listOf("सफरचंद", "सेब", "Apple"), listOf("केळी", "केला", "Banana"), listOf("द्राक्षे", "अंगूर", "Grapes"),
        listOf("संत्रे", "संतरा", "Orange"), listOf("डाळिंब", "अनार", "Pomegranate"), listOf("आंबा", "आम", "Mango"),
        listOf("पेरू", "अमरूद", "Guava"), listOf("पपई", "पपीता", "Papaya"), listOf("कलिंगड", "तरबूज", "Watermelon")
    )
    fun find(vararg candidates: String): Triple<String, String, String>? {
        val keys = candidates.filter { it.isNotBlank() }.map { it.trim().lowercase() }
        val row = rows.firstOrNull { item -> item.any { it.lowercase() in keys } } ?: return null
        return Triple(row[0], row[1], row[2])
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
