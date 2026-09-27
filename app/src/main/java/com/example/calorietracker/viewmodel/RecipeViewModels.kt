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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val RECIPE_CATEGORIES = listOf("Завтраки", "Супы", "Основные блюда", "Салаты", "Перекусы")

class RecipesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = application.graph.recipes

    val query = MutableStateFlow("")
    val category = MutableStateFlow<String?>(null)

    val recipes: StateFlow<List<Recipe>> = combine(repo.all(), query, category) { all, q, c ->
        val needle = q.trim().lowercase()
        all.filter { (c == null || it.category == c) && (needle.isEmpty() || it.name.lowercase().contains(needle)) }
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

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            graph.recipes.delete(recipeId)
            onDone()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = application.graph

    val name = MutableStateFlow("")
    val category = MutableStateFlow(RECIPE_CATEGORIES[2])
    val servings = MutableStateFlow(2)
    val minutes = MutableStateFlow(30)
    val steps = MutableStateFlow("")
    val ingredients = MutableStateFlow<List<RecipeIngredient>>(emptyList())

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
        viewModelScope.launch {
            onSaved(
                graph.recipes.save(
                    name.value.trim(), category.value, servings.value, minutes.value,
                    steps.value.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n"),
                    ingredients.value
                )
            )
        }
    }
}
