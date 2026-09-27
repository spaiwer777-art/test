package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.AppDatabase
import com.example.calorietracker.data.DayTotals
import com.example.calorietracker.data.DiaryRepository
import com.example.calorietracker.data.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class StatsRange(val days: Int, val label: String) {
    WEEK(7, "7 дней"),
    MONTH(30, "30 дней")
}

data class StatsUiState(
    val range: StatsRange = StatsRange.WEEK,
    /** One item per calendar day in the range, oldest first; days without entries are zeros. */
    val days: List<DayTotals> = emptyList(),
    val dailyGoal: Double = 2000.0
) {
    private val loggedDays: List<DayTotals> get() = days.filter { it.calories > 0 }
    val loggedDayCount: Int get() = loggedDays.size
    val averageCalories: Double get() = loggedDays.map { it.calories }.average().takeIf { !it.isNaN() } ?: 0.0
    val averageProtein: Double get() = loggedDays.map { it.protein }.average().takeIf { !it.isNaN() } ?: 0.0
    val daysWithinGoal: Int get() = loggedDays.count { it.calories <= dailyGoal }

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

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val diaryRepo = DiaryRepository(AppDatabase.get(application))
    private val settingsRepo = SettingsRepository(application)

    private val range = MutableStateFlow(StatsRange.WEEK)

    val uiState: StateFlow<StatsUiState> = range
        .flatMapLatest { r ->
            val today = LocalDate.now().toEpochDay()
            val from = today - r.days + 1
            combine(diaryRepo.dailyTotals(from, today), settingsRepo.dailyGoal) { totals, goal ->
                val byDay = totals.associateBy { it.epochDay }
                val filled = (from..today).map { day -> byDay[day] ?: DayTotals(day, 0.0, 0.0, 0.0, 0.0) }
                StatsUiState(range = r, days = filled, dailyGoal = goal)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun setRange(value: StatsRange) {
        range.value = value
    }
}
