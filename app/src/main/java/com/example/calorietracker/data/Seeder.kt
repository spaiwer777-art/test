package com.example.calorietracker.data

import android.content.Context
import com.example.calorietracker.data.json.BuiltinFood
import com.example.calorietracker.data.json.BuiltinRecipe
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

/**
 * Loads the built-in reference foods and recipes from assets into the database.
 * Re-runs whenever SEED_VERSION is bumped, replacing only built-in rows; user
 * data and diary entries (which snapshot their values) are untouched.
 */
object Seeder {
    private const val SEED_VERSION = 1

    suspend fun run(context: Context, db: AppDatabase, settings: SettingsRepository) {
        backfillSearchNames(db)
        if (settings.seedVersion.first() >= SEED_VERSION) return

        val gson = Gson()
        val foods: List<BuiltinFood> = context.assets.open("foods_ru.json").reader().use {
            gson.fromJson(it, object : TypeToken<List<BuiltinFood>>() {}.type)
        }
        val recipes: List<BuiltinRecipe> = context.assets.open("recipes_ru.json").reader().use {
            gson.fromJson(it, object : TypeToken<List<BuiltinRecipe>>() {}.type)
        }

        db.foodDao().deleteBuiltin()
        db.foodDao().insertAll(foods.map { it.toFood() })

        db.recipeDao().deleteBuiltinIngredients()
        db.recipeDao().deleteBuiltinRecipes()
        val byName = foods.associateBy { it.name }
        recipes.forEach { r ->
            val ingredients = r.ingredients.map { ing ->
                val f = byName[ing.name]
                RecipeIngredient(
                    recipeId = 0,
                    name = ing.name,
                    grams = ing.grams,
                    caloriesPer100g = f?.kcal ?: 0.0,
                    proteinPer100g = f?.protein ?: 0.0,
                    fatPer100g = f?.fat ?: 0.0,
                    carbsPer100g = f?.carbs ?: 0.0
                )
            }
            db.recipeDao().insertWithIngredients(
                buildRecipe(r.name, r.category, r.servings, r.minutes, r.steps.joinToString("\n"), true, ingredients),
                ingredients
            )
        }
        settings.setSeedVersion(SEED_VERSION)
    }

    /** Rows migrated from v1 have an empty searchName; fill it in Kotlin (Cyrillic-aware lowercase). */
    private suspend fun backfillSearchNames(db: AppDatabase) {
        db.foodDao().missingSearchName().forEach { db.foodDao().setSearchName(it.id, it.name.lowercase()) }
    }

    private fun BuiltinFood.toFood() = Food(
        name = name,
        caloriesPer100g = kcal,
        proteinPer100g = protein,
        fatPer100g = fat,
        carbsPer100g = carbs,
        category = category,
        source = FoodSource.BUILTIN,
        servingGrams = servingGrams,
        servingLabel = servingLabel
    )
}

/** Recipe row with per-serving totals computed from its ingredients. */
fun buildRecipe(
    name: String,
    category: String,
    servings: Int,
    minutes: Int,
    steps: String,
    isBuiltin: Boolean,
    ingredients: List<RecipeIngredient>,
    id: Long = 0
): Recipe {
    val s = servings.coerceAtLeast(1)
    return Recipe(
        id = id,
        name = name,
        category = category,
        servings = s,
        minutes = minutes,
        steps = steps,
        isBuiltin = isBuiltin,
        caloriesPerServing = ingredients.sumOf { it.calories } / s,
        proteinPerServing = ingredients.sumOf { it.protein } / s,
        fatPerServing = ingredients.sumOf { it.fat } / s,
        carbsPerServing = ingredients.sumOf { it.carbs } / s
    )
}
