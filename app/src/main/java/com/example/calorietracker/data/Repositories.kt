package com.example.calorietracker.data

import com.example.calorietracker.data.json.AiMealJson
import com.example.calorietracker.data.json.MealPlanData
import com.example.calorietracker.data.json.PlanDay
import com.example.calorietracker.data.json.PlanMeal
import com.example.calorietracker.network.ChatCompletionRequest
import com.example.calorietracker.network.ChatMessage
import com.example.calorietracker.network.NetworkModule
import com.example.calorietracker.network.OffProduct
import com.example.calorietracker.network.ResponseFormat
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import kotlin.math.roundToInt

private val PREFERRED_GROQ_MODELS = listOf("openai/gpt-oss-120b", "openai/gpt-oss-20b", "qwen/qwen3.8-27b")
private val NON_CHAT_MODEL_MARKERS = listOf("whisper", "guard", "orpheus", "tts", "safeguard")

/** Model used for AI requests; replaced at runtime if Groq retires it. */
@Volatile private var groqModel = PREFERRED_GROQ_MODELS.first()

class FoodRepository(private val db: AppDatabase) {
    fun allFoods(sources: List<FoodSource> = FoodSource.entries): Flow<List<Food>> = db.foodDao().getAll(sources)
    /**
     * Word-based search: every word of the query must start a word of the name,
     * in any order and ignoring endings and ё ("маринованные огурцы" finds
     * "Огурцы маринованные", "свекла" finds "Свёкла").
     */
    fun searchFoods(query: String, sources: List<FoodSource> = FoodSource.entries): Flow<List<Food>> {
        val stems = FoodSearch.stems(query)
        if (stems.isEmpty()) return allFoods(sources)
        return db.foodDao().changes().map {
            val pattern = FoodSearch.likePattern(stems.maxBy { it.length })
            FoodSearch.rank(query, stems, db.foodDao().candidates(pattern, sources)).take(150)
        }
    }
    fun observe(id: Long): Flow<Food?> = db.foodDao().observe(id)

    suspend fun get(id: Long): Food? = db.foodDao().get(id)
    suspend fun saveFood(food: Food): Long = db.foodDao().insert(food)
    suspend fun deleteFood(id: Long) = db.foodDao().delete(id)
    suspend fun findByBarcode(barcode: String): Food? = db.foodDao().findByBarcode(barcode)
    suspend fun builtinFoods(): List<Food> = db.foodDao().builtin()

    /** Looks up a barcode on Open Food Facts and saves it locally, or returns null if not found. */
    suspend fun lookupBarcodeOnline(barcode: String): Food? {
        // Open Food Facts answers 404 for barcodes it doesn't know: that's "not found", not an error.
        val response = try {
            NetworkModule.openFoodFactsApi.getProduct(barcode)
        } catch (e: HttpException) {
            if (e.code() == 404) return null else throw e
        }
        if (response.status != 1) return null
        val food = response.product?.toFood(FoodSource.BARCODE, fallbackName = "Продукт $barcode") ?: return null
        val id = db.foodDao().insert(food.copy(barcode = barcode))
        return food.copy(id = id, barcode = barcode)
    }

    /** Full-text search on Open Food Facts; results are not saved until the user opens one. */
    suspend fun searchOnline(query: String): List<Food> =
        try {
            NetworkModule.offSearchApi.search(query).hits.orEmpty()
        } catch (e: HttpException) {
            if (e.code() == 404) emptyList() else throw e
        }
            .mapNotNull { it.toFood(FoodSource.ONLINE, fallbackName = null) }
            .distinctBy { it.barcode ?: it.name }

    /** Saves an online result (reusing an existing row with the same barcode) and returns its id. */
    suspend fun saveOnline(food: Food): Long {
        food.barcode?.let { code -> db.foodDao().findByBarcode(code)?.let { return it.id } }
        return db.foodDao().insert(food)
    }

