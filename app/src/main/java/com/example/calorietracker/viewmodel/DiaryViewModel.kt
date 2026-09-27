package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.AppDatabase
import com.example.calorietracker.data.DiaryEntry
import com.example.calorietracker.data.DiaryRepository
import com.example.calorietracker.data.MealType
import com.example.calorietracker.data.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Share of the daily goal suggested for each meal, like YAZIO's per-meal targets. */
val MealType.goalShare: Double
    get() = when (this) {
        MealType.BREAKFAST -> 0.25
        MealType.LUNCH -> 0.35
        MealType.DINNER -> 0.30
        MealType.SNACK -> 0.10
    }

/** Macro goals derived from the calorie goal: 20% protein, 30% fat, 50% carbs by energy. */
data class MacroGoals(val protein: Double, val fat: Double, val carbs: Double) {
    companion object {
        fun fromCalories(goal: Double) = MacroGoals(
            protein = goal * 0.20 / 4.0,
            fat = goal * 0.30 / 9.0,
            carbs = goal * 0.50 / 4.0
        )
    }
}

data class DiaryUiState(
    val epochDay: Long = LocalDate.now().toEpochDay(),
    val entries: List<DiaryEntry> = emptyList(),
    val dailyGoal: Double = 2000.0
) {
    val totalCalories: Double get() = entries.sumOf { it.calories }
    val totalProtein: Double get() = entries.sumOf { it.protein }
    val totalFat: Double get() = entries.sumOf { it.fat }
    val totalCarbs: Double get() = entries.sumOf { it.carbs }
    val remaining: Double get() = dailyGoal - totalCalories
    val macroGoals: MacroGoals get() = MacroGoals.fromCalories(dailyGoal)

    fun caloriesFor(meal: MealType): Double = entries.filter { it.mealType == meal }.sumOf { it.calories }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val diaryRepo = DiaryRepository(AppDatabase.get(application))
    private val settingsRepo = SettingsRepository(application)

    private val selectedEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())
    val selectedDay: StateFlow<Long> = selectedEpochDay.asStateFlow()

    val uiState: StateFlow<DiaryUiState> = selectedEpochDay
        .flatMapLatest { day ->
            combine(diaryRepo.entriesForDay(day), settingsRepo.dailyGoal) { entries, goal ->
                DiaryUiState(epochDay = day, entries = entries, dailyGoal = goal)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiaryUiState())

    fun shiftDay(delta: Long) {
        selectedEpochDay.value += delta
    }

    fun goToToday() {
        selectedEpochDay.value = LocalDate.now().toEpochDay()
    }

    // Add screens pop themselves right after calling these, which clears this
    // ViewModel; NonCancellable keeps the insert from being cancelled with it.
    fun addEntry(
        foodName: String,
        grams: Double,
        calsPer100: Double,
        proteinPer100: Double,
        fatPer100: Double,
        carbsPer100: Double,
        meal: MealType,
        epochDay: Long
    ) {
        viewModelScope.launch(NonCancellable) {
            val factor = grams / 100.0
            diaryRepo.addEntry(
                DiaryEntry(
                    foodName = foodName,
                    grams = grams,
                    calories = calsPer100 * factor,
                    protein = proteinPer100 * factor,
                    fat = fatPer100 * factor,
                    carbs = carbsPer100 * factor,
                    mealType = meal,
                    epochDay = epochDay
                )
            )
        }
    }

    fun addPrecomputedEntry(
        foodName: String,
        calories: Double,
        protein: Double,
        fat: Double,
        carbs: Double,
        meal: MealType,
        epochDay: Long
    ) {
        viewModelScope.launch(NonCancellable) {
            diaryRepo.addEntry(
                DiaryEntry(
                    foodName = foodName,
                    grams = 0.0,
                    calories = calories,
                    protein = protein,
                    fat = fat,
                    carbs = carbs,
                    mealType = meal,
                    epochDay = epochDay
                )
            )
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { diaryRepo.deleteEntry(id) }
    }
}
