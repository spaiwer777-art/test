package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.Calc
import com.example.calorietracker.data.Diet
import com.example.calorietracker.data.Profile
import com.example.calorietracker.graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Daily calories for a diet: maintenance (TDEE) adjusted by the diet's percentage, rounded to 10. */
fun dietCalories(profile: Profile, diet: Diet): Double {
    val floor = if (profile.sex == com.example.calorietracker.data.Sex.MALE) 1500.0 else 1200.0
    val kcal = Calc.tdee(profile) * (1 + diet.calorieAdjustPct / 100.0)
    return (maxOf(kcal, floor) / 10).roundToInt() * 10.0
}

class DietsViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = application.graph
    val diets: StateFlow<List<Diet>> = graph.diets.all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeId: StateFlow<Long> = graph.settings.activeDietId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
}

class DietDetailsViewModel(application: Application, handle: SavedStateHandle) : AndroidViewModel(application) {
    private val graph = application.graph
    private val dietId: Long = handle["dietId"] ?: 0L

    val diet: StateFlow<Diet?> = graph.diets.observe(dietId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val isActive: StateFlow<Boolean> = graph.settings.activeDietId.map { it == dietId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Profile with the latest logged weight, for the personal calorie number. */
    val profile: StateFlow<Profile> = combine(graph.settings.profile, graph.tracking.latestWeight()) { p, w ->
        if (w != null) p.copy(weightKg = w.kg) else p
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Profile())

    fun start(setCalories: Boolean) {
        viewModelScope.launch {
            val d = graph.diets.get(dietId) ?: return@launch
            graph.settings.setActiveDiet(d)
            if (setCalories) graph.settings.setDailyGoal(dietCalories(profile.value, d))
        }
    }

    fun stop() {
        viewModelScope.launch { graph.settings.setActiveDiet(null) }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            if (graph.settings.activeDietId.first() == dietId) graph.settings.setActiveDiet(null)
            graph.diets.delete(dietId)
            onDone()
        }
    }
}

class DietEditorViewModel(application: Application, handle: SavedStateHandle) : AndroidViewModel(application) {
    private val graph = application.graph
    /** Diet to edit (user diet) or to copy from (built-in); 0 = start from scratch. */
    private val sourceId: Long = handle["dietId"] ?: 0L
    private var editingId = 0L

    val name = MutableStateFlow("")
    val description = MutableStateFlow("")
    val protein = MutableStateFlow(25)
    val fat = MutableStateFlow(30)
    val adjust = MutableStateFlow(0)
    val meals = MutableStateFlow(4)
    val recommended = MutableStateFlow("")
    val avoid = MutableStateFlow("")

    init {
        viewModelScope.launch {
            val src = graph.diets.get(sourceId) ?: return@launch
            if (!src.isBuiltin) editingId = src.id
            name.value = if (src.isBuiltin) "${src.name} (моя)" else src.name
            description.value = src.description
            protein.value = src.proteinPct
            fat.value = src.fatPct
            adjust.value = src.calorieAdjustPct
            meals.value = src.mealsPerDay
            recommended.value = src.recommended
            avoid.value = src.avoid.split(',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")
        }
    }

    val isEditing: Boolean get() = editingId != 0L

    fun save(onSaved: (Long) -> Unit) {
        val p = protein.value
        val f = fat.value
        val c = 100 - p - f
        if (name.value.isBlank() || c < 0) return
        viewModelScope.launch {
            val diet = Diet(
                id = editingId,
                name = name.value.trim(),
                description = description.value.trim(),
                proteinPct = p, fatPct = f, carbsPct = c,
                calorieAdjustPct = adjust.value,
                mealsPerDay = meals.value,
                recommended = recommended.value.trim(),
                avoid = avoid.value.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.joinToString(","),
                isBuiltin = false
            )
            val id = graph.diets.save(diet)
            // Keep the macro split in sync if the edited diet is the active one.
            if (graph.settings.activeDietId.first() == id) graph.settings.setActiveDiet(diet.copy(id = id))
            onSaved(id)
        }
    }
}
