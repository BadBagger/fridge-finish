package com.fridgefinish.app.data

import com.fridgefinish.app.domain.FoodCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class BarcodeProduct(
    val barcode: String,
    val name: String,
    val category: FoodCategory,
    val imageUrl: String?
)

class ProductLookupRepository {
    suspend fun lookupBarcode(barcode: String): BarcodeProduct? = withContext(Dispatchers.IO) {
        val url = URL("https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=product_name,brands,categories_tags,image_front_url,image_url")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", "FridgeFinish/1.0 Android - local-first food organizer")
        }

        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            if (json.optInt("status") != 1) return@withContext null
            val product = json.optJSONObject("product") ?: return@withContext null
            val productName = product.optString("product_name").trim()
            val brand = product.optString("brands").trim()
            val name = listOf(brand, productName)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" ")
                .ifBlank { "Scanned item" }
            val categories = product.optJSONArray("categories_tags")
            BarcodeProduct(
                barcode = barcode,
                name = name,
                category = inferCategory(categories),
                imageUrl = product.optString("image_front_url").ifBlank {
                    product.optString("image_url").ifBlank { null }
                }
            )
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun inferCategory(categories: org.json.JSONArray?): FoodCategory {
        val tags = buildList {
            if (categories != null) {
                for (index in 0 until categories.length()) add(categories.optString(index).lowercase())
            }
        }.joinToString(" ")
        return when {
            tags.contains("meat") || tags.contains("poultry") || tags.contains("beef") || tags.contains("chicken") -> FoodCategory.MEAT
            tags.contains("dair") || tags.contains("milk") || tags.contains("cheese") || tags.contains("yogurt") -> FoodCategory.DAIRY
            tags.contains("vegetable") || tags.contains("fruit") || tags.contains("produce") -> FoodCategory.PRODUCE
            tags.contains("frozen") -> FoodCategory.FROZEN
            tags.contains("drink") || tags.contains("beverage") || tags.contains("juice") -> FoodCategory.DRINKS
            tags.contains("snack") || tags.contains("chips") || tags.contains("candy") -> FoodCategory.SNACKS
            tags.contains("sauce") || tags.contains("condiment") || tags.contains("dressing") -> FoodCategory.CONDIMENTS
            tags.contains("rice") || tags.contains("pasta") || tags.contains("cereal") || tags.contains("pantry") -> FoodCategory.PANTRY
            else -> FoodCategory.OTHER
        }
    }
}
