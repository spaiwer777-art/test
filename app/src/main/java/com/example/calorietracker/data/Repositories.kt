package com.example.calorietracker.data

import com.example.calorietracker.network.AiNutritionEstimate
import com.example.calorietracker.network.ChatCompletionRequest
import com.example.calorietracker.network.ChatMessage
import com.example.calorietracker.network.NetworkModule
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

private val PREFERRED_GROQ_MODELS = listOf("openai/gpt-oss-120b", "openai/gpt-oss-20b", "qwen/qwen3.8-27b")
private val NON_CHAT_MODEL_MARKERS = listOf("whisper", "guard", "orpheus", "tts", "safeguard")

/** Model used for AI estimates; replaced at runtime if Groq retires it. */
@Volatile private var groqModel = PREFERRED_GROQ_MODELS.first()

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
            Название блюда пиши по-русски.
            Ответь СТРОГО в формате JSON, без пояснений и без markdown:
            {"name": "краткое название блюда", "calories": число, "protein": число, "fat": число, "carbs": число}
        """.trimIndent()
        val messages = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = description)
        )
        val bearer = "Bearer $apiKey"

        val response = try {
            NetworkModule.groqApi.chatCompletion(bearer, ChatCompletionRequest(groqModel, messages))
        } catch (e: HttpException) {
            // Groq retires models regularly and then answers 404 model_not_found.
            // Switch to whatever chat model the account can currently use.
            if (e.code() != 404) throw e
            groqModel = pickAvailableModel(bearer) ?: throw e
            NetworkModule.groqApi.chatCompletion(bearer, ChatCompletionRequest(groqModel, messages))
        }
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

    private suspend fun pickAvailableModel(bearer: String): String? {
        val ids = NetworkModule.groqApi.listModels(bearer).data.map { it.id }
        return PREFERRED_GROQ_MODELS.firstOrNull { it in ids }
            ?: ids.firstOrNull { id -> NON_CHAT_MODEL_MARKERS.none { id.contains(it) } }
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
    fun dailyTotals(fromDay: Long, toDay: Long): Flow<List<DayTotals>> = db.diaryDao().dailyTotals(fromDay, toDay)

    suspend fun addEntry(entry: DiaryEntry) = db.diaryDao().insert(entry)
    suspend fun deleteEntry(id: Long) = db.diaryDao().delete(id)
}
