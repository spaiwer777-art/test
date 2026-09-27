package com.example.calorietracker.data

import android.content.Context
import com.example.calorietracker.data.json.BuiltinDiet
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
    private const val SEED_VERSION = 4

    suspend fun run(context: Context, db: AppDatabase, settings: SettingsRepository) {
        backfillSearchNames(db)
        if (settings.seedVersion.first() >= SEED_VERSION) return

        val gson = Gson()
        val foods: List<BuiltinFood> = context.assets.open("foods_ru.json").reader().use {
            gson.fromJson(it, object : TypeToken<List<BuiltinFood>>() {}.type)
        }
        val usda: List<BuiltinFood> = context.assets.open("foods_usda.json").reader().use {
            gson.fromJson(it, object : TypeToken<List<BuiltinFood>>() {}.type)
        }
        val recipes: List<BuiltinRecipe> = context.assets.open("recipes_ru.json").reader().use {
            gson.fromJson(it, object : TypeToken<List<BuiltinRecipe>>() {}.type)
        }

        val diets: List<BuiltinDiet> = context.assets.open("diets_ru.json").reader().use {
            gson.fromJson(it, object : TypeToken<List<BuiltinDiet>>() {}.type)
        }

        // Built-in rows get new ids on every reseed; remember them by name so diary
        // entries that link to a built-in food or recipe can be pointed at the new row.
        val oldFoods = db.foodDao().builtinNames()
        db.foodDao().deleteBuiltin()
        db.foodDao().insertAll(foods.map { it.toFood(FoodSource.BUILTIN) } + usda.map { it.toFood(FoodSource.USDA) })
        val newFoods = db.foodDao().builtinNames().associate { it.name to it.id }
        oldFoods.forEach { db.foodDao().remapDiaryFood(it.id, newFoods[it.name]) }

        val oldRecipes = db.recipeDao().builtinNames()
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
        val newRecipes = db.recipeDao().builtinNames().associate { it.name to it.id }
        oldRecipes.forEach { db.recipeDao().remapDiaryRecipe(it.id, newRecipes[it.name]) }

        // Built-in diets keep fixed ids (1..N) so the active diet survives reseeding;
        // user diets are created later and get ids above them.
        db.dietDao().insertAll(diets.map {
            Diet(
                id = it.id, name = it.name, description = it.description,
                proteinPct = it.proteinPct, fatPct = it.fatPct, carbsPct = it.carbsPct,
                calorieAdjustPct = it.calorieAdjustPct, mealsPerDay = it.mealsPerDay,
                recommended = it.recommended, avoid = it.avoid, isBuiltin = true
            )
        })
        settings.setSeedVersion(SEED_VERSION)
    }

    /** Rows migrated from v1 have an empty searchName; fill it in Kotlin (Cyrillic-aware lowercase). */
    private suspend fun backfillSearchNames(db: AppDatabase) {
        db.foodDao().missingSearchName().forEach { db.foodDao().setSearchName(it.id, it.name.lowercase()) }
    }

    private fun BuiltinFood.toFood(source: FoodSource) = Food(
        name = name,
        caloriesPer100g = kcal,
        proteinPer100g = protein,
        fatPer100g = fat,
        carbsPer100g = carbs,
        category = category,
        source = source,
        fiberPer100g = fiber,
        sugarPer100g = sugar,
        saturatedFatPer100g = satFat,
        saltPer100g = salt,
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
