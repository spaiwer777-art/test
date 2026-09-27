package com.example.calorietracker.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Backs up from one phone and restores on another whose built-in rows have
 * different ids, checking that links are rewired and built-in rows survive.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun db() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

    private fun builtin(name: String) = Food(name = name, caloriesPer100g = 100.0, proteinPer100g = 1.0, fatPer100g = 1.0, carbsPer100g = 1.0, source = FoodSource.BUILTIN)

    @Test
    fun roundTripRemapsIds() = runBlocking {
        val settings = SettingsRepository(context)
        val source = db()
        // Source phone: user food created before the reference base (migrated user).
        val myFood = source.foodDao().insert(Food(name = "Мой батончик", caloriesPer100g = 400.0, proteinPer100g = 20.0, fatPer100g = 15.0, carbsPer100g = 45.0))
        val egg = source.foodDao().insert(builtin("Яйцо куриное"))
        val recipeId = source.recipeDao().insertWithIngredients(
            buildRecipe("Мой омлет", "Завтраки", 1, 10, "", false, listOf(RecipeIngredient(0, 0, "Яйцо куриное", 110.0, 157.0, 12.7, 11.5, 0.7))),
            listOf(RecipeIngredient(0, 0, "Яйцо куриное", 110.0, 157.0, 12.7, 11.5, 0.7))
        )
        val dietId = source.dietDao().upsert(Diet(0, "Моя", "", 30, 30, 40, 0, 3, "", "", false))
        source.diaryDao().insert(DiaryEntry(foodName = "Мой батончик", grams = 50.0, calories = 200.0, protein = 10.0, fat = 7.5, carbs = 22.5, mealType = MealType.SNACK, epochDay = 20000, foodId = myFood))
        source.diaryDao().insert(DiaryEntry(foodName = "Яйцо", grams = 55.0, calories = 86.0, protein = 7.0, fat = 6.3, carbs = 0.4, mealType = MealType.BREAKFAST, epochDay = 20000, foodId = egg))
        source.diaryDao().insert(DiaryEntry(foodName = "Мой омлет", grams = 110.0, calories = 173.0, protein = 14.0, fat = 12.7, carbs = 0.8, mealType = MealType.BREAKFAST, epochDay = 20000, recipeId = recipeId))
        source.trackingDao().setWeightFor(20000, 80.5)
        settings.setActiveDiet(source.dietDao().get(dietId))

        val zip = ByteArrayOutputStream()
        BackupManager(context, source, settings).writeZip(zip, includePhotos = true)

        // Target phone: reference base seeded first, so ids differ from the source.
        val target = db()
        repeat(3) { target.foodDao().insert(builtin("Прочее $it")) }
        val targetEgg = target.foodDao().insert(builtin("Яйцо куриное"))
        BackupManager(context, target, settings).restoreZip(ByteArrayInputStream(zip.toByteArray()))

        val entries = target.diaryDao().getForDay(20000).first()
        val bar = entries.first { it.foodName == "Мой батончик" }
        assertEquals("Мой батончик", target.foodDao().get(bar.foodId!!)!!.name)
        assertEquals(targetEgg, entries.first { it.foodName == "Яйцо" }.foodId)
        val omelet = entries.first { it.foodName == "Мой омлет" }
        assertEquals("Мой омлет", target.recipeDao().observe(omelet.recipeId!!).first()!!.name)
        assertEquals(1, target.recipeDao().ingredients(omelet.recipeId!!).first().size)
        // Reference rows are untouched and not duplicated.
        assertEquals(4, target.foodDao().builtin().size)
        assertEquals(80.5, target.trackingDao().latestWeight().first()!!.kg, 0.001)
        val restoredDiet = target.dietDao().get(settings.activeDietId.first())
        assertEquals("Моя", restoredDiet?.name)
        assertNull(target.foodDao().findByBarcode("none"))
    }
}
