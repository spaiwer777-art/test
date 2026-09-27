package com.example.calorietracker.data

import com.example.calorietracker.network.AiNutritionEstimate
import com.example.calorietracker.network.ChatCompletionRequest
import com.example.calorietracker.network.ChatMessage
import com.example.calorietracker.network.NetworkModule
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.Flow

class FoodRepository(private val db: AppDatabase) {
    fun allFoods(): Flow<List<Food>> = db.foodDao().getAll()
    fun searchFoods(query: String): Flow<List<Food>> = db.foodDao().search(query)

    suspend fun saveFood(food: Food): Long = db.foodDao().insert(food)

    suspend fun findByBarcode(barcode: String): Food? = db.foodDao().findByBarcode(barcode)

    /** Looks up a barcode on Open Food Facts and saves it locally as a Food, or returns null if not found. */
    suspend fun lookupBarcodeOnline(barcode: String): Food? {
        val response = NetworkModule.openFoodFactsApi.getProduct(barcode)
        if (response.status != 1 || response.product == null) return null
        val p = response.product
        val n = p.nutriments ?: return null
        val food = Food(
            name = p.product_name?.takeIf { it.isNotBlank() } ?: "Продукт $barcode",
            caloriesPer100g = n.energyKcal100g ?: 0.0,
            proteinPer100g = n.proteins_100g ?: 0.0,
            fatPer100g = n.fat_100g ?: 0.0,
            carbsPer100g = n.carbohydrates_100g ?: 0.0,
            barcode = barcode
        )
        val id = db.foodDao().insert(food)
        return food.copy(id = id)
    }

    /**
     * Asks the Groq AI to estimate nutrition for a free-text food description,
     * e.g. "омлет из 2 яиц и тост с маслом". Requires the user's own Groq API key.
     */
    suspend fun estimateWithAi(description: String, apiKey: String): AiNutritionEstimate {
        val systemPrompt = """
            Ты — помощник по подсчёту калорий. Пользователь описывает, что он съел.
            Оцени общее количество калорий и БЖУ (белки, жиры, углеводы в граммах) для всего описанного.
            Ответь СТРОГО в формате JSON, без пояснений и без markdown:
            {"name": "краткое название блюда", "calories": число, "protein": число, "fat": число, "carbs": число}
        """.trimIndent()

        val request = ChatCompletionRequest(
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = description)
            )
        )
        val response = NetworkModule.groqApi.chatCompletion("Bearer $apiKey", request)
        val raw = response.choices.first().message.content
        val json = JsonParser.parseString(extractJson(raw)).asJsonObject
        return AiNutritionEstimate(
            name = json.get("name")?.asString ?: description,
            calories = json.get("calories")?.asDouble ?: 0.0,
            protein = json.get("protein")?.asDouble ?: 0.0,
            fat = json.get("fat")?.asDouble ?: 0.0,
            carbs = json.get("carbs")?.asDouble ?: 0.0
        )
    }

    /** The model sometimes wraps JSON in prose or code fences; pull out just the {...} block. */
    private fun extractJson(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start >= 0 && end > start) raw.substring(start, end + 1) else raw
    }
}

class DiaryRepository(private val db: AppDatabase) {
    fun entriesForDay(epochDay: Long): Flow<List<DiaryEntry>> = db.diaryDao().getForDay(epochDay)
    fun daysWithEntries(): Flow<List<Long>> = db.diaryDao().getDaysWithEntries()

    suspend fun addEntry(entry: DiaryEntry) = db.diaryDao().insert(entry)
    suspend fun deleteEntry(id: Long) = db.diaryDao().delete(id)
}
