package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.Calc
import com.example.calorietracker.data.Diet
import com.example.calorietracker.data.Profile
import com.example.calorietracker.graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = application.graph

    val profile = MutableStateFlow(Profile())
    val dietId = MutableStateFlow(0L)

    val diets: StateFlow<List<Diet>> = graph.diets.all()
        .map { list -> list.filter { it.isBuiltin } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Calorie goal for the current answers: the chosen diet's number, or the plain goal-based one. */
    fun calories(): Double {
        val diet = diets.value.firstOrNull { it.id == dietId.value }
        return if (diet != null) dietCalories(profile.value, diet)
        else (Calc.calorieTarget(profile.value) / 10).toInt() * 10.0
    }

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            val p = profile.value
            graph.settings.setProfile(p)
            graph.settings.setDailyGoal(calories())
            graph.settings.setCalorieAuto(true)
            graph.settings.setActiveDiet(diets.value.firstOrNull { it.id == dietId.value })
            graph.tracking.setWeight(LocalDate.now().toEpochDay(), p.weightKg)
            graph.settings.setOnboarded(true)
            onDone()
        }
    }

    fun skip(onDone: () -> Unit) {
        viewModelScope.launch {
            graph.settings.setOnboarded(true)
            onDone()
        }
    }
}