    private fun OffProduct.toFood(source: FoodSource, fallbackName: String?): Food? {
        val n = nutriments ?: return null
        val kcal = n.energyKcal100g ?: return null
        val name = displayName ?: fallbackName ?: return null
        return Food(
            name = name,
            caloriesPer100g = kcal,
            proteinPer100g = n.proteins100g ?: 0.0,
            fatPer100g = n.fat100g ?: 0.0,
            carbsPer100g = n.carbohydrates100g ?: 0.0,
            barcode = code,
            brand = brandText,
            source = source,
            fiberPer100g = n.fiber100g,
            sugarPer100g = n.sugars100g,
            saturatedFatPer100g = n.saturatedFat100g,
            saltPer100g = n.salt100g,
            servingGrams = servingGrams,
            servingLabel = servingGrams?.let { "порция" },
            nutriScore = nutriscoreGrade?.takeIf { it.length == 1 }
        )
    }
}

/** One ingredient of an AI estimate; values per 100 g so grams can be edited. */
data class AiIngredient(
    val name: String,
    val grams: Double,
    val kcal100: Double,
    val protein100: Double,
    val fat100: Double,
    val carbs100: Double,
    /** Name of the reference-base product whose values replaced the AI's guess, if any. */
    val baseName: String? = null
) {
    val calories get() = kcal100 * grams / 100
    val protein get() = protein100 * grams / 100
    val fat get() = fat100 * grams / 100
    val carbs get() = carbs100 * grams / 100
}

data class AiMealEstimate(val name: String, val items: List<AiIngredient>)

class AiRepository(private val foodRepo: FoodRepository) {
    private val gson = Gson()

    /**
     * Breaks a free-text meal description into ingredients with weights and
     * per-100g values, then swaps in reference-base values where an ingredient
     * is recognised. Summing per-ingredient numbers in code is far more
     * accurate than asking the model for one total.
     */
    suspend fun estimateMeal(description: String, apiKey: String): AiMealEstimate {
        val system = """
            Ты — диетолог-нутрициолог. Разбери описание еды на отдельные ингредиенты.
            Для каждого оцени массу в граммах. Если масса не указана — типичная порция в России:
            1 яйцо = 55 г, ломтик хлеба = 30 г, 1 ч.л. = 5 г, 1 ст.л. масла = 10 г, 1 ст.л. майонеза или сметаны = 20–25 г,
            стакан = 200 мл, тарелка супа = 300 г, порция гарнира = 150–200 г, котлета = 80 г.
            Для каждого ингредиента укажи пищевую ценность на 100 г по справочникам (Скурихин, USDA).
            Называй продукты по-русски и указывай состояние: сырой, варёный, жареный, сухой — если это важно.
            Крупы и макароны в готовом блюде — в варёном виде.
            Ответь СТРОГО JSON:
            {"name":"краткое название блюда","items":[{"name":"ингредиент","grams":число,"kcal100":число,"protein100":число,"fat100":число,"carbs100":число}]}
        """.trimIndent()
        val raw = chat(apiKey, system, description, reasoning = "medium", maxTokens = 4000)
        val parsed = gson.fromJson(extractJson(raw), AiMealJson::class.java)
        val base = foodRepo.builtinFoods()
        val items = parsed.items.orEmpty().mapNotNull { item ->
            val name = item.name?.trim().orEmpty().ifEmpty { return@mapNotNull null }
            val grams = item.grams?.takeIf { it > 0 } ?: return@mapNotNull null
            val match = FoodMatcher.match(name, base)
            if (match != null) {
                AiIngredient(
                    name, grams, match.caloriesPer100g, match.proteinPer100g, match.fatPer100g, match.carbsPer100g,
                    baseName = match.name
                )
            } else {
                AiIngredient(
                    name, grams, item.kcal100 ?: 0.0, item.protein100 ?: 0.0, item.fat100 ?: 0.0, item.carbs100 ?: 0.0
                )
            }
        }
        if (items.isEmpty()) throw IllegalStateException("empty estimate")
        return AiMealEstimate(parsed.name?.takeIf { it.isNotBlank() } ?: description, items)
    }

