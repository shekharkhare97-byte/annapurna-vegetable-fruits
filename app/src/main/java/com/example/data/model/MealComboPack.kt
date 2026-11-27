package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class MealComboIngredient(
    val productId: String,
    val nameMr: String,
    val nameHi: String,
    val nameEn: String,
    val quantityGrams: Int = 250,
    val unit: String = "g"
) {
    fun displayName(language: String): String = when (language) {
        "hi" -> nameHi.ifBlank { nameEn.ifBlank { nameMr } }
        "en" -> nameEn.ifBlank { nameMr }
        else -> nameMr.ifBlank { nameEn }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("productId", productId); put("nameMr", nameMr); put("nameHi", nameHi)
        put("nameEn", nameEn); put("quantityGrams", quantityGrams); put("unit", unit)
    }
}

data class MealComboPack(
    val id: String,
    val nameMr: String,
    val nameHi: String,
    val nameEn: String,
    val priceRupees: Double,
    val ingredients: List<MealComboIngredient>,
    val imageUrl: String = "",
    val servings: Int = 1
) {
    fun displayName(language: String): String = when (language) {
        "hi" -> nameHi.ifBlank { nameEn.ifBlank { nameMr } }
        "en" -> nameEn.ifBlank { nameMr }
        else -> nameMr.ifBlank { nameEn }
    }

    fun toJson(): String = JSONObject().apply {
        put("id", id); put("nameMr", nameMr); put("nameHi", nameHi); put("nameEn", nameEn)
        put("priceRupees", priceRupees); put("imageUrl", imageUrl); put("servings", servings)
        put("ingredients", JSONArray().apply { ingredients.forEach { put(it.toJson()) } })
    }.toString()

    companion object {
        fun fromJson(raw: String): MealComboPack? = runCatching {
            val json = JSONObject(raw)
            val list = json.optJSONArray("ingredients") ?: JSONArray()
            val ingredients = (0 until list.length()).mapNotNull { index ->
                val item = list.optJSONObject(index) ?: return@mapNotNull null
                MealComboIngredient(
                    productId = item.optString("productId"),
                    nameMr = item.optString("nameMr"),
                    nameHi = item.optString("nameHi"),
                    nameEn = item.optString("nameEn"),
                    quantityGrams = item.optInt("quantityGrams", 250),
                    unit = item.optString("unit", "g")
                )
            }
            MealComboPack(
                id = json.optString("id"),
                nameMr = json.optString("nameMr"),
                nameHi = json.optString("nameHi"),
                nameEn = json.optString("nameEn"),
                priceRupees = json.optDouble("priceRupees", 0.0),
                ingredients = ingredients,
                imageUrl = json.optString("imageUrl"),
                servings = json.optInt("servings", 1)
            )
        }.getOrNull()
    }
}
