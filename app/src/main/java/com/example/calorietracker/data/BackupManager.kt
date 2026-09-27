package com.example.calorietracker.data

import android.content.Context
import androidx.room.withTransaction
import com.example.calorietracker.data.json.BackupData
import com.example.calorietracker.data.json.MealPlanData
import com.google.gson.Gson
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Collects the user's data into a [BackupData] and restores it on any device.
 *
 * Restoring gives every row a fresh id (built-in rows on the target phone may
 * already use the old ones) and rewires the links: diary -> food/recipe,
 * recipe -> ingredients, meal plan -> recipe and the active diet.
 */
class BackupManager(private val context: Context, private val db: AppDatabase, private val settings: SettingsRepository) {
    private val gson = Gson()
    private val dao get() = db.backupDao()

    suspend fun collect(): BackupData {
        val refFoods = dao.refFoods().associate { it.id to it.name }
        val refRecipes = dao.refRecipes().associate { it.id to it.name }
        val diary = dao.diary()
        val photos = dao.photos()
        return BackupData(
            version = 1,
            createdAt = System.currentTimeMillis(),
            settings = settings.snapshot(),
            foods = dao.userFoods(),
            diary = diary,
            recipes = dao.userRecipes(),
            ingredients = dao.userIngredients(),
            diets = dao.userDiets(),
            weights = dao.weights(),
            water = dao.water(),
            plans = dao.plans(),
            photos = photos.map { it.copy(path = File(it.path).name) },
            refFoodNames = diary.mapNotNull { e -> e.foodId?.let { id -> refFoods[id]?.let { id to it } } }.toMap(),
            // All built-in recipes (a couple dozen): diary entries and meal plans may point at them.
            refRecipeNames = refRecipes
        )
    }

    fun toJson(data: BackupData): String = gson.toJson(data)
    fun fromJson(json: String): BackupData = gson.fromJson(json, BackupData::class.java)

    /** Zip with backup.json and, optionally, the meal photos. */
    suspend fun writeZip(out: OutputStream, includePhotos: Boolean) {
        val data = collect()
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(toJson(if (includePhotos) data else data.copy(photos = emptyList())).toByteArray())
            zip.closeEntry()
            if (includePhotos) {
                dao.photos().forEach { photo ->
                    val file = File(photo.path)
                    if (file.exists()) {
                        zip.putNextEntry(ZipEntry("photos/${file.name}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    /** Reads a zip written by [writeZip] and replaces the user's data with it. */
    suspend fun restoreZip(input: InputStream) {
        val photoDir = File(context.filesDir, "meal_photos").apply { mkdirs() }
        var data: BackupData? = null
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                when {
                    entry.name == "backup.json" -> data = fromJson(zip.readBytes().decodeToString())
                    entry.name.startsWith("photos/") && !entry.name.contains("..") ->
                        File(photoDir, entry.name.removePrefix("photos/")).outputStream().use { zip.copyTo(it) }
                }
            }
        }
        restore(data ?: throw IllegalArgumentException("В файле нет backup.json"))
    }

    suspend fun restore(data: BackupData) {
        val photoDir = File(context.filesDir, "meal_photos")
        db.withTransaction {
            dao.clearUserData()
            val refFoodsByName = dao.refFoods().associate { it.name to it.id }
            val refRecipesByName = dao.refRecipes().associate { it.name to it.id }

            val foodIds = data.foods.associate { it.id to dao.insertFood(it.copy(id = 0)) }
            val recipeIds = data.recipes.associate { it.id to dao.insertRecipe(it.copy(id = 0)) }
            dao.insertIngredients(data.ingredients.mapNotNull { ing ->
                recipeIds[ing.recipeId]?.let { ing.copy(id = 0, recipeId = it) }
            })
            val dietIds = data.diets.associate { it.id to dao.insertDiet(it.copy(id = 0)) }

            fun food(id: Long?) = id?.let { foodIds[it] ?: data.refFoodNames[it]?.let(refFoodsByName::get) }
            fun recipe(id: Long?) = id?.let { recipeIds[it] ?: data.refRecipeNames[it]?.let(refRecipesByName::get) }

            dao.insertDiary(data.diary.map { it.copy(id = 0, foodId = food(it.foodId), recipeId = recipe(it.recipeId)) })
            dao.insertWeights(data.weights)
            dao.insertWater(data.water)
            dao.insertPlans(data.plans.map { plan ->
                val parsed = runCatching { gson.fromJson(plan.json, MealPlanData::class.java) }.getOrNull()
                val json = parsed?.let { p ->
                    gson.toJson(p.copy(days = p.days.map { d -> d.copy(meals = d.meals.map { m -> m.copy(recipeId = recipe(m.recipeId)) }) }))
                } ?: plan.json
                plan.copy(id = 0, json = json)
            })
            dao.insertPhotos(data.photos.filter { File(photoDir, it.path).exists() }
                .map { it.copy(id = 0, path = File(photoDir, it.path).absolutePath) })

            dietIds
        }.let { dietIds ->
            val restoredSettings = data.settings.toMutableMap()
            restoredSettings["active_diet"]?.toLongOrNull()?.let { old ->
                // Built-in diets keep ids 1..N everywhere; user diets were re-numbered.
                restoredSettings["active_diet"] = (dietIds[old] ?: old).toString()
            }
            settings.restore(restoredSettings)
        }
    }
}
