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
    val servingGrams: Double?,
    val servingLabel: String?
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
