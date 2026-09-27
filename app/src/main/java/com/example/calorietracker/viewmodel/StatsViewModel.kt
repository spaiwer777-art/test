package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.DayTotals
import com.example.calorietracker.data.FoodTotal
import com.example.calorietracker.data.MealTotal
import com.example.calorietracker.data.MealType
import com.example.calorietracker.data.WaterEntry
import com.example.calorietracker.data.WeightEntry
import com.example.calorietracker.graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class StatsRange(val days: Int, val label: String) {
    WEEK(7, "7 дней"),
    MONTH(30, "30 дней"),
    QUARTER(90, "90 дней")
}

data class StatsUiState(
    val range: StatsRange = StatsRange.WEEK,
    /** One item per calendar day in the range, oldest first; days without entries are zeros. */
    val days: List<DayTotals> = emptyList(),
    val dailyGoal: Double = 2000.0,
    val previousAverage: Double? = null,
    val mealTotals: List<MealTotal> = emptyList(),
    val topFoods: List<FoodTotal> = emptyList(),
    val weights: List<WeightEntry> = emptyList(),
    val water: List<WaterEntry> = emptyList(),
    val waterGoal: Int = 2000,
    val streak: Int = 0,
    val split: com.example.calorietracker.data.MacroSplit = com.example.calorietracker.data.MacroSplit.DEFAULT
) {
    private val loggedDays: List<DayTotals> get() = days.filter { it.calories > 0 }
    val loggedDayCount: Int get() = loggedDays.size
    val averageCalories: Double get() = loggedDays.map { it.calories }.average().takeIf { !it.isNaN() } ?: 0.0
    val averageProtein: Double get() = loggedDays.map { it.protein }.average().takeIf { !it.isNaN() } ?: 0.0
    val averageFat: Double get() = loggedDays.map { it.fat }.average().takeIf { !it.isNaN() } ?: 0.0
    val averageCarbs: Double get() = loggedDays.map { it.carbs }.average().takeIf { !it.isNaN() } ?: 0.0
    val daysWithinGoal: Int get() = loggedDays.count { it.calories <= dailyGoal * 1.05 }
    val averageWater: Double get() = water.filter { it.ml > 0 }.map { it.ml.toDouble() }.average().takeIf { !it.isNaN() } ?: 0.0

    fun mealCalories(meal: MealType): Double = mealTotals.firstOrNull { it.mealType == meal }?.calories ?: 0.0

    /** Share of energy from protein / fat / carbs over the whole range (0..1 each). */
    val macroEnergySplit: Triple<Double, Double, Double>
        get() {
            val p = days.sumOf { it.protein } * 4
            val f = days.sumOf { it.fat } * 9
            val c = days.sumOf { it.carbs } * 4
            val total = p + f + c
            return if (total <= 0) Triple(0.0, 0.0, 0.0) else Triple(p / total, f / total, c / total)
        }
}

/** Consecutive logged days ending today (or yesterday, if today isn't logged yet). */
fun streakOf(daysWithEntries: List<Long>, today: Long): Int {
    val set = daysWithEntries.toHashSet()
    var day = if (today in set) today else today - 1
    var count = 0
    while (day in set) {
        count++
        day--
    }
    return count
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = application.graph
    private val range = MutableStateFlow(StatsRange.WEEK)

    val uiState: StateFlow<StatsUiState> = range
        .flatMapLatest { r ->
            val today = LocalDate.now().toEpochDay()
            val from = today - r.days + 1
            val prevFrom = from - r.days
            val flows = listOf(
                graph.diary.dailyTotals(from, today),
                graph.settings.dailyGoal,
                graph.diary.dailyTotals(prevFrom, from - 1),
                graph.diary.mealTotals(from, today),
                graph.diary.topFoods(from, today, 5),
                graph.tracking.weights(from, today),
                graph.tracking.waterRange(from, today),
                graph.settings.profile,
                graph.diary.daysWithEntries(),
                graph.settings.macroSplit
            )
            combine(flows) { v ->
                @Suppress("UNCHECKED_CAST")
                val totals = v[0] as List<DayTotals>
                val goal = v[1] as Double
                @Suppress("UNCHECKED_CAST")
                val prev = v[2] as List<DayTotals>
                val byDay = totals.associateBy { it.epochDay }
                val weights = @Suppress("UNCHECKED_CAST") (v[5] as List<WeightEntry>)
                val profile = v[7] as com.example.calorietracker.data.Profile
                StatsUiState(
                    range = r,
                    days = (from..today).map { day -> byDay[day] ?: DayTotals(day, 0.0, 0.0, 0.0, 0.0) },
                    dailyGoal = goal,
                    previousAverage = prev.filter { it.calories > 0 }.map { it.calories }.average().takeIf { !it.isNaN() },
                    mealTotals = @Suppress("UNCHECKED_CAST") (v[3] as List<MealTotal>),
                    topFoods = @Suppress("UNCHECKED_CAST") (v[4] as List<FoodTotal>),
                    weights = weights,
                    water = @Suppress("UNCHECKED_CAST") (v[6] as List<WaterEntry>),
                    waterGoal = com.example.calorietracker.data.Calc.waterMl(weights.lastOrNull()?.kg ?: profile.weightKg, profile.activity),
                    streak = streakOf(@Suppress("UNCHECKED_CAST") (v[8] as List<Long>), today),
                    split = v[9] as com.example.calorietracker.data.MacroSplit
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun setRange(value: StatsRange) {
        range.value = value
    }

    fun logWeight(kg: Double) {
        viewModelScope.launch { graph.tracking.setWeight(LocalDate.now().toEpochDay(), kg) }
    }
}