    /**
     * Translates a TheMealDB recipe to Russian, converts measures to grams and
     * estimates per-100g values; reference-base matches replace the estimates.
     */
    suspend fun translateRecipe(
        meal: com.example.calorietracker.network.MealDetails, apiKey: String, categories: List<String>
    ): Pair<com.example.calorietracker.data.json.AiRecipeJson, List<AiIngredient>> {
        val system = """
            Ты — кулинарный редактор и нутрициолог. Переведи рецепт на русский язык.
            Название — по-русски, как блюдо принято называть в России (Beef Stroganoff → Бефстроганов, Blini → Блины).
            Переведи меры в граммы (1 cup муки ≈ 125 г, 1 cup жидкости ≈ 240 г, 1 tbsp ≈ 15 г, 1 tsp ≈ 5 г, 1 lb ≈ 454 г, 1 oz ≈ 28 г; для штук — типичная масса).
            Для каждого ингредиента дай пищевую ценность на 100 г по справочникам, называй продукты по-русски и указывай состояние (сырой, варёный, сухой).
            Шаги приготовления — коротко, по-русски, без лишней воды. Оцени число порций и время в минутах.
            Категория — одна из: ${categories.joinToString(", ")}.
            Ответь СТРОГО JSON:
            {"name":"название","servings":число,"minutes":число,"category":"...","steps":["шаг"],"items":[{"name":"ингредиент","grams":число,"kcal100":число,"protein100":число,"fat100":число,"carbs100":число}]}
        """.trimIndent()
        val user = buildString {
            appendLine("Название: ${meal.name}")
            appendLine("Ингредиенты:")
            meal.ingredients.forEach { appendLine("- ${it.name}: ${it.measure}") }
            appendLine("Приготовление:")
            append(meal.instructions.take(4000))
        }
        val raw = chat(apiKey, system, user, reasoning = "low", maxTokens = 6000)
        val parsed = gson.fromJson(extractJson(raw), com.example.calorietracker.data.json.AiRecipeJson::class.java)
        val base = foodRepo.builtinFoods()
        val items = parsed.items.orEmpty().mapNotNull { item ->
            val name = item.name?.trim().orEmpty().ifEmpty { return@mapNotNull null }
            val grams = item.grams?.takeIf { it > 0 } ?: return@mapNotNull null
            val match = FoodMatcher.match(name, base)
            if (match != null) AiIngredient(name, grams, match.caloriesPer100g, match.proteinPer100g, match.fatPer100g, match.carbsPer100g, match.name)
            else AiIngredient(name, grams, item.kcal100 ?: 0.0, item.protein100 ?: 0.0, item.fat100 ?: 0.0, item.carbs100 ?: 0.0)
        }
        if (items.isEmpty()) throw IllegalStateException("empty recipe")
        return parsed to items
    }

    /** AI meal plan for [days] days; each day is scaled to land on the calorie goal. */
    suspend fun generatePlan(
        apiKey: String,
        calorieGoal: Double,
        protein: Double,
        fat: Double,
        carbs: Double,
        days: Int,
        includeSnack: Boolean,
        preferences: String,
        diet: Diet? = null
    ): MealPlanData {
        val meals = if (includeSnack) "BREAKFAST, LUNCH, DINNER, SNACK" else "BREAKFAST, LUNCH, DINNER"
        val dietText = diet?.let {
            val avoid = DietRules.keywords(it).map { k -> k.removeSuffix("=") }
            "\nДиета: «${it.name}». ${it.description}\nПравила: ${it.recommended.replace("\n", "; ")}" +
                (if (avoid.isNotEmpty()) "\nНе используй продукты, в названии которых есть: ${avoid.joinToString(", ")}." else "")
        }.orEmpty()
        val system = """
            Ты — диетолог. Составь рацион питания на $days дн.
            Цель на день: ${calorieGoal.roundToInt()} ккал (допуск ±5%), белки ≈ ${protein.roundToInt()} г, жиры ≈ ${fat.roundToInt()} г, углеводы ≈ ${carbs.roundToInt()} г.
            Приёмы пищи каждый день: $meals.$dietText
            Блюда — простая домашняя кухня из продуктов обычного российского супермаркета; не повторяй блюдо чаще раза в 2 дня.
            Для каждого приёма: название блюда, масса порции в граммах, КБЖУ порции и короткий состав с граммовками.
            Ответь СТРОГО JSON:
            {"days":[{"meals":[{"meal":"BREAKFAST","dish":"...","grams":число,"kcal":число,"protein":число,"fat":число,"carbs":число,"note":"состав"}]}]}
        """.trimIndent()
        val user = "Пожелания: ${preferences.ifBlank { "нет особых пожеланий" }}"
        val raw = chat(apiKey, system, user, reasoning = "low", maxTokens = 7000)
        val root = JsonParser.parseString(extractJson(raw)).asJsonObject
        val parsedDays = root.getAsJsonArray("days")?.map { dayEl ->
            val mealsJson = dayEl.asJsonObject.getAsJsonArray("meals")
            PlanDay(mealsJson?.map { gson.fromJson(it, PlanMeal::class.java) }.orEmpty()
                .filter { it.kcal > 0 && MealType.entries.any { m -> m.name == it.meal } })
        }.orEmpty().filter { it.meals.isNotEmpty() }
        if (parsedDays.isEmpty()) throw IllegalStateException("empty plan")
        return MealPlanData(
            calorieGoal, preferences, if (diet != null) "ИИ · ${diet.name}" else "ИИ",
            parsedDays.map { scaleDay(it, calorieGoal) }
        )
    }

