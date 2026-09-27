package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.AiIngredient
import com.example.calorietracker.data.RecipeIngredient
import com.example.calorietracker.data.json.AiRecipeJson
import com.example.calorietracker.graph
import com.example.calorietracker.network.MealDetails
import com.example.calorietracker.network.MealSummary
import com.example.calorietracker.network.NetworkModule
import com.example.calorietracker.network.meals
import com.example.calorietracker.network.toDetails
import com.example.calorietracker.network.toSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

/** TheMealDB areas (cuisines) with Russian labels; Russian first. */
val MEALDB_AREAS = listOf(
    "Russian" to "Русская", "Ukrainian" to "Украинская", "Polish" to "Польская", "Italian" to "Итальянская",
    "French" to "Французская", "Greek" to "Греческая", "Turkish" to "Турецкая", "Spanish" to "Испанская",
    "British" to "Британская", "American" to "Американская", "Mexican" to "Мексиканская", "Japanese" to "Японская",
    "Chinese" to "Китайская", "Thai" to "Тайская", "Vietnamese" to "Вьетнамская", "Indian" to "Индийская",
    "Moroccan" to "Марокканская", "Egyptian" to "Египетская"
)

/** TheMealDB categories with Russian labels. */
val MEALDB_CATEGORIES = listOf(
    "Breakfast" to "Завтраки", "Chicken" to "Курица", "Beef" to "Говядина", "Pork" to "Свинина", "Lamb" to "Баранина",
    "Seafood" to "Рыба и морепродукты", "Pasta" to "Паста", "Vegetarian" to "Вегетарианское", "Vegan" to "Веганское",
    "Side" to "Гарниры", "Starter" to "Закуски", "Dessert" to "Десерты", "Miscellaneous" to "Разное"
)

sealed class WorldListState {
    object Loading : WorldListState()
    data class Loaded(val meals: List<MealSummary>) : WorldListState()
    data class Error(val message: String) : WorldListState()
}

class WorldRecipesViewModel(application: Application) : AndroidViewModel(application) {
    private val api = NetworkModule.mealDbApi

    /** Selected filter: "a:Russian", "c:Beef" or "s:<query>". */
    val filter = MutableStateFlow("a:Russian")
    val query = MutableStateFlow("")
    private val _state = MutableStateFlow<WorldListState>(WorldListState.Loading)
    val state: StateFlow<WorldListState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        load()
    }

    fun select(value: String) {
        filter.value = value
        load()
    }

    fun setQuery(value: String) {
        query.value = value
        job?.cancel()
        if (value.trim().length >= 2) job = viewModelScope.launch {
            delay(600)
            filter.value = "s:${value.trim()}"
            fetch()
        }
    }

    fun load() {
        job?.cancel()
        job = viewModelScope.launch { fetch() }
    }

    private suspend fun fetch() {
        _state.value = WorldListState.Loading
        val f = filter.value
        _state.value = try {
            val arg = f.substring(2)
            val json = when (f[0]) {
                'a' -> api.byArea(arg)
                'c' -> api.byCategory(arg)
                else -> api.search(arg)
            }
            WorldListState.Loaded(json.meals().map { it.toSummary() })
        } catch (e: IOException) {
            WorldListState.Error("Нет подключения к интернету")
        } catch (e: Exception) {
            WorldListState.Error("TheMealDB не ответил. Попробуй позже.")
        }
    }
}

sealed class TranslateState {
    object Idle : TranslateState()
    object Loading : TranslateState()
    object NoApiKey : TranslateState()
    data class Done(val recipe: AiRecipeJson, val items: List<AiIngredient>) : TranslateState()
    data class Error(val message: String) : TranslateState()
}

class WorldRecipeViewModel(application: Application, handle: SavedStateHandle) : AndroidViewModel(application) {
    private val graph = application.graph
    private val mealId: String = handle["mealId"] ?: ""

    private val _meal = MutableStateFlow<MealDetails?>(null)
    val meal: StateFlow<MealDetails?> = _meal.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _translate = MutableStateFlow<TranslateState>(TranslateState.Idle)
    val translate: StateFlow<TranslateState> = _translate.asStateFlow()
    /** Id of an already saved copy, if this meal was imported before. */
    private val _savedId = MutableStateFlow<Long?>(null)
    val savedId: StateFlow<Long?> = _savedId.asStateFlow()

    init {
        viewModelScope.launch {
            _savedId.value = graph.recipes.findByExternal("TheMealDB:$mealId")?.id
            try {
                _meal.value = NetworkModule.mealDbApi.lookup(mealId).meals().firstOrNull()?.toDetails()
                    ?: throw IllegalStateException()
            } catch (e: IOException) {
                _error.value = "Нет подключения к интернету"
            } catch (e: Exception) {
                _error.value = "Не удалось загрузить рецепт"
            }
        }
    }

    fun translate() {
        val m = _meal.value ?: return
        viewModelScope.launch {
            val key = graph.settings.groqApiKey.first()
            if (key.isBlank()) {
                _translate.value = TranslateState.NoApiKey
                return@launch
            }
            _translate.value = TranslateState.Loading
            _translate.value = try {
                val (recipe, items) = graph.ai.translateRecipe(m, key, RECIPE_CATEGORIES)
                TranslateState.Done(recipe, items)
            } catch (e: Exception) {
                TranslateState.Error(aiErrorMessage(e))
            }
        }
    }

    fun save(onSaved: (Long) -> Unit) {
        val m = _meal.value ?: return
        val done = _translate.value as? TranslateState.Done ?: return
        val r = done.recipe
        viewModelScope.launch {
            val id = graph.recipes.saveImported(
                name = r.name?.takeIf { it.isNotBlank() } ?: m.name,
                category = r.category?.takeIf { it in RECIPE_CATEGORIES } ?: RECIPE_CATEGORIES[2],
                servings = (r.servings ?: 4).coerceIn(1, 20),
                minutes = (r.minutes ?: 45).coerceIn(1, 1440),
                steps = r.steps.orEmpty().joinToString("\n"),
                items = done.items.map {
                    RecipeIngredient(0, 0, it.name, it.grams, it.kcal100, it.protein100, it.fat100, it.carbs100)
                },
                imageUrl = m.thumb,
                externalId = "TheMealDB:${m.id}"
            )
            _savedId.value = id
            onSaved(id)
        }
    }
}
