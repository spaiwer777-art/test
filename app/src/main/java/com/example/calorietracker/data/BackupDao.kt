package com.example.calorietracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/** Bulk reads and a replace-all write of the user's own data, for backups. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM foods WHERE source NOT IN ('BUILTIN', 'USDA')") suspend fun userFoods(): List<Food>
    @Query("SELECT * FROM diary_entries") suspend fun diary(): List<DiaryEntry>
    @Query("SELECT * FROM recipes WHERE isBuiltin = 0") suspend fun userRecipes(): List<Recipe>
    @Query("SELECT * FROM recipe_ingredients WHERE recipeId IN (SELECT id FROM recipes WHERE isBuiltin = 0)") suspend fun userIngredients(): List<RecipeIngredient>
    @Query("SELECT * FROM diets WHERE isBuiltin = 0") suspend fun userDiets(): List<Diet>
    @Query("SELECT * FROM weight_entries") suspend fun weights(): List<WeightEntry>
    @Query("SELECT * FROM water_entries") suspend fun water(): List<WaterEntry>
    @Query("SELECT * FROM meal_plans") suspend fun plans(): List<MealPlanEntity>
    @Query("SELECT * FROM meal_photos") suspend fun photos(): List<MealPhoto>
    @Query("SELECT id, name FROM foods WHERE source IN ('BUILTIN', 'USDA')") suspend fun refFoods(): List<IdName>
    @Query("SELECT id, name FROM recipes WHERE isBuiltin = 1") suspend fun refRecipes(): List<IdName>

    @Query("DELETE FROM foods WHERE source NOT IN ('BUILTIN', 'USDA')") suspend fun clearFoods()
    @Query("DELETE FROM diary_entries") suspend fun clearDiary()
    @Query("DELETE FROM recipe_ingredients WHERE recipeId IN (SELECT id FROM recipes WHERE isBuiltin = 0)") suspend fun clearIngredients()
    @Query("DELETE FROM recipes WHERE isBuiltin = 0") suspend fun clearRecipes()
    @Query("DELETE FROM diets WHERE isBuiltin = 0") suspend fun clearDiets()
    @Query("DELETE FROM weight_entries") suspend fun clearWeights()
    @Query("DELETE FROM water_entries") suspend fun clearWater()
    @Query("DELETE FROM meal_plans") suspend fun clearPlans()
    @Query("DELETE FROM meal_photos") suspend fun clearPhotos()

    @Insert suspend fun insertFood(item: Food): Long
    @Insert suspend fun insertDiary(items: List<DiaryEntry>)
    @Insert suspend fun insertRecipe(item: Recipe): Long
    @Insert suspend fun insertIngredients(items: List<RecipeIngredient>)
    @Insert suspend fun insertDiet(item: Diet): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWeights(items: List<WeightEntry>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWater(items: List<WaterEntry>)
    @Insert suspend fun insertPlans(items: List<MealPlanEntity>)
    @Insert suspend fun insertPhotos(items: List<MealPhoto>)

    suspend fun clearUserData() {
        clearDiary(); clearIngredients(); clearRecipes(); clearFoods(); clearDiets()
        clearWeights(); clearWater(); clearPlans(); clearPhotos()
    }
}
