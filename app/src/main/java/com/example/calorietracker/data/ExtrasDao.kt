package com.example.calorietracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY isBuiltin ASC, name ASC")
    fun all(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes")
    suspend fun allOnce(): List<Recipe>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observe(id: Long): Flow<Recipe?>

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY id")
    fun ingredients(recipeId: Long): Flow<List<RecipeIngredient>>

    @Query("SELECT recipeId, SUM(grams) AS grams FROM recipe_ingredients GROUP BY recipeId")
    suspend fun totalGrams(): List<RecipeGrams>

    @Insert
    suspend fun insert(recipe: Recipe): Long

    @Insert
    suspend fun insertIngredients(items: List<RecipeIngredient>)

    @Transaction
    suspend fun insertWithIngredients(recipe: Recipe, items: List<RecipeIngredient>): Long {
        val id = insert(recipe)
        insertIngredients(items.map { it.copy(id = 0, recipeId = id) })
        return id
    }

    @Query("SELECT * FROM recipe_ingredients")
    suspend fun allIngredients(): List<RecipeIngredient>

    @Query("SELECT * FROM recipes WHERE externalId = :externalId LIMIT 1")
    suspend fun findByExternal(externalId: String): Recipe?

    @Query("SELECT id, name FROM recipes WHERE isBuiltin = 1")
    suspend fun builtinNames(): List<IdName>

    @Query("UPDATE diary_entries SET recipeId = :newId WHERE recipeId = :oldId")
    suspend fun remapDiaryRecipe(oldId: Long, newId: Long?)

    @androidx.room.Update
    suspend fun update(recipe: Recipe)

    /** Replaces a user recipe's row and ingredient lines in one go. */
    @Transaction
    suspend fun replace(recipe: Recipe, items: List<RecipeIngredient>) {
        update(recipe)
        deleteIngredients(recipe.id)
        insertIngredients(items.map { it.copy(id = 0, recipeId = recipe.id) })
    }

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredients(recipeId: Long)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: Long)

    @Transaction
    suspend fun delete(id: Long) {
        deleteIngredients(id)
        deleteRecipe(id)
    }

    @Query("DELETE FROM recipe_ingredients WHERE recipeId IN (SELECT id FROM recipes WHERE isBuiltin = 1)")
    suspend fun deleteBuiltinIngredients()

    @Query("DELETE FROM recipes WHERE isBuiltin = 1")
    suspend fun deleteBuiltinRecipes()
}

@Dao
interface TrackingDao {
    @Query("SELECT * FROM meal_photos WHERE epochDay = :epochDay ORDER BY createdAt")
    fun photosForDay(epochDay: Long): Flow<List<MealPhoto>>

    @Insert
    suspend fun insertPhoto(photo: MealPhoto): Long

    @Query("DELETE FROM meal_photos WHERE id = :id")
    suspend fun deletePhoto(id: Long)

    @Query("SELECT * FROM weight_entries WHERE epochDay BETWEEN :fromDay AND :toDay ORDER BY epochDay")
    fun weights(fromDay: Long, toDay: Long): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries ORDER BY epochDay DESC LIMIT 1")
    fun latestWeight(): Flow<WeightEntry?>

    @Upsert
    suspend fun upsertWeight(entry: WeightEntry)

    suspend fun setWeightFor(epochDay: Long, kg: Double) = upsertWeight(WeightEntry(epochDay, kg))

    @Query("SELECT * FROM water_entries WHERE epochDay = :epochDay")
    fun water(epochDay: Long): Flow<WaterEntry?>

    @Query("SELECT * FROM water_entries WHERE epochDay BETWEEN :fromDay AND :toDay ORDER BY epochDay")
    fun waterRange(fromDay: Long, toDay: Long): Flow<List<WaterEntry>>

    @Upsert
    suspend fun upsertWater(entry: WaterEntry)

    @Query("SELECT * FROM meal_plans ORDER BY createdAt DESC LIMIT 1")
    fun latestPlan(): Flow<MealPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: MealPlanEntity): Long
}

data class RecipeGrams(val recipeId: Long, val grams: Double)

@Dao
interface DietDao {
    @Query("SELECT * FROM diets ORDER BY isBuiltin DESC, id")
    fun all(): Flow<List<Diet>>

    @Query("SELECT * FROM diets WHERE id = :id")
    fun observe(id: Long): Flow<Diet?>

    @Query("SELECT * FROM diets WHERE id = :id")
    suspend fun get(id: Long): Diet?

    @Upsert
    suspend fun upsert(diet: Diet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(diets: List<Diet>)

    @Query("DELETE FROM diets WHERE id = :id AND isBuiltin = 0")
    suspend fun delete(id: Long)
}
