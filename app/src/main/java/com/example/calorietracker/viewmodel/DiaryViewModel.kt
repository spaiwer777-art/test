package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.example.calorietracker.data.Calc
import com.example.calorietracker.data.DayTotals
import com.example.calorietracker.data.MacroGrams
import com.example.calorietracker.data.MacroSplit
import kotlin.math.roundToInt
import com.example.calorietracker.data.DiaryEntry
import com.example.calorietracker.data.MealPhoto
import com.example.calorietracker.data.MealType
import com.example.calorietracker.data.PhotoStore
import com.example.calorietracker.graph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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

/** Macro goals in grams from the calorie goal and a split (the active diet's, or 20/30/50). */
data class MacroGoals(val protein: Double, val fat: Double, val carbs: Double) {
    companion object {
        fun fromCalories(goal: Double, split: MacroSplit = MacroSplit.DEFAULT, custom: MacroGrams? = null) =
            if (custom != null) MacroGoals(custom.protein, custom.fat, custom.carbs)
            else MacroGoals(
                protein = goal * split.protein / 100.0 / 4.0,
                fat = goal * split.fat / 100.0 / 9.0,
                carbs = goal * split.carbs / 100.0 / 4.0
            )
    }
}

data class DiaryUiState(
    val epochDay: Long = LocalDate.now().toEpochDay(),
    val entries: List<DiaryEntry> = emptyList(),
    val dailyGoal: Double = 2000.0,
    val split: MacroSplit = MacroSplit.DEFAULT,
    val customMacros: MacroGrams? = null,
    val dietName: String? = null,
    val streak: Int = 0,
    /** Calories per day for the week around [epochDay] (Mon..Sun), for the week strip. */
    val week: List<DayTotals> = emptyList()
) {
    val totalCalories: Double get() = entries.sumOf { it.calories }
    val totalProtein: Double get() = entries.sumOf { it.protein }
    val totalFat: Double get() = entries.sumOf { it.fat }
    val totalCarbs: Double get() = entries.sumOf { it.carbs }
    val remaining: Double get() = dailyGoal - totalCalories
    val macroGoals: MacroGoals get() = MacroGoals.fromCalories(dailyGoal, split, customMacros)

    fun caloriesFor(meal: MealType): Double = entries.filter { it.mealType == meal }.sumOf { it.calories }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = application.graph
    private val diaryRepo = graph.diary
    private val settingsRepo = graph.settings
    private val tracking = graph.tracking

    private val selectedEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())
    val selectedDay: StateFlow<Long> = selectedEpochDay.asStateFlow()

    private val activeDiet = settingsRepo.activeDietId.flatMapLatest { graph.diets.observe(it) }

    val uiState: StateFlow<DiaryUiState> = selectedEpochDay
        .flatMapLatest { day ->
            val monday = LocalDate.ofEpochDay(day).with(java.time.DayOfWeek.MONDAY).toEpochDay()
            combine(
                diaryRepo.entriesForDay(day),
                settingsRepo.dailyGoal,
                combine(settingsRepo.macroSplit, activeDiet, settingsRepo.macroGrams) { split, diet, custom -> Triple(split, diet?.name, custom) },
                diaryRepo.daysWithEntries(),
                diaryRepo.dailyTotals(monday, monday + 6)
            ) { entries, goal, (split, dietName, custom), days, week ->
                val byDay = week.associateBy { it.epochDay }
                DiaryUiState(
                    epochDay = day, entries = entries, dailyGoal = goal, split = split, customMacros = custom, dietName = dietName,
                    streak = streakOf(days, LocalDate.now().toEpochDay()),
                    week = (monday..monday + 6).map { byDay[it] ?: DayTotals(it, 0.0, 0.0, 0.0, 0.0) }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiaryUiState())

    val photos: StateFlow<List<MealPhoto>> = selectedEpochDay
        .flatMapLatest { tracking.photosForDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waterMl: StateFlow<Int> = selectedEpochDay
        .flatMapLatest { day -> tracking.water(day).map { it?.ml ?: 0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Water goal from the latest logged weight (or the profile weight). */
    val waterGoalMl: StateFlow<Int> = combine(settingsRepo.profile, tracking.latestWeight()) { profile, w ->
        Calc.waterMl(w?.kg ?: profile.weightKg, profile.activity)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    fun setWater(ml: Int) {
        val day = selectedEpochDay.value
        viewModelScope.launch { tracking.setWater(day, ml) }
    }

    /** Copies the picked/captured image into app storage and attaches it to the meal. */
    fun addPhoto(uri: Uri, meal: MealType) {
        val day = selectedEpochDay.value
        val app = getApplication<Application>()
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) { runCatching { PhotoStore.importImage(app, uri) }.getOrNull() }
                ?: return@launch
            tracking.addPhoto(MealPhoto(epochDay = day, mealType = meal, path = file.absolutePath, createdAt = System.currentTimeMillis()))
        }
    }

    fun deletePhoto(photo: MealPhoto) {
        viewModelScope.launch(Dispatchers.IO) { tracking.deletePhoto(photo) }
    }

    fun shiftDay(delta: Long) {
        selectedEpochDay.value += delta
    }

    /** Saves hand-entered macro goals; optionally makes their calories the daily goal. Null resets to auto. */
    fun setCustomMacros(value: MacroGrams?, alsoCalories: Boolean) {
        viewModelScope.launch {
            settingsRepo.setMacroGrams(value)
            if (value != null && alsoCalories) settingsRepo.setDailyGoal((value.calories / 10).roundToInt() * 10.0)
        }
    }

    fun selectDay(epochDay: Long) {
        selectedEpochDay.value = epochDay
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
        epochDay: Long,
        foodId: Long? = null
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
                    epochDay = epochDay,
                    foodId = foodId
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
        epochDay: Long,
        grams: Double = 0.0,
        recipeId: Long? = null
    ) {
        viewModelScope.launch(NonCancellable) {
            diaryRepo.addEntry(
                DiaryEntry(
                    foodName = foodName,
                    grams = grams,
                    calories = calories,
                    protein = protein,
                    fat = fat,
                    carbs = carbs,
                    mealType = meal,
                    epochDay = epochDay,
                    recipeId = recipeId
                )
            )
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { diaryRepo.deleteEntry(id) }
    }
}
