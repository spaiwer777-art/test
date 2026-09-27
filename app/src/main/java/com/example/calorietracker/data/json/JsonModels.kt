package com.example.calorietracker.data.json

/*
 * Classes parsed from / written to JSON with Gson (assets, AI answers, saved
 * meal plans). Gson fills fields by name via reflection, so this package is
 * kept from R8 obfuscation in proguard-rules.pro.
 */

data class BuiltinFood(
    val name: String,
    val category: String,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val servingGrams: Double? = null,
    val servingLabel: String? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val satFat: Double? = null,
    val salt: Double? = null
)

data class BuiltinDiet(
    val id: Long,
    val name: String,
    val description: String,
    val proteinPct: Int,
    val fatPct: Int,
    val carbsPct: Int,
    val calorieAdjustPct: Int,
    val mealsPerDay: Int,
    val recommended: String,
    val avoid: String
)

data class BuiltinIngredient(val name: String, val grams: Double)

data class BuiltinRecipe(
    val name: String,
    val category: String,
    val minutes: Int,
    val servings: Int,
    val ingredients: List<BuiltinIngredient>,
    val steps: List<String>
)

/** AI answer: dish broken into ingredients with per-100g values. */
data class AiMealJson(val name: String?, val items: List<AiItemJson>?)

data class AiItemJson(
    val name: String?,
    val grams: Double?,
    val kcal100: Double?,
    val protein100: Double?,
    val fat100: Double?,
    val carbs100: Double?
)

data class MealPlanData(
    val calorieGoal: Double,
    val preferences: String,
    val generatedBy: String,
    val days: List<PlanDay>
)

data class PlanDay(val meals: List<PlanMeal>) {
    val calories get() = meals.sumOf { it.kcal }
    val protein get() = meals.sumOf { it.protein }
    val fat get() = meals.sumOf { it.fat }
    val carbs get() = meals.sumOf { it.carbs }
}

data class PlanMeal(
    /** MealType name: BREAKFAST, LUNCH, DINNER or SNACK. */
    val meal: String,
    val dish: String,
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val note: String? = null,
    val recipeId: Long? = null
)

/**
 * Everything that belongs to the user, for backup files and cloud sync.
 * Built-in reference rows are not included; links to them are stored by name
 * ([refFoodNames], [refRecipeNames]) because their ids differ between installs.
 */
data class BackupData(
    val version: Int,
    val createdAt: Long,
    val settings: Map<String, String>,
    val foods: List<com.example.calorietracker.data.Food>,
    val diary: List<com.example.calorietracker.data.DiaryEntry>,
    val recipes: List<com.example.calorietracker.data.Recipe>,
    val ingredients: List<com.example.calorietracker.data.RecipeIngredient>,
    val diets: List<com.example.calorietracker.data.Diet>,
    val weights: List<com.example.calorietracker.data.WeightEntry>,
    val water: List<com.example.calorietracker.data.WaterEntry>,
    val plans: List<com.example.calorietracker.data.MealPlanEntity>,
    /** Photo rows; `path` holds just the file name inside the backup. */
    val photos: List<com.example.calorietracker.data.MealPhoto>,
    val refFoodNames: Map<Long, String>,
    val refRecipeNames: Map<Long, String>
)