    private suspend fun chat(apiKey: String, system: String, user: String, reasoning: String, maxTokens: Int): String {
        val bearer = "Bearer $apiKey"
        val messages = listOf(ChatMessage("system", system), ChatMessage("user", user))
        fun request() = ChatCompletionRequest(
            model = groqModel,
            messages = messages,
            temperature = 0.1,
            responseFormat = ResponseFormat("json_object"),
            reasoningEffort = if (groqModel.startsWith("openai/gpt-oss")) reasoning else null,
            maxCompletionTokens = maxTokens
        )
        val response = try {
            NetworkModule.groqApi.chatCompletion(bearer, request())
        } catch (e: HttpException) {
            // Groq retires models regularly and then answers 404 model_not_found.
            // Switch to whatever chat model the account can currently use.
            if (e.code() != 404) throw e
            groqModel = pickAvailableModel(bearer) ?: throw e
            NetworkModule.groqApi.chatCompletion(bearer, request())
        }
        return response.choices.first().message.content
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

/** Scales a plan day's portions so its total lands on the goal (within sane limits). */
fun scaleDay(day: PlanDay, goal: Double): PlanDay {
    val total = day.calories
    if (total <= 0) return day
    val factor = (goal / total).coerceIn(0.75, 1.3)
    if (kotlin.math.abs(factor - 1) < 0.03) return day
    return PlanDay(day.meals.map {
        it.copy(
            grams = (it.grams * factor).roundToInt().toDouble(),
            kcal = it.kcal * factor,
            protein = it.protein * factor,
            fat = it.fat * factor,
            carbs = it.carbs * factor
        )
    })
}

class DiaryRepository(private val db: AppDatabase) {
    fun entriesForDay(epochDay: Long): Flow<List<DiaryEntry>> = db.diaryDao().getForDay(epochDay)
    fun daysWithEntries(): Flow<List<Long>> = db.diaryDao().getDaysWithEntries()
    fun dailyTotals(fromDay: Long, toDay: Long): Flow<List<DayTotals>> = db.diaryDao().dailyTotals(fromDay, toDay)
    fun mealTotals(fromDay: Long, toDay: Long): Flow<List<MealTotal>> = db.diaryDao().mealTotals(fromDay, toDay)
    fun topFoods(fromDay: Long, toDay: Long, limit: Int = 5): Flow<List<FoodTotal>> =
        db.diaryDao().topFoods(fromDay, toDay, limit)

    suspend fun addEntry(entry: DiaryEntry) = db.diaryDao().insert(entry)
    suspend fun deleteEntry(id: Long) = db.diaryDao().delete(id)
}

class DietRepository(private val db: AppDatabase) {
    fun all(): Flow<List<Diet>> = db.dietDao().all()
    fun observe(id: Long): Flow<Diet?> = db.dietDao().observe(id)
    suspend fun get(id: Long): Diet? = db.dietDao().get(id)
    suspend fun save(diet: Diet): Long {
        val id = db.dietDao().upsert(diet)
        return if (diet.id != 0L) diet.id else id
    }
    suspend fun delete(id: Long) = db.dietDao().delete(id)
}

class RecipeRepository(private val db: AppDatabase) {
    fun all(): Flow<List<Recipe>> = db.recipeDao().all()
    fun observe(id: Long): Flow<Recipe?> = db.recipeDao().observe(id)
    fun ingredients(id: Long): Flow<List<RecipeIngredient>> = db.recipeDao().ingredients(id)
    suspend fun allOnce(): List<Recipe> = db.recipeDao().allOnce()
    suspend fun totalGrams(): Map<Long, Double> = db.recipeDao().totalGrams().associate { it.recipeId to it.grams }

    suspend fun save(
        name: String, category: String, servings: Int, minutes: Int, steps: String, items: List<RecipeIngredient>,
        cookedWeight: Double? = null
    ): Long {
        val recipe = buildRecipe(name, category, servings, minutes, steps, isBuiltin = false, ingredients = items)
            .copy(cookedWeight = cookedWeight)
        return db.recipeDao().insertWithIngredients(recipe, items)
    }

    suspend fun update(
        id: Long, name: String, category: String, servings: Int, minutes: Int, steps: String,
        cookedWeight: Double?, items: List<RecipeIngredient>
    ) {
        val recipe = buildRecipe(name, category, servings, minutes, steps, isBuiltin = false, ingredients = items, id = id)
            .copy(cookedWeight = cookedWeight)
        db.recipeDao().replace(recipe, items)
    }

    suspend fun ingredientsOnce(): List<RecipeIngredient> = db.recipeDao().allIngredients()

    /** Saves an imported recipe (e.g. from TheMealDB) with its photo; re-importing replaces the old copy. */
    suspend fun saveImported(
        name: String, category: String, servings: Int, minutes: Int, steps: String,
        items: List<RecipeIngredient>, imageUrl: String?, externalId: String
    ): Long {
        db.recipeDao().findByExternal(externalId)?.let { db.recipeDao().delete(it.id) }
        val recipe = buildRecipe(name, category, servings, minutes, steps, isBuiltin = false, ingredients = items)
            .copy(imageUrl = imageUrl, externalId = externalId)
        return db.recipeDao().insertWithIngredients(recipe, items)
    }

    suspend fun findByExternal(externalId: String): Recipe? = db.recipeDao().findByExternal(externalId)

    suspend fun delete(id: Long) = db.recipeDao().delete(id)
}

class TrackingRepository(private val db: AppDatabase) {
    private val dao get() = db.trackingDao()

    fun photosForDay(epochDay: Long): Flow<List<MealPhoto>> = dao.photosForDay(epochDay)
    suspend fun addPhoto(photo: MealPhoto) = dao.insertPhoto(photo)
    suspend fun deletePhoto(photo: MealPhoto) {
        dao.deletePhoto(photo.id)
        java.io.File(photo.path).delete()
    }

    fun weights(fromDay: Long, toDay: Long): Flow<List<WeightEntry>> = dao.weights(fromDay, toDay)
    fun latestWeight(): Flow<WeightEntry?> = dao.latestWeight()
    suspend fun setWeight(epochDay: Long, kg: Double) = dao.upsertWeight(WeightEntry(epochDay, kg))

    fun water(epochDay: Long): Flow<WaterEntry?> = dao.water(epochDay)
    fun waterRange(fromDay: Long, toDay: Long): Flow<List<WaterEntry>> = dao.waterRange(fromDay, toDay)
    suspend fun setWater(epochDay: Long, ml: Int) = dao.upsertWater(WaterEntry(epochDay, ml.coerceAtLeast(0)))

    fun latestPlan(): Flow<MealPlanEntity?> = dao.latestPlan()
    suspend fun savePlan(plan: MealPlanEntity) = dao.insertPlan(plan)
}
