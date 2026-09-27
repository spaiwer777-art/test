package com.example.calorietracker.data

import com.example.calorietracker.data.json.BuiltinFood
import com.example.calorietracker.data.json.BuiltinRecipe
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LogicTest {
    private val assets = File("src/main/assets")
    private val foods: List<BuiltinFood> = Gson().fromJson(
        File(assets, "foods_ru.json").readText(), object : TypeToken<List<BuiltinFood>>() {}.type
    )
    private val base = foods.mapIndexed { i, f ->
        Food(i.toLong(), f.name, f.kcal, f.protein, f.fat, f.carbs, source = FoodSource.BUILTIN)
    }

    private fun match(name: String) = FoodMatcher.match(name, base)?.name

    @Test fun recipesOnlyUseKnownIngredients() {
        val recipes: List<BuiltinRecipe> = Gson().fromJson(
            File(assets, "recipes_ru.json").readText(), object : TypeToken<List<BuiltinRecipe>>() {}.type
        )
        val names = foods.map { it.name }.toSet()
        recipes.flatMap { it.ingredients }.forEach { assertTrue("unknown ingredient ${it.name}", it.name in names) }
    }

    @Test fun builtinMacrosAreConsistentWithCalories() {
        // Energy from macros (4/9/4) should be close to the stated calories; fibre and
        // alcohol explain small gaps, so only flag gross typos.
        foods.filter { it.kcal > 30 && !it.name.contains("Пиво") && !it.name.contains("Вино") }.forEach {
            val fromMacros = it.protein * 4 + it.fat * 9 + it.carbs * 4
            assertTrue("${it.name}: ${it.kcal} vs $fromMacros", kotlin.math.abs(fromMacros - it.kcal) / it.kcal < 0.15)
        }
    }

    @Test fun matcherFindsCommonIngredients() {
        assertEquals("Яйцо куриное", match("яйцо"))
        assertEquals("Масло сливочное 82,5%", match("масло сливочное"))
        assertEquals("Гречка варёная", match("гречка (варёная)"))
        assertEquals("Гречка (сухая)", match("гречка сухая"))
        assertEquals("Майонез 67%", match("майонез"))
        assertEquals("Огурец", match("огурцы свежие"))
        assertEquals("Куриная грудка отварная", match("куриная грудка (варёная)"))
        assertEquals("Сметана 15%", match("сметана"))
    }

    @Test fun matcherRefusesAmbiguousOrUnknown() {
        assertNull("state unknown for buckwheat", match("гречка"))
        assertNull(match("устрицы"))
        assertNull(match(""))
    }

    @Test fun calculators() {
        val p = Profile(Sex.MALE, age = 30, heightCm = 180.0, weightKg = 80.0, activity = ActivityLevel.MODERATE)
        assertEquals(1780.0, Calc.bmr(p), 0.01) // 800 + 1125 - 150 + 5
        assertEquals(1780.0 * 1.55, Calc.tdee(p), 0.01)
        assertEquals(24.69, Calc.bmi(80.0, 180.0), 0.01)
        assertEquals(Calc.BmiClass.NORMAL, Calc.bmiClass(24.69))
        assertEquals(2700, Calc.waterMl(80.0, ActivityLevel.MODERATE))
        val fat = Calc.bodyFatNavy(Sex.MALE, 180.0, neckCm = 38.0, waistCm = 85.0, hipCm = 0.0)
        assertNotNull(fat)
        assertEquals(16.0, fat!!, 1.5)
        assertEquals(560.0, Calc.exerciseKcal(8.4, 80.0, 50.0), 0.01)
        val m = Calc.macros(p)
        assertEquals(Calc.calorieTarget(p), m.protein * 4 + m.fat * 9 + m.carbs * 4, 1.0)
    }

    @Test fun planDayIsScaledToGoal() {
        val day = com.example.calorietracker.data.json.PlanDay(
            listOf(com.example.calorietracker.data.json.PlanMeal("LUNCH", "Суп", 300.0, 1800.0, 90.0, 60.0, 200.0))
        )
        assertEquals(2000.0, scaleDay(day, 2000.0).calories, 0.01)
    }
}
