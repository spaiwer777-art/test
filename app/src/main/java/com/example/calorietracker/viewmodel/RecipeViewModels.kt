package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.Food
import com.example.calorietracker.data.Recipe
import com.example.calorietracker.data.RecipeIngredient
import com.example.calorietracker.graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val RECIPE_CATEGORIES = listOf("Завтраки", "Супы", "Основные блюда", "Салаты", "Перекусы")

@OptIn(ExperimentalCoroutinesApi::class)
class RecipesViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = application.graph
    private val repo = graph.recipes

    val query = MutableStateFlow("")
    val category = MutableStateFlow<String?>(null)
    val onlyDiet = MutableStateFlow(false)

    val activeDiet: StateFlow<com.example.calorietracker.data.Diet?> = graph.settings.activeDietId
        .flatMapLatest { graph.diets.observe(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recipes: StateFlow<List<Recipe>> = combine(repo.all(), query, category, onlyDiet, activeDiet) { all, q, c, only, diet ->
        val needle = q.trim().lowercase()
        val byRecipe = if (only && diet != null) repo.ingredientsOnce().groupBy { it.recipeId } else emptyMap()
        all.filter { r ->
            (c == null || r.category == c) && (needle.isEmpty() || r.name.lowercase().contains(needle)) &&
                (!only || diet == null || com.example.calorietracker.data.DietRules.fits(
                    diet, r.name, *byRecipe[r.id].orEmpty().map { it.name }.toTypedArray()
                ))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class RecipeDetailsViewModel(application: Application, handle: SavedStateHandle) : AndroidViewModel(application) {
    private val graph = application.graph
    private val recipeId: Long = handle["recipeId"] ?: 0L

    val recipe: StateFlow<Recipe?> = graph.recipes.observe(recipeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val ingredients: StateFlow<List<RecipeIngredient>> = graph.recipes.ingredients(recipeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val dailyGoal: StateFlow<Double> = graph.settings.dailyGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000.0)

    /** "Diet name: keyword" if the recipe or one of its ingredients is on the active diet's avoid list. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val dietConflict: StateFlow<String?> = combine(
        graph.settings.activeDietId.flatMapLatest { graph.diets.observe(it) }, recipe, ingredients
    ) { diet, r, items ->
        if (diet == null || r == null) null
        else (listOf(r.name) + items.map { it.name }).firstNotNullOfOrNull { com.example.calorietracker.data.DietRules.conflict(diet, it) }
            ?.let { "«${diet.name}»: $it" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            graph.recipes.delete(recipeId)
            onDone()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeEditorViewModel(application: Application, handle: SavedStateHandle) : AndroidViewModel(application) {
    private val graph = application.graph
    /** Recipe to edit (user recipe) or copy (built-in); 0 = new recipe. */
    private val sourceId: Long = handle["recipeId"] ?: 0L
    private var editingId = 0L
    val isEditing: Boolean get() = editingId != 0L

    /** Weight of the finished dish, grams; blank = sum of raw ingredients. */
    val cookedWeight = MutableStateFlow("")

    val name = MutableStateFlow("")
    val category = MutableStateFlow(RECIPE_CATEGORIES[2])
    val servings = MutableStateFlow(2)
    val minutes = MutableStateFlow(30)
    val steps = MutableStateFlow("")
    val ingredients = MutableStateFlow<List<RecipeIngredient>>(emptyList())

    init {
        if (sourceId != 0L) viewModelScope.launch {
            val r = graph.recipes.observe(sourceId).first() ?: return@launch
            if (!r.isBuiltin) editingId = r.id
            name.value = if (r.isBuiltin) "${r.name} (мой)" else r.name
            category.value = r.category
            servings.value = r.servings
            minutes.value = r.minutes
            steps.value = r.steps
            cookedWeight.value = r.cookedWeight?.let { com.example.calorietracker.ui.components.formatGrams(it) }.orEmpty()
            ingredients.value = graph.recipes.ingredients(sourceId).first()
        }
    }

    val foodQuery = MutableStateFlow("")
    val foodResults: StateFlow<List<Food>> = foodQuery
        .flatMapLatest { q -> if (q.isBlank()) flowOf(emptyList()) else graph.foods.searchFoods(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addIngredient(food: Food, grams: Double) {
        ingredients.value += RecipeIngredient(
            recipeId = 0, name = food.name, grams = grams,
            caloriesPer100g = food.caloriesPer100g, proteinPer100g = food.proteinPer100g,
            fatPer100g = food.fatPer100g, carbsPer100g = food.carbsPer100g
        )
    }

    fun setIngredientGrams(index: Int, grams: Double) {
        ingredients.value = ingredients.value.mapIndexed { i, it -> if (i == index) it.copy(grams = grams) else it }
    }

    fun removeIngredient(index: Int) {
        ingredients.value = ingredients.value.filterIndexed { i, _ -> i != index }
    }

    val canSave: Boolean get() = name.value.isNotBlank() && ingredients.value.isNotEmpty()

    fun save(onSaved: (Long) -> Unit) {
        if (!canSave) return
        val stepsText = steps.value.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
        val cooked = cookedWeight.value.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
        viewModelScope.launch {
            if (isEditing) {
                graph.recipes.update(
                    editingId, name.value.trim(), category.value, servings.value, minutes.value, stepsText, cooked, ingredients.value
                )
                onSaved(editingId)
            } else {
                onSaved(
                    graph.recipes.save(
                        name.value.trim(), category.value, servings.value, minutes.value, stepsText, ingredients.value, cooked
                    )
                )
            }
        }
    }
}
