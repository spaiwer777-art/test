package com.example.calorietracker.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import com.example.calorietracker.data.AccentColor
import com.example.calorietracker.data.DayTotals
import com.example.calorietracker.data.DiaryEntry
import com.example.calorietracker.data.MealType
import com.example.calorietracker.data.ThemeMode
import com.example.calorietracker.ui.theme.CalorieTrackerTheme
import com.example.calorietracker.viewmodel.DiaryUiState
import com.example.calorietracker.viewmodel.StatsRange
import com.example.calorietracker.viewmodel.StatsUiState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
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
        entries = listOf(
            DiaryEntry(1, "Овсянка с бананом", 250.0, 310.0, 9.0, 6.0, 55.0, MealType.BREAKFAST, today),
            DiaryEntry(2, "Капучино", 200.0, 110.0, 6.0, 5.0, 9.0, MealType.BREAKFAST, today),
            DiaryEntry(3, "Гречка с курицей", 320.0, 520.0, 42.0, 12.0, 58.0, MealType.LUNCH, today),
            DiaryEntry(4, "Салат с тунцом", 220.0, 260.0, 24.0, 14.0, 8.0, MealType.DINNER, today),
            DiaryEntry(5, "Яблоко", 150.0, 78.0, 0.5, 0.3, 19.0, MealType.SNACK, today)
        )
    )

    private val sampleStats = StatsUiState(
        range = StatsRange.WEEK,
        dailyGoal = 2100.0,
        days = listOf(1850.0, 2250.0, 1980.0, 0.0, 2050.0, 2400.0, 1278.0).mapIndexed { i, kcal ->
            DayTotals(today - 6 + i, kcal, kcal * 0.05, kcal * 0.035, kcal * 0.12)
        }
    )

    private fun themed(dark: Boolean, accent: AccentColor = AccentColor.GREEN, content: @androidx.compose.runtime.Composable () -> Unit) {
        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig.PIXEL_6.copy(nightMode = if (dark) NightMode.NIGHT else NightMode.NOTNIGHT)
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

    private fun diary(dark: Boolean, accent: AccentColor = AccentColor.GREEN) = themed(dark, accent) {
        DiaryContent(
            state = sampleDiary, selectedDay = today,
            onShiftDay = {}, onToday = {}, onDelete = {},
            onAddFood = { _, _ -> }, onScanBarcode = { _, _ -> }, onAiQuickAdd = { _, _ -> },
            bottomBar = { BottomTabs(Routes.DIARY) {} }
        )
    }

    private fun stats(dark: Boolean) = themed(dark) {
        StatsContent(sampleStats, onRange = {}, bottomBar = { BottomTabs(Routes.STATS) {} })
    }

    @Test fun diaryLight() = diary(dark = false)
    @Test fun diaryDark() = diary(dark = true)
    @Test fun diaryVioletLight() = diary(dark = false, accent = AccentColor.VIOLET)
    @Test fun statsLight() = stats(dark = false)
    @Test fun statsDark() = stats(dark = true)
}
