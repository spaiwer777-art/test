package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.AppDatabase
import com.example.calorietracker.data.DiaryEntry
import com.example.calorietracker.data.DiaryRepository
import com.example.calorietracker.data.MealType
import com.example.calorietracker.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DiaryUiState(
    val entries: List<DiaryEntry> = emptyList(),
    val dailyGoal: Double = 2000.0
) {
    val totalCalories: Double get() = entries.sumOf { it.calories }
    val totalProtein: Double get() = entries.sumOf { it.protein }
    val totalFat: Double get() = entries.sumOf { it.fat }
    val totalCarbs: Double get() = entries.sumOf { it.carbs }
    val remaining: Double get() = dailyGoal - totalCalories
}

class DiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val diaryRepo = DiaryRepository(AppDatabase.get(application))
    private val settingsRepo = SettingsRepository(application)

    private val selectedEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())

    val uiState: StateFlow<DiaryUiState> = selectedEpochDay
        .flatMapLatest { day ->
            combine(diaryRepo.entriesForDay(day), settingsRepo.dailyGoal) { entries, goal ->
                DiaryUiState(entries = entries, dailyGoal = goal)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiaryUiState())

    // Add screens pop themselves right after calling these, which clears this
    // ViewModel; NonCancellable keeps the insert from being cancelled with it.
    fun addEntry(foodName: String, grams: Double, calsPer100: Double, proteinPer100: Double, fatPer100: Double, carbsPer100: Double, meal: MealType) {
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
                    epochDay = selectedEpochDay.value
                )
            )
        }
    }

    fun addPrecomputedEntry(foodName: String, calories: Double, protein: Double, fat: Double, carbs: Double, meal: MealType) {
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
                    epochDay = selectedEpochDay.value
                )
            )
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { diaryRepo.deleteEntry(id) }
    }
}
