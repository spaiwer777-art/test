package com.example.calorietracker.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import com.example.calorietracker.data.AccentColor
import com.example.calorietracker.data.Diet
import com.example.calorietracker.data.ActivityLevel
import com.example.calorietracker.data.Food
import com.example.calorietracker.data.FoodSource
import com.example.calorietracker.data.FoodTotal
import com.example.calorietracker.data.MealTotal
import com.example.calorietracker.data.Profile
import com.example.calorietracker.data.Recipe
import com.example.calorietracker.data.RecipeIngredient
import com.example.calorietracker.data.Sex
import com.example.calorietracker.data.WaterEntry
import com.example.calorietracker.data.WeightEntry
import com.example.calorietracker.data.WeightGoal
import com.example.calorietracker.data.json.MealPlanData
import com.example.calorietracker.data.json.PlanDay
import com.example.calorietracker.data.json.PlanMeal
import com.example.calorietracker.viewmodel.PlanGenState
import com.example.calorietracker.data.DayTotals
import com.example.calorietracker.data.DiaryEntry
import com.example.calorietracker.data.MealType
import com.example.calorietracker.data.ThemeMode
import com.example.calorietracker.ui.theme.CalorieTrackerTheme
import com.example.calorietracker.viewmodel.DiaryUiState
import com.example.calorietracker.viewmodel.StatsRange
import com.example.calorietracker.viewmodel.StatsUiState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalInspectionMode
import com.example.calorietracker.viewmodel.AccountStatus
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Renders the main screens with sample data so the UI can be reviewed without a device. */
class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, maxPercentDifference = 0.1)

    private val today = LocalDate.now().toEpochDay()

    private val sampleDiary = DiaryUiState(
        epochDay = today,
        dailyGoal = 2100.0,
        dietName = "Средиземноморская",
        streak = 12,
        week = LocalDate.ofEpochDay(today).with(java.time.DayOfWeek.MONDAY).toEpochDay().let { mon ->
            listOf(1980.0, 2240.0, 1850.0, 2050.0, 1278.0, 0.0, 0.0).mapIndexed { i, k -> DayTotals(mon + i, if (mon + i <= today) k else 0.0, 0.0, 0.0, 0.0) }
        },
        entries = listOf(
            DiaryEntry(1, "Овсянка с бананом", 250.0, 310.0, 9.0, 6.0, 55.0, MealType.BREAKFAST, today),
            DiaryEntry(2, "Капучино", 200.0, 110.0, 6.0, 5.0, 9.0, MealType.BREAKFAST, today),
            DiaryEntry(3, "Гречка с курицей", 320.0, 520.0, 42.0, 12.0, 58.0, MealType.LUNCH, today),
            DiaryEntry(4, "Салат с тунцом", 220.0, 260.0, 24.0, 14.0, 8.0, MealType.DINNER, today),
            DiaryEntry(5, "Яблоко", 150.0, 78.0, 0.5, 0.3, 19.0, MealType.SNACK, today)
        )
    )

    private val monthKcal = listOf(
        2050, 1890, 2210, 0, 1980, 2340, 1760, 2120, 1950, 2080, 2290, 1840, 0, 2010, 1930,
        2150, 2380, 1870, 1990, 2060, 2240, 1780, 2020, 1960, 2110, 2300, 1850, 1970, 2090, 1278
    )
    private val sampleStats = StatsUiState(
        range = StatsRange.MONTH,
        dailyGoal = 2100.0,
        previousAverage = 2180.0,
        days = monthKcal.mapIndexed { i, kcal ->
            DayTotals(today - 29 + i, kcal.toDouble(), kcal * 0.05, kcal * 0.035, kcal * 0.12)
        },
        mealTotals = listOf(
            MealTotal(MealType.BREAKFAST, 14000.0), MealTotal(MealType.LUNCH, 21000.0),
            MealTotal(MealType.DINNER, 17500.0), MealTotal(MealType.SNACK, 5600.0)
        ),
        topFoods = listOf(
            FoodTotal("Гречка с курицей", 6240.0, 12), FoodTotal("Овсянка с бананом", 4960.0, 16),
            FoodTotal("Капучино", 2750.0, 25), FoodTotal("Сырники", 2100.0, 4), FoodTotal("Яблоко", 1400.0, 18)
        ),
        weights = listOf(82.4, 82.1, 81.9, 81.6, 81.8, 81.3, 81.0, 80.8).mapIndexed { i, kg ->
            WeightEntry(today - 28 + i * 4L, kg)
        },
        water = (0..29).map { WaterEntry(today - it, 1500 + (it % 5) * 250) },
        waterGoal = 2450,
        streak = 12
    )

    private val sampleDiets = listOf(
        Diet(1, "Сбалансированная", "Классическое питание по рекомендациям ВОЗ: всего понемногу, упор на овощи, цельные крупы и белок.", 20, 30, 50, 0, 4, "Овощи и фрукты — не меньше 400 г в день", "", true),
        Diet(2, "Мягкое похудение", "Дефицит 15% от нормы, больше белка и клетчатки, чтобы не чувствовать голод. Без жёстких запретов.", 30, 30, 40, -15, 4, "Половина тарелки — овощи\nБелок в каждом приёме пищи\nПей 30 мл воды на кг веса", "сахар=,кола,лимонад,торт,пирожн,чипс", true),
        Diet(5, "Кето", "Очень мало углеводов (~5%), много жиров.", 20, 75, 5, -10, 3, "", "хлеб,рис", true),
        Diet(6, "Средиземноморская", "Оливковое масло, рыба, овощи, бобовые и цельные злаки.", 18, 35, 47, 0, 4, "", "", true),
        Diet(12, "Моя сушка", "Белок побольше, углеводы поменьше.", 35, 30, 35, -20, 5, "", "", false)
    )

    private val mayo = Food(
        id = 1, name = "Майонез «Провансаль» 67%", caloriesPer100g = 620.0, proteinPer100g = 0.5, fatPer100g = 67.0,
        carbsPer100g = 2.5, barcode = "4600528347265", brand = "Ряба", source = FoodSource.BARCODE,
        saturatedFatPer100g = 6.0, sugarPer100g = 1.8, saltPer100g = 1.0, servingGrams = 25.0,
        servingLabel = "1 ст.л.", nutriScore = "e"
    )

    private val borsch = Recipe(
        1, "Борщ", "Супы", 8, 120,
        "Свари бульон из говядины (1,5 часа), мясо нарежь.\nОбжарь лук, морковь и свёклу, добавь томатную пасту.\nПоложи картофель и капусту, затем зажарку и мясо.",
        isBuiltin = true, caloriesPerServing = 170.0, proteinPerServing = 12.4, fatPerServing = 7.9, carbsPerServing = 12.6
    )
    private val borschIngredients = listOf(
        "Говядина (сырая)" to 400.0, "Свёкла (сырая)" to 300.0, "Капуста белокочанная" to 300.0,
        "Картофель (сырой)" to 300.0, "Морковь" to 100.0, "Лук репчатый" to 100.0
    ).map { (n, g) -> RecipeIngredient(0, 1, n, g, 50.0, 2.0, 1.0, 8.0) }

    private val recipes = listOf(
        borsch,
        Recipe(2, "Овсянка с бананом и мёдом", "Завтраки", 1, 10, "", true, 407.0, 13.5, 8.6, 70.9),
        Recipe(3, "Гречка по-купечески с курицей", "Основные блюда", 4, 40, "", true, 340.0, 30.6, 8.9, 34.8),
        Recipe(4, "Греческий салат", "Салаты", 3, 15, "", true, 198.0, 7.2, 16.3, 7.4),
        Recipe(5, "Мой протеиновый завтрак", "Завтраки", 1, 5, "", false, 320.0, 32.0, 9.0, 27.0)
    )

    private val plan = MealPlanData(
        2100.0, "", "ИИ",
        listOf(
            PlanDay(
                listOf(
                    PlanMeal("BREAKFAST", "Сырники со сметаной", 220.0, 520.0, 30.0, 22.0, 48.0, "творог 5% 180 г, мука 20 г, яйцо, сметана 15% 30 г"),
                    PlanMeal("LUNCH", "Борщ с говядиной и хлебом", 400.0, 610.0, 32.0, 21.0, 70.0, "борщ 350 г, хлеб бородинский 50 г"),
                    PlanMeal("DINNER", "Лосось с гречкой и огурцом", 380.0, 700.0, 42.0, 30.0, 60.0, "лосось 150 г, гречка 180 г, огурец 100 г"),
                    PlanMeal("SNACK", "Кефир и яблоко", 380.0, 260.0, 7.0, 3.0, 45.0, "кефир 1% 200 мл, яблоко 180 г")
                )
            )
        )
    )

    @Test fun dietsList() = themed(dark = false) {
        DietsContent(sampleDiets, activeId = 6, onOpen = {}, onCreate = {}, onBack = {})
    }

    @Test fun dietDetails() = themed(dark = false, tall = true) {
        DietDetailsContent(sampleDiets[1], active = false, profile = Profile(Sex.FEMALE, 29, 168.0, 70.0, ActivityLevel.LIGHT), onStart = {}, onStop = {}, onEdit = {}, onDelete = null, onBack = {})
    }

    @Test fun onboardingAuth() = themed(dark = false) {
        AuthContent(true, true, AccountStatus.Idle, null, { _, _ -> }, { _, _ -> }, {}, {}, {}, {})
    }

    @Test fun onboardingAuthConfirmDark() = themed(dark = true) {
        AuthContent(
            true, false, AccountStatus.Done("Аккаунт создан. Подтверди email по ссылке из письма и войди."),
            null, { _, _ -> }, { _, _ -> }, {}, {}, {}, {}, initialLogin = true
        )
    }

    @Test fun onboardingSignedIn() = themed(dark = false) {
        AuthContent(true, true, AccountStatus.Idle, SignedIn("anna@mail.ru", true), { _, _ -> }, { _, _ -> }, {}, {}, {}, {})
    }

    @Test fun searchTools() = themed(dark = false) {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(16.dp)) {
            SearchTools(com.example.calorietracker.viewmodel.SearchFilter.OFF, {}, {}, {}, {})
        }
    }

    @Test fun searchToolsLargeFont() = themed(dark = true, fontScale = 1.3f) {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(16.dp)) {
            SearchTools(com.example.calorietracker.viewmodel.SearchFilter.RU, {}, {}, {}, {})
        }
    }

    @Test fun appIconSettings() = themed(dark = false) {
        androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(16.dp)) {
            AppIconContent(
                com.example.calorietracker.AppIcon.CLASSIC, {}, "Мой дневник", {}, null, {}, {}, true, {}, null
            )
        }
    }

    @Test fun profile() = themed(dark = false, tall = true) {
        ProfileContent(
            com.example.calorietracker.viewmodel.ProfileUiState(
                profile = Profile(weightKg = 82.0), dailyGoal = 2350.0, calorieAuto = true, suggestedCalories = 2350.0,
                ownWaterMl = 2500, suggestedWaterMl = 2450
            ),
            {}, {}, {}, {}, {}
        )
    }

    @Test fun profileDark() = themed(dark = true) {
        ProfileContent(
            com.example.calorietracker.viewmodel.ProfileUiState(profile = Profile(), dailyGoal = 2100.0, suggestedCalories = 2280.0),
            {}, {}, {}, {}, {}
        )
    }

    @Test fun onboardingAbout() = themed(dark = false) {
        OnboardingContent(Profile(), {}, sampleDiets, 0, {}, 2100.0, {}, {}, {})
    }

    @Test fun onboardingGoalDark() = themed(dark = true) {
        OnboardingContent(Profile(), {}, sampleDiets, 0, {}, 2350.0, {}, {}, {}, initialStep = 1)
    }

    @Test fun worldRecipes() = themed(dark = false) {
        WorldRecipesGrid(
            com.example.calorietracker.viewmodel.WorldListState.Loaded(
                listOf("Бефстроганов", "Блины", "Щи", "Уха", "Пельмени", "Сырники").mapIndexed { i, n ->
                    com.example.calorietracker.network.MealSummary("$i", n, null)
                }
            ),
            "a:Russian", "", {}, {}, {}, {}, header = {}
        )
    }

    @Test fun foodDetails() = themed(dark = false, tall = true) {
        FoodDetailsContent(mayo, 2100.0, MealType.LUNCH, onAdd = { _, _ -> }, onDelete = {}, onBack = {})
    }

    @Test fun recipesList() = themed(dark = false) {
        RecipesContent(recipes, "", null, {}, {}, {}, {}, bottomBar = { BottomTabs(Routes.RECIPES) {} })
    }

    @Test fun recipeDetailsDark() = themed(dark = true, tall = true) {
        RecipeDetailsContent(borsch, borschIngredients, 2100.0, MealType.LUNCH, onAdd = { _, _ -> }, onDelete = null, onBack = {})
    }

    @Test fun planScreen() = themed(dark = false, tall = true) {
        PlanContent(plan, PlanGenState.Idle, 2100.0, "Средиземноморская", { _, _, _ -> }, { _, _ -> }, {}, {}, {}, {})
    }

    @Test fun calculatorCalories() = themed(dark = false, tall = true) {
        CalculatorContent(CalcType.CALORIES, Profile(Sex.MALE, 32, 180.0, 82.0, ActivityLevel.MODERATE, WeightGoal.LOSE), {}, {}, {}, {})
    }

    @Test fun calculatorBmiDark() = themed(dark = true, tall = true) {
        CalculatorContent(CalcType.BMI, Profile(Sex.FEMALE, 28, 165.0, 68.0), {}, {}, {}, {})
    }

    /** [tall] renders a long screen so everything below the fold can be reviewed too. */
    private fun themed(
        dark: Boolean,
        accent: AccentColor = AccentColor.GREEN,
        tall: Boolean = false,
        fontScale: Float = 1f,
        content: @androidx.compose.runtime.Composable () -> Unit
    ) {
        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig.PIXEL_6.copy(
                nightMode = if (dark) NightMode.NIGHT else NightMode.NOTNIGHT,
                screenHeight = if (tall) 6000 else DeviceConfig.PIXEL_6.screenHeight,
                fontScale = fontScale
            )
        )
        paparazzi.snapshot {
            // One frame only: tell animated components to render their final state.
            CompositionLocalProvider(LocalInspectionMode provides true) {
            CalorieTrackerTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, accent = accent) {
                androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    content()
                }
            }
            }
        }
    }

    private fun diary(dark: Boolean, accent: AccentColor = AccentColor.GREEN, tall: Boolean = false) = themed(dark, accent, tall) {
        DiaryContent(
            state = sampleDiary, selectedDay = today, photos = emptyList(), waterMl = 1250, waterGoalMl = 2450,
            onShiftDay = {}, onToday = {}, onSelectDay = {}, onDelete = {}, onSetWater = {}, onRequestPhoto = { _, _ -> }, onDeletePhoto = {},
            onAddFood = { _, _ -> }, onScanBarcode = { _, _ -> }, onAiQuickAdd = { _, _ -> },
            onOpenFood = { _, _, _ -> }, onOpenRecipe = { _, _, _ -> },
            bottomBar = { BottomTabs(Routes.DIARY) {} }
        )
    }

    private fun stats(dark: Boolean) = themed(dark, tall = true) {
        StatsContent(sampleStats, onRange = {}, onLogWeight = {}, bottomBar = { BottomTabs(Routes.STATS) {} })
    }

    @Test fun diaryLight() = diary(dark = false)
    @Test fun diaryDark() = diary(dark = true)
    @Test fun diaryFull() = diary(dark = false, tall = true)
    @Test fun diaryVioletLight() = diary(dark = false, accent = AccentColor.VIOLET)
    @Test fun statsLight() = stats(dark = false)
    @Test fun statsDark() = stats(dark = true)
}

