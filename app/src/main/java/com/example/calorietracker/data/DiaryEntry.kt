package com.example.calorietracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK
}

/**
 * One logged serving of food in the user's diary. Nutrition values are
 * snapshotted at the moment of adding (grams * per-100g values), so later
 * edits to a Food don't retroactively change past diary days.
 */
@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodName: String,
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val mealType: MealType,
    /** Day this entry belongs to, as an epoch day number (days since 1970-01-01). */
    val epochDay: Long,
    /** Source product or recipe, if any, so the entry can open its card. */
    val foodId: Long? = null,
    val recipeId: Long? = null
)
