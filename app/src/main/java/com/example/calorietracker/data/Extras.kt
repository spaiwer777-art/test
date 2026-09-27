package com.example.calorietracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Recipe with nutrition totals per serving, denormalized for fast listing. */
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val servings: Int,
    val minutes: Int,
    /** Steps separated by newlines. */
    val steps: String,
    val isBuiltin: Boolean,
    val caloriesPerServing: Double,
    val proteinPerServing: Double,
    val fatPerServing: Double,
    val carbsPerServing: Double
)

/** Ingredient line; per-100g values are copied in so a recipe never depends on the foods table. */
@Entity(tableName = "recipe_ingredients", indices = [Index("recipeId")])
data class RecipeIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val name: String,
    val grams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double
) {
    val calories get() = caloriesPer100g * grams / 100
    val protein get() = proteinPer100g * grams / 100
    val fat get() = fatPer100g * grams / 100
    val carbs get() = carbsPer100g * grams / 100
}

/** Photo attached to a meal on a given day; the file lives in the app's private storage. */
@Entity(tableName = "meal_photos", indices = [Index("epochDay")])
data class MealPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val mealType: MealType,
    val path: String,
    val createdAt: Long
)

@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey val epochDay: Long,
    val kg: Double
)

@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey val epochDay: Long,
    val ml: Int
)

/** Generated meal plan, stored as JSON (see MealPlanData). */
@Entity(tableName = "meal_plans")
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val title: String,
    val json: String
)
