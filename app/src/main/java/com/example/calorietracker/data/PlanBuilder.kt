package com.example.calorietracker.data

import com.example.calorietracker.data.json.MealPlanData
import com.example.calorietracker.data.json.PlanDay
import com.example.calorietracker.data.json.PlanMeal
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Offline meal plan assembled from the app's recipes: each meal slot gets a
 * recipe of a fitting category, and the portion is scaled to that slot's share
 * of the calorie goal. Works without a network or an AI key.
 */
object PlanBuilder {
    private val slotCategories = mapOf(
        MealType.BREAKFAST to listOf("Завтраки"),
        MealType.LUNCH to listOf("Супы", "Основные блюда"),
        MealType.DINNER to listOf("Основные блюда", "Салаты"),
        MealType.SNACK to listOf("Перекусы", "Салаты")
    )

    fun build(
        recipes: List<Recipe>,
        recipeGrams: Map<Long, Double>,
        calorieGoal: Double,
        days: Int,
        includeSnack: Boolean,
        seed: Long = System.currentTimeMillis(),
        diet: Diet? = null,
        ingredients: List<RecipeIngredient> = emptyList()
    ): MealPlanData {
        val random = Random(seed)
        val byRecipe = ingredients.groupBy { it.recipeId }
        // With a diet, prefer recipes whose name and ingredients don't hit its "avoid" list.
        val allowed = if (diet == null) recipes else recipes.filter { r ->
            DietRules.fits(diet, r.name, *byRecipe[r.id].orEmpty().map { it.name }.toTypedArray())
        }.ifEmpty { recipes }
        val slots = if (includeSnack) MealType.entries else MealType.entries - MealType.SNACK
        val shares = slots.associateWith { it.planShare }.let { m -> m.mapValues { it.value / m.values.sum() } }
        var previousDay = emptySet<Long>()

        val planDays = (0 until days).map {
            val usedToday = mutableSetOf<Long>()
            val meals = slots.mapNotNull { slot ->
                val categories = slotCategories.getValue(slot)
                val pool = allowed.filter { it.category in categories && it.caloriesPerServing > 0 }
                    .ifEmpty { allowed.filter { it.caloriesPerServing > 0 } }
                val fresh = pool.filter { it.id !in usedToday && it.id !in previousDay }
                val recipe = (fresh.ifEmpty { pool.filter { it.id !in usedToday } }.ifEmpty { pool })
                    .randomOrNull(random) ?: return@mapNotNull null
                usedToday += recipe.id
                val target = calorieGoal * shares.getValue(slot)
                // Portions in quarter-servings between half and two and a half servings.
                val portions = ((target / recipe.caloriesPerServing) * 4).roundToInt().coerceIn(2, 10) / 4.0
                val gramsPerServing = (recipeGrams[recipe.id] ?: 0.0) / recipe.servings
                PlanMeal(
                    meal = slot.name,
                    dish = recipe.name,
                    grams = (gramsPerServing * portions).roundToInt().toDouble(),
                    kcal = recipe.caloriesPerServing * portions,
                    protein = recipe.proteinPerServing * portions,
                    fat = recipe.fatPerServing * portions,
                    carbs = recipe.carbsPerServing * portions,
                    note = "${formatPortions(portions)} из рецепта",
                    recipeId = recipe.id
                )
            }
            previousDay = usedToday
            PlanDay(meals)
        }
        return MealPlanData(calorieGoal, "", if (diet != null) "Рецепты · ${diet.name}" else "Рецепты приложения", planDays)
    }

    private fun formatPortions(p: Double): String {
        val text = if (p % 1.0 == 0.0) p.toInt().toString() else p.toString().replace('.', ',')
        return "$text порц."
    }

    private val MealType.planShare: Double
        get() = when (this) {
            MealType.BREAKFAST -> 0.25
            MealType.LUNCH -> 0.35
            MealType.DINNER -> 0.30
            MealType.SNACK -> 0.10
        }
}
